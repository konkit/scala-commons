package com.avsystem.commons
package macros.serialization

import com.avsystem.commons.macros.{AbstractMacroCommons, TypeClassDerivation}

import scala.reflect.macros.blackbox

abstract class CodecMacroCommons(ctx: blackbox.Context) extends AbstractMacroCommons(ctx) {

  import c.universe._

  val SerializationPkg = q"$CommonsPackage.serialization"
  val NameAnnotType = getType(tq"$SerializationPkg.name")
  val JavaInteropObj = q"$CommonsPackage.jiop.JavaInterop"
  val JListObj = q"$JavaInteropObj.JList"
  val JListCls = tq"$JavaInteropObj.JList"
  val ListBufferCls = tq"$CollectionPkg.mutable.ListBuffer"
  val BMapCls = tq"$CollectionPkg.Map"
  val NOptObj = q"$CommonsPackage.misc.NOpt"
  val NOptCls = tq"$CommonsPackage.misc.NOpt"

  def tupleGet(i: Int) = TermName(s"_${i + 1}")

  def annotName(sym: Symbol): String =
    getAnnotations(sym, NameAnnotType).headOption.map(_.tree.children.tail).map {
      case Literal(Constant(str: String)) :: _ => str
      case param :: _ => c.abort(param.pos, s"@name argument must be a string literal")
    }.getOrElse(sym.name.decodedName.toString)

  def getAnnotations(sym: Symbol, annotTpe: Type): List[Annotation] = {
    val syms =
      if (sym.isClass) sym.asClass.baseClasses
      else sym :: sym.overrides
    syms.flatMap(_.annotations).filter(_.tree.tpe <:< annotTpe)
  }
}

class GenCodecMacros(ctx: blackbox.Context) extends CodecMacroCommons(ctx) with TypeClassDerivation {

  import c.universe._

  val TransparentAnnotType = getType(tq"$SerializationPkg.transparent")
  val TransientDefaultAnnotType = getType(tq"$SerializationPkg.transientDefault")
  val GenCodecObj = q"$SerializationPkg.GenCodec"
  val GenCodecCls = tq"$SerializationPkg.GenCodec"

  def mkTupleCodec[T: c.WeakTypeTag](elementCodecs: c.Tree*): c.Tree = {
    val tupleTpe = weakTypeOf[T]
    val indices = elementCodecs.indices
    q"""
        new $GenCodecObj.ListCodec[$tupleTpe] {
          protected def nullable = true
          protected def readList(input: $SerializationPkg.ListInput) =
            (..${indices.map(i => q"${elementCodecs(i)}.read(input.nextElement())")})
          protected def writeList(output: $SerializationPkg.ListOutput, value: $tupleTpe) = {
            ..${indices.map(i => q"${elementCodecs(i)}.write(output.writeElement(), value.${tupleGet(i)})")}
          }
        }
     """
  }

  def typeClass = GenCodecCls
  def typeClassName = "GenCodec"
  def wrapInAuto(tree: Tree) = q"$GenCodecObj.Auto($tree)"
  def implementDeferredInstance(tpe: Type): Tree = q"new $GenCodecObj.Deferred[$tpe]"

  def forSingleton(tpe: Type, singleValueTree: Tree): Tree =
    q"new $GenCodecObj.SingletonCodec[$tpe]($singleValueTree)"

  def isTransparent(sym: Symbol): Boolean =
    getAnnotations(sym, TransparentAnnotType).nonEmpty

  def isTransientDefault(param: ApplyParam) =
    param.defaultValue.nonEmpty && param.sym.annotations.exists(_.tree.tpe <:< TransientDefaultAnnotType)

