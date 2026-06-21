package com.helix.flashcards.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Max width for content on large screens so tablets don't look stretched. */
val ContentMaxWidth = 560.dp

/** Centers content and caps its width on wide screens. */
fun Modifier.responsiveWidth(): Modifier = this
    .fillMaxWidth()
    .widthIn(max = ContentMaxWidth)

/** "01. My card" / "03. Image" — position is 0-based. */
fun cardLabel(position: Int, frontText: String): String {
    val num = (position + 1).toString().padStart(2, '0')
    val name = frontText.ifBlank { "Image" }
    return "$num. $name"
}

/** Slim header — no wasted vertical space. */
@Composable
fun CompactHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AccentLight)
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = AccentLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
        HorizontalDivider(color = DarkCard, thickness = 1.dp)
    }
}