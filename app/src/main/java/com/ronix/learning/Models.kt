package com.ronix.learning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class Profile(val id: String, val name: String = "", val email: String = "", val role: String = "student", @SerialName("created_at") val createdAt: String = "")
@Serializable data class Course(val id: String, val title: String, val description: String? = null, @SerialName("thumbnail_url") val thumbnailUrl: String? = null, val access: String = "free", val published: Boolean = false, val featured: Boolean = false, @SerialName("created_at") val createdAt: String = "")
@Serializable data class CourseIn(val title: String, val description: String?, @SerialName("thumbnail_url") val thumbnailUrl: String?, val access: String, val published: Boolean, val featured: Boolean = false)
@Serializable data class Chapter(val id: String, @SerialName("course_id") val courseId: String, val title: String, val position: Int = 0)
@Serializable data class ChapterIn(@SerialName("course_id") val courseId: String, val title: String, val position: Int)
@Serializable data class Lesson(val id: String, @SerialName("chapter_id") val chapterId: String, @SerialName("course_id") val courseId: String, val title: String, val position: Int = 0)
@Serializable data class LessonIn(@SerialName("chapter_id") val chapterId: String, @SerialName("course_id") val courseId: String, val title: String, val position: Int)
@Serializable data class LessonVideo(@SerialName("lesson_id") val lessonId: String, @SerialName("youtube_id") val youtubeId: String)
@Serializable data class Enrollment(val id: String, @SerialName("user_id") val userId: String, @SerialName("course_id") val courseId: String, val status: String, @SerialName("created_at") val createdAt: String = "")
@Serializable data class EnrollIn(@SerialName("user_id") val userId: String, @SerialName("course_id") val courseId: String, val status: String)
@Serializable data class Progress(@SerialName("user_id") val userId: String, @SerialName("lesson_id") val lessonId: String, @SerialName("course_id") val courseId: String, @SerialName("last_watched") val lastWatched: String = "")
@Serializable data class ProgressIn(@SerialName("user_id") val userId: String, @SerialName("lesson_id") val lessonId: String, @SerialName("course_id") val courseId: String)
@Serializable data class ActivityRow(val day: String)
@Serializable data class ActivityIn(@SerialName("user_id") val userId: String, val day: String)
@Serializable data class NameOnly(val name: String = "")
@Serializable data class Comment(val id: String, @SerialName("lesson_id") val lessonId: String, @SerialName("user_id") val userId: String, val body: String, @SerialName("created_at") val createdAt: String = "", val profiles: NameOnly? = null)
@Serializable data class CommentIn(@SerialName("lesson_id") val lessonId: String, val body: String)
@Serializable data class Announcement(val id: String, val title: String, val body: String = "", @SerialName("created_at") val createdAt: String = "")
@Serializable data class AnnouncementIn(val title: String, val body: String)
@Serializable data class VideoIn(@SerialName("lesson_id") val lessonId: String, @SerialName("youtube_id") val youtubeId: String)
