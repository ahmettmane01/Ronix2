package com.ronix.learning.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ronix.learning.Course
import kotlinx.coroutines.launch

/** Tiny async loader: data / loading / error + reload. */
class Ld<T> {
    var data by mutableStateOf<T?>(null); var err by mutableStateOf<String?>(null); var loading by mutableStateOf(true)
    var reload: () -> Unit = {}
}

@Composable fun <T> load(key: Any? = Unit, f: suspend () -> T): Ld<T> {
    val ld = remember(key) { Ld<T>() }
    val scope = rememberCoroutineScope()
    ld.reload = { scope.launch { ld.loading = true; runCatching { f() }.onSuccess { ld.data = it; ld.err = null }.onFailure { ld.err = it.message ?: "Something went wrong" }; ld.loading = false } }
    LaunchedEffect(key) { ld.reload() }
    return ld
}

@Composable fun Loading() = Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = Purple, strokeWidth = 2.dp) }
@Composable fun Empty(text: String) = Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text(text, color = TextS) }
@Composable fun ErrorBox(msg: String, retry: () -> Unit) = Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(msg, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp)); OutlinedButton(retry) { Text("Retry") }
}
/** Shows loading / error / content for an Ld. */
@Composable fun <T> Ld<T>.Show(content: @Composable (T) -> Unit) {
    val d = data
    when { d != null -> content(d); loading -> Loading(); err != null -> ErrorBox(err!!) { reload() } }
}

@Composable fun Title(t: String, modifier: Modifier = Modifier) = Text(t, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = modifier)
@Composable fun Section(t: String) = Text(t, color = TextS, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))

@Composable fun TopBar(title: String, onBack: () -> Unit) = Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1)
}

@Composable fun Pill(text: String, selected: Boolean = false, color: androidx.compose.ui.graphics.Color? = null, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(m, shape = RoundedCornerShape(6.dp), color = if (selected) Purple else Elev) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, color = color ?: if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFC4B5FD))
    }
}

@Composable fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Button(onClick, modifier.fillMaxWidth().height(48.dp), enabled = enabled, shape = RoundedCornerShape(10.dp)) { Text(text, fontWeight = FontWeight.SemiBold) }

@Composable fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, password: Boolean = false, lines: Int = 1) =
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = lines == 1, minLines = lines,
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(10.dp))

@Composable fun Thumb(url: String?, modifier: Modifier) {
    if (url.isNullOrBlank()) Box(modifier.background(Elev, RoundedCornerShape(8.dp)), Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Purple2) }
    else AsyncImage(url, null, modifier.background(Elev, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
}

@Composable fun CourseRow(c: Course, sub: String = "", onClick: () -> Unit) = Surface(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(10.dp), color = Panel, border = BorderStroke(1.dp, Border)) {
    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumb(c.thumbnailUrl, Modifier.size(96.dp, 60.dp))
        Column(Modifier.weight(1f)) {
            Text(c.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
            if (sub.isNotBlank()) Text(sub, color = TextS, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Pill(if (c.access == "paid") "Paid" else "Free", selected = c.access == "paid")
        }
    }
}
