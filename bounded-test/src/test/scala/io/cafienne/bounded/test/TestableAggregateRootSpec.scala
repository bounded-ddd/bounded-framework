/*
 * Copyright (C) 2016-2018 Cafienne B.V. <https://www.cafienne.io/bounded>
 */

package io.cafienne.bounded.test

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.TestKit
import akka.util.Timeout
import io.cafienne.bounded.{BuildInfo, RuntimeInfo}
import io.cafienne.bounded.aggregate.{AggregateRootId, CommandMetaData, MetaData}
import io.cafienne.bounded.test.TestableAggregateRoot.{CommandHandlingException, IllegalCommandException}
//import io.cafienne.bounded.test.DomainProtocol.InitialStateCreated
import io.cafienne.bounded.test.TestAggregateRoot.TestAggregateRootState
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class TestableAggregateRootSpec extends AsyncWordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  //Setup required supporting classes
  implicit val timeout                = Timeout(10.seconds)
  implicit val system                 = ActorSystem("TestSystem", SpecConfig.testConfig)
  implicit val logger: LoggingAdapter = Logging(system, getClass)
  implicit val buildInfo              = BuildInfo("spec", "1.0")
  implicit val runtimeInfo            = RuntimeInfo("current")

  val testAggregateRootCreator = new TestAggregateRootCreator(system)

  val currentMeta = MetaData(ZonedDateTime.parse("2018-01-01T17:43:00+01:00"), None, None, buildInfo, runtimeInfo)

  "The testable aggregate root" must {

    "create without state in given" in {
      val testAggregateRootId1 = TestAggregateRootId("1")

      val targetState = TestAggregateRootState("new")

      val ar = TestableAggregateRoot
        .given[TestAggregateRoot, TestAggregateRootState](testAggregateRootCreator, testAggregateRootId1)

      ar.currentState map { state =>
        assert(state.isEmpty)
      }
    }

    "create initial state in given" in {
      val testAggregateRootId1 = TestAggregateRootId("2")
      val targetState          = TestAggregateRootState("new")

      val ar = TestableAggregateRoot
        .given[TestAggregateRoot, TestAggregateRootState](
          testAggregateRootCreator,
          testAggregateRootId1,
          InitialStateCreated(currentMeta, testAggregateRootId1, "new")
        )

      ar.currentState map { state =>
        assert(state.isDefined, s"There is no defined state but expected $targetState")
        assert(state.get == targetState)
      }
    }

    "handle the command to OK in when" in {
      val testAggregateRootId1 = TestAggregateRootId("3")
      val commandMetaData      = CommandMetaData(currentMeta.timestamp, None)
      val updateStateCommand   = UpdateState(commandMetaData, testAggregateRootId1, "updated")
      val targetState          = TestAggregateRootState("updated")

      val ar = TestableAggregateRoot
        .given[TestAggregateRoot, TestAggregateRootState](
          testAggregateRootCreator,
          testAggregateRootId1,
          InitialStateCreated(currentMeta, testAggregateRootId1, "new")
        )
        .when(updateStateCommand)

      ar.events should contain(StateUpdated(MetaData.fromCommand(commandMetaData), testAggregateRootId1, "updated"))

      ar.currentState map { state =>
        assert(state.isDefined, s"There is no defined state but expected $targetState")
        assert(state.get == targetState)
      }
    }

    "handle the CommandHandlingException to KO in when" in {
      val testAggregateRootId1 = TestAggregateRootId("3")
      val commandMetaData      = CommandMetaData(currentMeta.timestamp, None)
      val updateStateCommand   = UpdateState(commandMetaData, testAggregateRootId1, "updated")

      an[CommandHandlingException] should be thrownBy {
        val ar = TestableAggregateRoot
          .given[TestAggregateRoot, TestAggregateRootState](
            testAggregateRootCreator,
            testAggregateRootId1,
            InitialStateCreated(currentMeta, testAggregateRootId1, "wronginitial")
          )
          .when(updateStateCommand)
      }
    }

    "handle the IllegalCommandException to KO in when" in {
      val testAggregateRootId1               = TestAggregateRootId("4")
      val testAggregateRootIdWrongForCommand = TestAggregateRootId("5")
      val commandMetaData                    = CommandMetaData(currentMeta.timestamp, None)
      val updateStateCommand                 = UpdateState(commandMetaData, testAggregateRootIdWrongForCommand, "updated")

      an[IllegalCommandException] should be thrownBy {
        val ar = TestableAggregateRoot
          .given[TestAggregateRoot, TestAggregateRootState](
            testAggregateRootCreator,
            testAggregateRootId1,
            InitialStateCreated(currentMeta, testAggregateRootId1, "new")
          )
          .when(updateStateCommand)
      }
    }

  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system, 30.seconds, verifySystemShutdown = true)
  }

}
