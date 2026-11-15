package com.prayergate.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.prayergate.app.R
import com.prayergate.app.data.AppLanguage

/** The Activity behind a Compose [Context], or null. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Saves the language and redraws the screen in it. The screen is recreated,
 * and the screen you were on is kept (see rememberSaveable in AppRoot).
 * The widget is redrawn too, so it changes language with the app.
 */
@Composable
fun rememberLanguageSetter(): (AppLanguage) -> Unit {
    val context = LocalContext.current
    return { language ->
        if (language != AppLanguage.saved(context)) {
            AppLanguage.save(context, language)
            PrayerGateWidget.refresh(context)
            context.findActivity()?.recreate()
        }
    }
}

/** True when the screen is showing Korean, whether that was chosen or comes from the phone. */
@Composable
fun isKoreanShown(): Boolean = LocalConfiguration.current.locales[0].language == "ko"

/**
 * "English · 한국어": a small two-way switch for the welcome screen, where a
 * new user has not yet had a way to find Settings. The language being shown is
 * underlined, so colour is not the only signal. Names are always written in
 * their own language.
 */
@Composable
fun LanguageToggle(modifier: Modifier = Modifier) {
    val setLanguage = rememberLanguageSetter()
    val korean = isKoreanShown()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((language, name, isShown) in listOf(
            Triple(AppLanguage.ENGLISH, stringResource(R.string.lang_english), !korean),
            Triple(AppLanguage.KOREAN, stringResource(R.string.lang_korean), korean),
        )) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .semantics { selected = isShown }
                    .clickable(role = Role.RadioButton) { setLanguage(language) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isShown) 1f else 0.6f),
                    textDecoration = if (isShown) TextDecoration.Underline else null,
                )
            }
        }
    }
}
