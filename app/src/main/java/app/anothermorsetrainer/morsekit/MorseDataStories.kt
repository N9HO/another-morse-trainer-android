package app.anothermorsetrainer.morsekit

/**
 * Short public-domain passages (Aesop's fables, plainly retold) for the
 * continuous-copy "Short Stories" mode. Kept short and free of apostrophes and
 * quotes so the displayed text matches what can be sent in Morse cleanly;
 * periods and commas are shown on reveal but skipped when keying.
 *
 * Translated from MorseKit/MorseDataStories.swift. The Swift
 * `extension MorseData` becomes a standalone [MorseStories] object here, to
 * avoid editing the generated MorseData object.
 */
object MorseStories {

    /** A bundled practice passage for continuous copy. */
    data class Story(val id: String, val title: String, val text: String) {
        /** Rough length label for the picker (word count bucket). */
        val lengthLabel: String
            get() {
                val words = text.split(" ").count { it.isNotBlank() }
                return when {
                    words <= 30 -> "short"
                    words <= 55 -> "medium"
                    else -> "long"
                }
            }
    }

    val all: List<Story> = listOf(
        Story("fox-grapes", "The Fox and the Grapes",
            "A hungry fox saw clusters of ripe grapes hanging high on a vine. He jumped again and again but could not reach them. At last he gave up and walked away, saying the grapes were surely sour."),
        Story("tortoise-hare", "The Tortoise and the Hare",
            "A hare mocked a tortoise for being slow, so they agreed to race. The hare ran ahead and lay down to nap, sure of winning. The tortoise kept a steady pace and passed the sleeping hare to win."),
        Story("lion-mouse", "The Lion and the Mouse",
            "A lion caught a tiny mouse but let it go. Later the lion was caught in a hunters net. The little mouse heard him roar and gnawed the ropes until the lion was free. Even the small can help the great."),
        Story("crow-pitcher", "The Crow and the Pitcher",
            "A thirsty crow found a pitcher with a little water at the bottom, too low to reach. One by one she dropped in pebbles until the water rose to the top. Then she drank her fill. Patience and wit win the day."),
        Story("ant-grasshopper", "The Ant and the Grasshopper",
            "All summer the ant stored grain while the grasshopper sang and played. When winter came the grasshopper was hungry and cold. The ant had plenty. It is wise to prepare today for the needs of tomorrow."),
        Story("north-wind-sun", "The North Wind and the Sun",
            "The wind and the sun argued over who was stronger. They agreed the winner would make a traveler remove his coat. The wind blew hard but the man held tight. Then the sun shone warmly and he took it off."),
        Story("dog-bone", "The Dog and the Bone",
            "A dog carried a bone across a bridge and saw his own shadow in the water below. Thinking it was another dog with a larger bone, he snapped at it. His own bone fell into the river and was lost."),
        Story("golden-egg", "The Goose and the Golden Egg",
            "A farmer owned a goose that laid one golden egg each day. Greedy for more, he cut the goose open to take all the gold at once. He found nothing inside, and the goose was gone. Greed can ruin good fortune."),
        Story("wolf-crane", "The Wolf and the Crane",
            "A wolf had a bone stuck in his throat and begged a crane for help. The crane reached in with her long beak and pulled it out. When she asked for her reward, the wolf only laughed and walked away."),
        Story("oak-reeds", "The Oak and the Reeds",
            "A mighty oak stood proud beside a bed of slender reeds. A great storm came and the reeds bent low with the wind, but the stiff oak resisted and was torn up by the roots. Yielding can be its own strength.")
    )
}
