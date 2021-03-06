package backend.data.service

import model.blog.PostEssential
import model.blog.{ Post, PostEnriched }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future, blocking, future }

/**
 * The data service trait for enriched posts
 *
 * @author Stefan Bleibinhaus
 *
 */
trait PostEnrichedDataService extends PostChangeListener {
  protected var enrichedPosts: List[PostEnriched] = Nil
  protected var listedEnrichedPosts: List[PostEnriched] = Nil

  postDataService.addPostChangeListener(PostEnrichedDataService.this)
  initEnrichedPosts()

  /**
   * Gets a specifed part of listed enriched posts
   *
   * @param from Excluded
   * @param to Included
   * @return
   */
  def get(from: Int, to: Int): List[PostEnriched] =
    listedEnrichedPosts.drop(from).take(to - from)

  /**
   * @param n
   * @return True, if there are more listed enriched posts known than the given integer n
   */
  def hasMoreThan(n: Int): Boolean = listedEnrichedPosts.size > n

  /**
   * Gets an enriched post by its relative Url (listed or not)
   *
   * @param relUrl
   * @return
   */
  def getByRelUrl(relUrl: String): Option[PostEnriched] =
    enrichedPosts.find(_.relUrl == relUrl)

  /**
   * Gets listed enriched posts by a tag
   *
   * @param tag
   * @return
   */
  def getByTag(tag: String): List[PostEnriched] =
    listedEnrichedPosts.filter(_.tags.contains(tag))

  /**
   * Gets listed enriched posts by date
   *
   * @param year
   * @param month Optional
   * @return
   */
  def getByDate(year: Long, month: Option[Long] = None): List[PostEnriched] =
    month match {
      case Some(month) ⇒ listedEnrichedPosts.filter(post ⇒
        post.created.getYear == year && post.created.getMonthOfYear == month)
      case None ⇒ listedEnrichedPosts.filter(_.created.getYear == year)
    }

  /**
   * Gets listed enriched posts which have the given term in their name
   *
   * @param term
   * @return
   */
  def getByNameParticle(term: String): List[PostEnriched] = {
    val lowercaseTerm = term.toLowerCase
    listedEnrichedPosts.filter(_.title.toLowerCase.contains(lowercaseTerm))
  }

  def unlisted(): List[PostEnriched] = {
    enrichedPosts.filterNot(_.listed)
  }

  /**
   * Searchs for the given term in the title, tags and text of the listed posts.
   *
   * @param term
   * @return A future of the list of posts, which contain the term in the title, tags or text.
   * 	The results are ordered by the occurrence of the term.
   * 	Posts, which have the term in the title come first, _ tags second and _ text third.
   */
  def search(term: String): Future[List[PostEnriched]] = {
    val lowercaseTerm = term.toLowerCase
    future {
      val result = listedEnrichedPosts.par.foldLeft(
        (List[PostEnriched](), List[PostEnriched](), List[PostEnriched]())) {
          (res: (List[PostEnriched], List[PostEnriched], List[PostEnriched]),
          enrichedPost: PostEnriched) ⇒
            if (enrichedPost.title.toLowerCase.contains(lowercaseTerm))
              (enrichedPost :: res._1, res._2, res._3)
            else if (enrichedPost.tags.exists(tag ⇒ tag.toLowerCase.contains(lowercaseTerm)))
              (res._1, enrichedPost :: res._2, res._3)
            else if (enrichedPost.text.toLowerCase.contains(lowercaseTerm))
              (res._1, res._2, enrichedPost :: res._3)
            else
              (res._1, res._2, res._3)
        }
      result._1 ::: result._2 ::: result._3
    }
  }

  override def update(post: Post) = {
    val id = post.id.getOrElse(
      throw new IllegalArgumentException("update called with mssing post id in " + post))
    postDataService.get(id) match {
      case Some(post) ⇒
        enrichedPosts =
          (PostEnriched(post) :: enrichedPosts.filterNot(_.id.get == id)).sortBy(PostEssential.postSortFun)
      case None ⇒
        enrichedPosts = enrichedPosts.filterNot(_.id.get == id)
    }
    listedEnrichedPosts = enrichedPosts.filter(_.listed)
  }

  protected def postDataService: PostDataService

  private def initEnrichedPosts(): Unit =
    future { blocking { postDataService.allAsList } } map {
      posts ⇒
        enrichedPosts = posts.par.map(PostEnriched(_)).toList.sortBy(PostEssential.postSortFun)
        listedEnrichedPosts = enrichedPosts.filter(_.listed)
    }
}
