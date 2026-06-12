/*
 * EngLIFT — EnhanceButton.kt
 *
 * A Jetpack Compose composable that renders the ✨ Enhance button and the
 * "Upgrade to Pro" paywall dialog.  Designed to slot into FlorisBoard's
 * existing Smartbar row (TextInputLayout.kt / Smartbar.kt).
 *
 * Drop this file into:
 *   app/src/main/kotlin/dev/patrickgold/florisboard/ime/enhance/
 */
package dev.patrickgold.florisboard.ime.enhance

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
package dev.patrickgold.florisboard.ime.enhance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Colour tokens  (override to match your active FlorisBoard theme)
// ─────────────────────────────────────────────────────────────────────────────

private val AccentPurple  = Color(0xFF7C4DFF)
private val AccentPurpleL = Color(0xFF9E6FFF)
private val SurfaceDark   = Color(0xFF1E1E2E)
private val OnSurface     = Color(0xFFEAEAEA)
private val GoldColor     = Color(0xFFFFD54F)

// ─────────────────────────────────────────────────────────────────────────────
// EnhanceButton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Drop-in composable for the Smartbar / quick-action row.
 *
 * @param enhanceManager   Singleton [EnhanceManager]; pass via LocalContext or DI.
 * @param getCurrentText   Lambda that reads the current editor text (selected or full).
 * @param onTextEnhanced   Lambda called with the enhanced string so the caller can
 *                         commit it via [FlorisImeService] → InputConnection.
 * @param modifier         Optional layout modifier.
 */
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

    // ── Button ────────────────────────────────────────────────────────────────
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
                            is EnhanceResult.Success     -> onTextEnhanced(result.enhancedText)
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
                    modifier = Modifier.size(14.dp),
                    color    = Color.White,
                    strokeWidth = 2.dp,
                )
                Text("Enhancing…", color = Color.White, fontSize = 13.sp)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("✨", fontSize = 13.sp)
                Text(
                    text       = "Enhance",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                // Usage badge
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

    // ── Inline error snackbar ─────────────────────────────────────────────────
    AnimatedVisibility(
        visible = errorMsg != null,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        errorMsg?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3_500)
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
                Text(msg, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }

    // ── Paywall dialog ────────────────────────────────────────────────────────
    if (showPaywall) {
        PaywallDialog(onDismiss = { showPaywall = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Paywall dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaywallDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape           = RoundedCornerShape(20.dp),
            backgroundColor = SurfaceDark,
            elevation       = 12.dp,
            modifier        = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Column(
                modifier                = Modifier.padding(24.dp),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(16.dp),
            ) {
                // Crown + title
                Text("👑", fontSize = 40.sp)
                Text(
                    text       = "Daily Limit Reached",
                    color      = OnSurface,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                )

                // Description
                Text(
                    text      = "You've used all 20 free enhancements for today.\n" +
                                "Upgrade to Pro for unlimited AI-powered enhancements — " +
                                "every single day.",
                    color     = OnSurface.copy(alpha = 0.75f),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                )

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Feature bullets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProFeature("♾️  Unlimited enhancements daily")
                    ProFeature("⚡  Faster processing priority")
                    ProFeature("🎯  Tone & style presets (coming soon)")
                    ProFeature("🌐  Multilingual support (coming soon)")
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Price chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldColor.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = "₹37 / month",
                        color      = GoldColor,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                // Upgrade CTA
                Button(
                    onClick  = {
                        // TODO: launch your billing flow / deep-link here
                        // e.g. context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yoursite.com/upgrade")))
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors   = ButtonDefaults.buttonColors(backgroundColor = AccentPurple),
                    shape    = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "Upgrade to Pro  →",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                    )
                }

                // Dismiss
                TextButton(onClick = onDismiss) {
                    Text(
                        "Maybe later",
                        color    = OnSurface.copy(alpha = 0.5f),
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
