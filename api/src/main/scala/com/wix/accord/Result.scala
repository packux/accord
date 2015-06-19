/*
  Copyright 2013-2014 Wix.com

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.wix.accord

/** A base trait for all violation types. */ 
trait Violation {
  /** The actual runtime value of the object under validation. */
  def value: Any
  /** A textual description of the constraint being violated (for example, "must not be empty"). */
  def constraint: String
  /** The textual description of the object under validation (this is the expression that, when evaluated at
    * runtime, produces the value in [[com.wix.accord.Violation.value]]). This is normally filled in
    * by the validation transform macro, but can also be explicitly provided via the DSL.
    */
  def description: Option[ String ]

  /** Rewrites the description for this violation (used internally by the validation transform macro). As
    * violations are immutable, in practice this returns a modified copy.
    *
    * @param rewrite The rewritten description.
    * @return A modified copy of this violation with the new description in place.
    */
  def withDescription( rewrite: String ): Violation

  /** Allows defining whether a violation should be considered fatal or not. Doing so enables external components to
    * determine whether a given violation should stop further processing of not.
    */
  def isFatal: Boolean
}

/** Describes the violation of a validation rule or constraint.
  * 
  * @param value The value of the object which failed the validation rule.
  * @param constraint A textual description of the constraint being violated (for example, "must not be empty").
  * @param description The textual description of the object under validation.
  * @param fatal Whether this rule violation should be considered fatal. By default, all rule violations are fatal.
  */
case class RuleViolation( value: Any, constraint: String, description: Option[ String ], fatal: Boolean = true ) extends Violation {
  def withDescription( rewrite: String ) = this.copy( description = Some( rewrite ) )

  override def isFatal: Boolean = fatal
}

/** Describes the violation of a group of constraints. For example, the [[com.wix.accord.combinators.Or]]
  * combinator produces a group violation when all predicates fail.
  *
  * @param value The value of the object which failed the validation rule.
  * @param constraint A textual description of the constraint being violated (for example, "must not be empty").
  * @param description The textual description of the object under validation.
  * @param children The set of violations contained within the group.
  */
case class GroupViolation( value: Any, constraint: String, description: Option[ String ], children: Set[ Violation ] )
  extends Violation {
  def withDescription( rewrite: String ) = this.copy( description = Some( rewrite ) )

  /** Evaluates if any of the validations included in this group is fatal. */
  override def isFatal: Boolean = children.exists(violation => violation.isFatal)

}

/** A base trait for validation results.
  * @see [[com.wix.accord.Success]], [[com.wix.accord.Failure]]
  */
sealed trait Result {

  /** Returns `true` if the `Result` is a `Success`, `false` otherwise.  */
  def isSuccess: Boolean

  /** Returns `true` if the `Result` is a `Failure`, `false` otherwise.  */
  def isFailure: Boolean


  def and( other: Result ): Result
  def or( other: Result ): Result

  /** Rewrites the description for all violations, if applicable.
    *
    * @param rewrite The rewritten description.
    * @return A modified copy of this result with the new description in place.
    */
  def withDescription( rewrite: String ): Result
}

/** An object representing a successful validation result. */
case object Success extends Result {
  def and( other: Result ) = other
  def or( other: Result ) = this
  def withDescription( rewrite: String ) = this

  def isSuccess: Boolean = true

  def isFailure: Boolean = false
}

/** An object representing a failed validation result.
  * @param violations The violations that caused the validation to fail.
  */
case class Failure( violations: Set[ Violation ] ) extends Result {
  def and( other: Result ) = other match {
    case Success => this
    case Failure( vother ) => Failure( violations ++ vother )
  }
  def or( other: Result ) = other match {
    case Success => other
    case Failure(_) => this
  }
  def withDescription( rewrite: String ) = Failure( violations map { _ withDescription rewrite } )

  def isSuccess: Boolean = false

  def isFailure: Boolean = true
}
