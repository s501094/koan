package com.tyell.koan.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The theming surface: presets on top, wheel in the middle, the two sliders
 * that decide how strongly the theme asserts itself at the bottom.
 *
 * Harmony is exposed as chips rather than being inferred from dot count the
 * way desktop Zen does it. On desktop you add and remove dots by clicking the
 * canvas and the harmony follows; on a phone that is fiddly, so choosing the
 * harmony directly and letting it place the dots is the better trade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    spec: ZenThemeSpec,
    onSpecChange: (ZenThemeSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("Presets")
            PresetStrip(spec, onSpecChange)

            SectionLabel("Harmony")
            HarmonyChips(spec, onSpecChange)

            ColorWheelPicker(
                spec = spec,
                onPrimaryMoved = { x, y ->
                    val primary = ColorDot(
                        x = x,
                        y = y,
                        isPrimary = true,
                        type = spec.dots.firstOrNull()?.type ?: DotType.ExplicitLightness,
                    )
                    onSpecChange(spec.copy(dots = ColorWheel.applyHarmony(primary, spec.harmony)))
                },
            )

            LabelledSlider(
                label = "Lightness",
                value = spec.lightness / 100f,
                onValueChange = {
                    val l = (it * 100).toInt().coerceIn(0, 100)
                    val primary = spec.dots.firstOrNull { d -> d.isPrimary } ?: return@LabelledSlider
                    onSpecChange(
                        spec.copy(
                            lightness = l,
                            dots = ColorWheel.applyHarmony(primary, spec.harmony),
                        ),
                    )
                },
            )

            LabelledSlider(
                label = "Opacity",
                value = spec.opacity.toFloat(),
                onValueChange = { onSpecChange(spec.copy(opacity = it.toDouble())) },
            )

            LabelledSlider(
                label = "Grain",
                value = spec.texture.toFloat(),
                onValueChange = { onSpecChange(spec.copy(texture = it.toDouble())) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
}

@Composable
private fun PresetStrip(spec: ZenThemeSpec, onSpecChange: (ZenThemeSpec) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemePresets.all.forEach { preset ->
            val presetSpec = remember(preset) { preset.toSpec(spec.opacity, spec.texture) }
            val selected = spec.dots.firstOrNull()?.let { d ->
                d.x == preset.x && d.y == preset.y && spec.lightness == preset.lightness &&
                    spec.harmony == preset.harmony
            } == true

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .zenGradientBackground(presetSpec, isDark = false)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        },
                        shape = CircleShape,
                    )
                    .clickable {
                        onSpecChange(preset.toSpec(spec.opacity, spec.texture))
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HarmonyChips(spec: ZenThemeSpec, onSpecChange: (ZenThemeSpec) -> Unit) {
    val labels = mapOf(
        Harmony.Floating to "Single",
        Harmony.Complementary to "Complement",
        Harmony.SingleAnalogous to "Pair",
        Harmony.Analogous to "Analogous",
        Harmony.SplitComplementary to "Split",
        Harmony.Triadic to "Triadic",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { (harmony, label) ->
            FilterChip(
                selected = spec.harmony == harmony,
                onClick = {
                    val primary = spec.dots.firstOrNull { it.isPrimary }
                        ?: spec.dots.firstOrNull()
                        ?: return@FilterChip
                    onSpecChange(
                        spec.copy(
                            harmony = harmony,
                            dots = ColorWheel.applyHarmony(
                                primary.copy(isPrimary = true),
                                harmony,
                            ),
                        ),
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(value * 100).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Slider(value = value, onValueChange = onValueChange)
    }
}
