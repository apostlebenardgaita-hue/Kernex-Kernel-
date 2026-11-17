package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CodeScript
import com.example.data.model.CustomLanguage
import com.example.data.model.LinuxProfile
import com.example.data.model.UiState
import com.example.data.repository.KernixRepository
import com.example.domain.CodeTemplate
import com.example.domain.ExecutionResult
import com.example.domain.KernixLanguageEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KernixViewModel(
    private val repository: KernixRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _draftProfile = MutableStateFlow(LinuxProfile(userId = currentUserId))
    val draftProfile: StateFlow<LinuxProfile> = _draftProfile.asStateFlow()

    private val _draftLanguage = MutableStateFlow(CustomLanguage(userId = currentUserId))
    val draftLanguage: StateFlow<CustomLanguage> = _draftLanguage.asStateFlow()

    private val _draftScript = MutableStateFlow(
        CodeScript(
            userId = currentUserId,
            title = "Linux Boot Probe",
            sourceCode = KernixLanguageEngine.templates.first().builder(CustomLanguage())
        )
    )
    val draftScript: StateFlow<CodeScript> = _draftScript.asStateFlow()

    private val _executionResult = MutableStateFlow<ExecutionResult?>(null)
    val executionResult: StateFlow<ExecutionResult?> = _executionResult.asStateFlow()

    private val _bashPreview = MutableStateFlow("")
    val bashPreview: StateFlow<String> = _bashPreview.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var profileInitializedFromCloud = false
    private var languageInitializedFromCloud = false
    private var scriptInitializedFromCloud = false

    val profilesState: StateFlow<UiState<List<LinuxProfile>>> =
        repository.observeProfiles(currentUserId)
            .onEach { list ->
                if (!profileInitializedFromCloud && list.isNotEmpty()) {
                    profileInitializedFromCloud = true
                    _draftProfile.value = list.first()
                }
            }
            .map<List<LinuxProfile>, UiState<List<LinuxProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing profiles", root)
                emit(UiState.Error(root.message ?: "Failed to load Linux profiles"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val languagesState: StateFlow<UiState<List<CustomLanguage>>> =
        repository.observeLanguages(currentUserId)
            .onEach { list ->
                if (!languageInitializedFromCloud && list.isNotEmpty()) {
                    languageInitializedFromCloud = true
                    _draftLanguage.value = list.first()
                }
            }
            .map<List<CustomLanguage>, UiState<List<CustomLanguage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing languages", root)
                emit(UiState.Error(root.message ?: "Failed to load custom languages"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val scriptsState: StateFlow<UiState<List<CodeScript>>> =
        repository.observeScripts(currentUserId)
            .onEach { list ->
                if (!scriptInitializedFromCloud && list.isNotEmpty()) {
                    scriptInitializedFromCloud = true
                    _draftScript.value = list.first()
                }
            }
            .map<List<CodeScript>, UiState<List<CodeScript>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing scripts", root)
                emit(UiState.Error(root.message ?: "Failed to load scripts"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        runCode()
    }

    // --- Profile Mutations ---
    fun selectProfile(profile: LinuxProfile) {
        _draftProfile.value = profile
    }

    fun newProfile() {
        _draftProfile.value = LinuxProfile(
            userId = currentUserId,
            name = "Custom Linux Build"
        )
    }

    fun updateProfile(transform: (LinuxProfile) -> LinuxProfile) {
        _draftProfile.value = transform(_draftProfile.value)
    }

    fun toggleKernelFlag(flag: String) {
        _draftProfile.value = _draftProfile.value.let { current ->
            val updated = if (flag in current.kernelFlags) {
                current.kernelFlags - flag
            } else if (current.kernelFlags.size < 40) {
                current.kernelFlags + flag
            } else {
                current.kernelFlags
            }
            current.copy(kernelFlags = updated)
        }
    }

    fun saveCurrentProfile() {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveProfile(currentUserId, _draftProfile.value)
            _isSaving.value = false
            result.onSuccess { docId ->
                if (_draftProfile.value.id.isBlank()) {
                    _draftProfile.value = _draftProfile.value.copy(id = docId)
                }
                _events.emit("Saved Linux & Kernel profile '${_draftProfile.value.name}'")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to save profile")
            }
        }
    }

    fun deleteProfile(profileId: String) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteProfile(currentUserId, profileId)
            result.onSuccess {
                if (_draftProfile.value.id == profileId) {
                    newProfile()
                }
                _events.emit("Deleted profile")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to delete profile")
            }
        }
    }

    // --- Language Mutations ---
    fun selectLanguage(lang: CustomLanguage) {
        _draftLanguage.value = lang
    }

    fun newLanguage() {
        _draftLanguage.value = CustomLanguage(userId = currentUserId, name = "MyLang")
    }

    fun updateLanguage(transform: (CustomLanguage) -> CustomLanguage) {
        _draftLanguage.value = transform(_draftLanguage.value)
    }

    fun applyLanguagePreset(preset: CustomLanguage) {
        _draftLanguage.value = preset.copy(
            id = _draftLanguage.value.id,
            userId = currentUserId,
            createdAt = _draftLanguage.value.createdAt
        )
    }

    fun saveCurrentLanguage() {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveLanguage(currentUserId, _draftLanguage.value)
            _isSaving.value = false
            result.onSuccess { docId ->
                if (_draftLanguage.value.id.isBlank()) {
                    _draftLanguage.value = _draftLanguage.value.copy(id = docId)
                }
                _events.emit("Saved language '${_draftLanguage.value.name}'")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to save language")
            }
        }
    }

    fun deleteLanguage(languageId: String) {
        if (languageId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteLanguage(currentUserId, languageId)
            result.onSuccess {
                if (_draftLanguage.value.id == languageId) {
                    newLanguage()
                }
                _events.emit("Deleted language")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to delete language")
            }
        }
    }

    // --- Script & IDE ---
    fun selectScript(script: CodeScript) {
        _draftScript.value = script
        runCode()
    }

    fun newScript() {
        val lang = _draftLanguage.value
        _draftScript.value = CodeScript(
            userId = currentUserId,
            title = "New Script",
            languageId = lang.id.ifBlank { "default" },
            sourceCode = "${lang.commentPrefix} Write ${lang.name} code\n${lang.kwPrint} \"Hello from \" + uname()"
        )
        runCode()
    }

    fun updateScriptTitle(title: String) {
        _draftScript.value = _draftScript.value.copy(title = title)
    }

    fun updateScriptSource(source: String) {
        _draftScript.value = _draftScript.value.copy(sourceCode = source)
    }

    fun insertKeywordSnippet(snippet: String) {
        val cur = _draftScript.value.sourceCode
        val sep = if (cur.isEmpty() || cur.endsWith("\n") || cur.endsWith(" ")) "" else " "
        _draftScript.value = _draftScript.value.copy(sourceCode = "$cur$sep$snippet")
    }

    fun applyTemplate(template: CodeTemplate) {
        val lang = _draftLanguage.value
        _draftScript.value = _draftScript.value.copy(
            title = template.title,
            languageId = lang.id.ifBlank { "default" },
            sourceCode = template.builder(lang)
        )
        runCode()
    }

    fun runCode() {
        val script = _draftScript.value
        val lang = _draftLanguage.value
        val profile = _draftProfile.value
        val result = KernixLanguageEngine.execute(script.sourceCode, lang, profile)
        _executionResult.value = result
        _bashPreview.value = KernixLanguageEngine.transpileToBash(script.sourceCode, lang, profile)
        val outText = if (result.errorMessage != null) {
            "ERROR: ${result.errorMessage}"
        } else {
            result.outputLines.joinToString("\n")
        }
        _draftScript.value = script.copy(lastOutput = outText.take(20000))
    }

    fun saveCurrentScript() {
        viewModelScope.launch {
            _isSaving.value = true
            val langId = _draftLanguage.value.id.ifBlank { "default" }
            val result = repository.saveScript(
                currentUserId,
                _draftScript.value.copy(languageId = langId)
            )
            _isSaving.value = false
            result.onSuccess { docId ->
                if (_draftScript.value.id.isBlank()) {
                    _draftScript.value = _draftScript.value.copy(id = docId)
                }
                _events.emit("Saved script '${_draftScript.value.title}'")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to save script")
            }
        }
    }

    fun deleteScript(scriptId: String) {
        if (scriptId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteScript(currentUserId, scriptId)
            result.onSuccess {
                if (_draftScript.value.id == scriptId) {
                    newScript()
                }
                _events.emit("Deleted script")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to delete script")
            }
        }
    }

    private companion object {
        const val TAG = "KernixVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
