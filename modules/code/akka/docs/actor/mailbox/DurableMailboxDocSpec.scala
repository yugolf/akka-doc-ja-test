/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.docs.actor.mailbox

//#imports
import akka.actor.Props

//#imports

import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.MustMatchers
import akka.testkit.AkkaSpec
import akka.actor.Actor

class MyActor extends Actor {
  def receive = {
    case x ⇒
  }
}

object DurableMailboxDocSpec {
  val config = """
    //#dispatcher-config
    my-dispatcher {
      mailbox-type = akka.actor.mailbox.FileBasedMailboxType
    }
    //#dispatcher-config
    """
}

class DurableMailboxDocSpec extends AkkaSpec(DurableMailboxDocSpec.config) {

  "configuration of dispatcher with durable mailbox" in {
    //#dispatcher-config-use
    val myActor = system.actorOf(Props[MyActor].
      withDispatcher("my-dispatcher"), name = "myactor")
    //#dispatcher-config-use
  }

}

//#custom-mailbox
import com.typesafe.config.Config
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.dispatch.Envelope
import akka.dispatch.MailboxType
import akka.dispatch.MessageQueue
import akka.actor.mailbox.DurableMessageQueue
import akka.actor.mailbox.DurableMessageSerialization

class MyMailboxType(systemSettings: ActorSystem.Settings, config: Config)
  extends MailboxType {

  override def create(owner: Option[ActorContext]): MessageQueue = owner match {
    case Some(o) ⇒ new MyMessageQueue(o)
    case None ⇒ throw new IllegalArgumentException(
      "requires an owner (i.e. does not work with BalancingDispatcher)")
  }
}

class MyMessageQueue(_owner: ActorContext)
  extends DurableMessageQueue(_owner) with DurableMessageSerialization {

  val storage = new QueueStorage

  def enqueue(receiver: ActorRef, envelope: Envelope) {
    val data: Array[Byte] = serialize(envelope)
    storage.push(data)
  }

  def dequeue(): Envelope = {
    val data: Option[Array[Byte]] = storage.pull()
    data.map(deserialize(_)).getOrElse(null)
  }

  def hasMessages: Boolean = !storage.isEmpty

  def numberOfMessages: Int = storage.size

  def cleanUp(owner: ActorContext, deadLetters: MessageQueue): Unit = ()

}
//#custom-mailbox

// dummy
class QueueStorage {
  import java.util.concurrent.ConcurrentLinkedQueue
  val queue = new ConcurrentLinkedQueue[Array[Byte]]
  def push(data: Array[Byte]): Unit = queue.offer(data)
  def pull(): Option[Array[Byte]] = Option(queue.poll())
  def isEmpty: Boolean = queue.isEmpty
  def size: Int = queue.size
}

//#custom-mailbox-test
import akka.actor.mailbox.DurableMailboxSpec

object MyMailboxSpec {
  val config = """
    MyStorage-dispatcher {
      mailbox-type = akka.docs.actor.mailbox.MyMailboxType
    }
    """
}

class MyMailboxSpec extends DurableMailboxSpec("MyStorage", MyMailboxSpec.config) {
  override def atStartup() {
  }

  override def atTermination() {
  }

  "MyMailbox" must {
    "deliver a message" in {
      val actor = createMailboxTestActor()
      implicit val sender = testActor
      actor ! "hello"
      expectMsg("hello")
    }

    // add more tests
  }
}