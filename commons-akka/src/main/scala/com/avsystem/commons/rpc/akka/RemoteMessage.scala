package com.avsystem.commons
package rpc.akka

import akka.util.ByteString
import com.avsystem.commons.rpc.akka.AkkaRPCFramework.RawValue
import com.avsystem.commons.serialization.GenCodec

/**
  * @author Wojciech Milewski
  */
private sealed trait RemoteMessage extends Serializable

private object RemoteMessage {
  implicit val byteStringCodec = GenCodec.create[ByteString](input => ByteString(input.readBinary()), (output, byteString) => output.writeBinary(byteString.toArray))
  implicit val rawInvocationCodec = GenCodec.materialize[RawInvocation]

  implicit val procedureInvocationMessageCodec: GenCodec[ProcedureInvocationMessage] = GenCodec.materialize[ProcedureInvocationMessage]
  implicit val functionInvocationMessageCodec: GenCodec[FunctionInvocationMessage] = GenCodec.materialize[FunctionInvocationMessage]
  implicit val observableInvocationMessageCodec: GenCodec[ObservableInvocationMessage] = GenCodec.materialize[ObservableInvocationMessage]

  implicit val invocationSuccessCodec: GenCodec[InvocationSuccess] = GenCodec.materialize[InvocationSuccess]
  implicit val invocationFailureCodec: GenCodec[InvocationFailure] = GenCodec.materialize[InvocationFailure]

  implicit val continueCodec: GenCodec[MonixProtocol.Continue.type] = GenCodec.materialize[MonixProtocol.Continue.type]
  implicit val stopCodec: GenCodec[MonixProtocol.Stop.type] = GenCodec.materialize[MonixProtocol.Stop.type]
  implicit val subscribeCodec: GenCodec[MonixProtocol.Subscribe.type] = GenCodec.materialize[MonixProtocol.Subscribe.type]
  implicit val streamCompletedCodec: GenCodec[MonixProtocol.StreamCompleted.type] = GenCodec.materialize[MonixProtocol.StreamCompleted.type]
  implicit val heatBeatCodec: GenCodec[MonixProtocol.Heartbeat.type] = GenCodec.materialize[MonixProtocol.Heartbeat.type]
}

private final case class RawInvocation(rpcName: String, argLists: List[List[RawValue]]) extends RemoteMessage

private sealed trait InvocationMessage extends RemoteMessage {
  def getterChain: Seq[RawInvocation]
  def name: String
  def argLists: List[List[RawValue]]
}
private final case class ProcedureInvocationMessage(name: String, argLists: List[List[RawValue]], getterChain: Seq[RawInvocation]) extends InvocationMessage
private final case class FunctionInvocationMessage(name: String, argLists: List[List[RawValue]], getterChain: Seq[RawInvocation]) extends InvocationMessage
private final case class ObservableInvocationMessage(name: String, argLists: List[List[RawValue]], getterChain: Seq[RawInvocation]) extends InvocationMessage

private sealed trait InvocationResult extends RemoteMessage
private final case class InvocationSuccess(value: RawValue) extends InvocationResult
private final case class InvocationFailure(exceptionName: String, remoteMessage: String) extends InvocationResult

private object MonixProtocol {
  sealed trait RemoteAck extends RemoteMessage
  case object Continue extends RemoteAck
  case object Stop extends RemoteAck

  case object Subscribe extends RemoteMessage
  case object StreamCompleted extends RemoteMessage

  case object Heartbeat extends RemoteMessage
}