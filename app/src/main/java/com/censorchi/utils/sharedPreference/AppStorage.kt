package com.censorchi.utils.sharedPreference

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.censorchi.utils.Constants.BOTTOM_BLUR
import com.censorchi.utils.Constants.FULL_BLUR
import com.censorchi.utils.Constants.GET_STARTED
import com.censorchi.utils.Constants.IS_BACK
import com.censorchi.utils.Constants.SHARED_PREFERENCE_BOTTOM
import com.censorchi.utils.Constants.SHARED_PREFERENCE_FULL
import com.censorchi.utils.Constants.SHARED_PREFERENCE_TOP
import com.censorchi.utils.Constants.TOP_BLUR

object AppStorage {
    private lateinit var prefs: SharedPreferences
    lateinit var sharedPreferencesTopBlur: SharedPreferences
    lateinit var sharedPreferencesBottomBlur: SharedPreferences
    lateinit var sharedPreferencesFullBlur: SharedPreferences
    fun init(context: Context) {
        sharedPreferencesTopBlur = context.getSharedPreferences(SHARED_PREFERENCE_TOP, MODE_PRIVATE)
        sharedPreferencesBottomBlur =
            context.getSharedPreferences(SHARED_PREFERENCE_BOTTOM, MODE_PRIVATE)
        sharedPreferencesFullBlur =
            context.getSharedPreferences(SHARED_PREFERENCE_FULL, MODE_PRIVATE)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun setGetStarted(getStarted: Boolean) {
        prefs.edit().putBoolean(GET_STARTED, getStarted).apply()
    }

    fun getGetStarted(): Boolean {
        return prefs.getBoolean(GET_STARTED, false)
    }

    fun setIsBack(isBack: Boolean) {
        prefs.edit().putBoolean(IS_BACK, isBack).apply()
    }

    fun getIsBack(): Boolean {
        return prefs.getBoolean(IS_BACK, false)
    }

    fun setTopBlur(token: String) {
        sharedPreferencesTopBlur.edit().putString(TOP_BLUR, token).apply()
    }

    fun getTopBlur(): String {
        return sharedPreferencesTopBlur.getString(TOP_BLUR, "")!!
    }

    fun setBottomBlurPref(token: String) {
        sharedPreferencesBottomBlur.edit().putString(BOTTOM_BLUR, token).apply()
    }

    fun getBottomBlur(): String {
        return sharedPreferencesBottomBlur.getString(BOTTOM_BLUR, "")!!
    }

    fun setFullBlurPref(token: String) {
        sharedPreferencesFullBlur.edit().putString(FULL_BLUR, token).apply()
    }

    fun getFullBlur(): String {
        return sharedPreferencesFullBlur.getString(FULL_BLUR, "")!!
    }

    fun clearSessionTopBlur() = sharedPreferencesTopBlur.edit().clear().apply()
    fun clearSessionBottomBlur() = sharedPreferencesBottomBlur.edit().clear().apply()
    fun clearSessionFullBlur() = sharedPreferencesFullBlur.edit().clear().apply()

}