package at.fabricate.liftdev.common
package snippet

import net.liftweb.http.DispatchSnippet
import net.liftweb.common.Logger
import model.MatchByID
import net.liftweb.mapper.Descending
import net.liftweb.mapper.StartAt
import net.liftweb.mapper.MaxRows
import net.liftweb.mapper.OrderBy
import scala.xml.NodeSeq
import net.liftweb.http.S
import scala.xml.Text
import net.liftweb.util.FieldError
import net.liftweb.mapper.Mapper
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds.DisplayMessage
import net.liftweb.mapper.KeyedMapper
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.util.CssSel
import model.BaseMetaEntityWithTitleAndDescription
import net.liftweb.util.CssBind
import net.liftweb.util.CssBindImpl
import net.liftweb.util.CSSHelpers
import net.liftweb.http.SHtml
import net.liftweb.http.RewriteRequest
import net.liftweb.http.ParsePath
import net.liftweb.http.RewriteResponse
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc.Hidden
import net.liftweb.common.Full
import net.liftweb.common.Empty
import lib.MatchString
import model.BaseEntityWithTitleAndDescription
import net.liftmodules.textile.TextileParser
import java.util.Locale
import at.fabricate.liftdev.common.lib.UrlLocalizer
import net.liftweb.http.RedirectResponse
import net.liftweb.common.Box
import at.fabricate.liftdev.common.model.TheGenericTranslation
import net.liftweb.http.RequestVar
import net.liftweb.http.SessionVar
import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.http.js.JsCmds.SetHtml
import scala.xml.Elem
import net.liftweb.http.js.JsCmds.Replace


abstract class BaseEntityWithTitleAndDescriptionSnippet[T <: BaseEntityWithTitleAndDescription[T]] extends AjaxPaginatorSnippet[T] with DispatchSnippet with Logger {

  // ### Things that have to be defined/refined in subclasses/traits ###
     type ItemType = T
    
     val TheItem : BaseMetaEntityWithTitleAndDescription[T] with MatchByID[T]
    
//    val TheItem = ItemType .getSingleton
    
    def itemBaseUrl = "item"    
      
    def itemViewUrl = "view"    
      
    def itemListUrl = "list"
      
//    val itemAddUrl = "add"
      
    def itemEditUrl = "edit"
      
    def viewTemplate = "viewItem"
      
    def listTemplate = "listItem"
      
    def editTemplate = "editItem"
      
    def viewTitle = "View Item"
      
    def listTitle = "List Item"
      
    def editTitle = "Edit Item"
  
  object MatchItemPath extends MatchString(itemBaseUrl)
  
  object MatchView extends MatchString(itemViewUrl)
  
  object MatchList extends MatchString(itemListUrl)

  object MatchEdit extends MatchString(itemEditUrl)
     
     
  
  val contentLanguage : RequestVar[Locale]  
     
  object unsavedContent extends SessionVar[Box[ItemType]](Empty){
       override protected def onShutdown(session: CleanUpParam): Unit = {
    		   println("shutdown session for sessionvar executed")
       }
     }

   //### methods that are fix ###
   final def dispatch : DispatchIt = localDispatch // orElse super.dispatch
   
   final def displayMessageAndHide(idToDisplayMessage : String, message : String, classAttr : String = "message" ) : JsCmd = {
    DisplayMessage(idToDisplayMessage, <span class={classAttr}>{message}</span>, 5 seconds, 1 second)
  }
   
   // generate the url rewrites
   def generateRewrites : PartialFunction[RewriteRequest,RewriteResponse] =  {
      case RewriteRequest(ParsePath(List(MatchItemPath(itemBasePath), "index"), _, _, _), _, _) =>
	      RewriteResponse(listTemplate :: Nil)
	  case RewriteRequest(ParsePath(List(MatchItemPath(itemBasePath), MatchList(listPath)), _, _, _), _, _) =>
	      RewriteResponse(listTemplate :: Nil)
	  case RewriteRequest(ParsePath(List(MatchItemPath(itemBasePath), MatchEdit(editPath), AsLong(itemID)), _, _, _), _, _) =>
	      RewriteResponse(editTemplate :: Nil, Map("id" -> urlDecode(itemID.toString)))
	  case RewriteRequest(ParsePath(List(MatchItemPath(itemBasePath), MatchEdit(editPath)), _, _, _), _, _) =>
	      RewriteResponse(editTemplate :: Nil)
	  case RewriteRequest(ParsePath(List(MatchItemPath(itemBasePath), MatchView(viewPath), AsLong(itemID)), _, _, _), _, _) =>
	      RewriteResponse(viewTemplate :: Nil, Map("id" -> urlDecode(itemID.toString)))
     }
     
