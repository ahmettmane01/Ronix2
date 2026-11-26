package com.ronix.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.time.LocalDate
import java.util.UUID

class CDraft(val id: String?, title: String) {
    var title by androidx.compose.runtime.mutableStateOf(title)
    val lessons = androidx.compose.runtime.mutableStateListOf<LDraft>()
}
class LDraft(val id: String?, title: String, url: String) {
    var title by androidx.compose.runtime.mutableStateOf(title)
    var url by androidx.compose.runtime.mutableStateOf(url)
}

/** All data access. Authorization is enforced by Supabase RLS, never by client checks. */
object Repo {
    private val c get() = Backend.client
    val uid: String? get() = c.auth.currentUserOrNull()?.id
    private fun me() = uid ?: error("Not signed in")

    suspend fun profile(): Profile? = c.from("profiles").select { filter { eq("id", me()) } }.decodeSingleOrNull<Profile>()
    suspend fun updateName(name: String) { c.from("profiles").update({ set("name", name.trim()) }) { filter { eq("id", me()) } } }

    suspend fun courses(all: Boolean = false) = c.from("courses").select {
        if (!all) filter { eq("published", true) }
        order("created_at", Order.DESCENDING)
    }.decodeList<Course>()
    suspend fun course(id: String) = c.from("courses").select { filter { eq("id", id) } }.decodeSingle<Course>()
    suspend fun chapters(courseId: String) = c.from("chapters").select { filter { eq("course_id", courseId) }; order("position", Order.ASCENDING) }.decodeList<Chapter>()
    suspend fun lessons(courseId: String) = c.from("lessons").select { filter { eq("course_id", courseId) }; order("position", Order.ASCENDING) }.decodeList<Lesson>()
    suspend fun lesson(id: String) = c.from("lessons").select { filter { eq("id", id) } }.decodeSingle<Lesson>()
    suspend fun videoId(lessonId: String) = c.from("lesson_videos").select { filter { eq("lesson_id", lessonId) } }.decodeSingleOrNull<LessonVideo>()?.youtubeId
    suspend fun videos(lessonIds: List<String>) = if (lessonIds.isEmpty()) emptyList() else c.from("lesson_videos").select { filter { isIn("lesson_id", lessonIds) } }.decodeList<LessonVideo>()

    suspend fun enrollment(courseId: String) = c.from("enrollments").select { filter { eq("course_id", courseId); eq("user_id", me()) } }.decodeSingleOrNull<Enrollment>()
    suspend fun myEnrollments() = c.from("enrollments").select { filter { eq("user_id", me()) }; order("created_at", Order.DESCENDING) }.decodeList<Enrollment>()
    /** Free courses are approved instantly; paid courses create a pending request for the admin. */
    suspend fun enroll(course: Course) { c.from("enrollments").insert(EnrollIn(me(), course.id, if (course.access == "free") "approved" else "pending")) }

    suspend fun touch(lesson: Lesson) {
        c.from("lesson_progress").upsert(ProgressIn(me(), lesson.id, lesson.courseId)) { onConflict = "user_id,lesson_id" }
        c.from("activity").upsert(ActivityIn(me(), LocalDate.now().toString())) { onConflict = "user_id,day" }
    }
    suspend fun progress() = c.from("lesson_progress").select { filter { eq("user_id", me()) }; order("last_watched", Order.DESCENDING) }.decodeList<Progress>()
    suspend fun activityDays() = c.from("activity").select { filter { eq("user_id", me()) } }.decodeList<ActivityRow>().map { it.day }

    suspend fun comments(lessonId: String) = c.from("comments").select(Columns.raw("*, profiles(name)")) {
        filter { eq("lesson_id", lessonId) }; order("created_at", Order.DESCENDING); limit(50)
    }.decodeList<Comment>().reversed()
    suspend fun postComment(lessonId: String, body: String) { c.from("comments").insert(CommentIn(lessonId, body.trim())) }
    suspend fun deleteComment(id: String) { c.from("comments").delete { filter { eq("id", id) } } }
    suspend fun announcements() = c.from("announcements").select { order("created_at", Order.DESCENDING); limit(50) }.decodeList<Announcement>()

    // ---- admin ----
    suspend fun students() = c.from("profiles").select { filter { eq("role", "student") }; order("created_at", Order.DESCENDING) }.decodeList<Profile>()
    suspend fun allEnrollments() = c.from("enrollments").select { order("created_at", Order.DESCENDING) }.decodeList<Enrollment>()
    suspend fun setEnrollment(id: String, status: String) { c.from("enrollments").update({ set("status", status) }) { filter { eq("id", id) } } }
    suspend fun setPublished(id: String, v: Boolean) { c.from("courses").update({ set("published", v) }) { filter { eq("id", id) } } }
    suspend fun setFeatured(id: String, v: Boolean) { c.from("courses").update({ set("featured", v) }) { filter { eq("id", id) } } }
    suspend fun deleteCourse(id: String) { c.from("courses").delete { filter { eq("id", id) } } }
    suspend fun postAnnouncement(title: String, body: String) { c.from("announcements").insert(AnnouncementIn(title.trim(), body.trim())) }

    suspend fun uploadThumbnail(bytes: ByteArray): String {
        val path = "${UUID.randomUUID()}.jpg"
        c.storage.from("thumbnails").upload(path, bytes) { upsert = true }
        return c.storage.from("thumbnails").publicUrl(path)
    }

    /** Saves course, chapters and lessons. A brand-new course is removed again if any later step fails. */
    suspend fun saveCourse(existing: String?, f: CourseIn, chapters: List<CDraft>, removedChapters: List<String>, removedLessons: List<String>) {
        val cid = if (existing == null) c.from("courses").insert(f) { select() }.decodeSingle<Course>().id
        else { c.from("courses").update(f) { filter { eq("id", existing) } }; existing }
        try {
            removedLessons.forEach { id -> c.from("lessons").delete { filter { eq("id", id) } } }
            removedChapters.forEach { id -> c.from("chapters").delete { filter { eq("id", id) } } }
            chapters.forEachIndexed { ci, ch ->
                val oldCh = ch.id
                val chId = if (oldCh == null) c.from("chapters").insert(ChapterIn(cid, ch.title.trim(), ci)) { select() }.decodeSingle<Chapter>().id
                else { c.from("chapters").update(ChapterIn(cid, ch.title.trim(), ci)) { filter { eq("id", oldCh) } }; oldCh }
                ch.lessons.forEachIndexed { li, l ->
                    val yt = youtubeId(l.url) ?: error("Invalid YouTube link in \"${l.title}\"")
                    val oldL = l.id
                    if (oldL == null) {
                        val ls = c.from("lessons").insert(LessonIn(chId, cid, l.title.trim(), li)) { select() }.decodeSingle<Lesson>()
                        c.from("lesson_videos").insert(VideoIn(ls.id, yt))
                    } else {
                        c.from("lessons").update(LessonIn(chId, cid, l.title.trim(), li)) { filter { eq("id", oldL) } }
                        c.from("lesson_videos").upsert(VideoIn(oldL, yt))
                    }
                }
            }
        } catch (e: Exception) {
            if (existing == null) runCatching { c.from("courses").delete { filter { eq("id", cid) } } }
            throw e
        }
    }
}
