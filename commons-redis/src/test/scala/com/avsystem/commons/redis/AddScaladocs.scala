package com.avsystem.commons
package redis

import scala.io.Source

object AddScaladocs {
  val urls = Map(
    "APPEND" -> "http://redis.io/commands/append",
    "AUTH" -> "http://redis.io/commands/auth",
    "BGREWRITEAOF" -> "http://redis.io/commands/bgrewriteaof",
    "BGSAVE" -> "http://redis.io/commands/bgsave",
    "BITCOUNT" -> "http://redis.io/commands/bitcount",
    "BITFIELD" -> "http://redis.io/commands/bitfield",
    "BITOP" -> "http://redis.io/commands/bitop",
    "BITPOS" -> "http://redis.io/commands/bitpos",
    "BLPOP" -> "http://redis.io/commands/blpop",
    "BRPOP" -> "http://redis.io/commands/brpop",
    "BRPOPLPUSH" -> "http://redis.io/commands/brpoplpush",
    "CLIENT KILL" -> "http://redis.io/commands/client-kill",
    "CLIENT LIST" -> "http://redis.io/commands/client-list",
    "CLIENT GETNAME" -> "http://redis.io/commands/client-getname",
    "CLIENT PAUSE" -> "http://redis.io/commands/client-pause",
    "CLIENT REPLY" -> "http://redis.io/commands/client-reply",
    "CLIENT SETNAME" -> "http://redis.io/commands/client-setname",
    "CLUSTER ADDSLOTS" -> "http://redis.io/commands/cluster-addslots",
    "CLUSTER COUNT-FAILURE-REPORTS" -> "http://redis.io/commands/cluster-count-failure-reports",
    "CLUSTER COUNTKEYSINSLOT" -> "http://redis.io/commands/cluster-countkeysinslot",
    "CLUSTER DELSLOTS" -> "http://redis.io/commands/cluster-delslots",
    "CLUSTER FAILOVER" -> "http://redis.io/commands/cluster-failover",
    "CLUSTER FORGET" -> "http://redis.io/commands/cluster-forget",
    "CLUSTER GETKEYSINSLOT" -> "http://redis.io/commands/cluster-getkeysinslot",
    "CLUSTER INFO" -> "http://redis.io/commands/cluster-info",
    "CLUSTER KEYSLOT" -> "http://redis.io/commands/cluster-keyslot",
    "CLUSTER MEET" -> "http://redis.io/commands/cluster-meet",
    "CLUSTER NODES" -> "http://redis.io/commands/cluster-nodes",
    "CLUSTER REPLICATE" -> "http://redis.io/commands/cluster-replicate",
    "CLUSTER RESET" -> "http://redis.io/commands/cluster-reset",
    "CLUSTER SAVECONFIG" -> "http://redis.io/commands/cluster-saveconfig",
    "CLUSTER SET-CONFIG-EPOCH" -> "http://redis.io/commands/cluster-set-config-epoch",
    "CLUSTER SETSLOT" -> "http://redis.io/commands/cluster-setslot",
    "CLUSTER SLAVES" -> "http://redis.io/commands/cluster-slaves",
    "CLUSTER SLOTS" -> "http://redis.io/commands/cluster-slots",
    "COMMAND" -> "http://redis.io/commands/command",
    "COMMAND COUNT" -> "http://redis.io/commands/command-count",
    "COMMAND GETKEYS" -> "http://redis.io/commands/command-getkeys",
    "COMMAND INFO" -> "http://redis.io/commands/command-info",
    "CONFIG GET" -> "http://redis.io/commands/config-get",
    "CONFIG REWRITE" -> "http://redis.io/commands/config-rewrite",
    "CONFIG SET" -> "http://redis.io/commands/config-set",
    "CONFIG RESETSTAT" -> "http://redis.io/commands/config-resetstat",
    "DBSIZE" -> "http://redis.io/commands/dbsize",
    "DEBUG OBJECT" -> "http://redis.io/commands/debug-object",
    "DEBUG SEGFAULT" -> "http://redis.io/commands/debug-segfault",
    "DECR" -> "http://redis.io/commands/decr",
    "DECRBY" -> "http://redis.io/commands/decrby",
    "DEL" -> "http://redis.io/commands/del",
    "DISCARD" -> "http://redis.io/commands/discard",
    "DUMP" -> "http://redis.io/commands/dump",
    "ECHO" -> "http://redis.io/commands/echo",
    "EVAL" -> "http://redis.io/commands/eval",
    "EVALSHA" -> "http://redis.io/commands/evalsha",
    "EXEC" -> "http://redis.io/commands/exec",
    "EXISTS" -> "http://redis.io/commands/exists",
    "EXPIRE" -> "http://redis.io/commands/expire",
    "EXPIREAT" -> "http://redis.io/commands/expireat",
    "FLUSHALL" -> "http://redis.io/commands/flushall",
    "FLUSHDB" -> "http://redis.io/commands/flushdb",
    "GEOADD" -> "http://redis.io/commands/geoadd",
    "GEOHASH" -> "http://redis.io/commands/geohash",
    "GEOPOS" -> "http://redis.io/commands/geopos",
    "GEODIST" -> "http://redis.io/commands/geodist",
    "GEORADIUS" -> "http://redis.io/commands/georadius",
    "GEORADIUSBYMEMBER" -> "http://redis.io/commands/georadiusbymember",
    "GET" -> "http://redis.io/commands/get",
    "GETBIT" -> "http://redis.io/commands/getbit",
    "GETRANGE" -> "http://redis.io/commands/getrange",
    "GETSET" -> "http://redis.io/commands/getset",
    "HDEL" -> "http://redis.io/commands/hdel",
    "HEXISTS" -> "http://redis.io/commands/hexists",
    "HGET" -> "http://redis.io/commands/hget",
    "HGETALL" -> "http://redis.io/commands/hgetall",
    "HINCRBY" -> "http://redis.io/commands/hincrby",
    "HINCRBYFLOAT" -> "http://redis.io/commands/hincrbyfloat",
    "HKEYS" -> "http://redis.io/commands/hkeys",
    "HLEN" -> "http://redis.io/commands/hlen",
    "HMGET" -> "http://redis.io/commands/hmget",
    "HMSET" -> "http://redis.io/commands/hmset",
    "HSET" -> "http://redis.io/commands/hset",
    "HSETNX" -> "http://redis.io/commands/hsetnx",
    "HSTRLEN" -> "http://redis.io/commands/hstrlen",
    "HVALS" -> "http://redis.io/commands/hvals",
    "INCR" -> "http://redis.io/commands/incr",
    "INCRBY" -> "http://redis.io/commands/incrby",
    "INCRBYFLOAT" -> "http://redis.io/commands/incrbyfloat",
    "INFO" -> "http://redis.io/commands/info",
    "KEYS" -> "http://redis.io/commands/keys",
    "LASTSAVE" -> "http://redis.io/commands/lastsave",
    "LINDEX" -> "http://redis.io/commands/lindex",
    "LINSERT" -> "http://redis.io/commands/linsert",
    "LLEN" -> "http://redis.io/commands/llen",
    "LPOP" -> "http://redis.io/commands/lpop",
    "LPUSH" -> "http://redis.io/commands/lpush",
    "LPUSHX" -> "http://redis.io/commands/lpushx",
    "LRANGE" -> "http://redis.io/commands/lrange",
    "LREM" -> "http://redis.io/commands/lrem",
    "LSET" -> "http://redis.io/commands/lset",
    "LTRIM" -> "http://redis.io/commands/ltrim",
    "MGET" -> "http://redis.io/commands/mget",
    "MIGRATE" -> "http://redis.io/commands/migrate",
    "MONITOR" -> "http://redis.io/commands/monitor",
    "MOVE" -> "http://redis.io/commands/move",
    "MSET" -> "http://redis.io/commands/mset",
    "MSETNX" -> "http://redis.io/commands/msetnx",
    "MULTI" -> "http://redis.io/commands/multi",
    "OBJECT" -> "http://redis.io/commands/object",
    "PERSIST" -> "http://redis.io/commands/persist",
    "PEXPIRE" -> "http://redis.io/commands/pexpire",
    "PEXPIREAT" -> "http://redis.io/commands/pexpireat",
    "PFADD" -> "http://redis.io/commands/pfadd",
    "PFCOUNT" -> "http://redis.io/commands/pfcount",
    "PFMERGE" -> "http://redis.io/commands/pfmerge",
    "PING" -> "http://redis.io/commands/ping",
    "PSETEX" -> "http://redis.io/commands/psetex",
    "PSUBSCRIBE" -> "http://redis.io/commands/psubscribe",
    "PUBSUB" -> "http://redis.io/commands/pubsub",
    "PTTL" -> "http://redis.io/commands/pttl",
    "PUBLISH" -> "http://redis.io/commands/publish",
    "PUNSUBSCRIBE" -> "http://redis.io/commands/punsubscribe",
    "QUIT" -> "http://redis.io/commands/quit",
    "RANDOMKEY" -> "http://redis.io/commands/randomkey",
    "READONLY" -> "http://redis.io/commands/readonly",
    "READWRITE" -> "http://redis.io/commands/readwrite",
    "RENAME" -> "http://redis.io/commands/rename",
    "RENAMENX" -> "http://redis.io/commands/renamenx",
    "RESTORE" -> "http://redis.io/commands/restore",
    "ROLE" -> "http://redis.io/commands/role",
    "RPOP" -> "http://redis.io/commands/rpop",
    "RPOPLPUSH" -> "http://redis.io/commands/rpoplpush",
    "RPUSH" -> "http://redis.io/commands/rpush",
    "RPUSHX" -> "http://redis.io/commands/rpushx",
    "SADD" -> "http://redis.io/commands/sadd",
    "SAVE" -> "http://redis.io/commands/save",
    "SCARD" -> "http://redis.io/commands/scard",
    "SCRIPT DEBUG" -> "http://redis.io/commands/script-debug",
    "SCRIPT EXISTS" -> "http://redis.io/commands/script-exists",
    "SCRIPT FLUSH" -> "http://redis.io/commands/script-flush",
    "SCRIPT KILL" -> "http://redis.io/commands/script-kill",
    "SCRIPT LOAD" -> "http://redis.io/commands/script-load",
    "SDIFF" -> "http://redis.io/commands/sdiff",
    "SDIFFSTORE" -> "http://redis.io/commands/sdiffstore",
    "SELECT" -> "http://redis.io/commands/select",
    "SET" -> "http://redis.io/commands/set",
    "SETBIT" -> "http://redis.io/commands/setbit",
    "SETEX" -> "http://redis.io/commands/setex",
    "SETNX" -> "http://redis.io/commands/setnx",
    "SETRANGE" -> "http://redis.io/commands/setrange",
    "SHUTDOWN" -> "http://redis.io/commands/shutdown",
    "SINTER" -> "http://redis.io/commands/sinter",
    "SINTERSTORE" -> "http://redis.io/commands/sinterstore",
    "SISMEMBER" -> "http://redis.io/commands/sismember",
    "SLAVEOF" -> "http://redis.io/commands/slaveof",
    "SLOWLOG" -> "http://redis.io/commands/slowlog",
    "SMEMBERS" -> "http://redis.io/commands/smembers",
    "SMOVE" -> "http://redis.io/commands/smove",
    "SORT" -> "http://redis.io/commands/sort",
    "SPOP" -> "http://redis.io/commands/spop",
    "SRANDMEMBER" -> "http://redis.io/commands/srandmember",
    "SREM" -> "http://redis.io/commands/srem",
    "STRLEN" -> "http://redis.io/commands/strlen",
    "SUBSCRIBE" -> "http://redis.io/commands/subscribe",
    "SUNION" -> "http://redis.io/commands/sunion",
    "SUNIONSTORE" -> "http://redis.io/commands/sunionstore",
    "SYNC" -> "http://redis.io/commands/sync",
    "TIME" -> "http://redis.io/commands/time",
    "TOUCH" -> "http://redis.io/commands/touch",
    "TTL" -> "http://redis.io/commands/ttl",
    "TYPE" -> "http://redis.io/commands/type",
    "UNSUBSCRIBE" -> "http://redis.io/commands/unsubscribe",
    "UNWATCH" -> "http://redis.io/commands/unwatch",
    "WAIT" -> "http://redis.io/commands/wait",
    "WATCH" -> "http://redis.io/commands/watch",
    "ZADD" -> "http://redis.io/commands/zadd",
    "ZCARD" -> "http://redis.io/commands/zcard",
    "ZCOUNT" -> "http://redis.io/commands/zcount",
    "ZINCRBY" -> "http://redis.io/commands/zincrby",
    "ZINTERSTORE" -> "http://redis.io/commands/zinterstore",
    "ZLEXCOUNT" -> "http://redis.io/commands/zlexcount",
    "ZRANGE" -> "http://redis.io/commands/zrange",
    "ZRANGEBYLEX" -> "http://redis.io/commands/zrangebylex",
    "ZREVRANGEBYLEX" -> "http://redis.io/commands/zrevrangebylex",
    "ZRANGEBYSCORE" -> "http://redis.io/commands/zrangebyscore",
    "ZRANK" -> "http://redis.io/commands/zrank",
    "ZREM" -> "http://redis.io/commands/zrem",
    "ZREMRANGEBYLEX" -> "http://redis.io/commands/zremrangebylex",
    "ZREMRANGEBYRANK" -> "http://redis.io/commands/zremrangebyrank",
    "ZREMRANGEBYSCORE" -> "http://redis.io/commands/zremrangebyscore",
    "ZREVRANGE" -> "http://redis.io/commands/zrevrange",
    "ZREVRANGEBYSCORE" -> "http://redis.io/commands/zrevrangebyscore",
    "ZREVRANK" -> "http://redis.io/commands/zrevrank",
    "ZSCORE" -> "http://redis.io/commands/zscore",
    "ZUNIONSTORE" -> "http://redis.io/commands/zunionstore",
    "SCAN" -> "http://redis.io/commands/scan",
    "SSCAN" -> "http://redis.io/commands/sscan",
    "HSCAN" -> "http://redis.io/commands/hscan",
    "ZSCAN" -> "http://redis.io/commands/zscan"
  )

  def main(args: Array[String]): Unit = {
    val MethodRegex = """^  def (\w+).*""".r

    def caps(str: String) = str.flatMap {
      case ch if ch.isUpper => s" $ch"
      case ch => ch.toUpper.toString
    }

    def hyph(str: String) = str.flatMap {
      case ch if ch.isUpper => s"-${ch.toLower}"
      case ch => ch.toString
    }

    val file = "commons-redis/src/main/scala/com/avsystem/commons/redis/commands/transactions.scala"
    Source.fromFile(file).getLines().foreach {
      case line@MethodRegex(methodName) =>
        caps(methodName).split(" ").iterator.scanLeft("")(_ + " " + _).map(_.trim).toList.reverse.find(urls.contains) match {
          case Some(cmd) =>
            println(s"  /** [[${urls(cmd)} $cmd]] */\n$line")
          case None =>
            println(line)
        }
      case line => println(line)
    }
  }
}