   def getMenu : List[Menu] = 
     List[Menu](
               Menu.i(viewTitle) / viewTemplate  >> Hidden,
               Menu.i(listTitle) / listTemplate ,
               Menu.i(editTitle) / editTemplate  >> Hidden
     )

   // lean pattern to get the Item from the supplied ID
   final def doWithMatchedItem(op : ItemType => ((NodeSeq) => NodeSeq) ) : ((NodeSeq) => NodeSeq) = 
    (S.param("id") openOr ID_NOT_SUPPLIED) match {
      case TheItem.MatchItemByID(item) => op(item)
      case _ => (node => Text("Object not found!"))
    
  }
   
   def doSave[T <: Mapper[T]](item : T) : Any = {
     item.save
   }
  
   // some handy saving methodes
   final def save[T,U <: Mapper[U]](item : U, successAction : () => T, errorAction : List[FieldError] => T) : T = 
            item.validate match {
              case Nil => {
	            doSave(item)	        	
	        	successAction()
              }
              case errors => {
                errorAction(errors)
              }
            }
                
   // TODO:
   // for a more detailed error message have a look at
   // https://groups.google.com/forum/#!topic/liftweb/4LCWldUaUVA
   final def saveAndDisplayAjaxMessages[T <: Mapper[T]](item : T, 
       successAction : () => JsCmd = () => JsCmds.Noop, 
       errorAction : List[FieldError] => JsCmd = errors => JsCmds.Noop, 
       idToDisplayMessages : String, 
       successMessage : String  = "Saved changes!", errorMessage: String  = "Error saving item!") : JsCmd =
		   save[JsCmd,T](item,
		    () => {
		      // TODO: maybe decide which one to use?
		      // S.xxx or DisplayMessage
			   S.notice(successMessage)
			   displayMessageAndHide(idToDisplayMessages, successMessage) &
			   successAction()
		     },
		     errors => {
		      // TODO: maybe decide which one to use?
		      // S.xxx or DisplayMessage
		       S.error(errorMessage)
               S.error(errors)
               displayMessageAndHide(idToDisplayMessages, errorMessage, "message error") &
               errorAction(errors)
                }
		     )

	final def saveAndDisplayMessages[T <: Mapper[T]](item : T, 
       successAction : () => Unit = () => Unit, 
       errorAction : List[FieldError] => Unit = errors => Unit, 
       idToDisplayMessages : String, 
       successMessage : String  = "Saved changes!", errorMessage: String  = "Error saving item!") : Unit =
		   save[Unit,T](item,
		    () => {
			   S.notice(successMessage)
			   successAction()
		     },
		     errors => {
		       S.error(errorMessage)
               S.error(errors)
               errorAction(errors)
                }
		     )
   
   final def saveAndRedirectToNewInstance[T, U <: LongKeyedMapper[U]](saveOp : ( U, () => T,  List[FieldError] => T) => T, item : U, 
       successAction : () => T = () => (), 
       errorAction : List[FieldError] => T = (errors : List[FieldError]) => ()) : T = 
     saveOp(item, () => {
    	 S.redirectTo(urlToViewItem(item))
    	 successAction()
    	 },
    	 errors => errorAction(errors))

    def urlToViewItem(item: KeyedMapper[_,_], locale : Locale): String =  "/%s/%s/%s/%s".format(locale.getLanguage, itemBaseUrl, itemViewUrl, item.primaryKeyField.toString)
   
    def urlToEditItem(item: KeyedMapper[_,_], locale : Locale): String =  "/%s/%s/%s/%s".format(locale.getLanguage, itemBaseUrl, itemEditUrl, item.primaryKeyField.toString)

    def urlToViewItem(item: KeyedMapper[_,_]): String =  urlToViewItem(item,S.locale)
   
    def urlToEditItem(item: KeyedMapper[_,_]): String =  urlToEditItem(item,S.locale)
    
   // ### methods that might be overridden in subclasses ###
    	     	 
   // Problem with User: User Item with negative ID matches to the actual user (or is that just because the field was null?)
  val ID_NOT_SUPPLIED = "-1"

  // All the external methodes
  def renderIt (in: scala.xml.NodeSeq) : scala.xml.NodeSeq =   
    // just a dummy implementation
    // later user asHtml
   ("#item" #> page.map(item => asHtml(item) ) 
       ).apply(in)
       
  def loadItemFromSessionOrCreate : ItemType = 
    unsavedContent.get.openOr({
    	val newItem = TheItem.createNewEntity(contentLanguage.get)
	     unsavedContent.set(Full(newItem))
	    newItem
    })
//     if (unsavedContent.get.isEmpty){
//	     val newItem = TheItem.createNewEntity(contentLanguage.get)
//	    //newItem.translations += newItem.TheTranslationMeta.create.translatedItem(newItem)
//	     unsavedContent.set(Full(newItem))
//	    newItem
//    } else {
//    	unsavedContent.get.get
//    }

