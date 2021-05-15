package jsonrpc.spi

trait Message[Json]:

  def jsonrpc: Option[String]

  def id: Option[Either[BigDecimal, String]]

  def method: Option[String]

  def params: Option[Either[List[Json], Map[String, Json]]]

  def result: Option[Json]

  def error: Option[CallError[Json]]


trait CallError[JsonValue]:

  def code: Option[Int]

  def message: Option[String]

  def data: Option[JsonValue]
