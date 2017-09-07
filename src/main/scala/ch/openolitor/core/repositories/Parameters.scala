/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.repositories

import ch.openolitor.core.scalax._
import scalikejdbc._

trait Parameters {
  def parameters[A](params: Tuple1[A])(
    implicit
    binder0: ParameterBinderFactory[A]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1)
    )
  }

  def parameters[A, B](params: Tuple2[A, B])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2)
    )
  }

  def parameters[A, B, C](params: Tuple3[A, B, C])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3)
    )
  }

  def parameters[A, B, C, D](params: Tuple4[A, B, C, D])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4)
    )
  }

  def parameters[A, B, C, D, E](params: Tuple5[A, B, C, D, E])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5)
    )
  }

  def parameters[A, B, C, D, E, F](params: Tuple6[A, B, C, D, E, F])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6)
    )
  }

  def parameters[A, B, C, D, E, F, G](params: Tuple7[A, B, C, D, E, F, G])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7)
    )
  }

  def parameters[A, B, C, D, E, F, G, H](params: Tuple8[A, B, C, D, E, F, G, H])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I](params: Tuple9[A, B, C, D, E, F, G, H, I])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J](params: Tuple10[A, B, C, D, E, F, G, H, I, J])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K](params: Tuple11[A, B, C, D, E, F, G, H, I, J, K])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L](params: Tuple12[A, B, C, D, E, F, G, H, I, J, K, L])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M](params: Tuple13[A, B, C, D, E, F, G, H, I, J, K, L, M])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N](params: Tuple14[A, B, C, D, E, F, G, H, I, J, K, L, M, N])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](params: Tuple15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](params: Tuple16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](params: Tuple17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](params: Tuple18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q],
    binder17: ParameterBinderFactory[R]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17),
      binder17(params._18)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](params: Tuple19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q],
    binder17: ParameterBinderFactory[R],
    binder18: ParameterBinderFactory[S]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17),
      binder17(params._18),
      binder18(params._19)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](params: Tuple20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q],
    binder17: ParameterBinderFactory[R],
    binder18: ParameterBinderFactory[S],
    binder19: ParameterBinderFactory[T]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17),
      binder17(params._18),
      binder18(params._19),
      binder19(params._20)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](params: Tuple21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q],
    binder17: ParameterBinderFactory[R],
    binder18: ParameterBinderFactory[S],
    binder19: ParameterBinderFactory[T],
    binder20: ParameterBinderFactory[U]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17),
      binder17(params._18),
      binder18(params._19),
      binder19(params._20),
      binder20(params._21)
    )
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](params: Tuple22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V])(
    implicit
    binder0: ParameterBinderFactory[A],
    binder1: ParameterBinderFactory[B],
    binder2: ParameterBinderFactory[C],
    binder3: ParameterBinderFactory[D],
    binder4: ParameterBinderFactory[E],
    binder5: ParameterBinderFactory[F],
    binder6: ParameterBinderFactory[G],
    binder7: ParameterBinderFactory[H],
    binder8: ParameterBinderFactory[I],
    binder9: ParameterBinderFactory[J],
    binder10: ParameterBinderFactory[K],
    binder11: ParameterBinderFactory[L],
    binder12: ParameterBinderFactory[M],
    binder13: ParameterBinderFactory[N],
    binder14: ParameterBinderFactory[O],
    binder15: ParameterBinderFactory[P],
    binder16: ParameterBinderFactory[Q],
    binder17: ParameterBinderFactory[R],
    binder18: ParameterBinderFactory[S],
    binder19: ParameterBinderFactory[T],
    binder20: ParameterBinderFactory[U],
    binder21: ParameterBinderFactory[V]
  ): Seq[ParameterBinder] = {
    Seq(
      binder0(params._1),
      binder1(params._2),
      binder2(params._3),
      binder3(params._4),
      binder4(params._5),
      binder5(params._6),
      binder6(params._7),
      binder7(params._8),
      binder8(params._9),
      binder9(params._10),
      binder10(params._11),
      binder11(params._12),
      binder12(params._13),
      binder13(params._14),
      binder14(params._15),
      binder15(params._16),
      binder16(params._17),
      binder17(params._18),
      binder18(params._19),
      binder19(params._20),
      binder20(params._21),
      binder21(params._22)
    )
  }
}