  def create(xhtml: NodeSeq) : NodeSeq  = 
    	(
    	toForm(loadItemFromSessionOrCreate) &
		  "#defaultlanguage"  #> "" &
		  "#defaultlanguagelabel"  #> ""
    	).apply(xhtml)
  
  
	
  def edit(xhtml: NodeSeq) : NodeSeq  =  { 
    doWithMatchedItem{
      item => toForm(item)
    }(xhtml)        
  }    	 
  
  def view(xhtml : NodeSeq) : NodeSeq  =  { 
    doWithMatchedItem{
      item => {
        //val locale = UrlLocalizer.getContentLocale
        if (item.getTranslationForItem(contentLanguage)!=Empty)
        	asHtml(item)
        else S.redirectTo(urlToViewItem(item,item.defaultTranslation.getObjectOrHead.language.isAsLocale))
      }
    }(xhtml) 
  }
   
     // define the page
  override def count = TheItem.count

  override def page = TheItem.findAll(StartAt(curPage*itemsPerPage), MaxRows(itemsPerPage), OrderBy(TheItem.primaryKeyField, Descending))

   
   // ### methods that will be stacked ###
   def localDispatch : DispatchIt = {    
    case "list" => renderIt(_)
    case "renderIt" => renderIt(_)
    case "edit" => edit _
    case "create" => create _
    case "view" => view(_)
    case "paginate" => paginate _
    case "paginatecss" => paginatecss(_)
  }
  
  
//   final def getLocaleForItem(item : ItemType ) : String = 
//    (S.param("lang") openOr(item.defaultTranslation.get)) 
   
     // internal helper fields that will be chained to create the complete css selector
  //   subclasses will implement that methodes with abstract override
  // this is the selector does nothing hopefully
  def toForm(item : ItemType) : CssSel = {
  
//      println("chaining toForm from BaseEntitySnippet")
    
    //item.translations.head.language.get
    
    //this is the locale the user wanted to have/get/see
     val locale = S.locale 
     // default behaviour, as only one locale exisits atm!
     var translation : TheGenericTranslation = item.getTranslationForItem(contentLanguage).
     	openOr(item.getNewTranslation)
     	    //TheTranslationMeta.create.translatedItem(item).language(UrlLocalizer.getContentLocale.toString).saveMe)
     
    def elem2NodeSeq(element : Box[Elem]) : NodeSeq = {
       element match {
         case Full(anElement) => NodeSeq.fromSeq(List(anElement))//anElement.
         case _ => NodeSeq.Empty 
       }
     }
     	
     def createNewTranslationForItem(localItem : ItemType)() : JsCmd = {
       translation = localItem.getNewTranslation
       println("new translation created")
       Replace("title",elem2NodeSeq(translation.title.toForm)) & 
       Replace("teaser",elem2NodeSeq(translation.teaser.toForm)) & 
       Replace("description",elem2NodeSeq(translation.description.toForm)) & 
       Replace("language",elem2NodeSeq(translation.language.toForm))
     }
     	
     "#title"  #> translation.title.toForm &
     "#teaser"  #> translation.teaser.toForm &
     "#description"  #> translation.description.toForm &
     "#language"  #> translation.language.toForm &
     "#newlanguage"  #> SHtml.a(Text("create new translation"),createNewTranslationForItem(item),  "id"->"newlanguage") &//translation.language.toForm &
     "#defaultlanguage"  #> item.defaultTranslation.toForm &
     "#formitem [action]" #> urlToEditItem(item) &
     "#itemsubmithidden" #> SHtml.hidden(() => {
       //translation.save
       saveAndRedirectToNewInstance(saveAndDisplayMessages(_:ItemType,_:()=>Unit,_:List[FieldError]=>Unit, "itemMessages") , item)
     })

//     "#created *"  #> item.createdAt  &
//     "#updated *"  #> item.updatedAt  &
//     "#edititem [href]" #> urlToEditItem(item) &
//     "#viewitem [href]" #> urlToViewItem(item) &
//     "#viewitem *" #> "View Item"
  }
    
		  //item.getTranslationForLocales(List(UrlLocalizer.getContentLocale), item.translations.head)
  
//  def getShortInfoForItem(item : ItemType)(translation : item.TheTranslation ) = {
//     //val translation : item.TheTranslation = getTranslationForItem(item)
//     if (translation.language.isAsLocale == UrlLocalizer.getContentLocale)
//       Text(translation.title.get)
//     else
//       Text(translation.title.get +" (%s)".format(translation.language.isAsLocale.getDisplayLanguage))
//  }
  def getShortInfoForItem(item : ItemType) = item.doDefaultWithTranslationFor(contentLanguage)
    
