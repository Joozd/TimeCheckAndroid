package nl.joozd.timecheck.tools

import android.os.Bundle

/**
 * Gives feedback from viewModel to UI class
 * ie. invalid data was received and nothing was saved
 * will work only once on "getEvent()", will return null every next time this is checked
 * inspiration from "https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150"
 */
class FeedbackEvent(val type: FeedbackEvents.Event) {
    var consumed = false
    fun getEvent(): FeedbackEvents.Event? {
        if (consumed)
            return null
        else {
            consumed = true
            return type
        }
    }
    val extraData = Bundle()

    fun putInt(value: Int): FeedbackEvent{
        extraData.putInt(INTEGER, value)
        return this
    }
    fun getInt(): Int = extraData.getInt(INTEGER)

    fun putString(value: String?): FeedbackEvent{
        extraData.putString(STRING, value)
        return this
    }
    fun getString(): String? = extraData.getString(STRING)

    companion object{
        const val INTEGER = "INTEGER_EXTRA_DATA_FOR_FEEDBACK"
        const val STRING = "STRING_EXTRA_DATA_FOR_FEEDBACK"
    }
}