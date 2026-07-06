package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 🎨 ZENITH STUDIO - PREMIUM OBSIDIAN & CYBER-AMBER STUDIO PALETTE
 * 
 * Reverting to an elite, modern, eye-safe creative dark room layout matching
 * our original styling references:
 * 
 * - Background Canvas: Midnight Obsidian (#12141A) reducing eye fatigue.
 * - Tool Panels & Sidebars: Dark Tech Slate (#1E2129 / #262A35) for clear structural layout bounds.
 * - Active Accent States: Industrial Cyber-Amber (#FFB300) and Energetic Gold.
 */

// Master Background Canvas (60%)
val DarkOnyx = Color(0xFF12141A)        // Deep Midnight Obsidian Black

// Interactive Controllers & Panels Backdrop (30%)
val SlatePanel = Color(0xFF161F32)      // Premium Carbon Slate Panel Background (Dark Steel Navy)
val MidSlate = Color(0xFF262A35)        // Midnight Charcoal Blue Satin

// Highlights & Controls (10%)
val IndustrialAmber = Color(0xFFFFB300) // Rich Industrial Gold/Amber for prime highlights
val EnergeticYellow = Color(0xFFFFD54F) // Vibrant Yellow for auxiliary active highlights & sliders
val MatteBlue = Color(0xFF2979FF)       // Cobalt Blue selection layer boundaries
val AdjustmentNodeColor = Color(0xFFA855F7) // Vibrant Neon Purple for state-dependent layout indicators
val GreenActive = Color(0xFF00E676)     // Emerald Action approved indicators

// Focus borders & outlines
val HighslateOutline = Color(0xFF383F51)// Modern Slate border stroke line

// Typography Contrast
val TextPrimary = Color(0xFFF3F4F6)     // Clean Matte Pure White
val TextSecondary = Color(0xFF9CA3AF)   // Sleek Carbon Slate gray for auxiliary text labels

/**
 * 🔄 Fallback styling pack for future alternative theme configurations
 */
object AlternativeStyleGuidePack {
    val AppShellBackground = Color(0xFF0C0D0E)
    val FloatingDrawersBackground = Color(0xFF21252B)
    val InteractiveHighlights = Color(0xFFE5A93C)
    
    val DarkOnyx = AppShellBackground
    val SlatePanel = FloatingDrawersBackground
    val MidSlate = FloatingDrawersBackground
    val HighslateOutline = Color(0xFF3B4048)
    val IndustrialAmber = InteractiveHighlights
    val EnergeticYellow = Color(0xFFF0C674)
    val MatteBlue = Color(0xFF81A1C1)
    val AdjustmentNodeColor = Color(0xFFA855F7)
    val GreenActive = Color(0xFFA3BE8C)
    
    val TextPrimary = Color(0xFFECEFF4)
    val TextSecondary = Color(0xFFD8DEE9)
}
