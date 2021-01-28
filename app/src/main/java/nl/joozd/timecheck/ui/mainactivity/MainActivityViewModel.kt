package nl.joozd.timecheck.ui.mainactivity

import TimeCheckProtocol.TimeStampData
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.*
import nl.joozd.timecheck.R
import nl.joozd.timecheck.comms.Comms
import nl.joozd.timecheck.data.Repository
import nl.joozd.timecheck.tools.FeedbackEvents.TimeStampEvents
import nl.joozd.timecheck.tools.JoozdViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivityViewModel: JoozdViewModel() {
    private var timeStampCodeJob = Job()
    private val _useWords = MutableLiveData(false)

    private val _currentTimeStampData = MutableLiveData(
        TimeStampData(
            NO_CODE,
            0
        )
    )
    private var refreshing = false

    private val _timestampCode2= MediatorLiveData<String>().apply{
        addSource(_currentTimeStampData){ _currentTimeStampData ->
            if (_useWords.value == true) {
                timeStampCodeJob.cancel()
                viewModelScope.launch{
                    val newValue = Repository.getInstance(context).codeToWords(_currentTimeStampData.code)
                    if (isActive) value = newValue.joinToString("\n")
                }
            }
            else value = _currentTimeStampData.code.sliceInThree().joinToString("\n")
        }

        addSource(_useWords){
            if (it == true) {
                timeStampCodeJob.cancel()
                viewModelScope.launch{
                    val newValue = Repository.getInstance(context).codeToWords(_currentTimeStampData.value!!.code)
                    if (isActive) value = newValue.joinToString("\n")
                }
            }
            else value = _currentTimeStampData.value!!.code.sliceInThree().joinToString("\n")
        }
    // value = Transformations.map(_currentTimeStampData) { it.code.sliceInThree().joinToString("\n") }
    }

    val timestampCode2: LiveData<String>; get() = _timestampCode2

    val timestampTime: LiveData<String> = Transformations.map(_currentTimeStampData) { epochToTimeString(it.instant) }

    val showingWords: LiveData<Boolean>
        get() = _useWords

    /**
     * Time found from [checkCode]
     */
    var foundTime: String = context.getString(R.string.bad_code)
        private set

    /**
     * code found from [checkCode]
     */
    var foundCode: String = context.getString(R.string.bad_code)
        private set

    var foundWords: String = "-\n-\n-"
        private set

    var aboutText: String = "ERROR"
        private set


    /**
     * Refresh timestamp from server
     */
    fun refreshClicked(){
        println("CLICKETY CLACKETY")
        if (!refreshing) {
            refreshing = true
            viewModelScope.launch {
                Comms.getTimeStamp()?.let {
                    println(it)
                    _currentTimeStampData.postValue(it)
                }
                refreshing = false
            }
        }
    }

    fun aboutClicked(){
        viewModelScope.launch (Dispatchers.IO) {
            aboutText = context.resources.openRawResource(R.raw.about).use { it.reader().readText() }
            feedback(TimeStampEvents.SHOW_ABOUT)
        }
    }

    fun toggleUseWords(){
        _useWords.value = !_useWords.value!!
    }

    /**
     * Checks code with server and puts found time into feedback for Activity to display
     */
    fun checkCode(code: String){
        viewModelScope.launch {
            val processedCode = Repository.getInstance(context).wordsToCodeIfAble(code)
            Comms.lookUpCode(processedCode)?.let{
                foundCode = it.code.sliceInThree().joinToString("-")
                foundTime = if (it.instant == -1L) context.getString(R.string.bad_code)
                else listOf(
                        Repository.getInstance(context).codeToWords(it.code).joinToString ("\n"),
                        epochToTimeString(it.instant)
                ).joinToString("\n\n")
                feedback(TimeStampEvents.CODE_RECEIVED)
            }
        }
    }

    fun copyCode(){
        val code = _currentTimeStampData.value?.code ?: NO_CODE
        if (code == NO_CODE) {
            feedback(TimeStampEvents.ERROR).putString(context.getString(R.string.no_code))
            return
        }
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("timestamp", code.sliceInThree().joinToString("-")))
        feedback(TimeStampEvents.CODE_COPIED)
    }

    /**
     * Make time string from epochSecond
     */

    private fun epochToTimeString(epochSecond: Long): String = Instant.ofEpochSecond(epochSecond).let{
                DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(it)
        }

    private fun String.sliceInThree(): List<String> {
        val minSize = length/3
        return when (length%3){
            1 ->  listOf (slice(0 until minSize), slice(minSize until 2*minSize + 1), slice(2*minSize+1 until length))
            2 ->  listOf (slice(0 until minSize+1), slice(minSize+1 until 2*minSize + 1), slice(2*minSize+1 until length))
            else -> chunked(minSize)
        }
    }

    companion object{
        const val NO_CODE = "----------"
        const val NO_WORDS = "-\n-\n-"
    }
}