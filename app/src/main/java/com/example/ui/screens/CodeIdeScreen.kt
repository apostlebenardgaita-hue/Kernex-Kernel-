package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.domain.KernixLanguageEngine
import com.example.ui.components.ConfigPreviewSheet
import com.example.ui.components.SavedItemStrip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.KernixViewModel

@Composable
fun CodeIdeScreen(
    viewModel: KernixViewModel,
    onShowMessage: (String) -> Unit
) {
    val script by viewModel.draftScript.collectAsStateWithLifecycle()
    val language by viewModel.draftLanguage.collectAsStateWithLifecycle()
    val scriptsState by viewModel.scriptsState.collectAsStateWithLifecycle()
    val execResult by viewModel.executionResult.collectAsStateWithLifecycle()
    val bashPreview by viewModel.bashPreview.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var selectedConsoleTab by remember { mutableIntStateOf(0) }
    var showBashExport by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val savedScripts = (scriptsState as? UiState.Success)?.data.orEmpty()
            SavedItemStrip(
                items = savedScripts.map { it.id to it.title },
                selectedId = script.id,
                newLabel = "New Script",
                onSelect = { id -> savedScripts.firstOrNull { it.id == id }?.let(viewModel::selectScript) },
                onNew = viewModel::newScript,
                onDeleteSelected = if (script.id.isNotBlank()) {
                    { viewModel.deleteScript(script.id) }
                } else null,
                testTagPrefix = "script"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = script.title,
                    onValueChange = viewModel::updateScriptTitle,
                    label = { Text("Script Title (${language.fileExtension})") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("script_title_input")
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = language.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Easy Starter Snippets",
                subtitle = "Auto-written using your custom ${language.name} keywords",
                icon = Icons.Rounded.Code
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KernixLanguageEngine.templates.forEach { tpl ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { viewModel.applyTemplate(tpl) }
                            .minimumInteractiveComponentSize()
                            .testTag("template_${tpl.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tpl.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            // One-tap keyword insertion bar
            val quickTokens = listOf(
                "${language.kwVar} x = " to language.kwVar,
                "${language.kwPrint} " to language.kwPrint,
                "${language.kwIf} cond {\n    \n}" to language.kwIf,
                "${language.kwLoop} i in 1..5 {\n    \n}" to "${language.kwLoop}..",
                "${language.kwFunc} fn(a) {\n    ${language.kwReturn} a\n}" to language.kwFunc,
                "uname()" to "uname()",
                "kernel_info()" to "kernel_info()",
                "de_info()" to "de_info()"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickTokens.forEach { (snippet, label) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.insertKeywordSnippet(snippet) }
                            .minimumInteractiveComponentSize()
                            .testTag("quick_insert_${label.replace("(", "").replace(")", "").replace(".", "_")}")
                    ) {
                        Text(
                            text = "+ $label",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            val lineCount = script.sourceCode.lines().size
            OutlinedTextField(
                value = script.sourceCode,
                onValueChange = viewModel::updateScriptSource,
                label = { Text("Source Code ($lineCount lines)") },
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                minLines = 8,
                maxLines = 18,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("code_editor_input")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::runCode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("run_code_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run")
                }

                OutlinedButton(
                    onClick = { showBashExport = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("to_bash_button")
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bash")
                }

                Button(
                    onClick = viewModel::saveCurrentScript,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_script_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = selectedConsoleTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedConsoleTab == 0,
                            onClick = { selectedConsoleTab = 0 },
                            text = { Text("Output") },
                            modifier = Modifier.testTag("console_tab_output")
                        )
                        Tab(
                            selected = selectedConsoleTab == 1,
                            onClick = { selectedConsoleTab = 1 },
                            text = { Text("Tokens & AST") },
                            modifier = Modifier.testTag("console_tab_tokens")
                        )
                        Tab(
                            selected = selectedConsoleTab == 2,
                            onClick = { selectedConsoleTab = 2 },
                            text = { Text("Bash") },
                            modifier = Modifier.testTag("console_tab_bash")
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(14.dp)
                    ) {
                        val res = execResult
                        when (selectedConsoleTab) {
                            0 -> {
                                if (res == null) {
                                    Text(
                                        text = "Tap Run to execute your ${language.name} script.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Executed in ${res.durationMs} ms • ${res.astSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    if (res.errorMessage != null) {
                                        Text(
                                            text = "Error: ${res.errorMessage}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else if (res.outputLines.isEmpty()) {
                                        Text(
                                            text = "(Program finished with no output)",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = res.outputLines.joinToString("\n"),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.testTag("console_output_text")
                                        )
                                    }
                                }
                            }
                            1 -> {
                                Text(
                                    text = res?.astSummary ?: "Run code to inspect AST",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = res?.tokenPreview?.ifEmpty { "(No tokens)" } ?: "",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = bashPreview,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBashExport) {
        val slug = script.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "script" }
        ConfigPreviewSheet(
            title = "Transpiled Linux Shell Script",
            filename = "$slug.sh",
            content = bashPreview,
            onDismiss = { showBashExport = false },
            onCopied = { onShowMessage("Copied $slug.sh to clipboard") }
        )
    }
}
