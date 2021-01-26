package nl.joozd.timecheck.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.timecheck.R
import java.util.*

class Repository private constructor(private val wordList: List<String>) {
    private val listSize = wordList.size

    /**
     * Transforms a code to words
     */
    fun codeToWords(code: String): List<String>{
        val usedChars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        require (code.all{it in usedChars}) {"Invalid code: $code"}
        var codeValue: Long = 0
        code.forEach {
            codeValue *= 34
            codeValue += usedChars.indexOf(it)
        }
        val words = mutableListOf<String>()

        while (codeValue > 0) {
            val word = (codeValue % listSize).toInt()
            words.add(wordList[word])
            codeValue /= listSize
        }
        return words
    }

    /**
     * Transforms a list of words to a code
     */
    fun wordsToCode(words: List<String>): String{
        val usedChars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        val workingWords = words.toMutableList()
        var codeValue = 0L
        while (workingWords.isNotEmpty()){
            codeValue *= listSize
            codeValue += wordList.indexOf(workingWords.removeLast())
        }
        val letters = mutableListOf<Char>()
        while (codeValue > 0){
            letters.add(usedChars[(codeValue%34).toInt()])
            codeValue /= 34
        }
        return letters.reversed().joinToString("")
    }

    /**
     *
     */
    fun wordsToCodeIfAble(s:String): String{
        val splits = s.toLowerCase(Locale.ROOT).split(',', ' ', '\n', '-').map{ it.trim()}.filter {it.isNotEmpty()}
        if (splits.all{it in wordList}){
            return wordsToCode(splits)
        }
        return splits.joinToString("").toUpperCase(Locale.ROOT)
    }


    companion object{
        private val INSTANCE: Repository? = null
        suspend fun getInstance(context: Context) = INSTANCE ?: withContext(Dispatchers.IO){
            Repository(context.resources.openRawResource(R.raw.words_nl).use { it.reader().readLines()
                    .map{ w-> w.filter { c -> c.isLetterOrDigit()}.toLowerCase(Locale.ROOT)}
                    .distinct()
                }
            )
        }
    }
}