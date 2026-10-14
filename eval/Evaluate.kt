// Runs SensitiveWordDetector over a labeled set and prints accuracy and speed.
// Two views:
//  1. detector: every space-separated token judged as a whole (what the detector alone can do)
//  2. keyboard: tokens are split the way HeliBoard composes words (it ends a word at symbols like '!' and
//     only starts words with a letter); measures what would actually end up learned.
//     mode "old" = each piece judged alone, mode "new" = pieces ending at a symbol wait for the whole chunk.
// Usage: EvaluateKt <dir> <dataset.tsv> <old|new> <output.md>
import helium314.keyboard.latin.privacy.SensitiveWordDetector
import helium314.keyboard.latin.privacy.UserBlockList
import java.io.File

data class Tok(val sid: Int, val idx: Int, val token: String, val label: Int, val category: String, val source: String)

// HeliBoard's word separators (res/values/donottranslate-config-spacing-and-punctuations.xml), without whitespace
const val SEPARATORS = "()[]{}*&<>+=|.,;:!?/_\\\"„“…‽"

/** The words HeliBoard would compose (and could learn) from one space-free chunk. */
fun pieces(chunk: String): List<String> {
    val out = mutableListOf<String>()
    val current = StringBuilder()
    var composing = false
    var prev: Char? = null
    for (c in chunk) {
        when {
            c in SEPARATORS -> { if (composing) { out += current.toString(); current.clear() }; composing = false }
            composing -> current.append(c)
            c.isLetter() && (prev == null || prev in SEPARATORS) -> { composing = true; current.append(c) }
        }
        prev = c
    }
    if (composing) out += current.toString()
    return out
}

fun main(args: Array<String>) {
    val dir = File(args[0]); val setName = args[1]; val mode = args[2]; val outName = args[3]
    val toks = File(dir, setName).readLines().drop(1).map { line ->
        val p = line.split('\t')
        Tok(p[0].toInt(), p[1].toInt(), p[2], p[3].toInt(), p[4], p[5])
    }
    val bySentence = toks.groupBy { it.sid }
    val prevs = toks.associateWith { t ->
        bySentence.getValue(t.sid).filter { it.idx < t.idx }.sortedByDescending { it.idx }.map { it.token }
    }
    fun sentenceOf(t: Tok) = bySentence.getValue(t.sid).sortedBy { it.idx }.joinToString(" ") { it.token }
    val out = StringBuilder()
    out.appendLine("# $setName, mode $mode")

    // ---- 1. detector view ----
    val predicted = toks.associateWith { SensitiveWordDetector.detect(it.token, prevs.getValue(it)) }
    fun row(name: String, tp: Int, fn: Int, fp: Int, tn: Int) {
        val precision = tp.toDouble() / (tp + fp); val recall = tp.toDouble() / (tp + fn)
        val f1 = 2 * precision * recall / (precision + recall); val fpr = fp.toDouble() / (fp + tn)
        out.appendLine("| $name | ${tp + fn + fp + tn} | $tp | $fn | $fp | $tn | %.3f | %.3f | %.3f | %.2f%% |".format(precision, recall, f1, fpr * 100))
    }
    out.appendLine("\n| view | units | TP | FN | FP | TN | precision | recall | F1 | false positive rate |")
    out.appendLine("|---|---|---|---|---|---|---|---|---|---|")
    row("detector (whole tokens)",
        toks.count { it.label == 1 && predicted[it] != null }, toks.count { it.label == 1 && predicted[it] == null },
        toks.count { it.label == 0 && predicted[it] != null }, toks.count { it.label == 0 && predicted[it] == null })

    // ---- 2. keyboard view ----
    // learned pieces per token; a token counts as "leaked" if any of its pieces is learned
    val learnedPieces = toks.associateWith { t ->
        val ps = pieces(t.token)
        val prev = prevs.getValue(t)
        if (mode == "new" && SensitiveWordDetector.isSensitive(t.token)) emptyList()
        else ps.filter { SensitiveWordDetector.detect(it, prev) == null }
    }
    val withPieces = toks.filter { pieces(it.token).isNotEmpty() }
    fun blocked(t: Tok) = learnedPieces.getValue(t).size < pieces(t.token).size
    fun leaked(t: Tok) = learnedPieces.getValue(t).isNotEmpty()
    row("keyboard (what gets learned)",
        withPieces.count { it.label == 1 && !leaked(it) }, withPieces.count { it.label == 1 && leaked(it) },
        withPieces.count { it.label == 0 && blocked(it) }, withPieces.count { it.label == 0 && !blocked(it) })
    out.appendLine("\nKeyboard view counts only tokens with at least one word the keyboard can learn (starts with a letter). " +
        "TP = sensitive token with nothing learned, FN = some part learned, FP = normal token with a part blocked.")

    out.appendLine("\nKeyboard view, sensitive tokens kept out of learning per category:")
    for ((cat, list) in withPieces.filter { it.label == 1 }.groupBy { it.category }.toSortedMap())
        out.appendLine("- $cat: ${list.count { !leaked(it) }}/${list.size}")
    out.appendLine("\nKeyboard view, wrongly blocked per normal source:")
    for ((src, list) in withPieces.filter { it.label == 0 }.groupBy { it.source }.toSortedMap())
        out.appendLine("- $src: ${list.count { blocked(it) }}/${list.size}")

    out.appendLine("\n## Leaked (keyboard view), unique")
    withPieces.filter { it.label == 1 && leaked(it) }.distinctBy { it.token }.sortedBy { it.category }
        .forEach { out.appendLine("- [${it.category}] `${it.token}` learns ${learnedPieces[it]} in \"${sentenceOf(it)}\"") }
    out.appendLine("\n## Wrongly blocked (keyboard view), unique")
    withPieces.filter { it.label == 0 && blocked(it) }.distinctBy { it.token to prevs[it]?.firstOrNull() }
        .forEach { out.appendLine("- `${it.token}` (${predicted[it]}) in \"${sentenceOf(it)}\"") }

    // ---- speed ----
    val inputs = toks.map { it.token to prevs.getValue(it) }
    repeat(20) { inputs.forEach { (w, p) -> SensitiveWordDetector.detect(w, p) } }
    var sink = 0
    val start = System.nanoTime()
    repeat(200) { inputs.forEach { (w, p) -> if (SensitiveWordDetector.detect(w, p) != null) sink++ } }
    val perCallUs = (System.nanoTime() - start) / 1000.0 / (200 * inputs.size)
    val seed = UserBlockList("salt", emptyList())
    val list = UserBlockList("salt", (1..50).map { seed.entryFor("secretword$it")!!.serialize() })
    repeat(5) { inputs.forEach { (w, p) -> SensitiveWordDetector.detect(w, p, list) } }
    val start2 = System.nanoTime()
    repeat(50) { inputs.forEach { (w, p) -> if (SensitiveWordDetector.detect(w, p, list) != null) sink++ } }
    val perCallListUs = (System.nanoTime() - start2) / 1000.0 / (50 * inputs.size)
    out.appendLine("\n## Speed (JVM on the build machine, not the phone)")
    out.appendLine("- detect(): %.2f us per word; with a 50-word user list: %.2f us".format(perCallUs, perCallListUs))
    out.appendLine("<!-- $sink -->")
    File(dir, outName).writeText(out.toString())
    print(out)
}
