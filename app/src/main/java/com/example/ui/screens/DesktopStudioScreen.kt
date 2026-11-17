package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LinuxProfile
import com.example.domain.ConfigGenerators
import com.example.ui.components.ConfigPreviewSheet
import com.example.ui.components.OptionChipGroup
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CrimsonCoral
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MatrixMint
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.VioletPulse
import com.example.ui.viewmodel.KernixViewModel
import kotlin.math.roundToInt

@Composable
fun DesktopStudioScreen(
    viewModel: KernixViewModel,
    onShowMessage: (String) -> Unit
) {
    val profile by viewModel.draftProfile.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var showDotfiles by remember { mutableStateOf(false) }

    val accentColor = remember(profile.accentPreset) {
        when (profile.accentPreset) {
            "Cyber Cyan" -> CyberCyan
            "Matrix Mint" -> MatrixMint
            "Solar Amber" -> SolarAmber
            "Crimson Coral" -> CrimsonCoral
            "Violet Pulse" -> VioletPulse
            else -> CyberCyan
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiveDesktopPreviewCard(profile = profile, accentColor = accentColor)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Compositor & Shell Stack",
                        subtitle = "Configure your Wayland/X11 environment and core tools",
                        icon = Icons.Rounded.DesktopWindows
                    )
                    OptionChipGroup(
                        label = "Desktop Environment / Compositor",
                        options = LinuxProfile.DESKTOP_ENVS,
                        selected = profile.desktopEnv,
                        onSelect = { v -> viewModel.updateProfile { it.copy(desktopEnv = v) } },
                        testTagPrefix = "de_env"
                    )
                    OptionChipGroup(
                        label = "Status Bar",
                        options = LinuxProfile.STATUS_BARS,
                        selected = profile.statusBar,
                        onSelect = { v -> viewModel.updateProfile { it.copy(statusBar = v) } },
                        testTagPrefix = "de_bar"
                    )
                    OptionChipGroup(
                        label = "Bar Position",
                        options = LinuxProfile.BAR_POSITIONS,
                        selected = profile.barPosition,
                        onSelect = { v -> viewModel.updateProfile { it.copy(barPosition = v) } },
                        testTagPrefix = "de_bar_pos"
                    )
                    OptionChipGroup(
                        label = "Terminal Emulator",
                        options = LinuxProfile.TERMINALS,
                        selected = profile.terminal,
                        onSelect = { v -> viewModel.updateProfile { it.copy(terminal = v) } },
                        testTagPrefix = "de_term"
                    )
                    OptionChipGroup(
                        label = "App Launcher",
                        options = LinuxProfile.LAUNCHERS,
                        selected = profile.launcher,
                        onSelect = { v -> viewModel.updateProfile { it.copy(launcher = v) } },
                        testTagPrefix = "de_launcher"
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Accent & Window Geometry",
                        subtitle = "Live-updated in the preview above",
                        icon = Icons.Rounded.Palette
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val swatches = listOf(
                            "Cyber Cyan" to CyberCyan,
                            "Matrix Mint" to MatrixMint,
                            "Solar Amber" to SolarAmber,
                            "Crimson Coral" to CrimsonCoral,
                            "Violet Pulse" to VioletPulse
                        )
                        swatches.forEach { (name, color) ->
                            val selected = profile.accentPreset == name
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.25f))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) color else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateProfile { it.copy(accentPreset = name) }
                                    }
                                    .minimumInteractiveComponentSize()
                                    .testTag("accent_swatch_${name.lowercase().replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }

                    LabeledSlider(
                        label = "Window Gaps",
                        valueText = "${profile.windowGaps} px",
                        value = profile.windowGaps.toFloat(),
                        range = 0f..32f,
                        onValueChange = { v -> viewModel.updateProfile { it.copy(windowGaps = v.roundToInt().toLong()) } },
                        testTag = "slider_gaps"
                    )
                    LabeledSlider(
                        label = "Border Width",
                        valueText = "${profile.borderWidth} px",
                        value = profile.borderWidth.toFloat(),
                        range = 0f..8f,
                        onValueChange = { v -> viewModel.updateProfile { it.copy(borderWidth = v.roundToInt().toLong()) } },
                        testTag = "slider_border"
                    )
                    LabeledSlider(
                        label = "Corner Radius",
                        valueText = "${profile.cornerRadius} px",
                        value = profile.cornerRadius.toFloat(),
                        range = 0f..24f,
                        onValueChange = { v -> viewModel.updateProfile { it.copy(cornerRadius = v.roundToInt().toLong()) } },
                        testTag = "slider_radius"
                    )
                    LabeledSlider(
                        label = "Compositor Blur",
                        valueText = "${profile.blurStrength} passes",
                        value = profile.blurStrength.toFloat(),
                        range = 0f..12f,
                        onValueChange = { v -> viewModel.updateProfile { it.copy(blurStrength = v.roundToInt().toLong()) } },
                        testTag = "slider_blur"
                    )
                    LabeledSlider(
                        label = "Inactive Opacity",
                        valueText = "${(profile.windowOpacity * 100).roundToInt()}%",
                        value = profile.windowOpacity.toFloat(),
                        range = 0.5f..1.0f,
                        onValueChange = { v ->
                            val rounded = (v * 100).roundToInt() / 100.0
                            viewModel.updateProfile { it.copy(windowOpacity = rounded) }
                        },
                        testTag = "slider_opacity"
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Motion & Keybindings",
                        icon = Icons.Rounded.Tune
                    )
                    OptionChipGroup(
                        label = "Animation Curve",
                        options = LinuxProfile.ANIMATION_STYLES,
                        selected = profile.animationStyle,
                        onSelect = { v -> viewModel.updateProfile { it.copy(animationStyle = v) } },
                        testTagPrefix = "de_anim"
                    )
                    OptionChipGroup(
                        label = "Keybind Ergonomics",
                        options = LinuxProfile.KEYBIND_STYLES,
                        selected = profile.keybindStyle,
                        onSelect = { v -> viewModel.updateProfile { it.copy(keybindStyle = v) } },
                        testTagPrefix = "de_keys"
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDotfiles = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("export_dotfiles_button")
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dotfiles")
                }

                Button(
                    onClick = viewModel::saveCurrentProfile,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_de_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save DE")
                    }
                }
            }
        }
    }

    if (showDotfiles) {
        val filename = when (profile.desktopEnv) {
            "Hyprland", "Niri" -> "hyprland.conf"
            "Sway" -> "sway/config"
            else -> "setup-de.sh"
        }
        ConfigPreviewSheet(
            title = "${profile.desktopEnv} Configuration",
            filename = filename,
            content = ConfigGenerators.generateDesktopConfig(profile),
            onDismiss = { showDotfiles = false },
            onCopied = { onShowMessage("Copied $filename to clipboard") }
        )
    }
}

