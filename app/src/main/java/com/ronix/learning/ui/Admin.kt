package com.ronix.learning.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.ronix.learning.*
import kotlinx.coroutines.launch

@Composable fun AdminShell(vm: AppVm) {
    val nav = rememberNavController()
    val tabs = listOf("admin" to ("Overview" to Icons.Default.Home), "courses" to ("Courses" to Icons.Default.PlayArrow), "students" to ("Students" to Icons.Default.Person), "requests" to ("Requests" to Icons.Default.Notifications))
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(containerColor = Bg, bottomBar = {
        if (route in tabs.map { it.first }) NavigationBar(containerColor = Panel) {
            tabs.forEach { (r, li) -> NavigationBarItem(route == r, { nav.navigate(r) { popUpTo("admin"); launchSingleTop = true } }, { Icon(li.second, li.first) }, label = { Text(li.first, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple2, selectedTextColor = Purple2, indicatorColor = Elev)) }
        }
    }) { pad ->
        NavHost(nav, "admin", Modifier.padding(pad)) {
            composable("admin") { AdminOverview(vm, nav) }
            composable("courses") { AdminCourses(nav) }
            composable("students") { AdminStudents() }
            composable("requests") { AdminRequests() }
            composable("edit/{id}") { CourseEditor(it.arguments!!.getString("id")!!, nav) }
        }
    }
}

@Composable fun AdminOverview(vm: AppVm, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val d = load { Triple(Repo.courses(true).size, Repo.students().size, Repo.allEnrollments().count { it.status == "pending" }) }
    var dialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("Overview", Modifier.padding(top = 4.dp, bottom = 16.dp))
        d.Show { (c, s, r) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Courses", c, Modifier.weight(1f)); Stat("Students", s, Modifier.weight(1f)); Stat("Pending", r, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Add course") { nav.navigate("edit/new") }; Spacer(Modifier.height(10.dp))
        OutlinedButton({ dialog = true }, Modifier.fillMaxWidth().height(48.dp)) { Text("Post announcement") }
        Spacer(Modifier.height(10.dp))
        TextButton({ vm.signOut() }) { Text("Exit admin", color = MaterialTheme.colorScheme.error) }
    }
    if (dialog) {
        var t by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }
        AlertDialog({ dialog = false }, title = { Text("Announcement") },
            text = { Column { Field(t, { t = it }, "Title"); Spacer(Modifier.height(8.dp)); Field(b, { b = it }, "Message", lines = 3) } },
            confirmButton = { TextButton({ scope.launch { runCatching { Repo.postAnnouncement(t, b) }; dialog = false } }, enabled = t.isNotBlank()) { Text("Post") } },
            dismissButton = { TextButton({ dialog = false }) { Text("Cancel") } })
    }
}

