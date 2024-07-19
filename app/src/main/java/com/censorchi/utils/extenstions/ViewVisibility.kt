package com.censorchi.utils;

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


fun View.viewVisible(): View {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
    return this
}

fun View.viewGone(): View {
    if (visibility != View.GONE) {
        visibility = View.GONE
    }
    return this
}
fun View.inVisible() {
    this.visibility = View.INVISIBLE
}

internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)
