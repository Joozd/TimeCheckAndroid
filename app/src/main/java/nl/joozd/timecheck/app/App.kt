package nl.joozd.timecheck.app

import android.app.Application
import android.content.Context

/**
 * Main Application context
 * This will run whenever Application is started
 */
class App : Application(){
    companion object {
        private var INSTANCE: Application? = null
        val instance: Application
            get() = INSTANCE!!
    }

    val ctx: Context by lazy {applicationContext}

    override fun onCreate() {
        super.onCreate()
        if (INSTANCE == null) INSTANCE = this
    }
}