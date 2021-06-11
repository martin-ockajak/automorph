package jsonrpc

enum Enum:

  case Zero
  case One

final case class Record(
  string: String,
  boolean: Boolean,
  byte: Byte,
  short: Short,
  int: Option[Int],
  long: Long,
  float: Option[Float],
  double: Double,
  enumeration: Enum,
  list: List[String],
  map: Map[String, Int],
  structure: Option[Structure],
  none: Option[String]
)

final case class Structure(
  value: String
)
