/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package common

import scala.tools.nsc.util.SourceFile

/**
 * The common interface for all changes.
 * 
 * Note: in versions < 0.4 Change was a case-class, now it's the
 * super type of `TextChange` and `NewFileChange`. `NewFileChanges
 * are used by refactorings that create new source files (Move Class).
 * 
 * Additionally, the `file` attribute is now of type `SourceFile`,
 * because parts of the refactoring process need to access the content
 * of the  underlying source file.
 */
sealed trait Change {
  val text: String
}

case class TextChange(file: SourceFile, from: Int, to: Int, text: String) extends Change

/**
 * The changes creates a new source file, indicated by the `fullName` parameter. It is of 
 * the form "some.package.FileName".
 */
case class NewFileChange(fullName: String, text: String) extends Change

object Change {
  /**
   * Applies the list of changes to the source string. NewFileChanges are ignored. 
   * Primarily used for testing / debugging.
   */
  def applyChanges(ch: List[Change], source: String): String = {
    val changes = ch collect {
      case tc: TextChange => tc
    }
    (source /: changes.sortBy(-_.to)) { (src, change) =>
      src.substring(0, change.from) + change.text + src.substring(change.to)
    }
  }
}
