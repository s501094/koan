package com.tyell.koan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tyell.koan.MainActivity
import com.tyell.koan.design.KoanShapes
import com.tyell.koan.engine.KoanComponents
import com.tyell.koan.engine.UrlInput
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.lib.state.ext.flow

@Composable
fun KoanApp(components: KoanComponents) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlow = remember(components, lifecycleOwner) {
        components.store.flow(lifecycleOwner)
    }
    val browserState by stateFlow.collectAsStateWithLifecycle(
        initialValue = components.store.state,
    )

    val tab = browserState.selectedTab
    val content = tab?.content

    var editing by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }

    BackHandler(enabled = editing || showTabs || content?.canGoBack == true) {
        when {
            editing -> editing = false
            showTabs -> showTabs = false
            else -> components.sessionUseCases.goBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                // Content sits inset from the chrome, the way desktop Zen's
                // `zen.theme.content-element-separation` frames the page.
                .clip(KoanShapes.medium),
        ) {
            EngineViewHost(components, Modifier.fillMaxSize())

            ProgressLine(
                progress = content?.progress ?: 0,
                loading = content?.loading == true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            Modifier
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Toolbar(
                url = content?.url.orEmpty().takeIf { it != "about:blank" }.orEmpty(),
                isLoading = content?.loading == true,
                isSecure = content?.securityInfo?.isSecure == true,
                tabCount = browserState.tabs.size,
                editing = editing,
                onEditingChange = { editing = it },
                onNavigate = { components.sessionUseCases.loadUrl(UrlInput.toUrl(it)) },
                onReload = { components.sessionUseCases.reload() },
                onStop = { components.sessionUseCases.stopLoading() },
                onTabsClick = { showTabs = true },
            )
        }
    }

    if (showTabs) {
        TabsSheet(
            tabs = browserState.tabs,
            selectedId = tab?.id,
            onSelect = {
                components.tabsUseCases.selectTab(it)
                showTabs = false
            },
            onClose = { components.tabsUseCases.removeTab(it) },
            onNewTab = {
                components.tabsUseCases.addTab(MainActivity.HOME_URL, selectTab = true)
                showTabs = false
            },
            onDismiss = { showTabs = false },
        )
    }
}

/**
 * A two-pixel line rather than a spinner. Gecko reports progress honestly, so
 * the bar is genuinely informative — and it disappears the moment it's done
 * instead of lingering at 100%.
 */
@Composable
private fun ProgressLine(
    progress: Int,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = if (loading) progress / 100f else 1f,
        animationSpec = tween(180),
        label = "progress",
    )

    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(260)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * fraction).toInt().coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width),
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primary),
        )
    }
}
