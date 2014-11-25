package at.fabricate
package model

import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.S
import scala.xml.Node
import scala.xml.Elem
import net.liftweb.http.SHtml
import net.liftweb.http.FileParamHolder
import java.util.Calendar


/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MetaMegaProtoUser[User] with CreatedUpdated {
  override def dbTableName = "users" // define the DB table name
  override def screenWrap = Full(<lift:surround with="default" at="content">
			       <lift:bind /></lift:surround>)
  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, userImage, firstName, lastName, email,
  locale, timezone, password, aboutMe)
  
  // define the order fields will appear in the edit page
  override def editFields = List(userImage, firstName, lastName, email,
  locale, timezone, aboutMe)

  // comment this line out to require email validations
  override def skipEmailValidation = true
  
  // just an idea for different signup process
  //override def signupFields = email :: userName :: password :: Nil 
}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] with CreatedUpdated with LongKeyedMapper[User] with OneToMany[Long,User] with ManyToMany {
  def getSingleton = User // what's the "meta" server
  
  /*
   * 
   * override def firstNameDisplayName = "Vorname"
   * override def lastNameDisplayName = "Nachname"
   * 
   * 
   */

  // define an additional field for a personal essay
  object aboutMe extends MappedTextarea(this, 2048) {
    override def textareaRows  = 10
    override def textareaCols = 50
    // TODO  implement later, as Crudify and Megaprotouser can not be mixed in at the same time
    override def displayName = S.?("about\u0020me")
    	  /**Genutzter Spaltenname in der DB-Tabelle*/
    override def dbColumnName = "about_me"
  }
  
  object userImage extends MappedBinary(this) {
    
    val maxWidth = 600
    val maxHeight = 800
    val jpegQuality : Float = 85 / 100.toFloat // HINT: you have to use toFloat, otherwise it will result in 0 !!!
    
    //var fileHolder: Box[FileParamHolder]
    
    
        // TODO  implement later, as Crudify and Megaprotouser can not be mixed in at the same time
    override def displayName = S.?("user\u0020image")
    	  /**Genutzter Spaltenname in der DB-Tabelle*/
    override def dbColumnName = "user_image"
      
     //override def asHtml = 
     //override def toForm = SHtml.fileUpload(func, attrs)
      def setFromUpload(fileHolder: Box[FileParamHolder]) = 
      fileHolder.map(fu => this.set(fu.file))
      //S3Sender.uploadImageToS3(path, fileHolder).map(this.set(_))

  override def asHtml:Node = {
      if (this.get.length > 0)
      	<img src={"/serve/userimage/"+this.fieldOwner.id.get}  ></img>
      else
        <img src={"/images/nouser.png"}  ></img>
    }
    //style={"max-width:" + maxWidth + ";max-height:"+maxHeight}
  
  override def _toForm: Box[Elem] = Full(SHtml.fileUpload(fu=>setFromUpload(Full(fu)))) //fu=>setFromUpload(Full(fu)) setFromUpload(Full(fu))))

  }
  
  object tools extends MappedManyToMany(UserHasTools, UserHasTools.user, UserHasTools.tool, Tool)
  
  /*
    /**Datumsfeld fuer die Anmeldung des Users */
  object registrationDate extends MappedDateTime(this){
    /**Genutzter Spaltenname in der DB-Tabelle*/
    override def dbColumnName = "registration_date"  
    
    /**Name des Datenfeldes für CRUD-Seiten*/
    override def displayName = S.?("registration\u0020date")
    
    /**Liste der durchzufuehrenden Validationen*/  
    //override def validations =  FieldValidations.isValidDate(this) _ :: Nil
    //override def validSelectValues: Box[List[(Long, String)]] = 
    //	DependencyFactory.inject[Date].map(d => List[(Long,String)] (d.toString(), d.toGMTString()))
    override def defaultValue = Calendar.getInstance.getTime
  }
  */
  
  
  def fullCommaName: String = this.lastName + ", " + this.firstName
}

