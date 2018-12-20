package com.example.company.myapplication.vocabulary

import android.util.Log
import java.util.regex.Pattern


class PrepositionsAndConjunctions {

    fun removeConjunctionsAndPrepositionsFromText(sourceText: String): String {
        val list = conjunctions + prepositions
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
        return sb.toString()
    }

    private val conjunctions: List<String> = listOf("а", "а ведь", "а именно", "а не то", "а то", "абы", "аж", "ажно", "ай",
            "аки", "ако", "али", "аль", "ан", "аще", "благо", "благодаря тому что", "благодаря чему", "бо", "буде", "будто",
            "в результате чего", "ведь", "впрочем", "всё", "всё-таки", "вследствие чего", "где", "где-то", "да", "дабы",
            "даже", "до тех пор пока не", "до тех пор, пока", "докуда", "дотоле", "егда", "едва", "еже", "ежели", "ежель",
            "если", "ж", "же", "зане", "занеже", "зато", "зачем", "значит", "и", "и/или", "ибо", "из-за того, что",
            "из-за этого", "или", "иль", "именно", "имже", "инако", "иначе", "инда", "ино", "итак", "кабы", "как", "как бы",
            "как бы не", "как и", "как то", "как-то", "каков", "какой", "ковда", "ковды", "когда", "когды", "коли", "коль",
            "который", "куда", "ли", "либо", "лишь", "лишь только", "ль", "настолько", "не прошло и, как", "нежели", "незомь",
            "ни", "ниже", "нижли", "но", "обаче", "однако", "однако же", "одначе", "отколь", "откуда", "откудова", "оттого",
            "отчего", "перед тем как", "по мере того как", "поелику", "пока", "пока не", "покамест", "покаместь", "покеда",
            "поколева", "поколику", "поколь", "покуль", "покуля", "понеже", "поскольку", "пота", "потолику", "потому",
            "потому как", "потому что", "почём", "почему", "правда", "преж", "прежде чем", "притом", "причём", "просто",
            "пускай", "пусть", "равно", "раз", "разве", "ровно", "с тем чтобы", "сиречь", "сколько", "следовательно", "словно",
            "словно бы", "столько", "так как", "также", "то", "то есть", "то ли", "тож", "тоже", "только", "хоть", "хотя",
            "хуле", "чем", "чи", "что", "чтоб", "чтобы", "чуть", "штоб", "штобы", "яко", "якобы")

    private val prepositions: List<String> = listOf("а-ля", "без", "без ведома", "безо", "благодаря", "близ", "близко от", "в",
            "в виде", "в зависимости от", "в интересах", "в качестве", "в лице", "в отличие от", "в отношении", "в пандан",
            "в пользу", "в преддверии", "в продолжение", "в результате", "в роли", "в связи с", "в силу", "в случае",
            "в соответствии с", "в течение", "в целях", "в честь", "вблизи", "ввиду", "вглубь", "вдогон", "вдоль", "вдоль по",
            "взамен", "включая", "вкруг", "вместо", "вне", "внизу", "внутри", "внутрь", "во", "во имя", "во славу", "вовнутрь",
            "возле", "вокруг", "вопреки", "вослед", "впереди", "вплоть до", "впредь до", "вразрез", "вроде", "вслед",
            "вслед за", "вследствие", "встречу", "выключая", "для", "для-ради", "до", "за", "за вычетом", "за исключением",
            "за счёт", "замест", "заместо", "из", "из-за", "из-под", "из-подо", "изнутри", "изо", "исключая", "исходя из", "к",
            "касаемо", "касательно", "ко", "кончая", "кроме", "кругом", "лицом к лицу с", "меж", "между", "мимо", "на", "на благо",
            "на виду у", "на глазах у", "на предмет", "наверху", "навроде", "навстречу", "над", "надо", "назад", "назади", "накануне",
            "наместо", "наперекор", "наперерез", "наперехват", "наподобие", "наподобье", "напротив", "наряду с", "насупротив", "насчёт",
            "начиная с", "не без", "не считая", "невзирая на", "недалеко от", "независимо от", "несмотря", "несмотря на", "ниже", "о",
            "об", "обо", "обок", "обочь", "около", "окрест", "окроме", "окромя", "округ", "опосля", "опричь", "от", "от имени",
            "от лица", "относительно", "ото", "перед", "передо", "по", "по линии", "по мере", "по направлению к", "по поводу",
            "по причине", "по случаю", "по сравнению с", "по-за", "по-над", "по-под", "поблизости от", "повдоль", "поверх", "под",
            "под видом", "под эгидой", "подле", "подо", "подобно", "позади", "позадь", "позднее", "помимо", "поперёд", "поперёк",
            "порядка", "посверху", "посереди", "посередине", "посерёдке", "посередь", "после", "посреди", "посредине", "посредством",
            "пред", "предо", "преж", "прежде", "при", "при помощи", "применительно к", "про", "промеж", "промежду", "против", "противно",
            "противу", "путём", "ради", "рядом с", "с", "с ведома", "с помощью", "с прицелом на", "с точки зрения", "с целью", "сверх",
            "сверху", "свыше")
}