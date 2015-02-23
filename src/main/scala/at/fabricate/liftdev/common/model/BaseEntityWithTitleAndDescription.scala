package at.fabricate.liftdev.common
package model

import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedTextarea
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.CreatedUpdated
import net.liftweb.mapper.OneToMany
import net.liftweb.mapper.MappedLongForeignKey
import net.liftweb.mapper.Ascending
import net.liftweb.mapper.MappedLocale
import net.liftweb.mapper.BaseMetaMapper
import java.util.Locale
import net.liftweb.common.Full
import net.liftweb.common.Box
import net.liftweb.common.Full
import net.liftweb.common.Empty
import net.liftweb.mapper.OrderBy
import net.liftweb.mapper.LongMappedMapper
import net.liftweb.mapper.By
import net.liftweb.util.FieldError
import at.fabricate.liftdev.common.lib.MappedLanguage

// This is the basic database entity 
// every db object should inherit from that
// specially needed for tags and skills that dont have an icon
// TODO: maybe even tags and skills want to have an icon?

trait BaseEntityWithTitleAndDescription [T <: (BaseEntityWithTitleAndDescription[T]) ] extends BaseEntity[T] with MatchByID[T] with CreatedUpdated with OneToMany[Long, T]
{
  self: T =>
        
    val titleLength = 60
    val titleValidations : List[String => List[FieldError]] = List( _ => List[FieldError]())
    val teaserLength = 150
    val teaserValidations : List[String => List[FieldError]] = List( _ => List[FieldError]())
    val descriptionLength = 2000
    val descriptionValidations : List[String => List[FieldError]] = List( _ => List[FieldError]())

   
    object defaultTranslation extends LongMappedMapper(this, TheTranslationMeta){
    	override def validSelectValues =
    			Full(TheTranslationMeta.findMap(
    					By(TheTranslationMeta.translatedItem,self.primaryKeyField),
    					OrderBy(TheTranslationMeta.id, Ascending)){
    					case t: TheTranslation => Full(t.id.get -> "%s (%s)".format(t.title,t.language.isAsLocale.getDisplayLanguage))
    			})
    	override def dbNotNull_? = true
    	def getObjectOrHead : self.TheTranslation = obj.getOrElse(self.translations.head)
    }
    
    object translationToSave extends LongMappedMapper(this, TheTranslationMeta)

	def getTranslationMapper : LongKeyedMetaMapper[_] = TheTranslationMeta
	
	  object translations extends MappedOneToMany(TheTranslationMeta, TheTranslationMeta.translatedItem) with Owned[TheTranslation]
with Cascade[TheTranslation]
	
      
	class TheTranslation extends BaseEntity[TheTranslation] with LongKeyedMapper[TheTranslation] with IdPK with TheGenericTranslation {
	  
		  type TheTranslationType = TheTranslation
		  type TheTranslatedItem = T
	  
    	  def getSingleton = TheTranslationMeta
    	  
    	  object translatedItem extends MappedLongForeignKey(this,self.getSingleton)

    	  object language extends MappedLanguage(this)
    	  
    	  object title extends MappedString(this, titleLength){
		    override def validations = titleValidations 
		  }
    	  object teaser extends MappedTextarea(this, teaserLength){
		    override def validations = teaserValidations 
		  }
    	  object description extends MappedTextarea(this, descriptionLength){
		    override def validations = descriptionValidations 
		  }
    	  
	}

	
	object TheTranslationMeta  extends TheTranslation with BaseMetaEntity[TheTranslation] with LongKeyedMetaMapper[TheTranslation] {
	  	  override def dbTableName =  self.getSingleton.dbTableName+"_translation"
	  	  
	  	  // Bugfix for the compilation issue
	  	  // solution by https://groups.google.com/forum/#!msg/liftweb/XYiKeS_wgjQ/KBEcrRZxF4cJ
	  	  override def createInstance = new TheTranslation
	  	  
	  	  // Hint by David Pollak on https://groups.google.com/forum/#!topic/liftweb/Rkz06yng-P8
	  	  override def getSingleton = this
	}
	// some convenience methodes to do things in 3 different states (although state notFound might not occur because of notNull)
	def doWithTranslationFor[V](languageExpected:Locale)(foundAction : TheGenericTranslation => V)(defaultTranslationAction : TheGenericTranslation => V)(notFound : V) : V = 
	  getTranslationForItem(languageExpected) match {
                 case Full(translationFound) => foundAction(translationFound)
                 case Empty => this.defaultTranslation.obj match {
                   case Full(defaultTranslationFound) => defaultTranslationAction(defaultTranslationFound)
                   case Empty => notFound
                 }
	  }
	
	val shortTitleTranslationFound = "%s"
	val shortTitleDefaultTranslationFound = "%s (no translation found, using %s)"
	val noTranslationFound = "No translation information for this item found"
	
	def doDefaultWithTranslationFor(expectedLanguage:Locale) : String = doWithTranslationFor(expectedLanguage)(
           translationFound => shortTitleTranslationFound.format(translationFound.title.get)
           )(
           defaultTranslation => shortTitleDefaultTranslationFound.format(defaultTranslation.title.get,defaultTranslation.language.isAsLocale.getDisplayLanguage)
           )(noTranslationFound)
	
	def getTranslationForItem(language : Locale) : Box[TheTranslation] = this.translations.find(_.language.isAsLocale.getLanguage() == language.getLanguage())

	def getNewTranslation(language : Locale) : TheTranslation = {
	  val translation = this.TheTranslationMeta.create.language(language.toString).translatedItem(this)
	  //this.translations += translation
	  this.translationToSave(translation)
	  translation
	}
	
	lazy val theEmptyTranslation = this.TheTranslationMeta.create.language("en_EN").translatedItem(this).title("NO TRANSLATION FOUND").description("NO TRANSLATION FOUND").teaser("NO TRANSLATION FOUND")

    // special save for MySQL - enforces saving of translation before the entity can be saved 
    abstract override def save = {
      // save the default translation
      this.defaultTranslation.obj.map(_.save)
      // save the unsaved translation
      this.translationToSave.obj.map(aTranslation => {
        aTranslation.save
        if (this.translations.find(_.id == aTranslation.id)==Empty)
          this.translations += aTranslation
      }
          )
      // save all the other translations
      //this.translations.map(_.save)
      super.save
    }
}

trait BaseMetaEntityWithTitleAndDescription[ModelType <: ( BaseEntityWithTitleAndDescription[ModelType]) ] extends BaseMetaEntity[ModelType] with CreatedUpdated
{
    self: ModelType  =>
      	  	  abstract override def getItemsToSchemify : List[BaseMetaMapper] =  getTranslationMapper :: super.getItemsToSchemify
	// some handy and secure create and save methodes
	def createNewEntity(language : Locale,title : String = null, teaser : String = null, description: String = null) : ModelType = {
	      val newItem = this.create
          // create a new translation with the submitted language
          val translation = newItem.TheTranslationMeta.create.language(language.toString)//.saveMe
          translation.title(title).teaser(teaser).description(description).translatedItem(newItem)//.save
          // append the new translation to the translations
          newItem.translations += translation
          // make this translation the default
          newItem.defaultTranslation(translation)//.saveMe
	}
}
