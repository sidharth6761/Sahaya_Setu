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
            .height(50.dp)
            .background(
                color = if (isRecording) Color(0xFFFFEBEE) else Color(0xFFF1F3F4),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Record/Stop Button
        IconButton(
            onClick = onMicClick,
            modifier = Modifier.size(38.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isRecording) Color(0xFFD32F2F) else Color(0xFF4A7C59),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isRecording) R.drawable.stopaudio else R.drawable.mic_svgrepo_com
                    ),
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Waveform
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 3.dp.toPx()
                val gap = 2.dp.toPx()
                val totalWidth = barWidth + gap

                audioWaveformData.forEachIndexed { index, amplitude ->
                    val x = index * totalWidth
                    if (x + barWidth <= size.width) {
                        val barHeight = (amplitude * size.height).coerceAtLeast(4.dp.toPx())
                        drawRoundRect(
                            color = if (isRecording) Color(0xFFD32F2F) else Color(0xFF9E9E9E),
                            topLeft = Offset(x, (size.height - barHeight) / 2),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }
    }
}