package automorph.openapi

/**
 * JSON serializer.
 */
private[automorph] object Json {
  private val space = "  "

  def none: String = "null"

  def string(value: Any): String =
    s"\"$value\""

  def number(value: Number): String =
    s"$value"

  def array(values: Iterable[_], indent: Int): String = {
    val start = "["
    val end = s"${space * indent}]"
    val items = values.map {
      case None => none
      case Some(value: Map[_, _]) => map(value, indent + 1)
      case Some(value: Iterable[_]) => array(value, indent + 1)
      case Some(value: Number) => number(value)
      case Some(value) => string(value)
      case value: Map[_, _] => map(value, indent + 1)
      case value: Iterable[_] => array(value, indent + 1)
      case value: Number => number(value)
      case value =>string(value)
    }.map { value =>
      s"${space * (indent + 1)}$value"
    }.mkString(",\n")
    s"$start\n$items\n$end"
  }

  def map(values: Map[_, _], indent: Int): String = {
    val start = "{"
    val end = s"${space * indent}}"
    val items = values.flatMap((entry: (_, _)) => entry match {
      case (_, None) => None
      case (key, Some(value: Map[_, _])) => Some(key -> map(value, indent + 1))
      case (key, Some(value: Iterable[_])) => Some(key -> array(value, indent + 1))
      case (key, Some(value: Number)) => Some(key -> number(value))
      case (key, Some(value)) => Some(key -> string(value))
      case (key, value: Map[_, _]) => Some(key -> map(value, indent + 1))
      case (key, value: Iterable[_]) => Some(key -> array(value, indent + 1))
      case (key, value: Number) => Some(key -> number(value))
      case (key, value) => Some(key -> string(value))
    }).map { case (key, value) =>
      s"${space * (indent + 1)}\"$key\": $value"
    }.mkString(",\n")
    s"$start\n$items\n$end"
  }
}
