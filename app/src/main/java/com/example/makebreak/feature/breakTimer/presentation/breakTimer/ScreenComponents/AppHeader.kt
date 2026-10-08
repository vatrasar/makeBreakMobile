package com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.makebreak.R

/**
 * Top branding header displaying the MakeBreak app logo and application title.
 *
 * Usage:
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * App branding icon image, application title text.
 *
 * Used In:
 * BreakTimerScreen
 */
@Composable
fun AppHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("appHeaderContainer"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(36.dp)
                .testTag("appHeaderLogo")
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
