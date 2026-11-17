package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class LinuxProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Kernix Workstation",
    val baseDistro: String = "Arch-Minimal",
    val architecture: String = "x86_64",
    val initSystem: String = "systemd",
    val filesystem: String = "btrfs",
    val kernelVersion: String = "6.12 LTS",
    val scheduler: String = "BORE",
    val preemption: String = "PREEMPT_DYNAMIC",
    val compiler: String = "Clang ThinLTO",
    val kernelFlags: List<String> = DEFAULT_KERNEL_FLAGS,
    val desktopEnv: String = "Hyprland",
    val statusBar: String = "Waybar",
    val barPosition: String = "Top",
    val terminal: String = "Ghostty",
    val launcher: String = "Rofi",
    val accentPreset: String = "Cyber Cyan",
    val windowGaps: Long = 10L,
    val borderWidth: Long = 2L,
    val cornerRadius: Long = 12L,
    val blurStrength: Long = 6L,
    val windowOpacity: Double = 0.92,
    val animationStyle: String = "Snappy Spring",
    val keybindStyle: String = "Vim-like",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        val BASE_DISTROS = listOf("Arch-Minimal", "Debian-Sid", "Alpine-musl", "Void-runit", "Gentoo-Source")
        val ARCHITECTURES = listOf("x86_64", "aarch64", "riscv64")
        val INIT_SYSTEMS = listOf("systemd", "OpenRC", "runit", "s6")
        val FILESYSTEMS = listOf("btrfs", "ext4", "zfs", "f2fs")
        val KERNEL_VERSIONS = listOf("6.12 LTS", "6.14 Mainline", "6.12-rt Realtime", "6.13-zen")
        val SCHEDULERS = listOf("EEVDF", "BORE", "MuQSS")
        val PREEMPTIONS = listOf("PREEMPT_DYNAMIC", "PREEMPT_RT", "PREEMPT_VOLUNTARY", "PREEMPT_NONE")
        val COMPILERS = listOf("Clang ThinLTO", "GCC 14 -O2", "GCC 14 -O3 Native")
        val DESKTOP_ENVS = listOf("Hyprland", "Sway", "KDE Plasma 6", "GNOME 47", "XFCE 4.20", "Niri")
        val STATUS_BARS = listOf("Waybar", "Polybar", "AGS", "Native Panel")
        val BAR_POSITIONS = listOf("Top", "Bottom")
        val TERMINALS = listOf("Ghostty", "Alacritty", "Kitty", "Foot")
        val LAUNCHERS = listOf("Rofi", "Wofi", "Fuzzel", "Vicinae")
        val ACCENT_PRESETS = listOf("Cyber Cyan", "Matrix Mint", "Solar Amber", "Crimson Coral", "Violet Pulse")
        val ANIMATION_STYLES = listOf("Snappy Spring", "Fluid Bezier", "Instant")
        val KEYBIND_STYLES = listOf("Vim-like", "Standard", "macOS-inspired")

        val DEFAULT_KERNEL_FLAGS = listOf(
            "CONFIG_WIREGUARD",
            "CONFIG_BPF_JIT",
            "CONFIG_ZRAM",
            "CONFIG_SECURITY_LANDLOCK",
            "CONFIG_CGROUPS",
            "CONFIG_USER_NS",
            "CONFIG_HZ_1000"
        )
    }
}

data class KernelFlagOption(
    val symbol: String,
    val title: String,
    val category: String,
    val description: String
)

