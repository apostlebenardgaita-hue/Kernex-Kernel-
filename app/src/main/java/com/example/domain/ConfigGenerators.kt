package com.example.domain

import com.example.data.model.LinuxProfile

object ConfigGenerators {

    fun accentHex(preset: String): String = when (preset) {
        "Cyber Cyan" -> "22d3ee"
        "Matrix Mint" -> "34d399"
        "Solar Amber" -> "fbbf24"
        "Crimson Coral" -> "fb7185"
        "Violet Pulse" -> "a78bfa"
        else -> "22d3ee"
    }

    fun generateKernelConfig(profile: LinuxProfile): String {
        val sb = StringBuilder()
        sb.appendLine("#")
        sb.appendLine("# Kernix Custom Linux Kernel Configuration")
        sb.appendLine("# Profile: ${profile.name}")
        sb.appendLine("# Kernel: ${profile.kernelVersion} | Arch: ${profile.architecture}")
        sb.appendLine("#")
        sb.appendLine()
        sb.appendLine("# Architecture & Toolchain")
        when (profile.architecture) {
            "x86_64" -> sb.appendLine("CONFIG_64BIT=y\nCONFIG_X86_64=y")
            "aarch64" -> sb.appendLine("CONFIG_64BIT=y\nCONFIG_ARM64=y")
            "riscv64" -> sb.appendLine("CONFIG_64BIT=y\nCONFIG_RISCV=y")
        }
        when (profile.compiler) {
            "Clang ThinLTO" -> sb.appendLine("CONFIG_CC_IS_CLANG=y\nCONFIG_LTO_CLANG_THIN=y")
            "GCC 14 -O2" -> sb.appendLine("CONFIG_CC_IS_GCC=y\nCONFIG_CC_OPTIMIZE_FOR_PERFORMANCE=y")
            "GCC 14 -O3 Native" -> sb.appendLine("CONFIG_CC_IS_GCC=y\nCONFIG_CC_OPTIMIZE_FOR_PERFORMANCE_O3=y\nCONFIG_MNATIVE_INTEL=y")
        }
        sb.appendLine()
        sb.appendLine("# Scheduler & Preemption")
        when (profile.scheduler) {
            "EEVDF" -> sb.appendLine("CONFIG_SCHED_CLASS_EXT=y")
            "BORE" -> sb.appendLine("CONFIG_SCHED_BORE=y")
            "MuQSS" -> sb.appendLine("CONFIG_SCHED_MUQSS=y")
        }
        when (profile.preemption) {
            "PREEMPT_DYNAMIC" -> sb.appendLine("CONFIG_PREEMPT_DYNAMIC=y\nCONFIG_PREEMPT=y")
            "PREEMPT_RT" -> sb.appendLine("CONFIG_PREEMPT_RT=y\nCONFIG_EXPERT=y")
            "PREEMPT_VOLUNTARY" -> sb.appendLine("CONFIG_PREEMPT_VOLUNTARY=y")
            "PREEMPT_NONE" -> sb.appendLine("CONFIG_PREEMPT_NONE=y")
        }
        sb.appendLine()
        sb.appendLine("# Root Filesystem Support (${profile.filesystem})")
        when (profile.filesystem) {
            "btrfs" -> sb.appendLine("CONFIG_BTRFS_FS=y\nCONFIG_BTRFS_FS_POSIX_ACL=y")
            "ext4" -> sb.appendLine("CONFIG_EXT4_FS=y\nCONFIG_EXT4_FS_POSIX_ACL=y")
            "zfs" -> sb.appendLine("CONFIG_MODULES=y\n# Out-of-tree OpenZFS DKMS module required")
            "f2fs" -> sb.appendLine("CONFIG_F2FS_FS=y\nCONFIG_F2FS_FS_COMPRESSION=y")
        }
        sb.appendLine()
        sb.appendLine("# Selected Subsystems & Drivers")
        profile.kernelFlags.sorted().forEach { flag ->
            sb.appendLine("$flag=y")
        }
        return sb.toString().trimEnd()
    }

    fun generateDistroBuildScript(profile: LinuxProfile): String {
        val slug = profile.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .ifEmpty { "kernix-os" }
        val ccFlags = if (profile.compiler.startsWith("Clang")) "LLVM=1 LLVM_IAS=1" else ""
        val bootstrapCmd = when (profile.baseDistro) {
            "Arch-Minimal" -> "pacstrap -K /mnt/kernix base linux-firmware ${profile.filesystem}-progs ${profile.terminal.lowercase()}"
            "Debian-Sid" -> "debootstrap --arch=${if (profile.architecture == "x86_64") "amd64" else profile.architecture} sid /mnt/kernix https://deb.debian.org/debian/"
            "Alpine-musl" -> "apk --root /mnt/kernix --initdb add alpine-base ${profile.initSystem.lowercase()}"
            "Void-runit" -> "XBPS_ARCH=${profile.architecture} xbps-install -S -r /mnt/kernix base-system"
            else -> "emerge --root=/mnt/kernix @system"
        }
        val kernelTag = profile.kernelVersion.substringBefore(" ")
        return """
            |#!/usr/bin/env bash
            |set -euo pipefail
            |# Kernix Automated Linux Build Script
            |# Target: ${profile.name} ($slug) | ${profile.baseDistro} (${profile.architecture})
            |
            |BUILD_DIR="/tmp/kernix-build/$slug"
            |mkdir -p "${'$'}BUILD_DIR" && cd "${'$'}BUILD_DIR"
            |
            |echo "[1/4] Fetching Linux Kernel $kernelTag..."
            |curl -LO "https://cdn.kernel.org/pub/linux/kernel/v6.x/linux-${kernelTag.removeSuffix("-rt").removeSuffix("-zen")}.tar.xz"
            |tar -xf linux-*.tar.xz && cd linux-*
            |
            |echo "[2/4] Applying Kernix .config (${profile.scheduler} / ${profile.preemption})..."
            |make $ccFlags defconfig
            |./scripts/config --enable ${profile.preemption}
            |${profile.kernelFlags.joinToString("\n") { "./scripts/config --enable ${it.removePrefix("CONFIG_")}" }}
            |make $ccFlags olddefconfig
            |
            |echo "[3/4] Compiling custom kernel..."
            |make $ccFlags -j"${'$'}(nproc)"
            |
            |echo "[4/4] Bootstrapping ${profile.baseDistro} rootfs (${profile.initSystem}, ${profile.filesystem})..."
            |# $bootstrapCmd
            |echo "Build blueprint complete for ${profile.name}!"
        """.trimMargin()
    }

