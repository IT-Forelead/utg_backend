package utg.utils

import scala.collection.mutable

import eu.timepit.refined.types.string.NonEmptyString
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

object RandomGenerator {
  def randomLink(len: Int): NonEmptyString = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new mutable.StringBuilder(len)
    val ab = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz"
    for (i <- 0 until len)
      sb.append(ab(rand.nextInt(ab.length)))
    sb.toString
  }
}
