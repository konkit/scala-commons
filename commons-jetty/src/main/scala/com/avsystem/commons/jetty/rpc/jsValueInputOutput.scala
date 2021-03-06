package com.avsystem.commons
package jetty.rpc

import java.util.Base64

import com.avsystem.commons.serialization.GenCodec.ReadFailure
import com.avsystem.commons.serialization.{FieldInput, InputType, ListInput, ObjectInput, _}
import upickle.Js

import scala.collection.mutable.ListBuffer

/**
  * Author: ghik
  * Created: 14/07/16.
  */
object JsValueOutput {
  def write[T: GenCodec](value: T): Js.Value = {
    var result: Opt[Js.Value] = Opt.Empty
    val output = new JsValueOutput(value => result = Opt(value))
    GenCodec.write(output, value)
    result.getOrElse(throw new Exception("No value written by GenCodec"))
  }
}

final class JsValueOutput(consumer: Js.Value => Unit) extends Output {
  def writeNull() = consumer(Js.Null)
  def writeBoolean(boolean: Boolean) = consumer(if (boolean) Js.True else Js.False)
  def writeString(str: String) = consumer(Js.Str(str))
  def writeInt(int: Int) = consumer(Js.Num(int))
  def writeLong(long: Long) = consumer(Js.Str(long.toString))
  def writeDouble(double: Double) = consumer(Js.Num(double))
  def writeBinary(binary: Array[Byte]) = consumer(Js.Str(Base64.getEncoder.encodeToString(binary)))
  def writeList() = new JsValueListOutput(consumer)
  def writeObject() = new JsValueObjectOutput(consumer)
}

final class JsValueListOutput(consumer: Js.Arr => Unit) extends ListOutput {
  private val buffer = new ListBuffer[Js.Value]

  def writeElement() = new JsValueOutput(buffer += _)
  def finish() = consumer(Js.Arr(buffer: _*))
}

final class JsValueObjectOutput(consumer: Js.Obj => Unit) extends ObjectOutput {
  private val buffer = new ListBuffer[(String, Js.Value)]

  def writeField(key: String) = new JsValueOutput(v => buffer += ((key, v)))
  def finish() = consumer(Js.Obj(buffer: _*))
}

object JsValueInput {
  def read[T: GenCodec](value: Js.Value): T =
    GenCodec.read(new JsValueInput(value))
}

class JsValueInput(value: Js.Value) extends Input {
  private def jsonType = value match {
    case _: Js.Str => "string"
    case _: Js.Num => "number"
    case _: Js.Arr => "array"
    case _: Js.Obj => "object"
    case Js.True | Js.False => "boolean"
    case Js.Null => "null"
  }

  def inputType: InputType = value match {
    case _: Js.Arr => InputType.List
    case _: Js.Obj => InputType.Object
    case Js.Null => InputType.Null
    case _ => InputType.Simple
  }

  private def expectedError(expected: String) =
    throw new ReadFailure(s"Expected JSON $expected but got $jsonType")

  def readNull() = value match {
    case Js.Null => null
    case _ => expectedError("null")
  }
  def readBoolean() = value match {
    case Js.True => true
    case Js.False => false
    case _ => expectedError("boolean")
  }
  def readString() = value match {
    case Js.Str(string) => string
    case _ => expectedError("string")
  }
  def readBinary() = value match {
    case Js.Str(string) => try Base64.getDecoder.decode(string) catch {
      case e: IllegalArgumentException => throw new ReadFailure("error decoding Base64: " + e.getMessage)
    }
    case _ => expectedError("string")
  }
  def readInt() = value match {
    case Js.Num(num) =>
      if (num.isWhole && num >= Int.MinValue.toDouble && num <= Int.MaxValue.toDouble)
        num.toInt
      else
        throw new ReadFailure(s"not an int: $num")
    case _ => expectedError("number")
  }
  def readLong() = value match {
    case Js.Str(string) => try string.toLong catch {
      case e: NumberFormatException => throw new ReadFailure("error parsing number: " + e.getMessage)
    }
    case _ => expectedError("string")
  }
  def readDouble() = value match {
    case Js.Num(num) => num
    case _ => expectedError("number")
  }
  def readList() = value match {
    case arr: Js.Arr => new JsValueListInput(arr.value.iterator)
    case _ => expectedError("array")
  }
  def readObject() = value match {
    case obj: Js.Obj => new JsValueObjectInput(obj.value.iterator)
    case _ => expectedError("object")
  }
  def skip() = ()
}

final class JsValueFieldInput(val fieldName: String, value: Js.Value) extends JsValueInput(value) with FieldInput

final class JsValueListInput(it: Iterator[Js.Value]) extends ListInput {
  def hasNext = it.hasNext
  def nextElement() = new JsValueInput(it.next())
}

final class JsValueObjectInput(it: Iterator[(String, Js.Value)]) extends ObjectInput {
  def hasNext = it.hasNext
  def nextField() = it.next() match {
    case (k, v) => new JsValueFieldInput(k, v)
  }
}
