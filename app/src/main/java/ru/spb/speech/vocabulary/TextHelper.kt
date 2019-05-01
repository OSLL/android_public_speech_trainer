package ru.spb.speech.vocabulary

import android.content.res.Resources
import ru.spb.speech.R
import java.text.BreakIterator
import java.util.regex.Pattern


class TextHelper(private val res: Resources) {
    private val stemmer = StemmerPorterRU()
    private val conjAndPrep = res.getStringArray(R.array.prepositionsAndConjunctions)

    fun removeConjunctionsAndPrepositionsFromText(sourceText: String): String {
        var text = sourceText.toLowerCase()

        for (word in conjAndPrep) {
            text = deleteAllOccurrencesOfWord(text, word)
        }

        return text
    }

    fun getTop10WordsRmConjStemm(src: String): List<Pair<String, Int>>  {
        return this.getTop10Words(removeConjunctionsAndPrepositionsFromText(src))
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

        return sb.toString()
    }

    private fun getTop10Words(text: String) : List<Pair<String, Int>> {
        val dictionary = java.util.HashMap<String, Int>()
        val originals = java.util.HashMap<String, String>()

        val iterator = BreakIterator.getWordInstance()
        iterator.setText(text)

        var endIndex = iterator.first()
        while (BreakIterator.DONE != endIndex) {
            val startIndex = endIndex
            endIndex = iterator.next()
            if (endIndex != BreakIterator.DONE && Character.isLetterOrDigit(text[startIndex])) {
                val word = text.substring(startIndex, endIndex)
                val count = dictionary[word] ?: 0
                val stemmedWord = stemmer.stem(word)
                val original = originals[stemmedWord] ?: ""
                dictionary[stemmedWord] = count + 1
                if (original.isEmpty()) originals[stemmedWord] = word
                else originals[stemmedWord] = getBestWord(original, word)
            }
        }

        return dictionary.toList()
                .sortedByDescending { it.second }
                .take(10)
                .map { Pair(originals[it.first]!!, it.second) }
    }

    private inline fun getBestWord(oldWord: String, newWord: String): String {
        return listOf(oldWord, newWord).sortedWith(compareBy({ it.length }, { it })).first()
    }
}