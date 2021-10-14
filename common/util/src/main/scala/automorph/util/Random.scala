package automorph.util

/** Random value generator. */
object Random {

  private lazy val random = new scala.util.Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Generate random numeric identifier.
   *
   * @return numeric identifier
   */
  def id: String = Math.abs(random.nextInt()).toString
}
