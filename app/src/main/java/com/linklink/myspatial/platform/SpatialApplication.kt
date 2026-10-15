package com.linklink.myspatial.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.linklink.myspatial.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
