package com.twitter.schmemcached.protocol

import org.jboss.netty.buffer.ChannelBuffer

sealed abstract class Command

abstract class StorageCommand(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer) extends Command
abstract class ArithmeticCommand(key: ChannelBuffer, delta: Int)                      extends Command
abstract class RetrievalCommand(keys: Seq[ChannelBuffer])                             extends Command

case class Set(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer)     extends StorageCommand(key, flags, expiry, value)
case class Add(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer)     extends StorageCommand(key, flags, expiry, value)
case class Replace(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer) extends StorageCommand(key, flags, expiry, value)
case class Append(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer)  extends StorageCommand(key, flags, expiry, value)
case class Prepend(key: ChannelBuffer, flags: Int, expiry: Int, value: ChannelBuffer) extends StorageCommand(key, flags, expiry, value)

case class Get(keys: Seq[ChannelBuffer])                                              extends RetrievalCommand(keys)
case class Gets(keys: Seq[ChannelBuffer])                                             extends RetrievalCommand(keys)

case class Delete(key: ChannelBuffer)                                                 extends Command
case class Incr(key: ChannelBuffer, value: Int)                                       extends ArithmeticCommand(key, value)
case class Decr(key: ChannelBuffer, value: Int)                                       extends ArithmeticCommand(key, -value)