package nl.joozd.timecheck.tools

object FeedbackEvents {
    interface Event

    enum class GeneralEvents :
        Event {
        DONE,
        NOT_IMPLEMENTED,
        ERROR,
        OK
    }

    enum class TimeStampEvents: Event {
        CODE_RECEIVED,
        CODE_COPIED,
        SHOW_ABOUT,
        ERROR
    }
}