    fun generateDesktopConfig(profile: LinuxProfile): String {
        val hex = accentHex(profile.accentPreset)
        val term = profile.terminal.lowercase()
        val launcher = profile.launcher.lowercase()
        val bar = profile.statusBar.lowercase()
        val mod = if (profile.keybindStyle == "macOS-inspired") "ALT" else "SUPER"
        val gaps = profile.windowGaps
        val border = profile.borderWidth
        val radius = profile.cornerRadius
        val blur = profile.blurStrength
        val opacity = String.format(java.util.Locale.US, "%.2f", profile.windowOpacity)
        val animEnabled = profile.animationStyle != "Instant"

        return when (profile.desktopEnv) {
            "Hyprland", "Niri" -> """
                |# Kernix ${profile.desktopEnv} Configuration
                |# Accent: ${profile.accentPreset} (#$hex) | Bar: ${profile.statusBar} (${profile.barPosition})
                |
                |${'$'}terminal = $term
                |${'$'}menu = $launcher --show drun
                |${'$'}mainMod = $mod
                |
                |exec-once = $bar &
                |
                |general {
                |    gaps_in = ${gaps / 2}
                |    gaps_out = $gaps
                |    border_size = $border
                |    col.active_border = rgb($hex)
                |    col.inactive_border = rgba(2d3952aa)
                |    layout = dwindle
                |}
                |
                |decoration {
                |    rounding = $radius
                |    active_opacity = 1.0
                |    inactive_opacity = $opacity
                |    blur {
                |        enabled = ${blur > 0}
                |        size = ${blur.coerceAtLeast(1)}
                |        passes = 2
                |    }
                |}
                |
                |animations {
                |    enabled = $animEnabled
                |    bezier = kernixCurve, ${if (profile.animationStyle == "Snappy Spring") "0.05, 0.9, 0.1, 1.05" else "0.25, 1.0, 0.5, 1.0"}
                |    animation = windows, 1, 5, kernixCurve
                |}
                |
                |bind = ${'$'}mainMod, Return, exec, ${'$'}terminal
                |bind = ${'$'}mainMod, Space, exec, ${'$'}menu
                |bind = ${'$'}mainMod, Q, killactive
            """.trimMargin()

            "Sway" -> """
                |# Kernix Sway (Wayland) Configuration
                |set ${'$'}mod Mod4
                |set ${'$'}term $term
                |set ${'$'}menu $launcher --show drun
                |
                |gaps inner ${gaps / 2}
                |gaps outer $gaps
                |default_border pixel $border
                |client.focused #$hex #$hex #0c1019 #$hex
                |
                |bar {
                |    swaybar_command $bar
                |    position ${profile.barPosition.lowercase()}
                |}
                |
                |bindsym ${'$'}mod+Return exec ${'$'}term
                |bindsym ${'$'}mod+d exec ${'$'}menu
                |bindsym ${'$'}mod+Shift+q kill
            """.trimMargin()

            else -> """
                |#!/usr/bin/env bash
                |# Kernix ${profile.desktopEnv} Setup Script
                |# Bar: ${profile.statusBar} (${profile.barPosition}) | Terminal: ${profile.terminal}
                |
                |echo "Configuring ${profile.desktopEnv} with ${profile.accentPreset} (#$hex)..."
                |# Window rounding: ${radius}px | Gaps: ${gaps}px | Opacity: $opacity
                |# Keybinds: ${profile.keybindStyle} | Launcher: ${profile.launcher}
                |mkdir -p ~/.config/kernix-de
                |cat << 'EOF' > ~/.config/kernix-de/theme.env
                |KERNIX_DE="${profile.desktopEnv}"
                |KERNIX_ACCENT="#$hex"
                |KERNIX_TERMINAL="$term"
                |KERNIX_LAUNCHER="$launcher"
                |KERNIX_BAR="$bar"
                |KERNIX_BAR_POS="${profile.barPosition.lowercase()}"
                |EOF
                |echo "Applied ${profile.desktopEnv} profile!"
            """.trimMargin()
        }
    }
}
