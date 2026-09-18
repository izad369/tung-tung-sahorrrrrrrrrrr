package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HorrorColorScheme =
  darkColorScheme(
    primary = BloodRed,
    onPrimary = BoneWhite,
    primaryContainer = DarkBlood,
    onPrimaryContainer = BoneWhite,
    secondary = FlashlightAmber,
    onSecondary = HorrorBlack,
    secondaryContainer = HorrorCardSurface,
    onSecondaryContainer = BoneWhite,
    tertiary = DecayGreen,
    background = HorrorBlack,
    onBackground = BoneWhite,
    surface = HorrorDarkSurface,
    onSurface = BoneWhite,
    surfaceVariant = HorrorCardSurface,
    onSurfaceVariant = BoneWhite,
    error = BloodRed
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = HorrorColorScheme,
    typography = Typography,
    content = content
  )
}
