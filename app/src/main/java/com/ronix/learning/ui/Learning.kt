package com.ronix.learning.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ronix.learning.*
import kotlinx.coroutines.launch

private data class LearnData(val courses: Map<String, Course>, val enrollments: List<Enrollment>, val total: Map<String, Int>, val watched: Map<String, Int>, val streak: Int)

@Composable fun LearningScreen(nav: NavHostController) {
    val d = load {
        val en = Repo.myEnrollments(); val all = Repo.courses().associateBy { it.id }
        val lessons = en.filter { it.status == "approved" }.flatMap { Repo.lessons(it.courseId) }
        val prog = Repo.progress()
        LearnData(all, en, lessons.groupingBy { it.courseId }.eachCount(), prog.groupingBy { it.courseId }.eachCount(), streak(Repo.activityDays()))
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Title("My Learning", Modifier.padding(top = 24.dp, bottom = 12.dp))
        d.Show { x ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(14.dp)) { Text("Daily streak", color = TextS, fontSize = 12.sp); Text("${x.streak} ${if (x.streak == 1) "day" else "days"}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Purple2) }
            }
            if (x.enrollments.isEmpty()) Empty("You have not enrolled in any course yet.")
            LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(x.enrollments, key = { it.id }) { e ->
                    val c = x.courses[e.courseId] ?: return@items
                    val pct = progressPercent(x.watched[c.id] ?: 0, x.total[c.id] ?: 0)
                    Column {
                        CourseRow(c, when (e.status) { "pending" -> "Request pending"; "rejected" -> "Request rejected"; else -> "$pct% watched" }) { nav.navigate("course/${c.id}") }
                        if (e.status == "approved") LinearProgressIndicator({ pct / 100f }, Modifier.fillMaxWidth().padding(top = 6.dp), color = Purple, trackColor = Border)
                    }
                }
            }
        }
    }
}

@Composable fun ProfileScreen(vm: AppVm, p: Profile, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(p.name) }; var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("Profile", Modifier.padding(top = 4.dp, bottom = 16.dp))
        Text(p.email, color = TextS); Text("Joined ${p.createdAt.take(10)}", color = TextS, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp)); Field(name, { name = it; saved = false }, "Name")
        Spacer(Modifier.height(10.dp))
        OutlinedButton({ scope.launch { runCatching { Repo.updateName(name) }.onSuccess { saved = true; vm.loadProfile() } } }, enabled = name.isNotBlank() && name != p.name) { Text(if (saved) "Saved" else "Save name") }
        Spacer(Modifier.height(16.dp))
        TextButton({ nav.navigate("notifications") }) { Text("Notifications", color = TextP) }
        TextButton({ vm.signOut() }) { Text("Log out", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable fun NotificationsScreen(nav: NavHostController) {
    val d = load { Repo.announcements() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TopBar("Notifications") { nav.popBackStack() }
        d.Show { list ->
            if (list.isEmpty()) Empty("No notifications yet")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { a ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.padding(14.dp)) { Pill("Announcement"); Spacer(Modifier.height(6.dp)); Text(a.title, fontWeight = FontWeight.SemiBold); if (a.body.isNotBlank()) Text(a.body, color = TextS, fontSize = 13.sp); Text(a.createdAt.take(10), color = TextS, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
