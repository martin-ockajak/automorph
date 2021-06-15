package test

object Enum extends Enumeration {
  type Enum = Value
  val Zero, One = Value

  def fromOrdinal(ordinal: Int): Enum = this.values.toSeq(ordinal)
}
