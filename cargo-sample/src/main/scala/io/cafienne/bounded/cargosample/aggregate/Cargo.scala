// Copyright (C) 2018 the original author or authors.
// See the LICENSE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.cafienne.bounded.cargosample.aggregate

import akka.actor._
import akka.persistence.RecoveryCompleted
import io.cafienne.bounded.akka.CommandHandling
import io.cafienne.bounded.commands.{AggregateRootCreator, AggregateRootId, AggregateRootState, CommandNotProcessedException}
import io.cafienne.bounded.cargosample.aggregate.Cargo.CargoAggregateRootState
import io.cafienne.bounded.cargosample.aggregate.CargoDomainProtocol._

import scala.collection.immutable.Seq

/**
 * Aggregate root that keeps the logic of the cargo.
 * @param cargoId unique identifier for cargo.
 */
class Cargo(cargoId: CargoId) extends CommandHandling with ActorLogging {

  override def persistenceId: String = cargoId.id.toString

  private var state: Option[CargoAggregateRootState] = None

  private def updateState(evt: CargoDomainEvent) {
    log.debug("Updating State for aggregate {} with event {}", cargoId, evt)
    evt match {
      case CargoPlanned(meta, cargoId, trackingId, routeSpecification) => state = Some(CargoAggregateRootState(trackingId, routeSpecification))
      case NewRouteSpecified(meta, cargoId, routeSpecification) => state = state.map(s => s.copy(routeSpecification = routeSpecification))
    }
  }

  override def receiveRecover: Receive = {
    case _: RecoveryCompleted => // initialize further processing when required
    case evt: CargoDomainEvent => updateState(evt)
    case other => log.error("received unknown event to recover:" + other)
  }

  commandHandler {
    case cmd: CargoDomainCommand =>
      val originalSender = sender()
      handleCommand(cmd) match {
        case Right(evt) =>
          persistAll[CargoDomainEvent](evt) { e =>
            updateState(e)
          }
          log.debug("Command handled for {} gives events {}", persistenceId, evt)
          originalSender ! Right(evt)
        case Left(exc) => sender() ! Left(CommandNotProcessedException("Could not handle command.", exc))
      }

    //case other => sender() ! Left(CommandNotProcessedException("unknown message " + other))
  }

  private def handleCommand(command: CargoDomainCommand): Either[Exception, Seq[CargoDomainEvent]] = {
    command match {
      case cmd: PlanCargo => Right(Seq(CargoPlanned(cmd.metaData, cmd.cargoId, cmd.trackingId, cmd.routeSpecification)))
      case cmd: SpecifyNewRoute => Right(Seq(NewRouteSpecified(cmd.metaData, cmd.cargoId, cmd.routeSpecification)))
      case other => Left(CommandNotProcessedException("unknown command: " + other))
    }
  }

}

object Cargo extends AggregateRootCreator {

  override def create(id: AggregateRootId): Props = props(id)

  case class CargoAggregateRootState(trackingId: TrackingId, routeSpecification: RouteSpecification) extends AggregateRootState

  def props(cargoId: AggregateRootId): Props = Props(classOf[Cargo], cargoId)

  final val aggregateRootTag = "ar-cargo" // used to tag the events and read them
}

