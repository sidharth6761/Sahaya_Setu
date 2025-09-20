package com.sid.civilq_1.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sid.civilq_1.R

@Composable
fun ProfileScreen(navController: NavHostController) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 🔹 Full-width profile image at the top
        Image(
            painter = painterResource(id = R.drawable.userproficon),
            contentDescription = "Profile Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp), // adjust height as needed
            contentScale = ContentScale.Crop
        )

        // 👇 Added green "50 points" text below profile image
        Text(
            text = "50 points",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50), // green color
            modifier = Modifier

                .padding(start = 46.dp, top = 4.dp) // align left with margin
        )

        Spacer(modifier = Modifier.height(0.dp))

        Image(
            painter = painterResource(id = R.drawable.progbar),
            contentDescription = "Profile Progress",
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp), // adjust height as needed
            contentScale = ContentScale.Crop
        )

        // 🔹 Profile Name & Action
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Active Citizen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Post a Complaint Now",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    // navController.navigate("complaint")
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 Your Details Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Your Details", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "EDIT",
                color = Color.Red,
                modifier = Modifier.clickable {
                    // navController.navigate("editProfile")
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ProfileDetailRow(icon = Icons.Default.Phone, info = "9339386594")
        ProfileDetailRow(icon = Icons.Default.Email, info = "Add your email")
        ProfileDetailRow(
            icon = Icons.Default.Place,
            info = "King's Place - 15, Chandaka Industrial Estate, Patia, Bhubaneswar, Odisha 751024, India"
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can also",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val menuItems = listOf(
            "Check Notifications",
            "Change language",
            "Rate Us on Playstore",
            "Report if something isn’t working",
            "Read our privacy policy",
            "Check What’s New",
            "Logout"
        )

        menuItems.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* handle click */ }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item, style = MaterialTheme.typography.bodyMedium)
            }
            Divider()
        }
    }
}

@Composable
fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, info: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = info, style = MaterialTheme.typography.bodyMedium)
    }
}
