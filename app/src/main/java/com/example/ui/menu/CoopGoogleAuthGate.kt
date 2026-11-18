package com.example.ui.menu

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.BorderSteel
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.DriftingEmbersBackdrop
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldBuff
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.GrimdarkBevelPanel
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.InputDark
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.ui.theme.grimdarkBevelShape
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun attemptAutoGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val activity = context.findActivity() ?: context
    scope.launch {
        try {
            val cred = withTimeout(10000L) {
                credentialManager.getCredential(activity, request).credential
            }
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                withContext(Dispatchers.Main) {
                    onAuthSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onUnauthenticated()
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                onUnauthenticated()
            }
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to locate foreground Activity for Google Sign-In.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val cred = withTimeout(15000L) {
                credentialManager.getCredential(activity, request).credential
            }
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                withContext(Dispatchers.Main) {
                    onAuthSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onAuthError("Unexpected credential type returned.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("Auth", "Google Sign-In timed out or Play Services unresponsive: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onAuthError("Google Play Services is not responding. Please update Google Play Services or try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onAuthCancelled()
            }
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials found on device: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onAuthError("No Google account available on this device. Please sign into a Google account in device Settings.")
            }
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google Sign-In credential error", e)
            withContext(Dispatchers.Main) {
                onAuthError(e.localizedMessage ?: "Google Sign-In failed (Play Services unresponsive)")
            }
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            withContext(Dispatchers.Main) {
                onAuthError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }
}

fun signInWithCommanderEmail(
    commanderName: String,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val sanitized = commanderName.trim().ifBlank { "Commander" }
    val email = "cmd_${sanitized.lowercase().replace(Regex("[^a-z0-9]"), "")}_${Math.abs(sanitized.hashCode()) % 100000}@crimsonsector.app"
    val password = "crimson_sector_secure_pass"
    scope.launch {
        try {
            try {
                Firebase.auth.signInWithEmailAndPassword(email, password).await()
            } catch (_: Exception) {
                Firebase.auth.createUserWithEmailAndPassword(email, password).await()
            }
            onAuthSuccess()
        } catch (e: Exception) {
            onAuthError(e.localizedMessage ?: "Commander Login failed")
        }
    }
}

fun signOutGoogleAccount(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onCleanupBeforeSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            runCatching { onCleanupBeforeSignOut() }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun CoopGoogleAuthScreen(
    onBackToMenu: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler { onBackToMenu() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var commanderName by remember { mutableStateOf("Commander") }

    LaunchedEffect(Unit) {
        attemptAutoGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF171D28), VoidObsidian),
                    radius = 1400f
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_setup_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.16f,
            modifier = Modifier.matchParentSize()
        )
        DriftingEmbersBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GrimdarkBevelPanel(
                cut = 10.dp,
                borderColor = ImperialGold,
                topColor = CardSlate,
                bottomColor = VoidObsidian,
                accentBarColor = TacticalCyan,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 580.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onBackToMenu,
                            shape = grimdarkBevelShape(4.dp),
                            border = BorderStroke(1.dp, BorderSteel),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = InputDark,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("MENU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.Public, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(26.dp))
                    }

                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(40.dp))

                    Text(
                        text = "ONLINE CO-OP COMMAND LINK",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Sign in with Google to create or join long-distance online rooms using a 6-digit Room Code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    errorMessage?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(5.dp))
                                .background(CrimsonDeep.copy(alpha = 0.7f))
                                .border(1.dp, CrimsonSoft, CutCornerShape(5.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonSoft, modifier = Modifier.size(16.dp))
                            Text(text = err, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading && !isCheckingSilent,
                        shape = grimdarkBevelShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSurface,
                            contentColor = EmeraldBuff
                        ),
                        border = BorderStroke(1.5.dp, EmeraldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("coop_google_signin_button")
                    ) {
                        if (isLoading || isCheckingSilent) {
                            CircularProgressIndicator(
                                color = EmeraldBuff,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("AUTHENTICATING...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        } else {
                            Text("SIGN IN WITH GOOGLE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = commanderName,
                        onValueChange = { commanderName = it },
                        label = { Text("Commander Name", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TacticalCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalCyan,
                            unfocusedBorderColor = BorderSteel,
                            focusedLabelColor = TacticalCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = TacticalCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            signInWithCommanderEmail(
                                commanderName = commanderName,
                                scope = scope,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                }
                            )
                        },
                        enabled = !isLoading && !isCheckingSilent,
                        shape = grimdarkBevelShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSlate,
                            contentColor = TacticalCyan
                        ),
                        border = BorderStroke(1.5.dp, TacticalCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("coop_commander_login_button")
                    ) {
                        Text("QUICK COMMANDER LOGIN (BYPASS GOOGLE)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onBackToMenu,
                        shape = grimdarkBevelShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = InputDark,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, BorderSteel),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("coop_return_menu_button")
                    ) {
                        Text("RETURN TO MAIN MENU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
