package com.devlomi.fireapp.views.lavafab.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

/**
 * Created by Alexander Kolpakov on 27.08.2018
 */

@ColorInt
fun Context.getThemeAccentColor(): Int {
    val value = TypedValue()
    theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, value, true)
    return value.data
}

fun Context.getFloatDimen(@DimenRes res: Int) = resources.getDimension(res)

fun Context.getVectorDrawable(@DrawableRes resId: Int): VectorDrawableCompat? {
    return try {
        return VectorDrawableCompat.create(resources, resId, null)
    } catch (e: Resources.NotFoundException) {
        null
    }
}