/*
 * Copyright (C) 2016-2018 Cafienne B.V. <https://www.cafienne.io/bounded>
 */

package io.cafienne.bounded.cargosample.domain

import akka.actor._
import io.cafienne.bounded.aggregate._
import io.cafienne.bounded.cargosample.domain.Cargo.CargoAggregateState
import io.cafienne.bounded.cargosample.domain.CargoDomainProtocol._

import scala.collection.immutable.Seq

/**
  * Aggregate root that keeps the logic of the cargo.
  * @param cargoId unique identifier for cargo.
  */
class Cargo(cargoId: AggregateRootId, locationsProvider: LocationsProvider)
  extends AggregateRootActor[CargoAggregateState] {

  override def aggregateId: AggregateRootId = cargoId

  override def handleCommand(command: DomainCommand, state: Option[CargoAggregateState]): Reply = {
    command match {
      case cmd: PlanCargo =>
        Ok(Seq(CargoPlanned(cmd.metaData, cmd.cargoId, cmd.trackingId, cmd.routeSpecification)))
      case cmd: SpecifyNewRoute =>
        Ok(Seq(NewRouteSpecified(cmd.metaData, cmd.cargoId, cmd.routeSpecification)))
      case other => Ko(new UnexpectedCommand(other))
    }
  }

  override def newState(evt: DomainEvent): AggregateState = {
    evt match {
      case evt: CargoPlanned =>
        new CargoAggregateState(evt.trackingId, evt.routeSpecification)
      case _ =>
        throw new IllegalArgumentException(s"Event $evt is not valid to create a new CargoAggregateState")
    }
  }

}

object Cargo {

  case class CargoAggregateState(trackingId: TrackingId, routeSpecification: RouteSpecification)
      extends AggregateState {
    override def update(evt: DomainEvent): AggregateState = {
      evt match {
        case CargoPlanned(meta, cargoId, trackingId, routeSpecification) =>
          CargoAggregateState(trackingId, routeSpecification)
        case NewRouteSpecified(meta, cargoId, routeSpecification) =>
          this.copy(routeSpecification = routeSpecification)
        case other => this
      }
    }
  }

  final val aggregateRootTag = "ar-cargo" // used to tag the events and read them

}

/**
  * The Aggregate Root needs dependencies. These are given via the Creator.
  * A Creator returns the props that is used to create an Aggregate Root Actor.
  * @param system as a sample dependency the actor system is passed.
  * @param locations a dependency is used inside the Aggregate Root.
  */
class CargoCreator(system: ActorSystem, locations: LocationsProvider) extends AggregateRootCreator {

  override def props(cargoId: AggregateRootId): Props = {
    system.log.debug("Returning new Props for {}", cargoId)
    Props(classOf[Cargo], cargoId, locations)
  }

}
