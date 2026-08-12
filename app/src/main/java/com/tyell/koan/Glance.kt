package com.tyell.koan

import mozilla.components.concept.engine.HitResult

/**
 * Glance — peek a link without leaving the page you're on.
 *
 * Desktop Zen binds this to alt-click and flies the new page in along an arc
 * from the cursor. Touch has no modifier key and no cursor, so the trigger is a
 * long press and the motion is a spring from the centre. The idea survives the
 * translation better than most of Zen's features — arguably it belongs on a
 * phone more than it does on a desktop, where a background tab is cheap and
 * peeking is not.
 */
object Glance {

    /** Zen's `zen.glance.animation-duration`. */
    const val ANIMATION_MS = 350

    /**
     * The link behind a long press, or null if the press wasn't on one.
     *
     * A plain link comes back as [HitResult.UNKNOWN] with the href in `src`;
     * a linked image comes back as [HitResult.IMAGE_SRC], where `src` is the
     * image and `uri` is the link — the link is what we want. Everything else,
     * including `mailto:` and `tel:`, is somebody else's job.
     */
    fun linkFrom(hitResult: HitResult?): String? {
        val candidate = when (hitResult) {
            is HitResult.UNKNOWN -> hitResult.src
            is HitResult.IMAGE_SRC -> hitResult.uri
            else -> null
        }
        return candidate?.takeIf { isGlanceable(it) }
    }

    /**
     * Only http(s) is worth previewing. `#fragment` links are excluded too —
     * a Glance onto the page you're already reading is just confusing.
     */
    fun isGlanceable(url: String): Boolean {
        if (url.isBlank()) return false
        if (url.startsWith("#")) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
