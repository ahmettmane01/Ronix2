package com.ronix.learning.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.ronix.learning.*
import kotlinx.coroutines.launch

private data class NavTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable fun StudentShell(vm: AppVm, p: Profile) {
    val nav = rememberNavController()
    val tabs = listOf(NavTab("home", "Home", Icons.Default.Home), NavTab("explore", "Explore", Icons.Default.Search), NavTab("learning", "My Learning", Icons.Default.PlayArrow), NavTab("profile", "Profile", Icons.Default.Person))
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(containerColor = Bg, bottomBar = {
        if (route in tabs.map { it.route }) NavigationBar(containerColor = Panel) {
            tabs.forEach { t -> NavigationBarItem(route == t.route, { nav.navigate(t.route) { popUpTo("home"); launchSingleTop = true } }, { Icon(t.icon, t.label) }, label = { Text(t.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple2, selectedTextColor = Purple2, indicatorColor = Elev)) }
        }
    }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(p, nav) }
            composable("explore") { ExploreScreen(nav) }
            composable("learning") { LearningScreen(nav) }
            composable("profile") { ProfileScreen(vm, p, nav) }
            composable("notifications") { NotificationsScreen(nav) }
            composable("course/{id}") { CourseScreen(it.arguments!!.getString("id")!!, nav) }
            composable("lesson/{id}") { LessonScreen(it.arguments!!.getString("id")!!, nav) }
        }
    }
}

@Composable fun HomeScreen(p: Profile, nav: NavHostController) {
    val courses = load { Repo.courses() }
    val prog = load { Repo.progress() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Welcome back", color = TextS, fontSize = 13.sp); Title("Hi, ${p.name.ifBlank { "there" }}") }
                IconButton({ nav.navigate("notifications") }) { Icon(Icons.Default.Notifications, "Notifications") }
            }
        }
        item {
            courses.Show { list ->
                if (list.isEmpty()) Empty("No courses yet. New courses will appear here.")
                else {
                    list.firstOrNull { it.featured }?.let { f ->
                        Section("Featured"); CourseRow(f, "Featured course") { nav.navigate("course/${f.id}") }
                    }
                    val last = prog.data?.firstOrNull()
                    list.firstOrNull { it.id == last?.courseId }?.let { c ->
                        Section("Continue learning"); CourseRow(c, "Pick up where you left off") { nav.navigate("course/${c.id}") }
                    }
                    Section("All courses")
                }
            }
        }
        courses.data?.let { list -> items(list, key = { it.id }) { c -> Box(Modifier.padding(bottom = 10.dp)) { CourseRow(c) { nav.navigate("course/${c.id}") } } } }
    }
}

@Composable fun ExploreScreen(nav: NavHostController) {
    val courses = load { Repo.courses() }
    var q by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("all") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Title("Explore", Modifier.padding(top = 24.dp, bottom = 12.dp))
        Field(q, { q = it }, "Search courses")
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("All", filter == "all") { filter = "all" }; Pill("Free", filter == "free") { filter = "free" }; Pill("Paid", filter == "paid") { filter = "paid" }
        }
        courses.Show { list ->
            val shown = list.filter { (filter == "all" || it.access == filter) && (it.title.contains(q, true) || (it.description ?: "").contains(q, true)) }
            if (shown.isEmpty()) Empty("No courses found")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(shown, key = { it.id }) { c -> CourseRow(c) { nav.navigate("course/${c.id}") } } }
        }
    }
}

@Composable fun CourseScreen(id: String, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val d = load(id) { Triple(Repo.course(id), Repo.chapters(id), Repo.lessons(id)) }
    val en = load(id) { Repo.enrollment(id) }
    var busy by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf<String?>(null) }
    val open = remember { mutableStateMapOf<String, Boolean>() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TopBar("Course") { nav.popBackStack() }
        d.Show { (course, chapters, lessons) ->
            val status = en.data?.status
            LazyColumn {
                item {
                    Thumb(course.thumbnailUrl, Modifier.fillMaxWidth().height(180.dp)); Spacer(Modifier.height(12.dp))
                    Title(course.title); Spacer(Modifier.height(4.dp))
                    Text("${lessons.size} lessons · ${chapters.size} chapters", color = TextS, fontSize = 13.sp); Spacer(Modifier.height(8.dp))
                    Pill(if (course.access == "paid") "Paid" else "Free", course.access == "paid")
                    course.description?.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(10.dp)); Text(it, color = TextS) }
                    Spacer(Modifier.height(14.dp))
                    when (status) {
                        "approved" -> Text("You are enrolled", color = Purple2)
                        "pending" -> PrimaryButton("Request pending", false) {}
                        else -> PrimaryButton(if (course.access == "paid") "Request to enroll" else "Enroll", !busy) {
                            scope.launch { busy = true; runCatching { Repo.enroll(course) }.onSuccess { en.reload(); msg = if (course.access == "paid") "Request sent. The admin will review it." else null }.onFailure { msg = it.message }; busy = false }
                        }
                    }
                    if (status == "rejected") Text("Your last request was rejected.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    msg?.let { Text(it, color = TextS, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                    Section("Curriculum")
                }
                items(chapters, key = { it.id }) { ch ->
                    Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { open[ch.id] = open[ch.id] != true }, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${ch.position + 1} · ${ch.title}", fontWeight = FontWeight.SemiBold); Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                            if (open[ch.id] == true) lessons.filter { it.chapterId == ch.id }.forEach { l ->
                                Row(Modifier.fillMaxWidth().padding(top = 12.dp).clickable(enabled = status == "approved") { nav.navigate("lesson/${l.id}") }, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (status == "approved") Icons.Default.PlayArrow else Icons.Default.Lock, null, tint = Purple2, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(l.title)
                                }
                            }
                        }
                    }
                }
                item { if (chapters.isEmpty()) Empty("The curriculum is not available yet") }
            }
        }
    }
}
