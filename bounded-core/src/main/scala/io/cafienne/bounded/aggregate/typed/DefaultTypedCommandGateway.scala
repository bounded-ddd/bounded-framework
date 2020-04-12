/*
 * Copyright (C) 2016-2020 Cafienne B.V. <https://www.cafienne.io/bounded>
 */

package io.cafienne.bounded.aggregate.typed

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, Scheduler, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import io.cafienne.bounded.aggregate.{CommandValidator, DomainCommand, ValidateableCommand}

import scala.concurrent.{ExecutionContext, Future}

trait TypedCommandGateway[T <: DomainCommand] {
  def sendAndAsk(command: T)(implicit validator: ValidateableCommand[T]): Future[_]
  def send(command: T)(implicit validator: ValidateableCommand[T]): Future[_]
}

class DefaultTypedCommandGateway[Cmd <: DomainCommand](
  system: ActorSystem[_],
  aggregateRootCreator: TypedAggregateRootCreator[Cmd]
)(implicit timeout: Timeout, ec: ExecutionContext, scheduler: Scheduler)
    extends TypedCommandGateway[Cmd] {

  import akka.actor.typed.scaladsl.AskPattern._

  object CommandGatewayGuardian {

    sealed trait AggregateControl
    final case class SpawnAggregate(aggregateId: String, replyTo: ActorRef[ActorRef[Cmd]]) extends AggregateControl
    final case class StopAggregate(aggregateId: String)                                    extends AggregateControl
    private case class AggregateTerminated(aggregateId: String)                            extends AggregateControl
    final case object GracefulShutdown                                                     extends AggregateControl

    val aggregates = collection.mutable.Map[String, ActorRef[Cmd]]()

    def apply(): Behavior[AggregateControl] =
      Behaviors
        .setup[AggregateControl] { context =>
          behaviors
        }

    val behaviors: Behavior[CommandGatewayGuardian.AggregateControl] = Behaviors
      .receive[AggregateControl] { (context, message) =>
        message match {
          case SpawnAggregate(aggregateId, replyTo) =>
            context.log.info("Spawning Aggregate {}!", aggregateId)
            val ref = aggregates.getOrElseUpdate(
              aggregateId, {
                val ref = context.spawn(aggregateRootCreator.behavior(aggregateId), name = aggregateId)
                context.watchWith(ref, AggregateTerminated(aggregateId))
                ref
              }
            )
            replyTo ! ref
            Behaviors.same
          case GracefulShutdown =>
            context.log.info("Initiating graceful shutdown...")
//            aggregates.foreach((aggregateId: String, aggregateRef: ActorRef[Cmd]) => {
//              //Need to know the stop message in the specific actor implmentation.
//            })
            Behaviors.stopped { () =>
              //cleanup(context.system.log)
              context.log.debug("Stopped base on message {}", message)
            }
          case AggregateTerminated(aggregateId) =>
            context.log.debug("Aggregate {} terminated", aggregateId)
            aggregates.-=(aggregateId)
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, PostStop) =>
          context.log.info("Typed Command Gateway Guardian stopped")
          Behaviors.same
        case (context, Terminated(ref)) =>
          context.log.debug("Terminated actor with ref {}", ref)
          Behaviors.same
      }
  }

  val gateway = system.systemActorOf(CommandGatewayGuardian(), "typedcommandgateway")

  override def sendAndAsk(command: Cmd)(implicit validator: ValidateableCommand[Cmd]): Future[_] = {
    Future.failed(new RuntimeException("Please use send with a ReplyBehavior in your protocol"))
  }

  override def send(command: Cmd)(implicit validator: ValidateableCommand[Cmd]): Future[_] = {
    system.log.debug("Received send {}", command)
    CommandValidator
      .validate(command)
      .flatMap { validatedCommand =>
        spawnAggregateRoot(validatedCommand.aggregateRootId).map(ar => ar.tell(validatedCommand))
      }
  }

  private def spawnAggregateRoot(aggregateRootId: String): Future[ActorRef[Cmd]] = {
    gateway.ask(CommandGatewayGuardian.SpawnAggregate(aggregateRootId, _))
  }
}
