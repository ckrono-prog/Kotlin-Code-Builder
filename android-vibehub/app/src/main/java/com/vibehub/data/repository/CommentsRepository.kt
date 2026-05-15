package com.vibehub.data.repository

import com.vibehub.data.local.dao.UserDao
import com.vibehub.domain.model.Comment
import com.vibehub.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
) {
    suspend fun getCommentsForPost(postId: String): Result<List<Comment>> = runCatching {
        val remoteComments = supabase.postgrest["comments"]
            .select {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<RemoteComment>()

        remoteComments.map { rc ->
            val author = userDao.getUser(rc.authorId)?.toDomain() ?: User(id = rc.authorId)
            rc.toDomain(author)
        }
    }

    suspend fun addComment(
        postId: String,
        text: String,
        parentId: String? = null,
    ): Result<Comment> = runCatching {
        val inserted = supabase.postgrest["comments"]
            .insert(buildMap<String, Any?> {
                put("post_id", postId)
                put("text", text)
                parentId?.let { put("parent_id", it) }
            })
            .decodeSingle<RemoteComment>()

        val author = userDao.getUser(inserted.authorId)?.toDomain() ?: User(id = inserted.authorId)
        inserted.toDomain(author)
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = runCatching {
        supabase.postgrest["comments"].delete { filter { eq("id", commentId) } }
    }

    suspend fun toggleLike(commentId: String): Result<Unit> = runCatching {
        // In a real app: check if like exists, then insert or delete accordingly
        supabase.postgrest["comment_likes"].insert(mapOf("comment_id" to commentId))
    }
}

@Serializable
private data class RemoteComment(
    val id: String = "",
    @SerialName("post_id")    val postId: String = "",
    @SerialName("author_id")  val authorId: String = "",
    val text: String = "",
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("parent_id")  val parentId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

private fun RemoteComment.toDomain(author: User) = Comment(
    id          = id,
    postId      = postId,
    authorId    = authorId,
    author      = author,
    text        = text,
    likesCount  = likesCount,
    parentId    = parentId,
    createdAt   = createdAt,
)
