package com.tyell.koan.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Desktop Zen defaults to `--zen-border-radius: 7px` and separates content
 * from chrome by `zen.theme.content-element-separation: 8px`. Both scale up
 * a little on a phone, where everything is physically smaller but touched.
 */
object KoanDimens {
    val borderRadius = 10.dp
    val contentSeparation = 8.dp
    val toolbarHeight = 52.dp
    val essentialsIcon = 44.dp
}

val KoanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(KoanDimens.borderRadius),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
