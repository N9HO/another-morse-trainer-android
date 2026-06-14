package app.anothermorsetrainer

/**
 * Matches the messy free-form text a speech recogniser returns to one of a
 * drill's answer options. Single letters are the hard case (a recogniser hears
 * "B" as "be"/"bee", "C" as "see") so we accept the letter itself, its NATO
 * phonetic, common homophones, and digit words. Words/meanings match directly.
 */
object AnswerMatch {

    private val nato = mapOf(
        'A' to "alpha", 'B' to "bravo", 'C' to "charlie", 'D' to "delta", 'E' to "echo",
        'F' to "foxtrot", 'G' to "golf", 'H' to "hotel", 'I' to "india", 'J' to "juliet",
        'K' to "kilo", 'L' to "lima", 'M' to "mike", 'N' to "november", 'O' to "oscar",
        'P' to "papa", 'Q' to "quebec", 'R' to "romeo", 'S' to "sierra", 'T' to "tango",
        'U' to "uniform", 'V' to "victor", 'W' to "whiskey", 'X' to "xray", 'Y' to "yankee", 'Z' to "zulu"
    )

    private val letterHomophones = mapOf(
        'A' to listOf("a", "ay", "eh"), 'B' to listOf("be", "bee"), 'C' to listOf("see", "sea", "cee"),
        'D' to listOf("dee"), 'E' to listOf("e", "ee"), 'G' to listOf("gee", "jee"), 'I' to listOf("i", "eye"),
        'J' to listOf("jay"), 'K' to listOf("kay", "okay"), 'L' to listOf("el", "ell"), 'M' to listOf("em"),
        'N' to listOf("en"), 'O' to listOf("oh", "owe", "o"), 'P' to listOf("pee", "pea"),
        'Q' to listOf("cue", "queue"), 'R' to listOf("are", "arr"), 'S' to listOf("es", "ess"),
        'T' to listOf("tee", "tea"), 'U' to listOf("you", "ewe"), 'V' to listOf("vee"),
        'W' to listOf("double you", "dub"), 'X' to listOf("ex", "eks"), 'Y' to listOf("why", "wy"), 'Z' to listOf("zee", "zed")
    )

    private val digitWords = mapOf(
        '0' to listOf("zero", "oh", "o"), '1' to listOf("one", "won"), '2' to listOf("two", "to", "too"),
        '3' to listOf("three"), '4' to listOf("four", "for", "fore"), '5' to listOf("five"),
        '6' to listOf("six"), '7' to listOf("seven"), '8' to listOf("eight", "ate"), '9' to listOf("nine")
    )

    /** The best-matching option for the recognised [candidates], or null if none fits. */
    fun match(candidates: List<String>, options: List<String>): String? {
        val cands = candidates.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (cands.isEmpty()) return null

        // 1. Direct (case-insensitive) equality.
        for (c in cands) options.firstOrNull { it.trim().lowercase() == c }?.let { return it }

        // 2. Per option, the appropriate fuzzy rules.
        for (option in options) {
            val key = option.trim()
            if (key.length == 1) {
                val ch = key[0].uppercaseChar()
                for (c in cands) {
                    if (nato[ch] == c) return option
                    if (letterHomophones[ch]?.contains(c) == true) return option
                    if (digitWords[ch]?.contains(c) == true) return option
                    // A single spoken character, or a phonetic word starting with the letter.
                    if (c.length == 1 && c[0].uppercaseChar() == ch) return option
                    if (nato[ch] != null && c == nato[ch]) return option
                }
            } else {
                val norm = key.lowercase().replace(" ", "")
                for (c in cands) if (c.replace(" ", "") == norm) return option
            }
        }
        return null
    }
}
