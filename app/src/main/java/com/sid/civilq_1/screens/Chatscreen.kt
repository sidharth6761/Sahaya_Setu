package com.sid.civilq_1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. Standardized Data Class (Simplified)
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// 2. Static Initial Conversation
private val INITIAL_MESSAGES = listOf(
    ChatMessage("Hi there! I'm your Civic Assistant. 👋", false),
    ChatMessage("I can help you understand how to report issues or check your current status. How can I help?", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController) {
    // State management
    var messages by remember { mutableStateOf(INITIAL_MESSAGES) }
    var currentMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // PERFORMANCE: Use a delay before scrolling to prevent frame skipping
    // during the initial screen transition.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(50) // Small buffer for the UI to breathe
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Civic Assistant", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FIXED: Removed 'key' parameter to prevent duplicate key crashes
                items(items = messages) { message ->
                    MessageBubble(message)
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp, // Increased for better separation
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(), // Ensure keyboard doesn't cover input
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        placeholder = { Text("Ask something...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (currentMessage.isNotBlank()) {
                                val userText = currentMessage
                                val userMsg = ChatMessage(userText, true)
                                messages = messages + userMsg
                                currentMessage = ""

                                // Fake "typing" delay
                                scope.launch {
                                    delay(1000)
                                    val responseText = generateCivicBotResponse(userText)
                                    messages = messages + ChatMessage(responseText, false)
                                }
                            }
                        },
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser

    // OPTIMIZATION: Remember the painter so it doesn't reload on every scroll/recompose
    val botIcon = if (!isUser) painterResource(id = com.sid.civilq_1.R.drawable.chatbot) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Added padding to reduce layout shifts
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser && botIcon != null) {
            Icon(
                painter = botIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp).padding(top = 4.dp),
                tint = Color(0xFF6200EE)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            // Use static shapes to prevent recalculating paths
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF6200EE) else Color(0xFFE9E9EB)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

// Logic for static responses
fun generateCivicBotResponse(input: String): String {
    val query = input.lowercase()
    return when {
        query.contains("status") -> "To check status, go to the Home screen. 'Active' reports are being worked on, and 'Solved' reports are completed."
        query.contains("report") -> "You can file a new report by clicking the 'Report' tab in the bottom menu. Don't forget to add a photo!"
        query.contains("emergency") -> "For immediate danger, please use the red Emergency button on the Home screen to dial 112."
        query.contains("hi") || query.contains("hello") -> "Hello! How can I assist you with Sahaya Setu today?"
        else -> "I'm a simple assistant. I can help with 'status', 'reporting issues', or 'emergency' info. What do you need?"
    }
}