package com.avsystem.commons
package redis.config

import java.net.InetSocketAddress

import akka.io.Inet
import akka.util.Timeout
import com.avsystem.commons.redis.actor.RedisConnectionActor.{DebugListener, DevNullListener}
import com.avsystem.commons.redis.{NodeAddress, RedisBatch, RedisOp}

import scala.concurrent.duration._

/**
  * Configuration of a [[com.avsystem.commons.redis.RedisClusterClient RedisClusterClient]]
  *
  * @param nodeConfigs                 function that returns [[NodeConfig]] given the address of the node.
  * @param monitoringConnectionConfigs function that returns [[ConnectionConfig]] for a monitoring connection
  *                                    used to monitor node with given address. The cluster client keeps single
  *                                    monitoring connection for every cluster master. Monitoring connections are used
  *                                    to refresh Redis Cluster state (current masters and slot mapping).
  * @param autoRefreshInterval         interval between routine cluster state refresh operations
  * @param minRefreshInterval          minimal interval between consecutive cluster state refresh operations.
  *                                    Normally, cluster state is not refreshed more frequently than specified by
  *                                    `autoRefreshInterval` but additional refresh operations may be forced when
  *                                    cluster redirections are observed. `minRefreshInterval` prevents too many
  *                                    refresh operations from being executed in such situations.
  * @param nodesToQueryForState        function that determines how many randomly selected masters should be queried
  *                                    for cluster state during routine state refresh operation. The function takes
  *                                    current number of known masters as its argument.
  * @param maxRedirections             maximum number of consecutive redirections automatically handled by
  *                                    [[com.avsystem.commons.redis.RedisClusterClient RedisClusterClient]].
  *                                    When set to 0, redirections are not handled at all.
  * @param nodeClientCloseDelay        Delay after which [[com.avsystem.commons.redis.RedisNodeClient RedisNodeClient]]
  *                                    is closed when it's master leaves cluster state (goes down or becomes a slave).
  *                                    Note that the node client is NOT operational during that delay. Trying to
  *                                    execute commands on it will result in
  *                                    [[com.avsystem.commons.redis.exception.NodeRemovedException NodeRemovedException]]
  */
case class ClusterConfig(
  nodeConfigs: NodeAddress => NodeConfig = _ => NodeConfig(),
  monitoringConnectionConfigs: NodeAddress => ConnectionConfig = _ => ConnectionConfig(),
  autoRefreshInterval: FiniteDuration = 5.seconds,
  minRefreshInterval: FiniteDuration = 1.seconds,
  nodesToQueryForState: Int => Int = _ min 5,
  maxRedirections: Int = 3,
  nodeClientCloseDelay: FiniteDuration = 1.seconds
)

/**
  * Configuration of a [[com.avsystem.commons.redis.RedisNodeClient RedisNodeClient]], used either as a standalone
  * client or internally by [[com.avsystem.commons.redis.RedisClusterClient RedisClusterClient]].
  *
  * @param poolSize          number of connections used by node client. Commands are distributed between connections using
  *                          a round-robin scenario. Number of connections in the pool is constant and cannot be changed.
  *                          Due to single-threaded nature of Redis, the number of concurrent connections should be kept
  *                          low for best performance. The only situation where the number of connections should be increased
  *                          is when using `WATCH`-`MULTI`-`EXEC` transactions with optimistic locking.
  * @param initOp            a [[com.avsystem.commons.redis.RedisOp RedisOp]] executed by this client upon initialization.
  *                          This may be useful for things like script loading, especially when using cluster client which
  *                          may create and close node clients dynamically as reactions on cluster state changes.
  * @param initTimeout       timeout used by initialization operation (`initOp`)
  * @param connectionConfigs a function that returns [[ConnectionConfig]] for a connection given its id. Connection ID
  *                          is its index in the connection pool, i.e. an int ranging from 0 to `poolSize`-1.
  */
case class NodeConfig(
  poolSize: Int = 4,
  initOp: RedisOp[Any] = RedisOp.unit,
  initTimeout: Timeout = Timeout(10.seconds),
  connectionConfigs: Int => ConnectionConfig = _ => ConnectionConfig()
) {
  require(poolSize > 0, "Pool size must be positive")
}

/**
  * Configuration options for a single Redis connection.
  *
  * `initCommands` usage example:
  * {{{
  *   implicit val actorSystem = ActorSystem()
  *   import RedisApi.Batches.StringTyped._
  *   val nodeClient = new RedisNodeClient(
  *     config = NodeConfig(
  *       poolSize = 8,
  *       connectionConfigs = connectionId => ConnectionConfig(
  *         initCommands = auth("mypassword") *> clientSetname(s"conn_$$connectionId") *> select(1)
  *       )
  *     )
  *   )
  * }}}
  *
  * @param initCommands         commands always sent upon establishing a Redis connection (and every time it's reconnected).
  *                             The most common reason to configure `initCommands` is to specify authentication password used by every
  *                             connection (`AUTH` command), but it's also useful for commands like `CLIENT SETNAME`, `SELECT`, etc.
  *                             Note that these are all commands that can't be executed directly by
  *                             [[com.avsystem.commons.redis.RedisNodeClient RedisNodeClient]] or
  *                             [[com.avsystem.commons.redis.RedisClusterClient RedisClusterClient]].
  * @param actorName            name of the actor representing the connection
  * @param localAddress         local bind address for the connection
  * @param socketOptions        socket options for the connection
  * @param connectTimeout       timeout for establishing connection
  * @param maxWriteSizeHint     hint for maximum number of bytes sent in a single network write message (the actual number
  *                             of bytes sent may be slightly larger)
  * @param reconnectionStrategy a [[RetryStrategy]] used to determine what delay should be used when reconnecting
  *                             a failed connection. NOTE: `reconnectionStrategy` is ignored by `RedisConnectionClient`
  * @param debugListener        listener for traffic going through this connection. Only for debugging and testing
  *                             purposes
  */
case class ConnectionConfig(
  initCommands: RedisBatch[Any] = RedisBatch.unit,
  actorName: OptArg[String] = OptArg.Empty,
  localAddress: OptArg[InetSocketAddress] = OptArg.Empty,
  socketOptions: List[Inet.SocketOption] = Nil,
  connectTimeout: OptArg[FiniteDuration] = OptArg.Empty,
  maxWriteSizeHint: OptArg[Int] = 50000,
  reconnectionStrategy: RetryStrategy = ExponentialBackoff(1.seconds, 32.seconds),
  debugListener: DebugListener = DevNullListener
)

trait RetryStrategy {
  /**
    * Determines a delay that will be waited before restarting a failed Redis connection.
    * If this method returns `Opt.Empty`, the connection will not be restarted and will remain in a broken state.
    *
    * @param retry indicates which consecutive reconnection retry it is after the connection was lost, starting from 0
    */
  def retryDelay(retry: Int): Opt[FiniteDuration]
}

case class ExponentialBackoff(firstDelay: FiniteDuration, maxDelay: FiniteDuration)
  extends RetryStrategy {

  private def expDelay(retry: Int) =
    firstDelay * (1 << (retry - 1))

  private val maxRetry =
    Iterator.from(1).find(i => expDelay(i) >= maxDelay).getOrElse(Int.MaxValue)

  def retryDelay(retry: Int) = Opt {
    if (retry == 0) Duration.Zero
    else if (retry >= maxRetry) maxDelay
    else expDelay(retry)
  }
}

case object NoRetryStrategy extends RetryStrategy {
  def retryDelay(retry: Int) = Opt.Empty
}

object ConfigDefaults {
  val Dispatcher = "redis.pinned-dispatcher"
}