@Composable private fun Stat(label: String, n: Int, m: Modifier) = Surface(m, shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
    Column(Modifier.padding(14.dp)) { Text("$n", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(label, color = TextS, fontSize = 12.sp) }
}

@Composable fun AdminCourses(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val d = load { Repo.courses(true) }
    var del by remember { mutableStateOf<Course?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Title("Courses"); FilledTonalButton({ nav.navigate("edit/new") }) { Icon(Icons.Default.Add, null); Text("New") }
        }
        d.Show { list ->
            if (list.isEmpty()) Empty("No courses yet. Tap New to add your first course.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { c ->
                    Column {
                        CourseRow(c, if (c.published) "Published" else "Draft") { nav.navigate("edit/${c.id}") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Published", fontSize = 12.sp, color = TextS); Switch(c.published, { v -> scope.launch { runCatching { Repo.setPublished(c.id, v) }; d.reload() } }, Modifier.padding(horizontal = 6.dp))
                            Text("Featured", fontSize = 12.sp, color = TextS); Switch(c.featured, { v -> scope.launch { runCatching { Repo.setFeatured(c.id, v) }; d.reload() } }, Modifier.padding(horizontal = 6.dp))
                            Spacer(Modifier.weight(1f)); IconButton({ del = c }) { Icon(Icons.Default.Delete, "Delete course", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
    del?.let { c ->
        AlertDialog({ del = null }, title = { Text("Delete course?") }, text = { Text("\"${c.title}\" and all its lessons and comments will be removed.") },
            confirmButton = { TextButton({ scope.launch { runCatching { Repo.deleteCourse(c.id) }; del = null; d.reload() } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ del = null }) { Text("Cancel") } })
    }
}

@Composable fun AdminStudents() {
    val d = load { Repo.students() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Title("Students", Modifier.padding(top = 24.dp, bottom = 12.dp))
        d.Show { list ->
            if (list.isEmpty()) Empty("No students yet")
            else LazyColumn { items(list, key = { it.id }) { s -> Column(Modifier.padding(vertical = 8.dp)) { Text(s.name.ifBlank { "Student" }, fontWeight = FontWeight.SemiBold); Text("${s.email} · ${s.createdAt.take(10)}", color = TextS, fontSize = 12.sp); HorizontalDivider(Modifier.padding(top = 8.dp), color = Border) } } }
        }
    }
}

@Composable fun AdminRequests() {
    val scope = rememberCoroutineScope()
    val d = load { Triple(Repo.allEnrollments().filter { it.status == "pending" }, Repo.students().associateBy { it.id }, Repo.courses(true).associateBy { it.id }) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Title("Enrollment requests", Modifier.padding(top = 24.dp, bottom = 12.dp))
        d.Show { (list, students, courses) ->
            if (list.isEmpty()) Empty("No pending requests")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { e ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(students[e.userId]?.name?.ifBlank { null } ?: students[e.userId]?.email ?: "Student", fontWeight = FontWeight.SemiBold)
                            Text("wants to enroll in ${courses[e.courseId]?.title ?: "a course"}", color = TextS, fontSize = 12.sp)
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton({ scope.launch { runCatching { Repo.setEnrollment(e.id, "rejected") }; d.reload() } }, Modifier.weight(1f)) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                                Button({ scope.launch { runCatching { Repo.setEnrollment(e.id, "approved") }; d.reload() } }, Modifier.weight(1f)) { Text("Accept") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun CourseEditor(id: String, nav: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = id == "new"
    var title by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; var access by remember { mutableStateOf("free") }
    var thumb by remember { mutableStateOf<String?>(null) }; var picked by remember { mutableStateOf<Uri?>(null) }
    val chapters = remember { mutableStateListOf<CDraft>() }
    val removedC = remember { mutableStateListOf<String>() }; val removedL = remember { mutableStateListOf<String>() }
    var published by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }; var loaded by remember { mutableStateOf(isNew) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) picked = it }

    LaunchedEffect(id) {
        if (!isNew) runCatching {
            val c = Repo.course(id); title = c.title; desc = c.description ?: ""; access = c.access; thumb = c.thumbnailUrl; published = c.published
            val ls = Repo.lessons(id); val vids = Repo.videos(ls.map { it.id }).associate { it.lessonId to it.youtubeId }
            Repo.chapters(id).forEach { ch -> chapters.add(CDraft(ch.id, ch.title).also { d -> ls.filter { it.chapterId == ch.id }.forEach { l -> d.lessons.add(LDraft(l.id, l.title, vids[l.id]?.let { v -> "https://youtu.be/$v" } ?: "")) } }) }
            loaded = true
        }.onFailure { err = it.message; loaded = true }
    }

    fun save(publish: Boolean) {
        err = when {
            title.isBlank() -> "Course title is required"
            publish && (chapters.isEmpty() || chapters.any { it.lessons.isEmpty() }) -> "Add at least one chapter with lessons before publishing"
            chapters.any { it.title.isBlank() || it.lessons.any { l -> l.title.isBlank() || youtubeId(l.url) == null } } -> "Every chapter needs a title and every lesson a title and a valid YouTube link"
            else -> null
        }
        if (err != null) return
        scope.launch {
            busy = true
            runCatching {
                var url = thumb
                picked?.let { u -> val bytes = ctx.contentResolver.openInputStream(u)!!.use { it.readBytes() }; if (bytes.size > 5_000_000) error("Thumbnail must be under 5 MB"); url = Repo.uploadThumbnail(bytes) }
                Repo.saveCourse(if (isNew) null else id, CourseIn(title.trim(), desc.trim().ifBlank { null }, url, access, publish), chapters.toList(), removedC.toList(), removedL.toList())
            }.onSuccess { nav.popBackStack() }.onFailure { err = it.message ?: "Could not save the course" }
            busy = false
        }
    }

    if (!loaded) { Loading(); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TopBar(if (isNew) "Add course" else "Edit course") { nav.popBackStack() } }
        item {
            Thumb(picked?.toString() ?: thumb, Modifier.fillMaxWidth().height(150.dp))
            OutlinedButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Choose thumbnail from this device") }
        }
        item { Field(title, { title = it }, "Title") }
        item { Field(desc, { desc = it }, "Description (optional)", lines = 3) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Access", color = TextS); Pill("Free", access == "free") { access = "free" }; Pill("Paid", access == "paid") { access = "paid" }; if (access == "paid") Text("Students send a request you approve", color = TextS, fontSize = 11.sp) } }
        item { Section("Chapters"); Text("Use public or unlisted YouTube videos with embedding allowed, otherwise they cannot play in the app.", color = TextS, fontSize = 11.sp) }
        items(chapters.size) { ci ->
            val ch = chapters[ci]
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { Field(ch.title, { ch.title = it }, "Chapter ${ci + 1} title") }
                        IconButton({ if (ci > 0) { val t = chapters.removeAt(ci); chapters.add(ci - 1, t) } }) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                        IconButton({ ch.id?.let { removedC.add(it) }; chapters.removeAt(ci) }) { Icon(Icons.Default.Delete, "Delete chapter", tint = MaterialTheme.colorScheme.error) }
                    }
                    ch.lessons.forEachIndexed { li, l ->
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Field(l.title, { l.title = it }, "Lesson title")
                                Field(l.url, { l.url = it }, "YouTube link")
                                if (l.url.isNotBlank() && youtubeId(l.url) == null) Text("Invalid YouTube link", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                            IconButton({ l.id?.let { removedL.add(it) }; ch.lessons.removeAt(li) }) { Icon(Icons.Default.Delete, "Delete lesson", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    TextButton({ ch.lessons.add(LDraft(null, "", "")) }) { Text("+ Add lesson", color = Purple2) }
                }
            }
        }
        item { OutlinedButton({ chapters.add(CDraft(null, "")) }, Modifier.fillMaxWidth()) { Text("+ Add chapter") } }
        item { err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
        item {
            Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ save(false) }, Modifier.weight(1f).height(48.dp), enabled = !busy) { Text("Save draft") }
                Button({ save(true) }, Modifier.weight(1f).height(48.dp), enabled = !busy) { Text(if (busy) "Saving…" else "Publish") }
            }
        }
    }
}
