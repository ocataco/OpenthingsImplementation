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
import sun.util.locale.provider.LocaleDataMetaInfo


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
  
   // some handy saving methodes
   final def save[T](item : Mapper[_], successAction : () => T, errorAction : List[FieldError] => T) : T = 
            item.validate match {
              case Nil => {
	            item.save	        	
	        	successAction()
              }
              case errors => {
                errorAction(errors)
              }
            }
                
   // TODO:
   // for a more detailed error message have a look at
   // https://groups.google.com/forum/#!topic/liftweb/4LCWldUaUVA
   final def saveAndDisplayAjaxMessages(item : Mapper[_], 
       successAction : () => JsCmd = () => JsCmds.Noop, 
       errorAction : List[FieldError] => JsCmd = errors => JsCmds.Noop, 
       idToDisplayMessages : String, 
       successMessage : String  = "Saved changes!", errorMessage: String  = "Error saving item!") : JsCmd =
		   save[JsCmd](item,
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

	final def saveAndDisplayMessages(item : Mapper[_], 
       successAction : () => Unit = () => Unit, 
       errorAction : List[FieldError] => Unit = errors => Unit, 
       idToDisplayMessages : String, 
       successMessage : String  = "Saved changes!", errorMessage: String  = "Error saving item!") : Unit =
		   save[Unit](item,
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
   
   final def saveAndRedirectToNewInstance[T](saveOp : ( Mapper[_], () => T,  List[FieldError] => T) => T, item : KeyedMapper[_,_], 
       successAction : () => T = () => (), 
       errorAction : List[FieldError] => T = (errors : List[FieldError]) => ()) : T = 
     saveOp(item, () => {
    	 S.redirectTo(urlToViewItem(item))
    	 successAction()
    	 },
    	 errors => errorAction(errors))

    def urlToViewItem(item: KeyedMapper[_,_]): String =  "/%s/%s/%s/%s".format(S.locale, itemBaseUrl, itemViewUrl, item.primaryKeyField.toString)
   
    def urlToEditItem(item: KeyedMapper[_,_]): String =  "/%s/%s/%s/%s".format(S.locale, itemBaseUrl, itemEditUrl, item.primaryKeyField.toString)

   // ### methods that might be overridden in subclasses ###
    	     	 
   // Problem with User: User Item with negative ID matches to the actual user (or is that just because the field was null?)
  val ID_NOT_SUPPLIED = "-1"

  // All the external methodes
  def renderIt (in: scala.xml.NodeSeq) : scala.xml.NodeSeq =   
    // just a dummy implementation
    // later user asHtml
   ("#item" #> page.map(item => asHtml(item) ) 
       ).apply(in)

  def create(xhtml: NodeSeq) : NodeSeq  = toForm(TheItem.create)(xhtml)
  
	
  def edit(xhtml: NodeSeq) : NodeSeq  =  { 
    doWithMatchedItem{
      item => toForm(item)
    }(xhtml)        
  }    	 
  
  def view(xhtml : NodeSeq) : NodeSeq  =  { 
    doWithMatchedItem{
      item => asHtml(item)
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
     val locale = S.locale
     "#title"  #> item.title(locale).toForm &
     "#teaser"  #> item.teaser(locale).toForm &
     "#description"  #> item.description(locale).toForm &
     "#formitem [action]" #> urlToEditItem(item) &
     "#itemsubmithidden" #> SHtml.hidden(() => saveAndRedirectToNewInstance(saveAndDisplayMessages(_,_:()=>Unit,_:List[FieldError]=>Unit, "itemMessages") , item))

//     "#created *"  #> item.createdAt  &
//     "#updated *"  #> item.updatedAt  &
//     "#edititem [href]" #> urlToEditItem(item) &
//     "#viewitem [href]" #> urlToViewItem(item) &
//     "#viewitem *" #> "View Item"
  }
  
  def asHtml(item : ItemType) : CssSel = {
    
//    println("chaining asHtml from BaseEntitySnippet")
    
     val locale = S.locale
     //"#translations" #> item.translations.map(_.title.asHtml) &
     //"#language" #> locale.toString() & //LocaleDataMetaInfo.getSupportedLocaleString(locale)
     "#title *"  #> item.title(locale).asHtml &
     "#teaser *"  #> item.teaser(locale).asHtml &
     "#description *"  #> TextileParser.toHtml(item.description(locale).get) &
     "#created *+"  #> item.createdAt.asHtml  &
     "#updated *+"  #> item.updatedAt.asHtml  &
     "#edititem [href]" #> urlToEditItem(item) &
     "#viewitem [href]" #> urlToViewItem(item) &
     "#viewitem *" #> "View Item"
  }
}
