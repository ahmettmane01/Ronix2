package com.ronix.learning.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import com.ronix.learning.Backend
import com.ronix.learning.Profile
import com.ronix.learning.Repo
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppVm : ViewModel() {
    var profile by mutableStateOf<Profile?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private val auth get() = Backend.client.auth

    fun loadProfile() = viewModelScope.launch { runCatching { Repo.profile() }.onSuccess { profile = it } }

    /** One sign-in for everyone. The role stored in the database decides which area opens; RLS enforces it server-side. */
    fun signIn(email: String, pw: String) = viewModelScope.launch {
        busy = true; error = null
        runCatching {
            auth.signInWith(Email) { this.email = email.trim(); password = pw }
            profile = Repo.profile()
        }.onFailure { error = "Sign in failed. Check your email and password." }
        busy = false
    }

    fun signUp(name: String, email: String, pw: String) = viewModelScope.launch {
        busy = true; error = null
        runCatching {
            auth.signUpWith(Email) { this.email = email.trim(); password = pw; data = buildJsonObject { put("name", name.trim()) } }
        }.onSuccess { error = "Account created. If email confirmation is on, verify your email, then sign in." }.onFailure { error = it.message ?: "Registration failed" }
        busy = false
    }

    fun signOut() = viewModelScope.launch { runCatching { auth.signOut() }; profile = null }
}

@Composable fun AppRoot(vm: AppVm = viewModel()) {
    val status by Backend.client.auth.sessionStatus.collectAsState()
    Surface(Modifier.fillMaxSize(), color = Bg) {
        when (status) {
            is SessionStatus.Initializing -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { RonixLogo(96.dp); Spacer(Modifier.height(16.dp)); Text("Ronix", fontSize = 34.sp, androidx.compose.ui.text.font.FontWeight.Bold) } }
            is SessionStatus.NotAuthenticated -> AuthGraph(vm)
            else -> {
                LaunchedEffect(vm.busy) { if (!vm.busy && vm.profile == null) vm.loadProfile() }
                val p = vm.profile
                if (p == null) Box(Modifier.fillMaxSize(), Alignment.Center) { RonixLogo(72.dp) }
                else if (p.role == "admin") AdminShell(vm) else StudentShell(vm, p)
            }
        }
    }
}

@Composable fun AuthGraph(vm: AppVm) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.profile = null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).statusBarsPadding().imePadding()) {
        Spacer(Modifier.height(48.dp)); RonixLogo(48.dp); Spacer(Modifier.height(20.dp))
        Title(if (register) "Create your account" else "Welcome to Ronix")
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Sign in", !register) { register = false; vm.error = null }; Pill("Register", register) { register = true; vm.error = null } }
        Spacer(Modifier.height(8.dp))
        if (register) { Field(name, { name = it }, "Name"); Spacer(Modifier.height(10.dp)) }
        Field(email, { email = it }, "Email"); Spacer(Modifier.height(10.dp))
        Field(pw, { pw = it }, "Password", password = true)
        vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Spacer(Modifier.height(20.dp))
        val ok = email.isNotBlank() && pw.length >= 6 && (!register || name.isNotBlank())
        PrimaryButton(if (register) "Create account" else "Sign in", ok && !vm.busy) {
            if (register) vm.signUp(name, email, pw) else vm.signIn(email, pw)
        }
    }
}