  def forApplyUnapply(tpe: Type, apply: Symbol, unapply: Symbol, params: List[ApplyParam]): Tree = {
    val companion = unapply.owner.asClass.module
    val nameBySym = params.groupBy(p => annotName(p.sym)).map {
      case (name, List(param)) => (param.sym, name)
      case (name, ps) if ps.length > 1 =>
        c.abort(c.enclosingPosition, s"Parameters ${ps.map(_.sym.name).mkString(", ")} have the same @name: $name")
    }
    val depNames = params.map(p => (p.sym, c.freshName(TermName(p.sym.name.toString + "Codec")))).toMap
    def depDeclaration(param: ApplyParam) =
      q"lazy val ${depNames(param.sym)} = ${param.instance}"

    // don't use apply/unapply when they're synthetic (for case class) to avoid reference to companion object

    val caseClass = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass
    val canUseFields = caseClass && unapply.isSynthetic && params.forall { p =>
      alternatives(tpe.member(p.sym.name)).exists { f =>
        f.isTerm && f.asTerm.isCaseAccessor && f.isPublic && f.typeSignature.finalResultType =:= p.sym.typeSignature
      }
    }

    def applier(args: List[Tree]) =
      if (apply.isConstructor) q"new $tpe(..$args)"
      else q"$companion.apply[..${tpe.typeArgs}](..$args)"

    def writeObjectBody = params match {
      case Nil =>
        if (canUseFields)
          q"()"
        else
          q"""
            if(!$companion.unapply[..${tpe.typeArgs}](value)) {
              unapplyFailed
            }
           """
      case List(p) =>
        def writeField(value: Tree) = {
          val baseWrite = q"${depNames(p.sym)}.write(output.writeField(${nameBySym(p.sym)}), $value)"
          if (isTransientDefault(p))
            q"if($value != ${p.defaultValue}) { $baseWrite }"
          else
            baseWrite
        }

        if (canUseFields)
          writeField(q"value.${p.sym.name}")
        else
          q"""
            $companion.unapply[..${tpe.typeArgs}](value)
              .map(v => ${writeField(q"v")}).getOrElse(unapplyFailed)
           """
      case _ =>
        def writeField(p: ApplyParam, value: Tree) = {
          val baseWrite = q"${depNames(p.sym)}.write(output.writeField(${nameBySym(p.sym)}), $value)"
          if (isTransientDefault(p))
            q"if($value != ${p.defaultValue}) { $baseWrite }"
          else
            baseWrite
        }

        if (canUseFields)
          q"..${params.map(p => writeField(p, q"value.${p.sym.name}"))}"
        else
          q"""
            $companion.unapply[..${tpe.typeArgs}](value).map { t =>
              ..${params.zipWithIndex.map({ case (p, i) => writeField(p, q"t.${tupleGet(i)}") })}
            }.getOrElse(unapplyFailed)
           """
    }

    if (isTransparent(tpe.typeSymbol)) params match {
      case List(p) =>
        val writeBody = if (canUseFields)
          q"${depNames(p.sym)}.write(output, value.${p.sym.name})"
        else
          q"$companion.unapply[..${tpe.typeArgs}](value).map(${depNames(p.sym)}.write(output, _)).getOrElse(unapplyFailed)"

        q"""
           new $GenCodecObj.NullSafeCodec[$tpe] with $GenCodecObj.ErrorReportingCodec[$tpe] {
             ${depDeclaration(p)}
             protected def typeRepr = ${tpe.toString}
             protected def nullable = ${typeOf[Null] <:< tpe}
             protected def readNonNull(input: $SerializationPkg.Input): $tpe =
               ${applier(List(q"${depNames(p.sym)}.read(input)"))}
             protected def writeNonNull(output: $SerializationPkg.Output, value: $tpe): $UnitCls =
               $writeBody
           }
         """
      case _ =>
        abort(s"@transparent annotation found on class with ${params.size} parameters, expected exactly one.")
    } else {

      def optName(param: ApplyParam): TermName =
        TermName(nameBySym(param.sym) + "Opt")

      def readField(param: ApplyParam) = {
        val fieldName = nameBySym(param.sym)
        val defaultValueTree = param.defaultValue.orElse(q"fieldMissing($fieldName)")
        q"${optName(param)}.getOrElse($defaultValueTree)"
      }

      q"""
        new $GenCodecObj.ObjectCodec[$tpe] with $GenCodecObj.ErrorReportingCodec[$tpe] {
          ..${params.map(depDeclaration)}
          protected def typeRepr = ${tpe.toString}
          protected def nullable = ${typeOf[Null] <:< tpe}
          protected def readObject(input: $SerializationPkg.ObjectInput): $tpe = {
            ..${params.map(p => q"var ${optName(p)}: $NOptCls[${p.sym.typeSignature}] = $NOptObj.Empty")}
            while(input.hasNext) {
              val fi = input.nextField()
              fi.fieldName match {
                case ..${params.map(p => cq"${nameBySym(p.sym)} => ${optName(p)} = $NOptObj.some(${depNames(p.sym)}.read(fi))")}
                case _ => fi.skip()
              }
            }
            ${applier(params.map(readField))}
          }
          protected def writeObject(output: $SerializationPkg.ObjectOutput, value: $tpe) = $writeObjectBody
        }
       """
    }
  }

