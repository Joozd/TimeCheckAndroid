package nl.joozd.timecheck.ui.mainactivity

import timeCheckProtocol.TimeStampData
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.*
import nl.joozd.internetstatus.InternetStatus
import nl.joozd.timecheck.R
import nl.joozd.timecheck.app.App
import nl.joozd.timecheck.comms.Comms
import nl.joozd.timecheck.data.Repository
import nl.joozd.timecheck.tools.FeedbackEvents.TimeStampEvents
import nl.joozd.timecheck.tools.JoozdViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivityViewModel: JoozdViewModel() {
    //internet status monitor
    private val internetStatus = InternetStatus(App.instance)

    // reference so it can be canbcelled and restarted when internet status changes
    private var loadingJob: Job = Job()

    private val _useWords = MutableLiveData(false)

    private val _waitingReason = MutableLiveData(STARTING_UP)

    private val _currentTimeStampData = MutableLiveData(TimeStampData.EMPTY)

    private var refreshing = false

    private val _timestampCode2= MediatorLiveData<String>().apply{
        addSource(_currentTimeStampData){ tsdUnchecked ->
            tsdUnchecked.takeIf{ it?.code != TimeStampData.NO_CODE}?.let { tsd ->
                if (_useWords.value == true) {
                    //in coroutine for loading of wordslist from raw resource
                    viewModelScope.launch {
                        val newValue = Repository.getInstance(context).codeToWords(tsd.code)
                        value = newValue.joinToString("\n")
                    }
                } else value = tsd.code.sliceInThree().joinToString("\n")
            }
        }

        addSource(_useWords){
            if (it == true) {
                //in coroutine for loading of wordslist from raw resource
                viewModelScope.launch{
                    val newValue = _currentTimeStampData.value.takeIf{ it?.code != TimeStampData.NO_CODE }
                        ?.let { tsd ->
                            Repository.getInstance(context).codeToWords(tsd.code).joinToString("\n")
                        } ?: ""
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

    val isOnLine = InternetStatus(App.instance).internetAvailableLiveData

    val loading: LiveData<Boolean>
        get() = _currentTimeStampData.map { it?.code in listOf(null, TimeStampData.NO_CODE) }

    val waitingReason: LiveData<Int>
        get() = _waitingReason

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
     * refresh if data older than TIMESTAMP_TTL
     */

    fun refreshIfTooOld(){
        if (Instant.now().epochSecond - (_currentTimeStampData.value?.instant ?: 0) > TIMESTAMP_TTL) refreshClicked()
    }

    /**
     * Refresh timestamp from server
     */
    fun refreshClicked(){
        _waitingReason.postValue( if (internetStatus.internetAvailable) WAITING_FOR_SERVER else NO_INTERNET) // refresh will always remove current data even when no nwe will be found
        _currentTimeStampData.postValue(TimeStampData.EMPTY)
        if (!internetStatus.internetAvailable) {
            return
        }
        if (!refreshing) {
            refreshing = true // so it won't keep restarting on "refresh" spam
            loadingJob = viewModelScope.launch {
                Comms.getTimeStamp()?.let {
                    if (!isActive) return@launch // do nothing if job was canceled
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
        val code = _currentTimeStampData.value?.code ?: TimeStampData.NO_CODE
        if (code == TimeStampData.NO_CODE) {
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

    private fun internetStatusChanged(isOnline: Boolean) {
        if (isOnline)
            if (_currentTimeStampData.value?.code == TimeStampData.NO_CODE) {
                loadingJob.cancel()
                refreshClicked()
            }
            // no else, we already have a code and that should do
        else {
            loadingJob.cancel()
            _waitingReason.postValue(NO_INTERNET)
            }
    }

    init{
        internetStatus.addCallback{ isOnline ->
            internetStatusChanged(isOnline)
        }
        if (!internetStatus.internetAvailable) _waitingReason.value = NO_INTERNET
    }

    companion object{
        const val STARTING_UP = 0
        const val NO_INTERNET = 1
        const val WAITING_FOR_SERVER = 2

        private const val TIMESTAMP_TTL = 5*60 // seconds
    }
}