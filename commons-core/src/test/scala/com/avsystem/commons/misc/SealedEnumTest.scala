package com.avsystem.commons
package misc

import org.scalatest.FunSuite

class SealedEnumTest extends FunSuite {
  sealed abstract class SomeEnum(implicit val sourceInfo: SourceInfo) extends OrderedEnum
  object SomeEnum extends SealedEnumCompanion[SomeEnum] {
    case object First extends SomeEnum
    case object Second extends SomeEnum
    case object Third extends SomeEnum
    case object Fourth extends SomeEnum

    val values: List[SomeEnum] = caseObjects
  }

  test("case objects listing test") {
    import SomeEnum._
    assert(values == List(First, Second, Third, Fourth))
  }
}
