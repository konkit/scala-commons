package com.avsystem.commons
package redis

import java.io.Closeable

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.avsystem.commons.redis.RedisBatch.{ConnectionState, MessageBuffer}
import com.avsystem.commons.redis.Scope.Connection
import com.avsystem.commons.redis.actor.RedisConnectionActor
import com.avsystem.commons.redis.protocol.RedisMsg

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * Author: ghik
  * Created: 09/06/16.
  */
final class RedisConnectionClient(address: NodeAddress = NodeAddress.Default)
  (implicit system: ActorSystem) extends Closeable {self =>

  private val connectionActor = system.actorOf(Props(new RedisConnectionActor(address)))

  import system.dispatcher

  def execute[A](batch: ConnectionBatch[A])(implicit timeout: Timeout): Future[A] = {
    val buf = new ArrayBuffer[RedisMsg]
    val replyDecoder = batch.encodeCommands(new MessageBuffer(buf), inTransaction = false)
    connectionActor.ask(RedisConnectionActor.Request(buf)).map {
      case RedisConnectionActor.Response(replies) => replyDecoder.decodeReplies(replies, 0, replies.size, new ConnectionState)
      case RedisConnectionActor.Failure(cause) => throw cause
    }
  }

  def toExecutor(implicit timeout: Timeout): RedisExecutor[Connection] =
    new RedisExecutor[Connection] {
      def execute[A](batch: RedisBatch[A, Connection]) = self.execute(batch)
    }

  def close(): Unit =
    system.stop(connectionActor)
}
