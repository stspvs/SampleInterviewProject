package ru.company.sts.soundbound

import android.app.Application
import ru.company.sts.soundbound.utils.Log

/**
 * Created by sts on 28.03.2018.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.setEnabled(false)
    }
}