  private def dbNameBySymMap(subtypeSymbols: Seq[Symbol]): Map[Symbol, String] =
    subtypeSymbols.groupBy(st => annotName(st)).map {
      case (dbName, List(subtype)) => (subtype, dbName)
      case (dbName, kst) =>
        c.abort(c.enclosingPosition, s"Subclasses ${kst.map(_.name).mkString(", ")} have the same @name: $dbName")
    }

  def forSealedHierarchy(tpe: Type, subtypes: List[KnownSubtype]): Tree = {
    val dbNameBySym = dbNameBySymMap(subtypes.map(_.sym))
    val depNames = subtypes.map(st => (st.sym, c.freshName(TermName(st.sym.name.toString + "Codec")))).toMap
    def depDeclaration(subtype: KnownSubtype) =
      q"lazy val ${depNames(subtype.sym)} = ${subtype.instance}"

    q"""
      new $GenCodecObj.ObjectCodec[$tpe] with $GenCodecObj.ErrorReportingCodec[$tpe] {
        ..${subtypes.map(depDeclaration)}
        protected def typeRepr = ${tpe.toString}
        protected def nullable = ${typeOf[Null] <:< tpe}
        protected def readObject(input: $SerializationPkg.ObjectInput): $tpe = {
          if(input.hasNext) {
            val fi = input.nextField()
            val result = fi.fieldName match {
              case ..${subtypes.map(st => cq"${dbNameBySym(st.sym)} => ${depNames(st.sym)}.read(fi)")}
              case key => unknownCase(key)
            }
            if(input.hasNext) notSingleField(empty = false) else result
          } else notSingleField(empty = true)
        }
        protected def writeObject(output: $SerializationPkg.ObjectOutput, value: $tpe) = value match {
          case ..${subtypes.map(st => cq"value: ${st.tpe} => ${depNames(st.sym)}.write(output.writeField(${dbNameBySym(st.sym)}), value)")}
        }
      }
     """
  }

  def forUnknown(tpe: Type): Tree =
    typecheckException(s"Cannot automatically derive GenCodec for $tpe")

  def materializeRecursively[T: c.WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    q"""
       implicit def ${c.freshName(TermName("allow"))}[T]: $AllowImplicitMacroCls[$typeClass[T]] =
         $AllowImplicitMacroObj[$typeClass[T]]
       $GenCodecObj.materialize[$tpe]
     """
  }

  def materializeMacroCodec[T: c.WeakTypeTag]: Tree =
    q"$SerializationPkg.MacroCodec($GenCodecObj.materialize[${weakTypeOf[T]}])"

  def forSealedEnum[T: c.WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    q"$GenCodecObj.fromKeyCodec($SerializationPkg.GenKeyCodec.forSealedEnum[$tpe])"
  }
}

class GenKeyCodecMacros(ctx: blackbox.Context) extends CodecMacroCommons(ctx) {

  import c.universe._

  val GenKeyCodecObj = q"$SerializationPkg.GenKeyCodec"
  val GenKeyCodecCls = tq"$SerializationPkg.GenKeyCodec"

  def forSealedEnum[T: c.WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    knownSubtypes(tpe).map { subtypes =>
      def singleValue(st: Type) = singleValueFor(st).getOrElse(abort(s"$st is not an object"))
      val nameBySym = subtypes.groupBy(st => annotName(st.typeSymbol)).map {
        case (name, List(subtype)) => (subtype.typeSymbol, name)
        case (name, kst) =>
          abort(s"Objects ${kst.map(_.typeSymbol.name).mkString(", ")} have the same @name: $name")
      }
      val result =
        q"""
          new $GenKeyCodecCls[$tpe] {
            def tpeString = ${tpe.toString}
            def read(key: String): $tpe = key match {
              case ..${subtypes.map(st => cq"${nameBySym(st.typeSymbol)} => ${singleValue(st)}")}
              case _ => throw new $SerializationPkg.GenCodec.ReadFailure(s"Cannot read $$tpeString, unknown object: $$key")
            }
            def write(value: $tpe): String = value match {
              case ..${subtypes.map(st => cq"_: $st => ${nameBySym(st.typeSymbol)}")}
            }
          }
         """
      withKnownSubclassesCheck(result, tpe)
    }.getOrElse(abort(s"$tpe is not a sealed trait or class"))
  }
}
