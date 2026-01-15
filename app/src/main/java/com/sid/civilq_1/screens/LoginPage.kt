package com.sid.civilq_1.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sid.civilq_1.Authentication.GoogleSignIn.GoogleSignInUtils
import com.sid.civilq_1.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Slideshow Data
    val slides = listOf(
        SlideData(R.drawable.eye_off, "Report Issues", "Easily report potholes, streetlights, and more with just a few taps."),
        SlideData(R.drawable.eye_on, "Track Progress", "Get real-time updates as authorities work on your reported concerns."),
        SlideData(R.drawable.mic_svgrepo_com, "Better Community", "Join hands with neighbors to make your city a cleaner, safer place.")
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-slide effect for smoothness
    LaunchedEffect(Unit) {
        while(true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        GoogleSignInUtils.doGoogleSignIn(context, scope, null) {
            navController.navigate("home") { popUpTo(0) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 90% SLIDESHOW SECTION ---
        Box(modifier = Modifier.weight(0.9f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                SlideContent(slides[page])
            }

            // Page Indicator (Dots)
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(slides.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFF4A7C59) else Color.LightGray
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        // --- 10% LOGIN SECTION (Minimal) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Google Sign In
            Button(
                onClick = { GoogleSignInUtils.doGoogleSignIn(context, scope, launcher) {
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                } },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3F4)),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Medium)
            }

            // Guest Login (Bypass for Emulator Testing)
            TextButton(
                onClick = {
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                }
            ) {
                Text(
                    "Test Mode: Continue as Guest",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun SlideContent(slide: SlideData) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = slide.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 300f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = slide.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = slide.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}

data class SlideData(val imageRes: Int, val title: String, val description: String)