package com.mealoo.app.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.mealoo.app.R

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: (GoogleSignInAccount) -> Unit,
    onSkipLogin: () -> Unit
) {
    val context = LocalContext.current

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    var isLoading by remember { mutableStateOf(false) }
    val ld = stringResource(R.string.login_loading)
    val ldp = stringResource(R.string.login_google_starting)
    var loadingText by remember { mutableStateOf(ld) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("LoginPage", "Google 登录成功: ${account.email}")
            onLoginSuccess(account)   // 这里把 account 往上抛
        } catch (e: ApiException) {
            Log.e("LoginPage", "Google 登录失败: ${e.statusCode}", e)
            isLoading = false
        }
    }

    // ------------------------------
    // UI Content
    // ------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Google 登录按钮
            Button(
                onClick = {
                    isLoading = true
                    loadingText = ldp
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.login_with_google))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 跳过登录按钮
            TextButton(
                onClick = {
                    if (!isLoading) onSkipLogin()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login_skip))
            }
        }

        // Loading Overlay
        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadingText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
