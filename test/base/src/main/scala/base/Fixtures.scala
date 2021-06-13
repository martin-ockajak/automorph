package base

import scala.util.{Try, Using}

trait Fixtures {

  /**
   * Fixture type.
   *
   * @tparam T fixture value type
   */
  type Fixture[T] = (T => Any) => Unit

  /**
   * Create loan fixture function using specified fixture creation function.
   *
   * @param createFixture fixture creation function
   * @tparam T fixture type
   * @return loan fixture function
   */
  def loanFixture[T: Using.Releasable](createFixture: => T): Fixture[T] =
    test => Using(createFixture)(fixture => test(fixture)).map(_ => ()).get

  /**
   * Create loan fixture function using specified fixture creation and release functions.
   *
   * @param createFixture fixture creation function
   * @param releaseFixture fixture release function
   * @tparam T fixture type
   * @return loan fixture function
   */
  def loanFixture[T](createFixture: => T, releaseFixture: T => Unit): Fixture[T] = test => {
    lazy val fixture = createFixture
    val tryTest = Try(test(fixture))
    releaseFixture(fixture)
    tryTest.get
    ()
  }
}
