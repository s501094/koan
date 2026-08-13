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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.tyell.koan.BoostsFeature
import com.tyell.koan.Glance
import com.tyell.koan.MainActivity
import com.tyell.koan.Popups
import com.tyell.koan.SpaceController
import com.tyell.koan.data.BoostEntity
import com.tyell.koan.data.SpaceEntity
import com.tyell.koan.data.SpaceRepository
import com.tyell.koan.design.KoanDimens
import com.tyell.koan.design.KoanShapes
import com.tyell.koan.engine.KoanComponents
import com.tyell.koan.engine.UrlInput
import com.tyell.koan.theme.KoanTheme
import com.tyell.koan.theme.LocalKoanTheme
import com.tyell.koan.theme.ThemePickerSheet
import com.tyell.koan.theme.ThemePresets
import com.tyell.koan.theme.zenGradientBackground
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.lib.state.ext.flow

@Composable
fun KoanApp(
    components: KoanComponents,
    repository: SpaceRepository,
    controller: SpaceController,
    boosts: BoostsFeature,
) {
    val activeSpace by repository.activeSpace.collectAsStateWithLifecycle(initialValue = null)
    val spec = remember(activeSpace) {
        activeSpace?.toThemeSpec() ?: ThemePresets.default.toSpec()
    }

    KoanTheme(spec = spec) {
        BrowserShell(components, repository, controller, boosts, activeSpace)
    }
}

