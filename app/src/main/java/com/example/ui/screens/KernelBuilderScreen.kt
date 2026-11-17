package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.KernelCatalog
import com.example.data.model.LinuxProfile
import com.example.data.model.UiState
import com.example.domain.ConfigGenerators
import com.example.ui.components.ConfigPreviewSheet
import com.example.ui.components.OptionChipGroup
import com.example.ui.components.SavedItemStrip
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.KernixViewModel

@Composable
fun KernelBuilderScreen(
    viewModel: KernixViewModel,
    onShowMessage: (String) -> Unit
) {
    val profile by viewModel.draftProfile.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var previewSheet by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kernel_hero),
                    contentDescription = "Linux kernel subsystem topology",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xE60C1019), Color(0x880C1019))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${profile.baseDistro} • ${profile.architecture}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Linux ${profile.kernelVersion} • ${profile.scheduler} • ${profile.kernelFlags.size} flags active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            val savedList = (profilesState as? UiState.Success)?.data.orEmpty()
            SavedItemStrip(
                items = savedList.map { it.id to it.name },
                selectedId = profile.id,
                newLabel = "New Distro",
                onSelect = { id -> savedList.firstOrNull { it.id == id }?.let(viewModel::selectProfile) },
                onNew = viewModel::newProfile,
                onDeleteSelected = if (profile.id.isNotBlank()) {
                    { viewModel.deleteProfile(profile.id) }
                } else null,
                testTagPrefix = "profile"
            )
            if (profilesState is UiState.Error) {
                Text(
                    text = (profilesState as UiState.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
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
                        title = "Linux Base & Architecture",
                        subtitle = "Choose your distribution userland, init & root filesystem",
                        icon = Icons.Rounded.SettingsApplications
                    )
                    OutlinedTextField(
                        value = profile.name,
                        onValueChange = { name -> viewModel.updateProfile { it.copy(name = name.take(60)) } },
                        label = { Text("Distro / Build Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("distro_name_input")
                    )
                    OptionChipGroup(
                        label = "Base Distribution",
                        options = LinuxProfile.BASE_DISTROS,
                        selected = profile.baseDistro,
                        onSelect = { v -> viewModel.updateProfile { it.copy(baseDistro = v) } },
                        testTagPrefix = "base_distro"
                    )
                    OptionChipGroup(
                        label = "CPU Architecture",
                        options = LinuxProfile.ARCHITECTURES,
                        selected = profile.architecture,
                        onSelect = { v -> viewModel.updateProfile { it.copy(architecture = v) } },
                        testTagPrefix = "arch"
                    )
                    OptionChipGroup(
                        label = "Init System",
                        options = LinuxProfile.INIT_SYSTEMS,
                        selected = profile.initSystem,
                        onSelect = { v -> viewModel.updateProfile { it.copy(initSystem = v) } },
                        testTagPrefix = "init_sys"
                    )
                    OptionChipGroup(
                        label = "Root Filesystem",
                        options = LinuxProfile.FILESYSTEMS,
                        selected = profile.filesystem,
                        onSelect = { v -> viewModel.updateProfile { it.copy(filesystem = v) } },
                        testTagPrefix = "fs"
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
                        title = "Custom Kernel Tuning",
                        subtitle = "Scheduler, preemption model & compiler flags",
                        icon = Icons.Rounded.Memory
                    )
                    OptionChipGroup(
                        label = "Kernel Branch",
                        options = LinuxProfile.KERNEL_VERSIONS,
                        selected = profile.kernelVersion,
                        onSelect = { v -> viewModel.updateProfile { it.copy(kernelVersion = v) } },
                        testTagPrefix = "kernel_ver"
                    )
                    OptionChipGroup(
                        label = "CPU Scheduler",
                        options = LinuxProfile.SCHEDULERS,
                        selected = profile.scheduler,
                        onSelect = { v -> viewModel.updateProfile { it.copy(scheduler = v) } },
                        testTagPrefix = "scheduler"
                    )
                    OptionChipGroup(
                        label = "Preemption Model",
                        options = LinuxProfile.PREEMPTIONS,
                        selected = profile.preemption,
                        onSelect = { v -> viewModel.updateProfile { it.copy(preemption = v) } },
                        testTagPrefix = "preempt"
                    )
                    OptionChipGroup(
                        label = "Compiler Toolchain",
                        options = LinuxProfile.COMPILERS,
                        selected = profile.compiler,
                        onSelect = { v -> viewModel.updateProfile { it.copy(compiler = v) } },
                        testTagPrefix = "compiler"
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Kernel Subsystems (menuconfig)",
                subtitle = "${profile.kernelFlags.size} / ${KernelCatalog.flags.size} modules compiled built-in (=y)",
                icon = Icons.Rounded.Build
            )
        }

        items(KernelCatalog.flags, key = { it.symbol }) { flag ->
            val enabled = flag.symbol in profile.kernelFlags
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = flag.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = flag.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = flag.symbol,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = flag.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.toggleKernelFlag(flag.symbol) },
                        modifier = Modifier.testTag("flag_switch_${flag.symbol}")
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            previewSheet = ".config" to ConfigGenerators.generateKernelConfig(profile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("view_kernel_config_button")
                    ) {
                        Text("Kernel .config")
                    }
                    OutlinedButton(
                        onClick = {
                            previewSheet = "build-kernix.sh" to ConfigGenerators.generateDistroBuildScript(profile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("view_build_script_button")
                    ) {
                        Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Build Script")
                    }
                }

                Button(
                    onClick = viewModel::saveCurrentProfile,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_kernel_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Distro & Kernel Profile")
                    }
                }
            }
        }
    }

    previewSheet?.let { (filename, content) ->
        ConfigPreviewSheet(
            title = "Generated Linux Build Artifact",
            filename = filename,
            content = content,
            onDismiss = { previewSheet = null },
            onCopied = { onShowMessage("Copied $filename to clipboard") }
        )
    }
}
