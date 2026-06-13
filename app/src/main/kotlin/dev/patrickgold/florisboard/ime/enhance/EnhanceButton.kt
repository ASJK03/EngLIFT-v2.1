package dev.patrickgold.florisboard.ime.enhance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AccentPurple = Color(0xFF7C4DFF)
private val SurfaceDark  = Color(0xFF1E1E2E)
private val OnSurface    = Color(0xFFEAEAEA)
private val GoldColor    = Color(0xFFFFD54F)

@Composable
fun EnhanceButton(
    enhanceManager: EnhanceManager,
    getCurrentText: () -> String,
    onTextEnhanced: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope       = rememberCoroutineScope()
    var isLoading   by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    val remaining   by remember { derivedStateOf { enhanceManager.remainingToday() } }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (!isLoading) AccentPurple else AccentPurple.copy(alpha = 0.5f))
            .clickable(enabled = !isLoading) {
                if (enhanceManager.isLimitReached()) {
                    showPaywall = true
                } else {
                    scope.launch {
                        isLoading = true
                        errorMsg  = null
                        val text   = getCurrentText()
                        val result = enhanceManager.enhance(text)
                        isLoading  = false
                        when (result) {
                            is EnhanceResult.Success      -> onTextEnhanced(result.enhancedText)
                            is EnhanceResult.LimitReached -> showPaywall = true
                            is EnhanceResult.Error        -> errorMsg = result.message
                        }
                    }
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(14.dp),
                    color       = Color.White,
                    strokeWidth = 2.dp,
                )
                Text(
                    text  = "Enhancing…",
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("✨", fontSize = 13.sp)
                Text(
                    text       = "Enhance",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text     = "$remaining",
                        color    = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = errorMsg != null,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        errorMsg?.let { msg ->
            LaunchedEffect(msg) {
                delay(3_500)
                errorMsg = null
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFB00020))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = msg,
                    color     = Color.White,
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showPaywall) {
        PaywallDialog(onDismiss = { showPaywall = false })
    }
}

@Composable
private fun PaywallDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("👑", fontSize = 40.sp)
                Text(
                    text       = "Daily Limit Reached",
                    color      = OnSurface,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                )
                Text(
                    text      = "You've used all 10 free enhancements for today.\n" +
                                "Upgrade to Pro for unlimited AI-powered enhancements.",
                    color     = OnSurface.copy(alpha = 0.75f),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProFeature("♾️  Unlimited enhancements daily")
                    ProFeature("⚡  Faster processing priority")
                    ProFeature("🎯  Tone & style presets (coming soon)")
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldColor.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = "₹72 / month",
                        color      = GoldColor,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Button(
                    onClick  = { onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text       = "Upgrade to Pro  →",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text  = "Maybe later",
                        color = OnSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProFeature(text: String) {
    Text(
        text      = text,
        color     = OnSurface.copy(alpha = 0.85f),
        fontSize  = 13.sp,
        modifier  = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
    )
}
