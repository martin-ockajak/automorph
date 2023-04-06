package test

import test.Enum.Enum

case object Enum:

  enum Enum:

    case Zero
    case One

  def fromOrdinal(ordinal: Int): Enum =
    Enum.fromOrdinal(ordinal)

  def toOrdinal(value: Enum): Int =
    value.ordinal
