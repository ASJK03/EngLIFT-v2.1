package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.enhance.EnhanceButton
import dev.patrickgold.florisboard.ime.enhance.EnhanceManager
import dev.patrickgold.florisboard.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.InlineSuggestionsStyleCache
import dev.patrickgold.florisboard.ime.smartbar.Smartbar
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsOverflowPanel
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardLayout
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.ui.SnyggIcon

@Composable
fun TextInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val prefs by FlorisPreferenceStore
    val state by keyboardManager.activeState.collectAsState()
    val evaluator by keyboardManager.activeEvaluator.collectAsState()
    val editorInstance by context.editorInstance()
    val enhanceManager = remember { EnhanceManager(context) }

    InlineSuggestionsStyleCache()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EnhanceButton(
                enhanceManager = enhanceManager,
                getCurrentText = {
                    val content = editorInstance.activeContent
                    val selected = content.selectedText.toString()
                    if (selected.isNotBlank()) selected
                    else content.text.toString()
                },
                onTextEnhanced = { enhanced ->
                    editorInstance.commitText(enhanced)
                },
                modifier = Modifier.padding(start = 6.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                Smartbar()
            }
        }
        if (state.isActionsOverflowVisible) {
            QuickActionsOverflowPanel()
        } else {
            Box {
                val incognitoDisplayMode by prefs.keyboard.incognitoDisplayMode.collectAsState()
                val showIncognitoIcon = evaluator.state.isIncognitoMode &&
                    incognitoDisplayMode == IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD
                if (showIncognitoIcon) {
                    SnyggIcon(
                        FlorisImeUi.IncognitoModeIndicator.elementName,
                        modifier = Modifier
                            .matchParentSize()
                            .align(Alignment.Center),
                        painter = painterResource(R.drawable.ic_incognito),
                    )
                }
                TextKeyboardLayout(evaluator = evaluator)
            }
        }
    }
}
