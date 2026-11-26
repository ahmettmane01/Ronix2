package com.ronix.learning.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.ronix.learning.Repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-app YouTube player (IFrame embed in a WebView). A Referer of https://<package> is sent so YouTube accepts the embed
 * (error 153 otherwise). Navigation away from the embed is blocked; fullscreen button is hidden; playback pauses with the app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable fun YouTubeEmbed(videoId: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val web = remember(videoId) {
        WebView(ctx).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val u = request.url
                    return !(u.host?.endsWith("youtube.com") == true && u.path?.startsWith("/embed/") == true)
                }
            }
            loadUrl("https://www.youtube.com/embed/$videoId?rel=0&playsinline=1&modestbranding=1&iv_load_policy=3&fs=0",
                mapOf("Referer" to "https://${ctx.packageName}"))
        }
    }
    DisposableEffect(web) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) web.onPause() else if (e == Lifecycle.Event.ON_RESUME) web.onResume() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); web.stopLoading(); web.destroy() }
    }
    AndroidView({ web }, modifier)
}

@Composable fun LessonScreen(id: String, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val d = load(id) { val l = Repo.lesson(id); Triple(l, Repo.videoId(id), Repo.course(l.courseId)).also { runCatching { Repo.touch(l) } } }
    val comments = load(id) { Repo.comments(id) }
    var text by remember { mutableStateOf("") }; var sending by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
    // Reliable refresh: new comments from other students appear without leaving the screen.
    LaunchedEffect(id) { while (true) { delay(15_000); runCatching { comments.data = Repo.comments(id) } } }
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.padding(horizontal = 12.dp)) { TopBar(d.data?.third?.title ?: "Lesson") { nav.popBackStack() } }
        d.Show { (lesson, vid, _) ->
            if (vid == null) ErrorBox("This video is not available.") { d.reload() }
            else YouTubeEmbed(vid, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(lesson.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp)); Text("Comments", fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            comments.Show { list ->
                if (list.isEmpty()) Empty("No comments yet. Start the conversation.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), reverseLayout = false) {
                    items(list, key = { it.id }) { c ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(32.dp), Alignment.Center) { Pill((c.profiles?.name?.firstOrNull() ?: '?').uppercase()) }
                            Column(Modifier.weight(1f)) {
                                Text("${c.profiles?.name?.ifBlank { "Student" } ?: "Student"}  ${c.createdAt.take(10)}", color = TextS, fontSize = 12.sp)
                                Text(c.body, fontSize = 14.sp)
                            }
                            if (c.userId == Repo.uid) IconButton({ scope.launch { runCatching { Repo.deleteComment(c.id) }; comments.reload() } }) { Icon(Icons.Default.Delete, "Delete comment", tint = TextS) }
                        }
                    }
                }
            }
        }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { if (it.length <= 500) text = it }, Modifier.weight(1f), placeholder = { Text("Add a comment…") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            FilledIconButton({
                scope.launch { sending = true; err = null; runCatching { Repo.postComment(id, text) }.onSuccess { text = ""; comments.reload() }.onFailure { err = "Could not send your comment" }; sending = false }
            }, Modifier.size(48.dp), enabled = text.isNotBlank() && !sending) { Icon(Icons.Default.Send, "Send") }
        }
    }
}
