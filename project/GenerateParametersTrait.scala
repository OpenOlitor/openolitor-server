object GenerateParametersTrait {
  def apply(n: Int): String = {
    s"""
package ch.openolitor.core.repositories

trait Parameters extends ${(1 to n) map (i => s"Parameters$i") mkString (" with ")}"""
  }
}