package at.fabricate.liftdev.common
package lib

import net.liftweb._
import net.liftweb.http._
import net.liftweb.http.provider._
import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import java.util.Locale

object UrlLocalizer {
  // capture the old localization function
  val oldLocalizeFunc = LiftRules.localeCalculator
  
  var isInitialized = false
  
  
  // will be used if we can find no locale!
  val defaultLocale = Locale.ENGLISH

  // will go to the requestLocale once it is set
  @volatile 
  object sessionSiteLocale extends SessionVar[Box[Locale]](Empty)
  
  // will be used to display content in different
  object contentLocale extends RequestVar(defaultLocale)

  /**
   * What are the available locales?
   */
  val locales: Map[String, Locale] =
    Map(Locale.getAvailableLocales.map(l => l.getLanguage -> l) :_*)


  val allLanguages = Locale.getISOLanguages().toList.map(new Locale(_))

  /**
   * Extract the locale
   */
  def unapply(in: String): Option[Locale] = locales.get(in)

  /**
   * Calculate the Locale
   */
  def getContentLocale : Locale = 
    if (contentLocale.set_?) {
      contentLocale.get
    }
    else {
      defaultLocale 
    }
  
  def setSiteLocale(aLocale : Box[Locale]) = sessionSiteLocale.set(aLocale)
    
  def getSiteLocale : Locale = 
    sessionSiteLocale.openOr(defaultLocale )
    
  def calcLocaleFromURL(in: Box[HTTPRequest]): Locale = getContentLocale
  

  def calcLocaleFromRequest(request: Box[HTTPRequest]) : Locale =
    sessionSiteLocale.openOr {
      val requestLocale = 
      (for {
        listOfLanguages <- tryo(S.getRequestHeader("Accept-Language").get.split(Array(',')).toList.map(_.split(Array('_', '-'))))
      } yield listOfLanguages.map {
        case Array(lang) => new Locale(lang)
        case Array(lang, country) => new Locale(lang, country)
      })

      requestLocale match {
        case Full(aListOfLocales) => {
          val useLocale = aListOfLocales.head
          println("requested locales: "+aListOfLocales.mkString(","))
          println("use locale: "+useLocale.getDisplayLanguage(useLocale))
          setSiteLocale(Full(useLocale)); useLocale
        }
        case _ => {
          println("no locale in request")
          Locale.getDefault
        }
      }
    }

  /**
   * Initialize the locale
   */
  def init() {
    // hook into Lift
    LiftRules.localeCalculator = calcLocaleFromRequest _
    

    // rewrite requests with a locale at the head
    // of the path
    LiftRules.statelessRewrite.append {
      case RewriteRequest(ParsePath(UrlLocalizer(locale) :: rest, _, _, _), _, _) => {
        contentLocale.set(locale)
        RewriteResponse(rest)
      }
    }
    isInitialized = true
  }
}