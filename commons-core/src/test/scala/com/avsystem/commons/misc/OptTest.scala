package com.avsystem.commons
package misc

import org.scalatest.FunSuite

import com.avsystem.commons.jiop.JavaInterop._

/**
  * Author: ghik
  * Created: 08/01/16.
  */
class OptTest extends FunSuite {
  test("nonempty test") {
    val opt = Opt(23)
    opt match {
      case Opt(num) => assert(num == 23)
    }
  }

  test("empty test") {
    val str: String = null
    val opt = Opt(str)
    opt match {
      case Opt.Empty =>
    }
  }

  test("null some test") {
    intercept[NullPointerException](Opt.some[String](null))
  }

  test("boxing unboxing test") {
    val opt: Opt[Int] = Opt(42)
    val boxedOpt: Opt[JInteger] = opt.boxed
    val unboxedOpt: Opt[Int] = boxedOpt.unboxed
    assert(opt == unboxedOpt)
  }
}
