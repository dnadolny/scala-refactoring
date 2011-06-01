/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.implementations

import implementations.OrganizeImports
import tests.util.{TestHelper, TestRefactoring}
      
class OrganizeImportsFullyRecomputeTest extends TestHelper with TestRefactoring {
  outer =>
    
  def organize(pro: FileSet) = new TestRefactoringImpl(pro) {
    val refactoring = new OrganizeImports with SilentTracing { val global = outer.global }
    val changes = performRefactoring(new refactoring.RefactoringParameters(deps = refactoring.Dependencies.FullyRecompute))
  }.changes
  
  def organizeWithoutCollapsing(pro: FileSet) = new TestRefactoringImpl(pro) {
    val refactoring = new OrganizeImports with SilentTracing { val global = outer.global }
    val options = List()
    val changes = performRefactoring(new refactoring.RefactoringParameters(options = options, deps = refactoring.Dependencies.FullyRecompute))
  }.changes
  
  def organizeExpand(pro: FileSet) = new TestRefactoringImpl(pro) {
    val refactoring = new OrganizeImports with SilentTracing { val global = outer.global }
    val options = List(refactoring.ExpandImports)
    val changes = performRefactoring(new refactoring.RefactoringParameters(options = options, deps = refactoring.Dependencies.FullyRecompute))
  }.changes
  
  @Test
  def testOrganizeOptions() {
    
    val src = """
      package tests.importing
      
      import scala.collection.mutable.{ListBuffer, HashMap}
      import scala.xml.QNode
      import scala.xml.Elem
      import scala.math.BigInt
      import scala.math._
      
      import scala.util.{Properties => ScalaProperties}
      """
      
    val restOfFile = """  
      object Main {
        // we need to actually use the imports, otherwise they are removed
        val lb = ListBuffer(1)
        val lb = HashMap(1 → 1)
        var no: QNode.type = null
        var elem: Elem = null
        var bigi: BigInt = null
        var bigd: BigDecimal = null
        var props: ScalaProperties = null
      }
      """
    
    new FileSet {
      (src + restOfFile) becomes
      """
      package tests.importing
      
      import scala.collection.mutable.HashMap
      import scala.collection.mutable.ListBuffer
      import scala.math.BigDecimal
      import scala.math.BigInt
      import scala.xml.Elem
      import scala.xml.QNode
      """ + restOfFile
    } applyRefactoring organizeWithoutCollapsing
    
    new FileSet {
      (src + restOfFile) becomes
      """
      package tests.importing
      
      import scala.collection.mutable.HashMap
      import scala.collection.mutable.ListBuffer
      import scala.math.BigDecimal
      import scala.math.BigInt
      import scala.xml.Elem
      import scala.xml.QNode
      """ + restOfFile
    } applyRefactoring organizeExpand
    
    new FileSet {
      (src + restOfFile) becomes
      """
      package tests.importing
      
      import scala.collection.mutable.{HashMap, ListBuffer}
      import scala.math.{BigDecimal, BigInt}
      import scala.xml.{Elem, QNode}
      """ + restOfFile
    } applyRefactoring organize
  }
  
  @Test
  def dependencyOnMultipleOverloadedMethods = new FileSet {
    """
      import scala.math.BigDecimal._

      class C {
        def m() {
          apply("5")
          apply(5l)
        }
      }
    """ becomes
    """
      import scala.math.BigDecimal.apply

      class C {
        def m() {
          apply("5")
          apply(5l)
        }
      }
    """
  } applyRefactoring organize
  
  @Test
  def expandImportsButNotWildcards = new FileSet {
    """
      package tests.importing

      import scala.collection.mutable.{ListBuffer => LB, _}
  
      object Main {val lb = LB(1) }
    """ becomes
    """
      package tests.importing

      import scala.collection.mutable.{ListBuffer => LB}
  
      object Main {val lb = LB(1) }
    """
  } applyRefactoring organizeExpand

  @Test
  def dontCollapseImports = new FileSet {
    """
      package tests.importing

      import scala.collection.mutable.ListBuffer
      import scala.collection.mutable.HashMap
  
      object Main {val lb = ListBuffer(1); val lb = HashMap(1 → 1) }
    """ becomes
    """
      package tests.importing

      import scala.collection.mutable.HashMap
      import scala.collection.mutable.ListBuffer
  
      object Main {val lb = ListBuffer(1); val lb = HashMap(1 → 1) }
    """
  } applyRefactoring organizeWithoutCollapsing
    
  @Test
  def collapse = new FileSet {
    """
      import java.lang.String
      import java.lang.Object
  
      object Main {val s: String = ""; var o: Object = null}
    """ becomes
    """
      import java.lang.{Object, String}
  
      object Main {val s: String = ""; var o: Object = null}
    """
  } applyRefactoring organize
    
  @Test
  def sortSelectors = new FileSet {
    """
      import java.lang.{String, Object}
  
      object Main {val s: String = ""; var o: Object = null}
    """ becomes
    """
      import java.lang.{Object, String}
  
      object Main {val s: String = ""; var o: Object = null}
    """
  } applyRefactoring organize
    
  @Test
  def sortAndCollapse = new FileSet {
    """
      import scala.collection.mutable.ListBuffer
      import java.lang.String
      import java.lang.Object
  
      object Main {val s: String = ""; var o: Object = null; val lb = ListBuffer(1)}
    """ becomes
    """
      import java.lang.{Object, String}
      import scala.collection.mutable.ListBuffer
  
      object Main {val s: String = ""; var o: Object = null; val lb = ListBuffer(1)}
    """
  } applyRefactoring organize
    
