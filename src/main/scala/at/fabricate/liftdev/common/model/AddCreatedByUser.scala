package at.fabricate.liftdev.common
package model

import net.liftweb.mapper.KeyedMapper
import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.KeyedMetaMapper
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedLongForeignKey
import net.liftweb.mapper.Mapper
import net.liftweb.mapper.OneToMany
import net.liftweb.mapper.OrderBy
import net.liftweb.mapper.Ascending
import net.liftweb.mapper.Schemifier
import net.liftweb.mapper.MappedLong
import net.liftweb.mapper.MetaMapper
import net.liftweb.mapper.BaseLongKeyedMapper
import net.liftweb.mapper.MappedForeignKey
import net.liftweb.mapper.BaseMetaMapper
import net.liftweb.mapper.ProtoUser
import net.liftweb.common.Box
import net.liftweb.mapper.MappedInt
import net.liftweb.mapper.MegaProtoUser
import net.liftweb.mapper.MetaMegaProtoUser

trait AddCreatedByUser[T <: (AddCreatedByUser[T]) ] extends BaseEntity[T]  with OneToMany[Long, T] { // 
	self: T =>
	  
	  def getCurrentUser : Box[ProtoUser[TheUserType]]
	  
	  type TheUserType <: MegaProtoUser[TheUserType] with BaseEntityWithTitleAndDescription[TheUserType]
	  
	  def theUserObject : MetaMegaProtoUser[TheUserType] with BaseMetaEntityWithTitleAndDescription[TheUserType]
	              
        object createdByUser extends MappedLongForeignKey(this, theUserObject){

		    override def defaultValue = theUserObject.currentUser.map(_.primaryKeyField.get ) openOr(-1)
		    
			  /**Genutzter Spaltenname in der DB-Tabelle*/
		    override def dbColumnName = "initiator"
		    
		    
		  }
	       

      
}

trait AddCreatedByUserMeta[ModelType <: (AddCreatedByUser[ModelType]) ] extends BaseMetaEntity[ModelType] {
	self: ModelType =>

}
