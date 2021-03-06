package backend.data.mongodb.dao

import backend.data.dao.PostDaoComponent
import backend.data.mongodb.service.UserDataServiceMongo
import backend.data.service.UserDataService
import com.mongodb.BasicDBList
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.commons.{ MongoDBList, MongoDBObject }
import com.mongodb.DBObject
import model.blog.Post
import org.bson.types.ObjectId
import org.joda.time.DateTime
import scala.Array.canBuildFrom

/**
 * A trait implementing the PostDaoComponent for MongoDB
 *
 * @author Stefan Bleibinhaus
 *
 */
trait PostDaoComponentMongo extends PostDaoComponent {
  override val dao = PostDaoMongo

  object PostDaoMongo extends BaseDaoMongo[Post] with PostDao {
    override protected def collectionName = "posts"
    protected val userService: UserDataService = UserDataServiceMongo

    RegisterJodaTimeConversionHelpers()

    override protected def writeConverter(post: Post): DBObject = {
      val objBuilder = MongoDBObject.newBuilder
      post.id foreach {
        id ⇒ objBuilder += ("_id" -> new ObjectId(id))
      }
      objBuilder += ("relUrl" -> post.relUrl)
      objBuilder += ("title" -> post.title)
      objBuilder += ("author" -> post.author.id)
      objBuilder += ("created" -> post.created)
      objBuilder += ("updated" -> post.updated)
      objBuilder += ("text" -> post.text)
      val tagsBuilder = MongoDBList.newBuilder
      for (tag ← post.tags)
        tagsBuilder += tag
      objBuilder += ("tags" -> tagsBuilder.result)
      objBuilder += ("listed" → post.listed)
      objBuilder += ("showUpdated" → post.showUpdated)
      objBuilder.result
    }

    override protected def readConverter(dbObject: DBObject): Post = {
      val tagObjs =
        if (dbObject.get("tags").isInstanceOf[BasicDBList])
          dbObject.get("tags").asInstanceOf[BasicDBList].toArray
        else
          dbObject.get("tags").asInstanceOf[MongoDBList].toArray
      val tags =
        for (tagObj ← tagObjs)
          yield tagObj.toString()
      val listed =
        dbObject.containsField("listed") match {
          case true  ⇒ dbObject.get("listed").asInstanceOf[Boolean]
          case false ⇒ true
        }
      val showUpdated =
        dbObject.containsField("showUpdated") match {
          case true  ⇒ dbObject.get("showUpdated").asInstanceOf[Boolean]
          case false ⇒ true
        }
      new Post(Some(dbObject.get("_id").toString),
        dbObject.get("relUrl").toString,
        dbObject.get("title").toString,
        userService.get(dbObject.get("author").toString).get,
        dbObject.get("created").asInstanceOf[DateTime],
        dbObject.get("updated").asInstanceOf[DateTime],
        dbObject.get("text").toString,
        tags.toList,
        listed,
        showUpdated)
    }

    override protected def insertId(post: Post, id: String): Post = post.copy(id = Some(id))
  }
}
