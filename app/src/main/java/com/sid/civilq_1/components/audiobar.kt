package com.sid.civilq_1.components

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import com.sid.civilq_1.R



@Composable
fun VoiceNoteBar(
    isRecording: Boolean,
    onMicClick: () -> Unit,
    audioWaveformData: List<Float>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMicClick) {
            Icon(
                painter = painterResource(
                    if (isRecording) R.drawable.mic_svgrepo_com else R.drawable.mic_off
                ),
                contentDescription = "Record",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF2196F3), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width / (audioWaveformData.size * 2)
                audioWaveformData.forEachIndexed { index, amplitude ->
                    drawRect(
                        color = Color(0xFF2196F3),
                        topLeft = Offset(
                            index * barWidth * 2,
                            size.height / 2 - amplitude * size.height / 2
                        ),
                        size = Size(barWidth, amplitude * size.height)
                    )
                }
            }
        }
    }
}
