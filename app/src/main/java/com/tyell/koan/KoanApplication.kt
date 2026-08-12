package com.tyell.koan

import android.app.Application
import android.content.Context
import com.tyell.koan.engine.KoanComponents

class KoanApplication : Application() {
    val components by lazy { KoanComponents(this) }
    val themeStore by lazy { ThemeStore(this) }
}

val Context.koanComponents: KoanComponents
    get() = (applicationContext as KoanApplication).components

val Context.koanThemeStore: ThemeStore
    get() = (applicationContext as KoanApplication).themeStore