@Composable
private fun LiveDesktopPreviewCard(profile: LinuxProfile, accentColor: Color) {
    val gapDp = (profile.windowGaps.coerceIn(0L, 28L) / 2f).dp
    val radiusDp = profile.cornerRadius.coerceIn(0L, 20L).toInt().dp
    val borderDp = profile.borderWidth.coerceIn(0L, 5L).toInt().dp
    val inactiveAlpha = profile.windowOpacity.toFloat().coerceIn(0.5f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A1220),
                        accentColor.copy(alpha = 0.22f),
                        Color(0xFF101726)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (profile.barPosition == "Top") {
                PreviewStatusBar(profile, accentColor)
                Spacer(modifier = Modifier.height(gapDp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                // Active focused terminal window
                Box(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(radiusDp))
                        .background(Color(0xE60B101A))
                        .then(
                            if (borderDp > 0.dp) {
                                Modifier.border(borderDp, accentColor, RoundedCornerShape(radiusDp))
                            } else Modifier
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "${profile.terminal} • active",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$ uname -sr\nLinux ${profile.kernelVersion}\n$ echo ${profile.desktopEnv}\n${profile.desktopEnv} (${profile.scheduler})",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Inactive secondary pane
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gapDp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .alpha(inactiveAlpha)
                            .clip(RoundedCornerShape(radiusDp))
                            .background(Color(0xCC162033))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(radiusDp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "main.kx\nsay \"Ready\"\n${profile.launcher}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxWidth()
                            .alpha(inactiveAlpha)
                            .clip(RoundedCornerShape(radiusDp))
                            .background(Color(0xCC162033))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(radiusDp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "gaps: ${profile.windowGaps}px | blur: ${profile.blurStrength}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (profile.barPosition == "Bottom") {
                Spacer(modifier = Modifier.height(gapDp))
                PreviewStatusBar(profile, accentColor)
            }
        }
    }
}

@Composable
private fun PreviewStatusBar(profile: LinuxProfile, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC0D131F),
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[1] 2 3  •  ${profile.statusBar}",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Text(
                text = "${profile.desktopEnv} | ${profile.keybindStyle}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.testTag(testTag)
        )
    }
}