object KernelCatalog {
    val flags = listOf(
        KernelFlagOption("CONFIG_WIREGUARD", "WireGuard VPN", "Network", "In-kernel encrypted mesh VPN"),
        KernelFlagOption("CONFIG_BPF_JIT", "eBPF JIT Compiler", "Network", "High-speed packet filtering & tracing"),
        KernelFlagOption("CONFIG_TCP_CONG_BBR", "TCP BBR Congestion", "Network", "Google BBR low-latency throughput"),
        KernelFlagOption("CONFIG_ZRAM", "ZRAM Compressed RAM", "Memory", "In-memory LZ4/Zstd swap block device"),
        KernelFlagOption("CONFIG_ZSWAP", "Zswap Cache", "Memory", "Compressed writeback cache for swap"),
        KernelFlagOption("CONFIG_TRANSPARENT_HUGEPAGE", "Transparent Hugepages", "Memory", "2MB pages for heavy workloads"),
        KernelFlagOption("CONFIG_SECURITY_LANDLOCK", "Landlock LSM", "Security", "Unprivileged process sandboxing"),
        KernelFlagOption("CONFIG_SECURITY_APPARMOR", "AppArmor MAC", "Security", "Path-based mandatory access control"),
        KernelFlagOption("CONFIG_SECCOMP_FILTER", "Seccomp Syscall Filter", "Security", "BPF syscall sandboxing for containers"),
        KernelFlagOption("CONFIG_CGROUPS", "Control Groups v2", "Container", "Resource isolation for systemd & Podman"),
        KernelFlagOption("CONFIG_USER_NS", "User Namespaces", "Container", "Rootless containers & Flatpak support"),
        KernelFlagOption("CONFIG_KVM", "KVM Hypervisor", "Virtualization", "Hardware-accelerated Linux VMs"),
        KernelFlagOption("CONFIG_DRM_AMDGPU", "AMDGPU DRM Driver", "Graphics", "Open-source AMD Radeon Wayland KMS"),
        KernelFlagOption("CONFIG_DRM_XE", "Intel Xe Graphics", "Graphics", "Modern Intel Arc & integrated GPU driver"),
        KernelFlagOption("CONFIG_HZ_1000", "1000Hz Timer Tick", "Timing", "Snappy desktop & audio responsiveness"),
        KernelFlagOption("CONFIG_SND_HDA_INTEL", "HD Audio Codec", "Audio", "Low-latency PipeWire hardware audio")
    )
}

@Keep
@IgnoreExtraProperties
data class CustomLanguage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "KoraLang",
    val fileExtension: String = ".kx",
    val commentPrefix: String = "//",
    val kwVar: String = "make",
    val kwPrint: String = "say",
    val kwFunc: String = "task",
    val kwIf: String = "when",
    val kwElse: String = "otherwise",
    val kwLoop: String = "repeat",
    val kwReturn: String = "give",
    val kwTrue: String = "yes",
    val kwFalse: String = "no",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        val COMMENT_PREFIXES = listOf("//", "#", "--")
        val PRESETS = listOf(
            CustomLanguage(
                name = "KoraLang (Ultra Easy)",
                fileExtension = ".kx",
                commentPrefix = "//",
                kwVar = "make",
                kwPrint = "say",
                kwFunc = "task",
                kwIf = "when",
                kwElse = "otherwise",
                kwLoop = "repeat",
                kwReturn = "give",
                kwTrue = "yes",
                kwFalse = "no"
            ),
            CustomLanguage(
                name = "PyLite (Clean)",
                fileExtension = ".pyl",
                commentPrefix = "#",
                kwVar = "let",
                kwPrint = "print",
                kwFunc = "def",
                kwIf = "if",
                kwElse = "else",
                kwLoop = "loop",
                kwReturn = "return",
                kwTrue = "true",
                kwFalse = "false"
            ),
            CustomLanguage(
                name = "NovaSys (Linux Dev)",
                fileExtension = ".nv",
                commentPrefix = "//",
                kwVar = "set",
                kwPrint = "emit",
                kwFunc = "fn",
                kwIf = "check",
                kwElse = "alt",
                kwLoop = "cycle",
                kwReturn = "ret",
                kwTrue = "on",
                kwFalse = "off"
            ),
            CustomLanguage(
                name = "FacilScript (Español)",
                fileExtension = ".fc",
                commentPrefix = "--",
                kwVar = "crear",
                kwPrint = "decir",
                kwFunc = "accion",
                kwIf = "si",
                kwElse = "sino",
                kwLoop = "repetir",
                kwReturn = "dar",
                kwTrue = "verdad",
                kwFalse = "falso"
            )
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CodeScript(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "System Boot Probe",
    val languageId: String = "default",
    val sourceCode: String = "",
    val lastOutput: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
