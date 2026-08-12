package com.tyell.koan

import android.app.Application
import android.content.Context
import com.tyell.koan.engine.KoanComponents

class KoanApplication : Application() {
    val components by lazy { KoanComponents(this) }
}

val Context.koanComponents: KoanComponents
    get() = (applicationContext as KoanApplication).components
