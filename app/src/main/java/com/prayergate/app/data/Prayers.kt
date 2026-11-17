package com.prayergate.app.data

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

/**
 * The themes a prayer can belong to. The user picks which ones to pray
 * through in Settings. The two-letter id prefix is set in tools/make_prayers.py.
 */
enum class PrayerTheme { TEMPTATION, JUDGING, NEIGHBOR, INFLUENCE, DISCERNMENT, COMPARISON, WORDS, TIME }

/** One short prayer, as stored in assets/prayers.json. */
data class Prayer(
    val id: String,              // e.g. "tm01"; the key for saved prayers, never renumbered
    val theme: PrayerTheme,
    val text: String,            // English
    val textKo: String,          // Korean
    val reference: String,       // "Matthew 26:41"
    val referenceKo: String,     // "마태복음 26:41"
    val verse: String,           // the words of that passage, World English Bible
    val verseKo: String,         // the same passage, 개역한글 (1961)
) {
    fun text(korean: Boolean) = if (korean) textKo else text
    fun reference(korean: Boolean) = if (korean) referenceKo else reference
    fun verse(korean: Boolean) = if (korean) verseKo else verse
}

/** Turns the text of prayers.json into a list of [Prayer] objects. */
fun parsePrayers(json: String): List<Prayer> {
    val array = JSONArray(json)
    return List(array.length()) { i ->
        val item = array.getJSONObject(i)
        Prayer(
            id = item.getString("id"),
            theme = PrayerTheme.valueOf(item.getString("theme")),
            text = item.getString("text"),
            textKo = item.getString("textKo"),
            reference = item.getString("reference"),
            referenceKo = item.getString("referenceKo"),
            verse = item.getString("verse"),
            verseKo = item.getString("verseKo"),
        )
    }
}

/** The prayers in the chosen themes, or all of them if no theme is chosen. */
private fun poolFor(all: List<Prayer>, themes: Set<PrayerTheme>): List<Prayer> =
    all.filter { it.theme in themes }.ifEmpty { all }

/**
 * Chooses the next prayer like drawing cards from a shuffled deck: every
 * prayer in the chosen themes comes up once before any comes up again.
 *
 * [recentIds] is the list of prayers already shown, newest first (from the
 * log). Walking back through it until every prayer in the pool has been seen
 * tells us which prayers are still "in the deck" this round. When the deck is
 * empty a new round starts, but the prayer shown last is left out so the same
 * one never appears twice in a row.
 *
 * Plain Kotlin with no Android code, so it can be unit tested.
 */
fun pickPrayer(
    all: List<Prayer>,
    recentIds: List<String>,
    themes: Set<PrayerTheme>,
    random: Random = Random.Default,
): Prayer {
    val pool = poolFor(all, themes)
    val poolIds = pool.map { it.id }.toSet()
    val seenThisRound = HashSet<String>()
    for (id in recentIds) {
        if (id !in poolIds) continue
        if (seenThisRound.size == poolIds.size - 1 && id !in seenThisRound) break
        seenThisRound.add(id)
    }
    var deck = pool.filter { it.id !in seenThisRound }
    if (deck.isEmpty()) {
        val last = recentIds.firstOrNull()
        deck = pool.filter { it.id != last }.ifEmpty { pool }
    }
    return deck[random.nextInt(deck.size)]
}

/**
 * "Today's prayer" for the home-screen widget: the same prayer all day, a
 * different one each day, from the chosen themes.
 *
 * @param epochDay days since 1 January 1970 (LocalDate.toEpochDay()).
 */
fun prayerOfTheDay(all: List<Prayer>, themes: Set<PrayerTheme>, epochDay: Long): Prayer {
    val pool = poolFor(all, themes).sortedBy { it.id }
    // Multiplying by a prime jumps around the list instead of going in order.
    val index = Math.floorMod(epochDay * 37, pool.size.toLong()).toInt()
    return pool[index]
}

/**
 * How long the Amen button waits before it can be held: about a fifth of a
 * second per English word, at least 4 seconds. Long enough to read the
 * prayer, short enough not to feel like a lock. The English word count is
 * used for both languages so a prayer takes the same time in each.
 */
fun readingMillis(prayer: Prayer): Long {
    val words = prayer.text.split(' ').count { it.isNotBlank() }
    return maxOf(4_000L, words * 200L)
}

/** Loads the bundled prayers once and keeps them in memory. */
object PrayerBook {
    private var cached: List<Prayer>? = null

    fun load(context: Context): List<Prayer> {
        cached?.let { return it }
        val json = context.assets.open("prayers.json").bufferedReader().use { it.readText() }
        return parsePrayers(json).also { cached = it }
    }
}
