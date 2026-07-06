package com.example.studio.rendering

import android.graphics.ColorMatrix

/**
 * Solid, production-grade representational nodes for stacked non-destructive effects.
 * Supports GPU-first pathways (AGSL / RenderEffect) and highly optimized CPU fallbacks.
 */
sealed class ZenEffectNode {
    abstract val id: String

    data class Blur(
        override val id: String = "blur",
        val radius: Float
    ) : ZenEffectNode()

    data class ColorMatrixEffect(
        override val id: String = "color_matrix",
        val matrix: ColorMatrix
    ) : ZenEffectNode()

    data class Warp(
        override val id: String = "warp",
        val strength: Float,
        val centerX: Float,
        val centerY: Float
    ) : ZenEffectNode()

    data class Emboss(
        override val id: String = "emboss",
        val strength: Float
    ) : ZenEffectNode()

    data class Grain(
        override val id: String = "grain",
        val intensity: Float,
        val seed: Float
    ) : ZenEffectNode()

    data class Ripple(
        override val id: String = "ripple",
        val amplitude: Float,
        val frequency: Float,
        val phase: Float,
        val centerX: Float,
        val centerY: Float
    ) : ZenEffectNode()

    data class GlowAndShadowSDF(
        override val id: String = "glow_shadow_sdf",
        val shadowRadius: Float,
        val shadowOffsetX: Float,
        val shadowOffsetY: Float,
        val shadowColor: Int,
        val glowSize: Float,
        val glowColor: Int
    ) : ZenEffectNode()
}

/**
 * Thread-safe Effect Graph container tracking active non-destructive rendering pipelines.
 */
class EffectGraph {
    private val effects = mutableListOf<ZenEffectNode>()

    @Synchronized
    fun add(effect: ZenEffectNode) {
        // Replace existing by ID or add to stack
        effects.removeAll { it.id == effect.id }
        effects.add(effect)
    }

    @Synchronized
    fun remove(id: String) {
        effects.removeAll { it.id == id }
    }

    @Synchronized
    fun clear() {
        effects.clear()
    }

    @Synchronized
    fun getAll(): List<ZenEffectNode> = effects.toList()
}
