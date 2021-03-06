package model.ui

import model.blog.Post
import model.blog.PostEnriched
import model.blog.User
import org.joda.time.DateTime

/**
 * The post draft class used for when the user is editing or creating a post
 *
 * @author Stefan Bleibinhaus
 *
 */
case class PostDraft(
  val id: Option[String],
  val title: String,
  val text: String,
  val tags: List[String],
  val listed: Boolean,
  val showEdit: Boolean,
  val resetTime: Boolean)

object PostDraft {
  // Do NOT format the XML!
  val templateDraft = PostDraft("", "",
    (<sbtl>
  <abstract>
    abstract
  </abstract>
  <text>
    <h1>header</h1>
    <p>text</p>
  </text>
</sbtl>).toString, "", true, true, false)

  def apply(
    id: String,
    title: String,
    text: String,
    tags: String,
    listed: Boolean,
    showEdit: Boolean,
    resetTime: Boolean): PostDraft = {
    val idOption = id match {
      case id if id.length > 1 ⇒ Some(id)
      case _ ⇒ None
    }
    /*
     * splits tags seperated by comma, trims them, filters out to short ones, replaces whitespace with '-'
     * and sorts them by their name
     */
    new PostDraft(idOption,
      title,
      text,
      tags.split(',').map(_.trim).filter(_.length > 1).map(_.replaceAll("\\s", "-")).sortBy(_.toString).toList,
      listed,
      showEdit,
      resetTime)
  }

  def unapplyToStrings(draft: PostDraft): Option[(String, String, String, String, Boolean, Boolean, Boolean)] =
    Some((draft.id.getOrElse(""), draft.title, draft.text, draft.tags.mkString("", ", ", ""), draft.listed, draft.showEdit, draft.resetTime))

  /**
   * Creates a draft from a post
   *
   * @param post
   * @return
   */
  def createDraft(post: Post): PostDraft =
    PostDraft(post.id, post.title, post.text, post.tags, post.listed, post.showUpdated, false)

  /**
   * Creates a new post from a draft
   *
   * @param draft
   * @param user
   * @return
   */
  def createPost(draft: PostDraft, user: User): Post =
    Post(draft.id, draft.title, user, draft.text, draft.tags, draft.listed, draft.showEdit)

  /**
   * Creates a post from a draft and an existing post
   *
   * @param draft
   * @param origPost
   * @return
   */
  def createPost(draft: PostDraft, origPost: Post): Post =
    draft.resetTime match {
      case true ⇒
        val newCreated = new DateTime()
        origPost.copy(title = draft.title,
          text = draft.text,
          created = newCreated,
          updated = newCreated,
          tags = draft.tags,
          listed = draft.listed,
          showUpdated = draft.showEdit)
      case false ⇒
        origPost.copy(title = draft.title,
          updated = new DateTime(),
          text = draft.text,
          tags = draft.tags,
          listed = draft.listed,
          showUpdated = draft.showEdit)
    }

  /**
   * @return A function creating an enriched post from a draft
   */
  def createPostEnrichedUserFun: (PostDraft, User) ⇒ PostEnriched =
    (draft: PostDraft, user: User) ⇒ PostEnriched(createPost(draft, user))

  /**
   * Creates an enriched post from a draft
   *
   * @param draft
   * @param user
   * @return
   */
  def createPostEnriched(draft: PostDraft, user: User): PostEnriched =
    createPostEnrichedUserFun(draft, user)

  /**
   * @return A function creating an enriched post from a draft and an existing post
   */
  def createPostEnrichedPostFun: (PostDraft, Post) ⇒ PostEnriched =
    (draft: PostDraft, origPost: Post) ⇒ PostEnriched(createPost(draft, origPost))

  /**
   * Creates an enriched post from a draft and an existing post
   *
   * @param draft
   * @param origPost
   * @return
   */
  def createPostEnriched(draft: PostDraft, origPost: Post): PostEnriched =
    createPostEnrichedPostFun(draft, origPost)
}
