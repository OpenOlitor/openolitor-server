package ch.openolitor.util

import org.specs2.mutable._

class ProductUtilSpec extends Specification {
  import ProductUtil._

  "ProductUtil" should {
    "create map of simple case class" in {
      case class SimpleCaseClass(name: String, counter: Int) extends Product
      val simple = SimpleCaseClass("myName", 42)
      simple.toMap === Map("name" -> "myName", "counter" -> 42)
    }

    "create map of simple case class with collection" in {
      case class SimpleCaseClass(name: String, counters: Seq[Int]) extends Product
      val simple = SimpleCaseClass("myName", Seq(1, 2, 3))
      simple.toMap === Map("name" -> "myName", "counters" -> Seq(1, 2, 3))
    }

    "create map of nested case class" in {
      case class Address(street: String)
      case class Person(name: String, address: Address) extends Product
      val simple = Person("myName", Address("myStreet"))
      simple.toMap === Map("name" -> "myName", "address" -> Map("street" -> "myStreet"))
    }

    "create map of nested case class in collection" in {
      case class Address(street: String)
      case class Person(name: String, addresses: Seq[Address]) extends Product
      val simple = Person("myName", Seq(Address("myStreet1"), Address("myStreet2")))
      simple.toMap === Map("name" -> "myName", "addresses" -> Seq(Map("street" -> "myStreet1"), Map("street" -> "myStreet2")))
    }
  }
}