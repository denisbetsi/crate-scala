package io.crate.client

import java.util.Date
import java.util.UUID

import scala.collection.JavaConverters._

trait SQLRequestTrait {

  def convertToJavaColumnType(o: Any): Object = {
    o match {
      case x: Array[Short] => x.map(_.asInstanceOf[java.lang.Short])
      case x: Array[Int] => x.map(_.asInstanceOf[java.lang.Integer])
      case x: Array[Long] => x.map(_.asInstanceOf[java.lang.Long])
      case x: Array[Float] => x.map(_.asInstanceOf[java.lang.Float])
      case x: Array[Double] => x.map(_.asInstanceOf[java.lang.Double])
      case x: Array[Byte] => x.map(_.asInstanceOf[java.lang.Byte])
      case x: Array[Boolean] => x.map(_.asInstanceOf[java.lang.Boolean])
      case m: Map[_, _] => m.asJava
      case t: Seq[_] => t.asJava
      case s: Some[_] => convertToJavaColumnType(s.get)
      case None => null
      case d: Date => d.getTime().asInstanceOf[java.lang.Long]
      case u: UUID => u.toString()
      case v: Any => v.asInstanceOf[AnyRef]
    }
  }

}