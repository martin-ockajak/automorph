package automorph.util

import scala.util.Random

private[automorph] object MessageId {
  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Generates next random message identifier.
   *
   * @return message identifier
   */
  val next: String = Math.abs(random.nextLong()).toString
}
