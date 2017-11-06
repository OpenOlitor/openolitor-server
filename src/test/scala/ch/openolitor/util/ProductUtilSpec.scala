package ch.openolitor.util

import org.specs2.mutable._
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

class ProductUtilSpec extends Specification {
  import ProductUtil._

  "ProductUtil" should {
    "create map of simple case class" in {
      case class SimpleCaseClass(name: String, counter: Int) extends Product
      val simple = SimpleCaseClass("myName", 42)
      simple.toMap() === Map("name" -> "myName", "counter" -> 42)
    }

    "create map of simple case class with collection" in {
      case class SimpleCaseClass(name: String, counters: Seq[Int]) extends Product
      val simple = SimpleCaseClass("myName", Seq(1, 2, 3))
      simple.toMap() === Map("name" -> "myName", "counters" -> Seq(1, 2, 3))
    }

    "create map of nested case class" in {
      case class Address(street: String)
      case class Person(name: String, address: Address) extends Product
      val simple = Person("myName", Address("myStreet"))
      simple.toMap() === Map("name" -> "myName", "address" -> Map("street" -> "myStreet"))
    }

    "create map of nested case class in collection" in {
      case class Address(street: String)
      case class Person(name: String, addresses: Seq[Address]) extends Product
      val simple = Person("myName", Seq(Address("myStreet1"), Address("myStreet2")))
      simple.toMap() === Map("name" -> "myName", "addresses" -> Seq(Map("street" -> "myStreet1"), Map("street" -> "myStreet2")))
    }
  }

  "ProductUtil with converters" should {
    "convert dateformat correctly" in {
      case class SimpleCaseClass(name: String, age: Int, birthdate: DateTime)
      val simple = SimpleCaseClass("myName", 42, new DateTime(2001, 2, 15, 1, 30, 15, DateTimeZone.UTC))
      val format = DateTimeFormat.forPattern("yyyy-dd-MM hh:mm:ssZ")
      simple.toMap({ case x: DateTime => format.print(x) }) === Map("name" -> "myName", "age" -> 42, "birthdate" -> "2001-15-02 01:30:15+0000")
    }
  }
}