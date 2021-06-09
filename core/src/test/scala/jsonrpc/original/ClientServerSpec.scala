//    "method notification" - {
//      "procedure with 0 arguments" in {
//        remoteNotify("method4")
//      }
//      "procedure with 1 argument" in {
//        remoteNotify("method5")
//      }
//    }
//
//    "method call" - {
//      val api = new Api
//      " with 0 arguments" in {
//        call[Double](api, "0") shouldBe 1.2d
//      }
//      " with 1 argument" in {
//        call[Int](
//          api,
//          "1",
//          Record("x", boolean = true, 0, 1, Some(2), 3, 4.5f, 6.7d, Enumeration.Zero, List(8, 9), Map("foo" -> "y", "bar" -> "z"), None)
//        ) shouldBe 3
//      }
//      " with 2 arguments" in {
//        val record = Record("x", boolean = true, 0, 1, Some(2), 3, 4.5f, 6.7d, Enumeration.Zero, List(8, 9), Map("foo" -> "y", "bar" -> "z"), None)
//        call[Record](api, "2", record, "test") shouldBe record.copy(
//          string = "x - test",
//          long = 4,
//          enumeration = Enumeration.One
//        )
//      }
//      " with 3 arguments" in {
//        call[Map[String, String]](api, "3", Some(true), 8.9f, List(0, 1, 2)) shouldBe Map(
//          "boolean" -> "true",
//          "float" -> "8.9",
//          "list" -> "0, 1, 2"
//        )
//      }
//      "incorrect number of arguments" in {
//        intercept[JsonRpcException] {
//          remoteCall("3", Some("none"))
//        }.code shouldBe ErrorCodes.InvalidParams
//      }
//      "invalid method name" in {
//        intercept[JsonRpcException] {
//          remoteCall("none")
//        }.code shouldBe ErrorCodes.MethodNotFound
//      }
//    }
//  }
