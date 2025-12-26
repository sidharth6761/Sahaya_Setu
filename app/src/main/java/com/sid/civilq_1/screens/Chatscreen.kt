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

// Standardized Data Class
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Static Initial Conversation
private val INITIAL_MESSAGES = listOf(
    ChatMessage("Hi there! I'm your Civic Assistant. 👋", false),
    ChatMessage("I can help you understand how to report issues or check your current status. How can I help?", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController) {
    var messages by remember { mutableStateOf(INITIAL_MESSAGES) }
    var currentMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // PERFORMANCE: Auto-scroll only when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = messages, key = { it.timestamp }) { message ->
                    MessageBubble(message)
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        placeholder = { Text("Ask something...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (currentMessage.isNotBlank()) {
                                val userMsg = ChatMessage(currentMessage, true)
                                val responseText = generateCivicBotResponse(currentMessage)
                                messages = messages + userMsg
                                currentMessage = ""

                                // Fake "typing" delay for better UX
                                scope.launch {
                                    delay(600)
                                    messages = messages + ChatMessage(responseText, false)
                                }
                            }
                        },
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                painter = painterResource(id = com.sid.civilq_1.R.drawable.chatbot),
                contentDescription = null,
                modifier = Modifier.size(24.dp).padding(top = 4.dp),
                tint = Color(0xFF6200EE)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
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