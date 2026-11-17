package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CustomLanguage
import com.example.data.model.UiState
import com.example.ui.components.OptionChipGroup
import com.example.ui.components.SavedItemStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.KernixViewModel

@Composable
fun LanguageDesignerScreen(
    viewModel: KernixViewModel,
    onOpenIde: () -> Unit
) {
    val language by viewModel.draftLanguage.collectAsStateWithLifecycle()
    val languagesState by viewModel.languagesState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "Custom language syntax studio",
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
                        text = "CUSTOM LANGUAGE FORGE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "${language.name} (${language.fileExtension})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Rename keywords into any style for effortless coding",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            val savedList = (languagesState as? UiState.Success)?.data.orEmpty()
            SavedItemStrip(
                items = savedList.map { it.id to it.name },
                selectedId = language.id,
                newLabel = "New Lang",
                onSelect = { id -> savedList.firstOrNull { it.id == id }?.let(viewModel::selectLanguage) },
                onNew = viewModel::newLanguage,
                onDeleteSelected = if (language.id.isNotBlank()) {
                    { viewModel.deleteLanguage(language.id) }
                } else null,
                testTagPrefix = "lang"
            )
            if (languagesState is UiState.Error) {
                Text(
                    text = (languagesState as UiState.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item {
            SectionHeader(
                title = "Quick Syntax Presets",
                subtitle = "Start with an easy style or invent your own keywords below",
                icon = Icons.Rounded.AutoAwesome
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomLanguage.PRESETS.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { viewModel.applyLanguagePreset(preset) }
                            .minimumInteractiveComponentSize()
                            .testTag("preset_${preset.fileExtension.removePrefix(".")}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${preset.kwVar} / ${preset.kwPrint} / ${preset.kwLoop}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Language Identity & Keywords",
                        subtitle = "Keywords use letters, digits, or underscores (no spaces)",
                        icon = Icons.Rounded.Translate
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = language.name,
                            onValueChange = { v -> viewModel.updateLanguage { it.copy(name = v.take(40)) } },
                            label = { Text("Language Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.6f)
                                .testTag("lang_name_input")
                        )
                        OutlinedTextField(
                            value = language.fileExtension,
                            onValueChange = { v -> viewModel.updateLanguage { it.copy(fileExtension = v.take(10)) } },
                            label = { Text("Extension") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_ext_input")
                        )
                    }

                    OptionChipGroup(
                        label = "Comment Prefix",
                        options = CustomLanguage.COMMENT_PREFIXES,
                        selected = language.commentPrefix,
                        onSelect = { v -> viewModel.updateLanguage { it.copy(commentPrefix = v) } },
                        testTagPrefix = "comment_prefix"
                    )

                    KeywordPairRow(
                        leftLabel = "Variable (let)",
                        leftValue = language.kwVar,
                        onLeftChange = { v -> viewModel.updateLanguage { it.copy(kwVar = cleanKeywordInput(v)) } },
                        leftTag = "kw_var_input",
                        rightLabel = "Output (print)",
                        rightValue = language.kwPrint,
                        onRightChange = { v -> viewModel.updateLanguage { it.copy(kwPrint = cleanKeywordInput(v)) } },
                        rightTag = "kw_print_input"
                    )
                    KeywordPairRow(
                        leftLabel = "Function (fn)",
                        leftValue = language.kwFunc,
                        onLeftChange = { v -> viewModel.updateLanguage { it.copy(kwFunc = cleanKeywordInput(v)) } },
                        leftTag = "kw_func_input",
                        rightLabel = "Return",
                        rightValue = language.kwReturn,
                        onRightChange = { v -> viewModel.updateLanguage { it.copy(kwReturn = cleanKeywordInput(v)) } },
                        rightTag = "kw_return_input"
                    )
                    KeywordPairRow(
                        leftLabel = "If Condition",
                        leftValue = language.kwIf,
                        onLeftChange = { v -> viewModel.updateLanguage { it.copy(kwIf = cleanKeywordInput(v)) } },
                        leftTag = "kw_if_input",
                        rightLabel = "Else Branch",
                        rightValue = language.kwElse,
                        onRightChange = { v -> viewModel.updateLanguage { it.copy(kwElse = cleanKeywordInput(v)) } },
                        rightTag = "kw_else_input"
                    )
                    KeywordPairRow(
                        leftLabel = "Loop / Repeat",
                        leftValue = language.kwLoop,
                        onLeftChange = { v -> viewModel.updateLanguage { it.copy(kwLoop = cleanKeywordInput(v)) } },
                        leftTag = "kw_loop_input",
                        rightLabel = "Boolean True",
                        rightValue = language.kwTrue,
                        onRightChange = { v -> viewModel.updateLanguage { it.copy(kwTrue = cleanKeywordInput(v)) } },
                        rightTag = "kw_true_input"
                    )
                    OutlinedTextField(
                        value = language.kwFalse,
                        onValueChange = { v -> viewModel.updateLanguage { it.copy(kwFalse = cleanKeywordInput(v)) } },
                        label = { Text("Boolean False") },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kw_false_input")
                    )
                }
            }
        }

        item {
            val previewCode = """
                ${language.commentPrefix} Sample in ${language.name} (${language.fileExtension})
                ${language.kwFunc} greet(user) {
                    ${language.kwReturn} "Ready on " + user
                }
                ${language.kwVar} active = ${language.kwTrue}
                ${language.kwIf} active {
                    ${language.kwLoop} i in 1..3 {
                        ${language.kwPrint} greet("Kernix #" + i)
                    }
                } ${language.kwElse} {
                    ${language.kwPrint} "Offline"
                }
            """.trimIndent()

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE SYNTAX PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = previewCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
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
                    onClick = onOpenIde,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("open_ide_from_lang_button")
                ) {
                    Text("Code in IDE")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = viewModel::saveCurrentLanguage,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_language_button")
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
                        Text("Save Lang")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordPairRow(
    leftLabel: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    leftTag: String,
    rightLabel: String,
    rightValue: String,
    onRightChange: (String) -> Unit,
    rightTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = leftValue,
            onValueChange = onLeftChange,
            label = { Text(leftLabel) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
            modifier = Modifier
                .weight(1f)
                .testTag(leftTag)
        )
        OutlinedTextField(
            value = rightValue,
            onValueChange = onRightChange,
            label = { Text(rightLabel) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
            modifier = Modifier
                .weight(1f)
                .testTag(rightTag)
        )
    }
}

private fun cleanKeywordInput(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_]"), "").take(24)