@Composable
private fun BrowserShell(
    components: KoanComponents,
    repository: SpaceRepository,
    controller: SpaceController,
    boosts: BoostsFeature,
    activeSpace: SpaceEntity?,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val stateFlow = remember(components, lifecycleOwner) {
        components.store.flow(lifecycleOwner)
    }
    val browserState by stateFlow.collectAsStateWithLifecycle(
        initialValue = components.store.state,
    )

    val spaces by repository.spaces.collectAsStateWithLifecycle(initialValue = emptyList())
    val essentials by remember(activeSpace) {
        activeSpace?.let { repository.essentials(it.id) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allSpaceTabs = controller.tabsIn(browserState, activeSpace)
    val tab = controller.selectedTabIn(browserState, activeSpace)
    val content = tab?.content

    var editing by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<SpaceEntity?>(null) }
    var creatingSpace by remember { mutableStateOf(false) }
    var glanceTabId by remember { mutableStateOf<String?>(null) }
    var showBoosts by remember { mutableStateOf(false) }
    var zapArmed by remember { mutableStateOf(false) }

    val currentHost = content?.url?.let { BoostEntity.patternFor(it) }
    val boost by remember(currentHost) {
        currentHost?.let { host ->
            repository.boostFor(host)
        } ?: flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    // A tap during Zap comes back from the content script; persist it and
    // disarm, so one arming hides exactly one element.
    LaunchedEffect(boosts) {
        boosts.picked.collect { picked ->
            boosts.zap(picked.url, picked.selector)
            zapArmed = false
        }
    }

    // A long press on a link lands in the tab's own state as a hit result.
    // Turn it into a Glance and consume it, so the same press can't fire twice.
    val hitResult = content?.hitResult
    LaunchedEffect(tab?.id, hitResult) {
        val ownerId = tab?.id ?: return@LaunchedEffect
        val link = Glance.linkFrom(hitResult) ?: return@LaunchedEffect
        components.store.dispatch(ContentAction.ConsumeHitResultAction(ownerId))
        if (glanceTabId == null) {
            glanceTabId = controller.openTab(link, activeSpace, selectTab = false)
        }
    }

    // A popup is parked on whichever tab called window.open, which isn't
    // necessarily the selected one — a background tab can open a window. Scan
    // all of them, and consume even when we refuse the request, or the same one
    // comes back on every state change.
    val opener = browserState.tabs.firstOrNull { it.content.windowRequest != null }
    LaunchedEffect(opener?.id, opener?.content?.windowRequest) {
        val tab = opener ?: return@LaunchedEffect
        val action = Popups.actionFor(tab)
        components.store.dispatch(ContentAction.ConsumeWindowRequestAction(tab.id))
        if (action != null) controller.handlePopup(action)
    }

    // The Glance tab is a real tab in the Space, just currently being shown in
    // the overlay rather than the main view. Hide it from the list so it can't
    // be selected out from under the peek.
    val spaceTabs = allSpaceTabs.filterNot { it.id == glanceTabId }

    fun dismissGlance() {
        glanceTabId?.let { components.tabsUseCases.removeTab(it) }
        glanceTabId = null
    }

    fun promoteGlance() {
        glanceTabId?.let { components.tabsUseCases.selectTab(it) }
        glanceTabId = null
    }

    fun stepSpace(delta: Int) {
        val next = controller.step(spaces, activeSpace, delta) ?: return
        scope.launch { controller.switchTo(next, components.store.state) }
    }

    BackHandler(enabled = editing || showTabs || content?.canGoBack == true) {
        when {
            editing -> editing = false
            showTabs -> showTabs = false
            else -> components.sessionUseCases.goBack()
        }
    }

    // KoanTheme already resolved the Space's spec and its dark/light decision;
    // read them back rather than recomputing and risking a mismatch.
    val theme = LocalKoanTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zenGradientBackground(theme.spec, theme.isDark),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                // The page floats on the gradient rather than covering it. This
                // inset is Zen's `zen.theme.content-element-separation`, and it
                // is the whole reason the theming is visible at all.
                .padding(
                    start = KoanDimens.contentSeparation,
                    end = KoanDimens.contentSeparation,
                    top = KoanDimens.contentSeparation,
                )
                .clip(KoanShapes.medium),
        ) {
            EngineViewHost(components, Modifier.fillMaxSize())

            ProgressLine(
                progress = content?.progress ?: 0,
                loading = content?.loading == true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (zapArmed) {
            ZapArmedBanner(
                onCancel = {
                    boosts.stopPicker()
                    zapArmed = false
                },
                modifier = Modifier.padding(bottom = 8.dp),
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
                tabCount = spaceTabs.size,
                editing = editing,
                onEditingChange = { editing = it },
                onNavigate = { components.sessionUseCases.loadUrl(UrlInput.toUrl(it)) },
                onReload = { components.sessionUseCases.reload() },
                onStop = { components.sessionUseCases.stopLoading() },
                onTabsClick = { showTabs = true },
                onSwipeSpace = ::stepSpace,
            )
        }
    }

    val glanceTab = glanceTabId?.let { id -> browserState.tabs.firstOrNull { it.id == id } }
    if (glanceTabId != null && glanceTab != null) {
        GlanceOverlay(
            components = components,
            tabId = glanceTab.id,
            title = glanceTab.content.title,
            url = glanceTab.content.url,
            onDismiss = ::dismissGlance,
            onPromote = ::promoteGlance,
        )
    }

    if (showTabs) {
        val currentUrl = content?.url
        TabsSheet(
            tabs = spaceTabs,
            selectedId = tab?.id,
            onSelect = {
                components.tabsUseCases.selectTab(it)
                showTabs = false
            },
            onClose = { components.tabsUseCases.removeTab(it) },
            onNewTab = {
                controller.openTab(MainActivity.HOME_URL, activeSpace)
                showTabs = false
            },
            onThemeClick = {
                showTabs = false
                showTheme = true
            },
            onBoostsClick = {
                showTabs = false
                showBoosts = true
            },
            onDismiss = { showTabs = false },
            spaces = spaces,
            activeSpace = activeSpace,
            essentials = essentials,
            canPinCurrent = currentUrl != null &&
                currentUrl != "about:blank" &&
                essentials.none { it.url == currentUrl },
            onSelectSpace = { space ->
                scope.launch { controller.switchTo(space, components.store.state) }
            },
            onCreateSpace = { creatingSpace = true },
            onEditSpace = { editingSpace = it },
            onOpenEssential = { essential ->
                controller.openEssential(essential, activeSpace, components.store.state)
                showTabs = false
            },
            onRemoveEssential = { scope.launch { repository.removeEssential(it) } },
            onPinCurrent = {
                val space = activeSpace ?: return@TabsSheet
                val url = currentUrl ?: return@TabsSheet
                scope.launch {
                    repository.addEssential(space.id, url, content.title.orEmpty())
                }
            },
        )
    }

    if (showTheme) {
        val space = activeSpace
        ThemePickerSheet(
            spec = space?.toThemeSpec() ?: ThemePresets.default.toSpec(),
            onSpecChange = { updated ->
                if (space != null) scope.launch { repository.saveTheme(space.id, updated) }
            },
            onDismiss = { showTheme = false },
        )
    }

    if (showBoosts) {
        val url = content?.url.orEmpty()
        BoostsSheet(
            url = url,
            boost = boost,
            onZapStart = {
                boosts.startPicker()
                zapArmed = true
                showBoosts = false
            },
            onRemoveZap = { selector ->
                boost?.let { b -> scope.launch { boosts.removeZap(b, selector) } }
            },
            onToggleEnabled = { enabled ->
                boost?.let { b -> scope.launch { boosts.setEnabled(b, enabled) } }
            },
            onSaveCss = { css ->
                scope.launch { boosts.saveCss(url, css) }
            },
            onClearAll = {
                boost?.let { b -> scope.launch { boosts.clear(b) } }
                showBoosts = false
            },
            onDismiss = { showBoosts = false },
        )
    }

    if (creatingSpace || editingSpace != null) {
        val target = editingSpace
        SpaceEditDialog(
            existing = target,
            canDelete = spaces.size > 1,
            onConfirm = { name, icon ->
                scope.launch {
                    if (target == null) {
                        val created = repository.createSpace(name, icon)
                        controller.switchTo(created, components.store.state)
                    } else {
                        repository.rename(target, name, icon)
                    }
                }
                creatingSpace = false
                editingSpace = null
            },
            onDelete = {
                target?.let {
                    scope.launch { controller.deleteSpace(it, components.store.state) }
                }
                editingSpace = null
            },
            onDismiss = {
                creatingSpace = false
                editingSpace = null
            },
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
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