//out.println( GERMANY.getCountry() );         // DE
//out.println( GERMANY.getLanguage() );        // de
//out.println( GERMANY.getVariant() );         //
//out.println( GERMANY.getDisplayCountry() );  // Deutschland
//out.println( GERMANY.getDisplayLanguage() ); // Deutsch
//out.println( GERMANY.getDisplayName()  );    // Deutsch (Deutschland)
//out.println( GERMANY.getDisplayVariant() );  //
//out.println( GERMANY.getISO3Country() );     // DEU
//out.println( GERMANY.getISO3Language() );    // deu
  
  
  // TODO: write that code!
  
  def ensureMinimumLenghOfElement(element : String, length: Int, before : String = "", behind : String = "") : String = {
    if (element == null ) "%s %s %s".format(before, "no title available", behind)
    else if (element.length() < length) "%s %s %s".format(before, element, behind)
    else element
  }
  
    def getShortInfoAndLinkToItem(item : ItemType) = //(translation : TheGenericTranslation) = 
      item.doWithTranslationFor(contentLanguage)(
          foundTranslation => <a href={urlToViewItem(item,foundTranslation.language.isAsLocale)} >{ensureMinimumLenghOfElement(foundTranslation.title.get, 10, "Title: ")}</a>
          )(
              defaultTranslation => <a href={urlToViewItem(item,defaultTranslation.language.isAsLocale)} >{"%s (%s)".format(ensureMinimumLenghOfElement(defaultTranslation.title.get, 10, "Title: "),defaultTranslation.language.get)}</a>
              )(<span>no translation found</span>)
//    		<a href={urlToViewItem(item,translation.language.isAsLocale)} >{item.doDefaultWithTranslationFor(contentLanguage)}</a>

    def getLinksToAllTranslations(localItem : ItemType) = {
      localItem.translations.map(itemTranslation => 
//        if (localItem.defaultTranslation.obj.map(_.id == itemTranslation.id).openOr(false))
//        	<a href={urlToViewItem(localItem,itemTranslation.language.isAsLocale)} >{ensureMinimumLenghOfElement(itemTranslation.title.asHtml, 10, "Title: ")}</a>
//        	else 
        	  <a href={urlToViewItem(localItem,itemTranslation.language.isAsLocale)} >{ itemTranslation.title.asString;" (%s)".format(itemTranslation.language.isAsLocale.getDisplayLanguage())}</a>
        	  )
    }
  
  def asHtml(item : ItemType) : CssSel = {
    
//    println("chaining asHtml from BaseEntitySnippet")	at.fabricate.liftdev.common.snippet.BaseEntityWithTitleAndDescriptionSnippet.ensureMinimumLenghOfElement(BaseEntityWithTitleAndDescriptionSnippet.scala:357)
    
     val locale = S.locale
     
     val translationBox = item.getTranslationForItem(contentLanguage)
     translationBox match {
       case Full(translation) => {
         val description = translation.description.get match {
           case null => NodeSeq.Empty 
           case _ => TextileParser.toHtml(translation.description.get)
         }
         "#translations *" #> getLinksToAllTranslations(item) &
	     "#language *" #> translation.language.isAsLocale.getDisplayLanguage & //LocaleDataMetaInfo.getSupportedLocaleString(locale)
	     "#shortinfo" #> getShortInfoForItem(item)  &
	     "#title *"  #> translation.title.asHtml &
	     "#teaser *"  #> translation.teaser.asHtml &
	     "#description *"  #> description &
	     "#created *+"  #> item.createdAt.asHtml  &
	     "#updated *+"  #> item.updatedAt.asHtml  &
	     "#edititem [href]" #> urlToEditItem(item,translation.language.isAsLocale) &
	     "#viewitem [href]" #> urlToViewItem(item,translation.language.isAsLocale) &
	     "#viewitem *" #> "View Item"
       }
       case _ => {
         // not the wanted content found!
         val translation = item.defaultTranslation.getObjectOrHead // could be extended to use the users default maybe?
         "#translations *" #> item.translations.map(itemTranslation => getShortInfoAndLinkToItem(item)) &
	     "#language *" #> translation.language.isAsLocale.getDisplayLanguage & //LocaleDataMetaInfo.getSupportedLocaleString(locale)
	     "#shortinfo" #> getShortInfoForItem(item)  &
	     "#created *+"  #> item.createdAt.asHtml  &
	     "#updated *+"  #> item.updatedAt.asHtml  &
	     "#edititem [href]" #> urlToEditItem(item,translation.language.isAsLocale) &
	     "#viewitem [href]" #> urlToViewItem(item,translation.language.isAsLocale) &
	     "#viewitem *" #> "View Item"
       }
     }

     
  }
}
