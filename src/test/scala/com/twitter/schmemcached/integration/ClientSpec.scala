package com.twitter.schmemcached.integration

import org.specs.Specification
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.service.Service
import com.twitter.schmemcached.Client
import com.twitter.schmemcached.Server
import com.twitter.schmemcached.protocol._
import com.twitter.schmemcached.protocol.text.Memcached
import com.twitter.schmemcached.util.ChannelBufferUtils._
import com.twitter.util.RandomSocket
import org.jboss.netty.util.CharsetUtil

object ClientSpec extends Specification {
  /**
   * Note: This test needs a real Memcached server running on 11211 to work!!
   */
  "ConnectedClient" should {

    "simple client" in {
      val service = ClientBuilder()
        .hosts("localhost:11211")
        .codec(new Memcached)
        .buildService[Command, Response]()
      val client = Client(service)

      client.delete("foo")()

      "set & get" in {
        client.get("foo")() mustEqual None
        client.set("foo", "bar")()
        client.get("foo")().get.toString(CharsetUtil.UTF_8) mustEqual "bar"
      }

      "gets" in {
        client.set("foo", "bar")()
        client.set("baz", "boing")()
        val result = client.get(Seq("foo", "baz", "notthere"))()
          .map { case (key, value) => (key, value.toString(CharsetUtil.UTF_8)) }
        result mustEqual Map(
          "foo" -> "bar",
          "baz" -> "boing"
        )
      }

      "append & prepend" in {
        client.set("foo", "bar")()
        client.append("foo", "rab")()
        client.get("foo")().get.toString(CharsetUtil.UTF_8) mustEqual "barrab"
        client.prepend("foo", "rab")()
        client.get("foo")().get.toString(CharsetUtil.UTF_8) mustEqual "rabbarrab"
      }

      "incr & decr" in {
        client.set("foo", "")()
        client.incr("foo")()    mustEqual 1
        client.incr("foo", 2)() mustEqual 3
        client.decr("foo")()    mustEqual 2
      }
    }

    "partitioned client" in {
      // we already proved above that we can hit a real memcache server,
      // so we can use our own for the partitioned client test.
      var server1: Server = null
      var server2: Server = null
      val address1 = RandomSocket()
      val address2 = RandomSocket()

      doBefore {
        server1 = new Server(address1)
        server1.start()
        server2 = new Server(address2)
        server2.start()
      }

      doAfter {
        server1.stop()
        server2.stop()
      }

      val service1 = ClientBuilder()
        .name("service1")
        .hosts("localhost:" + address1.getPort)
        .codec(new Memcached)
        .buildService[Command, Response]()

      val service2 = ClientBuilder()
        .name("service2")
        .hosts("localhost:" + address2.getPort)
        .codec(new Memcached)
        .buildService[Command, Response]()

      val client = Client(Seq(service1, service2))

      "doesn't blow up" in {
        client.delete("foo")()
        client.get("foo")() mustEqual None
        client.set("foo", "bar")()
        client.get("foo")().get.toString(CharsetUtil.UTF_8) mustEqual "bar"
      }
    }
  }
}