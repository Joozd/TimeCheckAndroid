package nl.joozd.timecheck.tools

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.timecheck.app.App

abstract class JoozdViewModel(): ViewModel() {
    protected val context: Context
        get() = App.instance

    private val _feedbackEvent = MutableLiveData<FeedbackEvent>()
    val feedbackEvent: LiveData<FeedbackEvent>
        get() = _feedbackEvent


    /**
     * Gives feedback to activity.
     * @param event: type of event
     * @param feedbackEvent: livedata to send feedback to
     * @return: The event that si being fed back
     * The [FeedbackEvent] that is being returned can be edited (ie. extraData can be filled)
     * with an [apply] statement. This is faster than the filling of the livedata so it works.
     */
    protected fun feedback(event: FeedbackEvents.Event): FeedbackEvent =
            FeedbackEvent(event).also{
                Log.d("Feedback", "event: $event, feedbackEvent: $feedbackEvent")
                viewModelScope.launch(Dispatchers.Main) {_feedbackEvent.value = it }
            }

    protected fun feedback(event: FeedbackEvents.Event, feedbackEvent: MutableLiveData<FeedbackEvent>): FeedbackEvent =
            FeedbackEvent(event).also{
                Log.d("Feedback2", "event: $event, feedbackEvent: $feedbackEvent")
                viewModelScope.launch(Dispatchers.Main) { feedbackEvent.value = it }
            }
}
