package com.censorchi.di

import android.app.Application
import android.content.Context

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {


    companion object {
        var appContext: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this

    }

}