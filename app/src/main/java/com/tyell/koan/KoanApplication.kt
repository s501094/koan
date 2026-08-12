package com.tyell.koan

import android.app.Application
import android.content.Context
import com.tyell.koan.data.KoanDatabase
import com.tyell.koan.data.SpaceRepository
import com.tyell.koan.engine.KoanComponents

class KoanApplication : Application() {
    val components by lazy { KoanComponents(this) }
    val database by lazy { KoanDatabase.create(this) }
    val spaces by lazy { SpaceRepository(this, database) }
    val spaceController by lazy { SpaceController(components, spaces) }
}

val Context.koanComponents: KoanComponents
    get() = (applicationContext as KoanApplication).components

val Context.koanSpaces: SpaceRepository
    get() = (applicationContext as KoanApplication).spaces

val Context.koanSpaceController: SpaceController
    get() = (applicationContext as KoanApplication).spaceController
