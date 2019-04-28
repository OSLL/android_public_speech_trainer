package ru.spb.speech.vocabulary

import android.content.Context
import android.util.Log
import ru.spb.speech.R
import java.util.regex.Pattern


class PrepositionsAndConjunctions(private val ctx: Context) {
    private val stemmer = StemmerPorterRU()

    fun removeConjunctionsAndPrepositionsFromText(sourceText: String): String {
        val list = ctx.resources.getStringArray(R.array.prepositionsAndConjunctions)
        var text = sourceText.toLowerCase()

        for (word in list) {
            text = deleteAllOccurrencesOfWord(text, word)
        }

        return text
    }

    private fun deleteAllOccurrencesOfWord(input: String, keyWord: String): String {
        val pattern = "((\\s+|^)($keyWord)(\\s+|$))"
        val r = Pattern.compile(pattern)
        val m = r.matcher(input)
        val sb = StringBuffer()

        while (m.find()) {
            m.appendReplacement(sb, m.group(0).replaceFirst(Pattern.quote(m.group(1)).toRegex(), " "))
        }

        m.appendTail(sb)

        var res = ""
        for (w in sb.toString().split(" "))
            res += " ${stemmer.stem(w)}"

        Log.d("sabdasb", res)
        return res
    }
}