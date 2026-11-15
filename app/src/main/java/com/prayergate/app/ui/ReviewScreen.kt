package com.prayergate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Difficulty
import com.prayergate.app.data.Question
import com.prayergate.app.data.QuestionBank
import com.prayergate.app.data.QuestionType
import com.prayergate.app.ui.theme.LocalAccent

/**
 * The Review tab: every question whose last answer was wrong, with the right
 * answer and the explanation. "Practise" opens the question screen in
 * practice mode; answering a question correctly there removes it from this list.
 */
@Composable
fun ReviewScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.questionProgressDao()
    val toReview by dao.observeToReview().collectAsStateWithLifecycle(initialValue = null)
    val allQuestions = remember { QuestionBank.load(context).associateBy { it.id } }
    val showKorean = remember { (context.applicationContext as PrayerGateApp).settings.koreanExplanations }

    val loaded = toReview ?: return
    val questions = loaded.mapNotNull { allQuestions[it.questionId] }

    LazyColumn(
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 36.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().readableWidth(),
    ) {
        item {
            Text(
                withAccentFullStop(stringResource(R.string.review_title)),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (questions.isEmpty()) R.string.review_empty else R.string.review_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (questions.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                PrimaryButton(
                    pluralStringResource(R.plurals.review_practise, questions.size, questions.size),
                    onClick = { context.startActivity(QuestionActivity.practiceIntent(context)) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(questions, key = { it.id }) { question ->
            ReviewCard(question, showKorean, Modifier.animateItem())
        }
    }
}

/** One question to review: its type and level, the question, the right answer and why. */
@Composable
private fun ReviewCard(question: Question, showKorean: Boolean, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current.accent
    val type = stringResource(
        if (question.type == QuestionType.VOCABULARY) R.string.type_vocabulary else R.string.type_grammar
    )
    val level = stringResource(
        when (question.difficulty) {
            Difficulty.EASY -> R.string.difficulty_easy
            Difficulty.MEDIUM -> R.string.difficulty_medium
            Difficulty.HARD -> R.string.difficulty_hard
        }
    )
    Column(
        modifier
            .fillMaxWidth()
            .card()
            .padding(20.dp),
    ) {
        SectionLabel(stringResource(R.string.review_card_label, type, level))
        Spacer(Modifier.height(8.dp))
        Text(question.question, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        // "Answer: risen", with the answer in the accent colour.
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.review_answer_prefix))
                append(" ")
                withStyle(SpanStyle(color = accent)) { append(question.correctAnswer) }
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(question.explanation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val korean = question.explanationKo
        if (showKorean && korean != null) {
            Text(
                keepKoreanWordsTogether(korean),
                style = koreanStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
