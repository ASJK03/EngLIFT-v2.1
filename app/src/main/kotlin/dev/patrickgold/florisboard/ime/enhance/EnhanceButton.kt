package dev.patrickgold.florisboard.ime.enhance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val AccentPurple = Color(0xFF7C4DFF)

@Composable
fun EnhanceButton(
    enhanceManager: EnhanceManager,
    getCurrentText: () -> String,
    onTextEnhanced: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val remaining by remember { derivedStateOf { enhanceManager.remainingToday() } }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AccentPurple)
            .clickable(enabled = !isLoading) {
                if (enhanceManager.isLimitReached()) {
                    // Show message: limit reached
                } else {
                    scope.launch {
                        isLoading = true
                        val text = getCurrentText()
                        val result = enhanceManager.enhance(text)
                        isLoading = false
                        when (result) {
                            is EnhanceResult.Success -> onTextEnhanced(result.enhancedText)
                            else -> {} // Error handling
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isLoading) "✨ Enhancing..." else "✨ Enhance ($remaining)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// Placeholder composable function for Text if needed
@Composable
fun Text(text: String, color: Color = Color.Black, fontSize: androidx.compose.ui.unit.TextUnit = sp(14), fontWeight: FontWeight? = null) {
    androidx.compose.material.Text(text = text, color = color, fontSize = fontSize, fontWeight = fontWeight)
}
