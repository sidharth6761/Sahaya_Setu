package com.sid.civilq_1.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sid.civilq_1.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    title: String,
    context: Context,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var mainAddress by remember { mutableStateOf("Fetching...") }
    var exactAddress by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getUserLocation(context) { main, exact ->
                mainAddress = main
                exactAddress = exact
            }
        } else {
            mainAddress = "Permission denied"
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    TopAppBar(
        title = {

                Row {
                    Image(
                        painter = painterResource(id = R.drawable.apppic),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .offset(y = 5.dp,x=-18.dp)
                    )

                    val AkayaKanadakaFont = FontFamily(
                        Font(R.font.akaya_kanadaka)
                    )

                    Text(
                        text = "Sahaya Setu",
                        fontSize = 43.sp,
                        fontFamily = AkayaKanadakaFont,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.offset(y = 8.dp,x=-14.dp)
                    )
                }

        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF98FB98) // pale green
        ),
        modifier = Modifier
            .shadow(8.dp) // adds elevation shadow
            // avoid coloring the notification bar
    )

}
