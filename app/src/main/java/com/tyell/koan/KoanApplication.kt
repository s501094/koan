package com.tyell.koan

import android.app.Application
import android.content.Context
import com.tyell.koan.data.KoanDatabase
import com.tyell.koan.data.SpaceRepository
import com.tyell.koan.engine.KoanComponents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class KoanApplication : Application() {
    val components by lazy { KoanComponents(this) }
    val database by lazy { KoanDatabase.create(this) }
    val spaces by lazy { SpaceRepository(this, database) }
    val spaceController by lazy { SpaceController(components, spaces) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val boosts by lazy { BoostsFeature(components.engine, database, appScope) }
}

val Context.koanComponents: KoanComponents
    get() = (applicationContext as KoanApplication).components

val Context.koanSpaces: SpaceRepository
    get() = (applicationContext as KoanApplication).spaces

val Context.koanSpaceController: SpaceController
    get() = (applicationContext as KoanApplication).spaceController

val Context.koanBoosts: BoostsFeature
    get() = (applicationContext as KoanApplication).boosts
