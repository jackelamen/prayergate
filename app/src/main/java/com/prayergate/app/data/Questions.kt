package com.prayergate.app.data

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

/** One multiple-choice question, as stored in assets/questions.json. */
data class Question(
    val id: String,
    val type: QuestionType,       // VOCABULARY or GRAMMAR
    val difficulty: Difficulty,
    val question: String,
    val choices: List<String>,    // always 4
    val answerIndex: Int,         // which of the 4 choices is correct (0-3)
    val explanation: String,
    val explanationKo: String? = null, // the same explanation in Korean
) {
    val correctAnswer: String get() = choices[answerIndex]
}

/**
 * The difficulty of a question, read from the first letter of its id
 * ("ev01" is Easy, "mg12" is Medium, "hv40" is Hard).
 */
fun difficultyOfId(id: String): Difficulty? = when (id.firstOrNull()) {
    'e' -> Difficulty.EASY
    'm' -> Difficulty.MEDIUM
    'h' -> Difficulty.HARD
    else -> null
}

/** Turns the text of questions.json into a list of [Question] objects. */
fun parseQuestions(json: String): List<Question> {
    val array = JSONArray(json)
    val result = ArrayList<Question>()
    for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        val choicesJson = item.getJSONArray("choices")
        val choices = List(choicesJson.length()) { choicesJson.getString(it) }
        result.add(
            Question(
                id = item.getString("id"),
                type = QuestionType.valueOf(item.getString("type")),
                difficulty = Difficulty.valueOf(item.getString("difficulty")),
                question = item.getString("question"),
                choices = choices,
                answerIndex = item.getInt("answerIndex"),
                explanation = item.getString("explanation"),
                explanationKo = item.optString("explanationKo").ifBlank { null },
            )
        )
    }
    return result
}

/**
 * Chooses the next question.
 *
 * The rule is "random, but wrong ones come back sooner". Every question that
 * matches the user's settings gets a number of raffle tickets (a weight):
 *
 *   - last answer was wrong  -> 6 tickets
 *   - never seen before      -> 2 tickets
 *   - last answer was right  -> 1 ticket
 *
 * Then one ticket is drawn at random. The question shown most recently is
 * left out, so the same question never appears twice in a row.
 *
 * This is a plain function with no Android code, so it can be unit tested.
 */
fun pickQuestion(
    all: List<Question>,
    progress: List<QuestionProgress>,
    difficulty: Difficulty,
    type: QuestionType,
    random: Random = Random.Default,
): Question {
    // 1. Keep only the questions that match the settings.
    var pool = all.filter { it.difficulty == difficulty && (type == QuestionType.BOTH || it.type == type) }
    if (pool.isEmpty()) pool = all

    // 2. Leave out the question the user saw most recently.
    val lastSeenId = progress.maxByOrNull { it.lastSeenAt }?.questionId
    if (pool.size > 1) pool = pool.filter { it.id != lastSeenId }

    // 3. Give each question its weight.
    val progressById = progress.associateBy { it.questionId }
    val weights = pool.map { question ->
        val p = progressById[question.id]
        when {
            p == null -> 2
            p.lastAnswerWrong -> 6
            else -> 1
        }
    }

    // 4. Draw one ticket.
    var ticket = random.nextInt(weights.sum())
    for (i in pool.indices) {
        ticket -= weights[i]
        if (ticket < 0) return pool[i]
    }
    return pool.last()
}

/**
 * "Today's question" for the home-screen widget: the same question all day,
 * a different one each day, chosen from the user's level and question type.
 *
 * @param epochDay days since 1 January 1970 (LocalDate.toEpochDay()).
 */
fun questionOfTheDay(all: List<Question>, difficulty: Difficulty, type: QuestionType, epochDay: Long): Question {
    var pool = all.filter { it.difficulty == difficulty && (type == QuestionType.BOTH || it.type == type) }
    if (pool.isEmpty()) pool = all
    pool = pool.sortedBy { it.id }
    // Multiplying by a prime jumps around the list instead of going in order.
    val index = Math.floorMod(epochDay * 37, pool.size.toLong()).toInt()
    return pool[index]
}

/** Loads the bundled questions once and keeps them in memory. */
object QuestionBank {
    private var cached: List<Question>? = null

    fun load(context: Context): List<Question> {
        cached?.let { return it }
        val json = context.assets.open("questions.json").bufferedReader().use { it.readText() }
        return parseQuestions(json).also { cached = it }
    }
}
