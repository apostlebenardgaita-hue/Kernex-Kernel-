package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CodeScript
import com.example.data.model.CustomLanguage
import com.example.data.model.LinuxProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KernixRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfiles(userId: String): Flow<List<LinuxProfile>> =
        db.collection("users").document(userId).collection("linuxProfiles")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LinuxProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeLanguages(userId: String): Flow<List<CustomLanguage>> =
        db.collection("users").document(userId).collection("languages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomLanguage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeScripts(userId: String): Flow<List<CodeScript>> =
        db.collection("users").document(userId).collection("scripts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CodeScript::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun saveProfile(userId: String, profile: LinuxProfile): Result<String> {
        val isCreate = profile.id.isBlank()
        val col = db.collection("users").document(userId).collection("linuxProfiles")
        val docRef = if (isCreate) col.document() else col.document(profile.id)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanName = profile.name.trim().ifEmpty { "Kernix Workstation" }.take(60)
            val cleanFlags = profile.kernelFlags.filter { it.isNotBlank() }.distinct().take(40)
            val gaps = profile.windowGaps.coerceIn(0L, 32L)
            val border = profile.borderWidth.coerceIn(0L, 8L)
            val radius = profile.cornerRadius.coerceIn(0L, 24L)
            val blur = profile.blurStrength.coerceIn(0L, 12L)
            val opacity = profile.windowOpacity.coerceIn(0.5, 1.0)

            val commonFields = mutableMapOf<String, Any>(
                "name" to cleanName,
                "baseDistro" to (if (profile.baseDistro in LinuxProfile.BASE_DISTROS) profile.baseDistro else LinuxProfile.BASE_DISTROS.first()),
                "architecture" to (if (profile.architecture in LinuxProfile.ARCHITECTURES) profile.architecture else LinuxProfile.ARCHITECTURES.first()),
                "initSystem" to (if (profile.initSystem in LinuxProfile.INIT_SYSTEMS) profile.initSystem else LinuxProfile.INIT_SYSTEMS.first()),
                "filesystem" to (if (profile.filesystem in LinuxProfile.FILESYSTEMS) profile.filesystem else LinuxProfile.FILESYSTEMS.first()),
                "kernelVersion" to (if (profile.kernelVersion in LinuxProfile.KERNEL_VERSIONS) profile.kernelVersion else LinuxProfile.KERNEL_VERSIONS.first()),
                "scheduler" to (if (profile.scheduler in LinuxProfile.SCHEDULERS) profile.scheduler else LinuxProfile.SCHEDULERS.first()),
                "preemption" to (if (profile.preemption in LinuxProfile.PREEMPTIONS) profile.preemption else LinuxProfile.PREEMPTIONS.first()),
                "compiler" to (if (profile.compiler in LinuxProfile.COMPILERS) profile.compiler else LinuxProfile.COMPILERS.first()),
                "kernelFlags" to cleanFlags,
                "desktopEnv" to (if (profile.desktopEnv in LinuxProfile.DESKTOP_ENVS) profile.desktopEnv else LinuxProfile.DESKTOP_ENVS.first()),
                "statusBar" to (if (profile.statusBar in LinuxProfile.STATUS_BARS) profile.statusBar else LinuxProfile.STATUS_BARS.first()),
                "barPosition" to (if (profile.barPosition in LinuxProfile.BAR_POSITIONS) profile.barPosition else LinuxProfile.BAR_POSITIONS.first()),
                "terminal" to (if (profile.terminal in LinuxProfile.TERMINALS) profile.terminal else LinuxProfile.TERMINALS.first()),
                "launcher" to (if (profile.launcher in LinuxProfile.LAUNCHERS) profile.launcher else LinuxProfile.LAUNCHERS.first()),
                "accentPreset" to (if (profile.accentPreset in LinuxProfile.ACCENT_PRESETS) profile.accentPreset else LinuxProfile.ACCENT_PRESETS.first()),
                "windowGaps" to gaps,
                "borderWidth" to border,
                "cornerRadius" to radius,
                "blurStrength" to blur,
                "windowOpacity" to opacity,
                "animationStyle" to (if (profile.animationStyle in LinuxProfile.ANIMATION_STYLES) profile.animationStyle else LinuxProfile.ANIMATION_STYLES.first()),
                "keybindStyle" to (if (profile.keybindStyle in LinuxProfile.KEYBIND_STYLES) profile.keybindStyle else LinuxProfile.KEYBIND_STYLES.first()),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isCreate) {
                commonFields["userId"] = uid
                commonFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(commonFields).await()
            } else {
                docRef.update(commonFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(userId: String, profileId: String): Result<Unit> {
        val path = "users/$userId/linuxProfiles/$profileId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(profileId.isNotBlank()) { "Profile ID cannot be blank" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun sanitizeKeyword(raw: String, fallback: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_]"), "")
        return if (cleaned.isNotEmpty() && (cleaned[0].isLetter() || cleaned[0] == '_')) {
            cleaned.take(24)
        } else {
            fallback
        }
    }

    suspend fun saveLanguage(userId: String, language: CustomLanguage): Result<String> {
        val isCreate = language.id.isBlank()
        val col = db.collection("users").document(userId).collection("languages")
        val docRef = if (isCreate) col.document() else col.document(language.id)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val extRaw = language.fileExtension.trim()
            val ext = (if (extRaw.startsWith(".")) extRaw else ".$extRaw")
                .take(10)
                .let { if (it.length < 2) ".kx" else it }
            val payload = mutableMapOf<String, Any>(
                "name" to language.name.trim().ifEmpty { "KoraLang" }.take(40),
                "fileExtension" to ext,
                "commentPrefix" to (if (language.commentPrefix in CustomLanguage.COMMENT_PREFIXES) language.commentPrefix else "//"),
                "kwVar" to sanitizeKeyword(language.kwVar, "make"),
                "kwPrint" to sanitizeKeyword(language.kwPrint, "say"),
                "kwFunc" to sanitizeKeyword(language.kwFunc, "task"),
                "kwIf" to sanitizeKeyword(language.kwIf, "when"),
                "kwElse" to sanitizeKeyword(language.kwElse, "otherwise"),
                "kwLoop" to sanitizeKeyword(language.kwLoop, "repeat"),
                "kwReturn" to sanitizeKeyword(language.kwReturn, "give"),
                "kwTrue" to sanitizeKeyword(language.kwTrue, "yes"),
                "kwFalse" to sanitizeKeyword(language.kwFalse, "no"),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                payload["userId"] = uid
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLanguage(userId: String, languageId: String): Result<Unit> {
        val path = "users/$userId/languages/$languageId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(languageId.isNotBlank()) { "Language ID cannot be blank" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveScript(userId: String, script: CodeScript): Result<String> {
        val isCreate = script.id.isBlank()
        val col = db.collection("users").document(userId).collection("scripts")
        val docRef = if (isCreate) col.document() else col.document(script.id)
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val payload = mutableMapOf<String, Any>(
                "title" to script.title.trim().ifEmpty { "Untitled Script" }.take(80),
                "languageId" to script.languageId.trim().ifEmpty { "default" }.take(128),
                "sourceCode" to script.sourceCode.take(50000),
                "lastOutput" to script.lastOutput.take(20000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCreate) {
                payload["userId"] = uid
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteScript(userId: String, scriptId: String): Result<Unit> {
        val path = "users/$userId/scripts/$scriptId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(scriptId.isNotBlank()) { "Script ID cannot be blank" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