  @Test
  def collapseWithRename = new FileSet {
    """
      import java.lang.{String => S}
      import java.lang.{Object => Objekt}
  
      object Main {val s: String = ""; var o: Objekt = null}
    """ becomes
    """
      import java.lang.{Object => Objekt}
  
      object Main {val s: String = ""; var o: Objekt = null}
    """
  } applyRefactoring organize
    
  @Test
  def removeOneFromMany = new FileSet {
    """
      import java.lang.{String, Math}
  
      object Main {val s: String = ""}
    """ becomes
    """
      import java.lang.String
  
      object Main {val s: String = ""}
    """
  } applyRefactoring organize
    
  @Test
  def importAll = new FileSet {
    """
      import java.lang._
      import java.lang.String
  
      object Main
    """ becomes
    """
      
  
      object Main
    """
  } applyRefactoring organize
    
  @Test
  def importOnTrait = new FileSet {
    """
      package importOnTrait
      import java.lang._
      import java.lang.String
  
      trait A
  
      trait Main extends A {
      }
    """ becomes
    """
      package importOnTrait
      
  
      trait A
  
      trait Main extends A {
      }
    """
  } applyRefactoring organize
    
  @Test
  def importWithSpace = new FileSet {
    """
  
      import scala.collection.mutable.ListBuffer
      import java.lang.String
  
      object Main { val s: String = ""; val lb = ListBuffer("") }
    """ becomes
    """
  
      import java.lang.String
      import scala.collection.mutable.ListBuffer
  
      object Main { val s: String = ""; val lb = ListBuffer("") }
    """
  } applyRefactoring organize
    
  @Test
  def importAllWithRename = new FileSet {
    """
      import java.lang._
      import java.lang.{String => S}
  
      object Main { val s: String = "" }
    """ becomes
    """
      import java.lang.String
  
      object Main { val s: String = "" }
    """
  } applyRefactoring organize
    
  @Test
  def importRemovesUnneeded = new FileSet {
    """
      import java.lang._
      import java.lang.{String => S}
      import java.util.Map
      import scala.io.Source
      import scala.collection.mutable.ListBuffer

      object Main {
        val s: String = ""
        val l = ListBuffer(1,2,3)
        val l2 = List(1,2,3)
      }
    """ becomes
    """
      import java.lang.String
      import scala.collection.mutable.ListBuffer

      object Main {
        val s: String = ""
        val l = ListBuffer(1,2,3)
        val l2 = List(1,2,3)
      }
    """
  } applyRefactoring organize
    
  @Test
  def multipleImportsOnOneLine = new FileSet { 
      """
      import java.lang.String, String._
  
      object Main {
        val s: String = ""
        val s1 = valueOf(2);
      }    """ becomes """
      import java.lang.String.valueOf
      import java.lang.String
  
      object Main {
        val s: String = ""
        val s1 = valueOf(2);
      }    """
  } applyRefactoring organize
    
  @Test
  def importsInNestedPackages = new FileSet {
    """
       package outer
       package inner

       import scala.collection.mutable.ListBuffer
       import scala.collection.mutable.HashMap

       object Main {
         var hm: HashMap[String, String] = null
       }
      """ becomes """
       package outer
       package inner

       import scala.collection.mutable.HashMap

       object Main {
         var hm: HashMap[String, String] = null
       }
      """
  } applyRefactoring organize
    
  @Test
  def importFromPackageObject = new FileSet {
    """
    import scala.collection.breakOut
    import scala.collection.mutable.ListBuffer

    object TestbreakOut {
      val xs: Map[Int, Int] = List((1, 1), (2, 2)).map(identity)(breakOut)
    }
    """ becomes """
    import scala.collection.breakOut

    object TestbreakOut {
      val xs: Map[Int, Int] = List((1, 1), (2, 2)).map(identity)(breakOut)
    }
    """
  } applyRefactoring organize
  
  @Test
  def unusedImportWildcards = new FileSet {
    """
      import java.util._
      import scala.collection._
 
      object Main {
      }    """ becomes
    """
      
 
      object Main {
      }    """
  } applyRefactoring organize
  
  @Test
  def simplifyWildcards = new FileSet {
    """
      import scala.collection.mutable._
      import scala.collection.mutable.ListBuffer
 
      object Main {
        var x: ListBuffer[Int] = null
      }    """ becomes """
      import scala.collection.mutable.ListBuffer
 
      object Main {
        var x: ListBuffer[Int] = null
      }    """
  } applyRefactoring organize
  
  @Test
  def appliedType = new FileSet {
    """
      import scala.collection.mutable.HashMap
      import scala.collection.mutable.ListBuffer
 
      trait SomeTrait {
        def m: Either[String, ListBuffer[ListBuffer[String]]]
      }    """ becomes
    """
      import scala.collection.mutable.ListBuffer
 
      trait SomeTrait {
        def m: Either[String, ListBuffer[ListBuffer[String]]]
      }    """
  } applyRefactoring organize
  
  @Test
  def annotation = new FileSet {
    """
      import scala.reflect.BeanProperty
      case class JavaPerson(@BeanProperty var name: String, @BeanProperty var addresses: java.lang.Object)
    """ becomes
    """
      import scala.reflect.BeanProperty
      case class JavaPerson(@BeanProperty var name: String, @BeanProperty var addresses: java.lang.Object)
    """
  } applyRefactoring organize
  
  @Test
  def selfTypeAnnotation = new FileSet {
    """
      import java.util.Observer
      trait X {
        self: Observer =>
      }
    """ becomes
    """
      import java.util.Observer
      trait X {
        self: Observer =>
      }
    """
  } applyRefactoring organize
}
