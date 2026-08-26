package com.example.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * 🎨 ZENITH STUDIO - PREMIUM MULTI-THEME ENGINE
 * 
 * Defines multiple professional aesthetic modes for Zenith Studio:
 * 1. Zenith Obsidian (Default) - Dark, technical workspace with high contrast Amber.
 * 2. Cyberpunk Neon - Dark violet neon-glowing cyber workspace.
 * 3. Nordic Sage - Calming, low-fatigue slate-teal organic workspace.
 * 4. Solarized Sand - Warm light mode cream design workspace.
 * 5. Obsidian Crimson - Intense dark carbon charcoal with scarlet accents.
 */
object CustomThemeState {
    val primary = mutableStateOf(Color(0xFFF59E0B))
    val secondary = mutableStateOf(Color(0xFFFBBF24))
    val tertiary = mutableStateOf(Color(0xFF3B82F6))
    val background = mutableStateOf(Color(0xFF08090C))
    val surface = mutableStateOf(Color(0xFF10121A))
    val surfaceVariant = mutableStateOf(Color(0xFF1B1E29))
    val outline = mutableStateOf(Color(0xFF232736))
    val onBackground = mutableStateOf(Color(0xFFFFFFFF))
    val onSurface = mutableStateOf(Color(0xFFF9FAFB))
    val onSurfaceVariant = mutableStateOf(Color(0xFF9CA3AF))
    val isDark = mutableStateOf(true)
    val isBrutalist = mutableStateOf(false)
    val isAdvanced = mutableStateOf(true)
    val fontStyle = mutableStateOf(FontFamily.SansSerif)
    val cornerRadius = mutableStateOf(12)
    val borderWidth = mutableStateOf(1)
    val iconStyle = mutableStateOf("standard")
    val panelSpacing = mutableStateOf(16)
}

enum class StudioTheme(
    val id: String,
    val displayName: String,
    val description: String,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    outline: Color,
    onBackground: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    isDark: Boolean = true,
    isBrutalist: Boolean = false,
    isAdvanced: Boolean = false,
    fontStyle: FontFamily = FontFamily.SansSerif,
    cornerRadius: Int = 12, // in dp
    borderWidth: Int = 1, // in dp
    iconStyle: String = "standard", // "standard", "sharp", "rounded"
    panelSpacing: Int = 16 // in dp
) {
    ZENITH_ELECTRIC(
        id = "zenith_electric",
        displayName = "Zenith Electric Black",
        description = "Vibrant electric blue highlight with a deep black background",
        primary = Color(0xFF6F01F6),
        secondary = Color(0xFF8A30FF),
        tertiary = Color(0xFF3B82F6),
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0F),
        surfaceVariant = Color(0xFF13131F),
        outline = Color(0xFF2E1C5E),
        onBackground = Color.White,
        onSurface = Color(0xFFFAFAFA),
        onSurfaceVariant = Color(0xFFD4C8FF)
    ),
    ZENITH_DARK(
        id = "zenith_dark",
        displayName = "Zenith Obsidian",
        description = "Industrial Obsidian with Warm Cyber-Amber highlights",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFBBF24),
        tertiary = Color(0xFF3B82F6),
        background = Color(0xFF08090C),
        surface = Color(0xFF10121A),
        surfaceVariant = Color(0xFF1B1E29),
        outline = Color(0xFF232736),
        onBackground = Color.White,
        onSurface = Color(0xFFF9FAFB),
        onSurfaceVariant = Color(0xFF9CA3AF)
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk Neon",
        description = "True Black canvas with hot magenta and cyan accents",
        primary = Color(0xFFEC4899),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFF8B5CF6),
        background = Color(0xFF03010A),
        surface = Color(0xFF0E0B1A),
        surfaceVariant = Color(0xFF1B142F),
        outline = Color(0xFF321E53),
        onBackground = Color.White,
        onSurface = Color(0xFFFAFAFA),
        onSurfaceVariant = Color(0xFFA78BFA)
    ),
    NORDIC_FOREST(
        id = "nordic_forest",
        displayName = "Nordic Sage",
        description = "Deep forest teal-slate with organic emerald highlights",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        tertiary = Color(0xFF0EA5E9),
        background = Color(0xFF0C0F12),
        surface = Color(0xFF12181C),
        surfaceVariant = Color(0xFF1A2329),
        outline = Color(0xFF243038),
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF94A3B8)
    ),
    SOLARIZED_LIGHT(
        id = "solarized_light",
        displayName = "Solarized Sand",
        description = "Warm sand-cream canvas with elegant deep indigo & terracotta",
        primary = Color(0xFFEA580C),
        secondary = Color(0xFFD97706),
        tertiary = Color(0xFF4F46E5),
        background = Color(0xFFFAF8F5),
        surface = Color(0xFFF3EFE6),
        surfaceVariant = Color(0xFFEBE5D5),
        outline = Color(0xFFD6CDB8),
        onBackground = Color(0xFF1E1B4B),
        onSurface = Color(0xFF312E81),
        onSurfaceVariant = Color(0xFF6B7280),
        isDark = false
    ),
    OBSIDIAN_CRIMSON(
        id = "obsidian_crimson",
        displayName = "Obsidian Crimson",
        description = "Sleek carbon charcoal with premium scarlet & crimson accents",
        primary = Color(0xFFDC2626),
        secondary = Color(0xFFF87171),
        tertiary = Color(0xFF6B7280),
        background = Color(0xFF0A0A0C),
        surface = Color(0xFF121217),
        surfaceVariant = Color(0xFF1A1A22),
        outline = Color(0xFF2B2B36),
        onBackground = Color.White,
        onSurface = Color(0xFFF9FAFB),
        onSurfaceVariant = Color(0xFF9CA3AF)
    ),
    SYNTHWAVE_SUNSET(
        id = "synthwave_sunset",
        displayName = "Synthwave Sunset",
        description = "Chroma retro-wave twilight canvas with neon sunset orange & violet",
        primary = Color(0xFFF97316),
        secondary = Color(0xFFD946EF),
        tertiary = Color(0xFF8B5CF6),
        background = Color(0xFF0F071B),
        surface = Color(0xFF1D0E34),
        surfaceVariant = Color(0xFF2B164C),
        outline = Color(0xFF4A1D73),
        onBackground = Color(0xFFFDF4FF),
        onSurface = Color(0xFFFAE8FF),
        onSurfaceVariant = Color(0xFFD8B4FE)
    ),
    RETRO_TERMINAL(
        id = "retro_terminal",
        displayName = "Retro Terminal",
        description = "Phosphor green cyberpunk CRT terminal styling",
        primary = Color(0xFF22C55E),
        secondary = Color(0xFF4ADE80),
        tertiary = Color(0xFF15803D),
        background = Color(0xFF020604),
        surface = Color(0xFF05170B),
        surfaceVariant = Color(0xFF092914),
        outline = Color(0xFF166534),
        onBackground = Color(0xFFDCFCE7),
        onSurface = Color(0xFFF0FDF4),
        onSurfaceVariant = Color(0xFF86EFAC)
    ),
    DEEP_SPACE(
        id = "deep_space",
        displayName = "Deep Space Blue",
        description = "Deep celestial navy background with starry cyan & sky blue highlights",
        primary = Color(0xFF0EA5E9),
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFF6366F1),
        background = Color(0xFF030712),
        surface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFF1E293B),
        outline = Color(0xFF334155),
        onBackground = Color(0xFFF0F9FF),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF38BDF8)
    ),
    ARCTIC_GLACIER(
        id = "arctic_glacier",
        displayName = "Arctic Glacier",
        description = "Crisp glacial light canvas with icy-blue highlights",
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFF0F766E),
        background = Color(0xFFF0F9FF),
        surface = Color(0xFFE0F2FE),
        surfaceVariant = Color(0xFFBAE6FD),
        outline = Color(0xFF7DD3FC),
        onBackground = Color(0xFF0369A1),
        onSurface = Color(0xFF075985),
        onSurfaceVariant = Color(0xFF0284C7),
        isDark = false
    ),
    MONOCHROME_ACTIVE(
        id = "monochrome_active",
        displayName = "Monochrome Active",
        description = "Sleek After-Motion style monochrome dark canvas with soft muted silver highlights for reduced eye fatigue",
        primary = Color(0xFFD4D4D8),
        secondary = Color(0xFFA1A1AA),
        tertiary = Color(0xFF00E676),
        background = Color(0xFF0E0E12),
        surface = Color(0xFF16161C),
        surfaceVariant = Color(0xFF202028),
        outline = Color(0xFF2E2E3A),
        onBackground = Color(0xFFE4E4E7),
        onSurface = Color(0xFFD4D4D8),
        onSurfaceVariant = Color(0xFF94A3B8)
    ),
    
    // --- ADVANCED RESHAPING THEMES (CRITICAL RE-LAYOUT AND EXPERIMENTAL GRAPHICS) ---
    LIFE_ENGINE(
        id = "life_engine",
        displayName = "Life Engine Retro",
        description = "Solid thick absolute-black borders, sharp edges, Monospace code fonts, flat 5dp solid offset shadows.",
        primary = Color(0xFFFBBF24), // Saturated Golden Yellow
        secondary = Color(0xFF2563EB), // Life Engine Blue
        tertiary = Color(0xFFEF4444), // Retro Crimson Coral
        background = Color(0xFFFAF9F6), // Pure soft organic ivory
        surface = Color(0xFFFFFFFF), // Bold high-contrast white panel
        surfaceVariant = Color(0xFFF3F4F6), // Clean Light Grey separator
        outline = Color(0xFF000000), // Thick absolute black border outline
        onBackground = Color(0xFF000000), // Sharp pitch-black typography
        onSurface = Color(0xFF000000), // Sharp pitch-black typography
        onSurfaceVariant = Color(0xFF374151), // Slate grey captions
        isDark = false,
        isBrutalist = true,
        isAdvanced = true,
        fontStyle = FontFamily.Monospace,
        cornerRadius = 0,
        borderWidth = 3,
        iconStyle = "sharp",
        panelSpacing = 20
    ),
    CYBER_BRUTALIST(
        id = "cyber_brutalist",
        displayName = "Cyber Brutalist Space",
        description = "True Black canvas with hot magenta accents, stark white lines, Monospace terminal text, sharp corners.",
        primary = Color(0xFFEC4899), // Hot Magenta pink
        secondary = Color(0xFF10B981), // Emerald Green accent
        tertiary = Color(0xFF06B6D4), // Cyan active border
        background = Color(0xFF000000), // Pitch dark abyss
        surface = Color(0xFF0F0E17), // Heavy carbon space container
        surfaceVariant = Color(0xFF1A1926), // Charcoal card panel
        outline = Color(0xFFFFFFFF), // Brilliant stark white border line
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFA7F3D0),
        isDark = true,
        isBrutalist = true,
        isAdvanced = true,
        fontStyle = FontFamily.Monospace,
        cornerRadius = 0,
        borderWidth = 3,
        iconStyle = "sharp",
        panelSpacing = 20
    ),
    RENAISSANCE_ROOM(
        id = "renaissance_room",
        displayName = "Renaissance Editorial",
        description = "Warm charcoal/terracotta slate with an elegant Serif book typography layout and soft spacing rules.",
        primary = Color(0xFFE07A5F), // terracotta
        secondary = Color(0xFFF4F1DE), // warm cream
        tertiary = Color(0xFF81B29A), // sage green
        background = Color(0xFF151515), // elegant deep room
        surface = Color(0xFF1E1D1C), // luxury warm charcoal panel
        surfaceVariant = Color(0xFF282725),
        outline = Color(0xFF3E3C3A), // gold-tinted charcoal line
        onBackground = Color(0xFFF4F1DE),
        onSurface = Color(0xFFF4F1DE),
        onSurfaceVariant = Color(0xFFD4CFC2),
        isDark = true,
        isBrutalist = false,
        isAdvanced = true,
        fontStyle = FontFamily.Serif,
        cornerRadius = 4,
        borderWidth = 1,
        iconStyle = "standard",
        panelSpacing = 18
    ),
    KINETIC_LIQUID(
        id = "kinetic_liquid",
        displayName = "Kinetic Pill-Organic",
        description = "Hyper-modern design, ultra rounded pill corners (24dp), high-fidelity liquid green & indigo.",
        primary = Color(0xFF10B981), // Liquid Emerald
        secondary = Color(0xFF6366F1), // Indigo
        tertiary = Color(0xFFF43F5E), // Neon Rose
        background = Color(0xFF080D1A), // Marine space
        surface = Color(0xFF101931), // Pill card panels
        surfaceVariant = Color(0xFF1B264B),
        outline = Color(0xFF2A3B6E),
        onBackground = Color(0xFFE2E8F0),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF94A3B8),
        isDark = true,
        isBrutalist = false,
        isAdvanced = true,
        fontStyle = FontFamily.SansSerif,
        cornerRadius = 24,
        borderWidth = 1,
        iconStyle = "rounded",
        panelSpacing = 12
    ),
    MATRIX_CARBON(
        id = "matrix_carbon",
        displayName = "Minimal Matrix Carbon",
        description = "Clean monochrome slate. Softened borders, monospace character spacings, minimalist micro-panels.",
        primary = Color(0xFFD4D4D8), // soft muted silver
        secondary = Color(0xFF9CA3AF),
        tertiary = Color(0xFF71717A),
        background = Color(0xFF0E0E12),
        surface = Color(0xFF18181D),
        surfaceVariant = Color(0xFF24242A),
        outline = Color(0xFF3B3B44),
        onBackground = Color(0xFFE4E4E7),
        onSurface = Color(0xFFD4D4D8),
        onSurfaceVariant = Color(0xFF8E8E93),
        isDark = true,
        isBrutalist = false,
        isAdvanced = true,
        fontStyle = FontFamily.Monospace,
        cornerRadius = 8,
        borderWidth = 1,
        iconStyle = "sharp",
        panelSpacing = 14
    ),
    CUSTOM(
        id = "custom_designer",
        displayName = "Custom Theme Designer",
        description = "Your personal custom-crafted color, typography and structural layout.",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFBBF24),
        tertiary = Color(0xFF3B82F6),
        background = Color(0xFF08090C),
        surface = Color(0xFF10121A),
        surfaceVariant = Color(0xFF1B1E29),
        outline = Color(0xFF232736),
        onBackground = Color.White,
        onSurface = Color(0xFFF9FAFB),
        onSurfaceVariant = Color(0xFF9CA3AF),
        isDark = true,
        isBrutalist = false,
        isAdvanced = true,
        fontStyle = FontFamily.SansSerif,
        cornerRadius = 12,
        borderWidth = 1,
        iconStyle = "standard",
        panelSpacing = 16
    );

    val primary: Color = primary
        get() = if (this == CUSTOM) CustomThemeState.primary.value else field

    val secondary: Color = secondary
        get() = if (this == CUSTOM) CustomThemeState.secondary.value else field

    val tertiary: Color = tertiary
        get() = if (this == CUSTOM) CustomThemeState.tertiary.value else field

    val background: Color = background
        get() = if (this == CUSTOM) CustomThemeState.background.value else field

    val surface: Color = surface
        get() = if (this == CUSTOM) CustomThemeState.surface.value else field

    val surfaceVariant: Color = surfaceVariant
        get() = if (this == CUSTOM) CustomThemeState.surfaceVariant.value else field

    val outline: Color = outline
        get() = if (this == CUSTOM) CustomThemeState.outline.value else field

    val onBackground: Color = onBackground
        get() = if (this == CUSTOM) CustomThemeState.onBackground.value else field

    val onSurface: Color = onSurface
        get() = if (this == CUSTOM) CustomThemeState.onSurface.value else field

    val onSurfaceVariant: Color = onSurfaceVariant
        get() = if (this == CUSTOM) CustomThemeState.onSurfaceVariant.value else field

    val isDark: Boolean = isDark
        get() = if (this == CUSTOM) CustomThemeState.isDark.value else field

    val isBrutalist: Boolean = isBrutalist
        get() = if (this == CUSTOM) CustomThemeState.isBrutalist.value else field

    val isAdvanced: Boolean = isAdvanced
        get() = if (this == CUSTOM) CustomThemeState.isAdvanced.value else field

    val fontStyle: FontFamily = fontStyle
        get() = if (this == CUSTOM) CustomThemeState.fontStyle.value else field

    val cornerRadius: Int = cornerRadius
        get() = if (this == CUSTOM) CustomThemeState.cornerRadius.value else field

    val borderWidth: Int = borderWidth
        get() = if (this == CUSTOM) CustomThemeState.borderWidth.value else field

    val iconStyle: String = iconStyle
        get() = if (this == CUSTOM) CustomThemeState.iconStyle.value else field

    val panelSpacing: Int = panelSpacing
        get() = if (this == CUSTOM) CustomThemeState.panelSpacing.value else field
}

// Dynamic Theme State Wrapper
var currentThemeStateBySelection = mutableStateOf(StudioTheme.MONOCHROME_ACTIVE)

// Low-End Device Performance Optimization Mode State
var isLowEndOptimizationEnabled = mutableStateOf(true)

// Master Background Canvas (60%)
val DarkOnyx: Color get() = currentThemeStateBySelection.value.background

// Interactive Controllers & Panels Backdrop (30%)
val SlatePanel: Color get() = currentThemeStateBySelection.value.surface
val MidSlate: Color get() = currentThemeStateBySelection.value.surfaceVariant

// Highlights & Controls (10%)
val IndustrialAmber: Color get() = currentThemeStateBySelection.value.primary
val EnergeticYellow: Color get() = currentThemeStateBySelection.value.secondary
val MatteBlue: Color get() = currentThemeStateBySelection.value.tertiary
val AdjustmentNodeColor: Color get() = currentThemeStateBySelection.value.tertiary
val GreenActive: Color get() = if (currentThemeStateBySelection.value == StudioTheme.MONOCHROME_ACTIVE) Color(0xFF00E676) else currentThemeStateBySelection.value.tertiary

// Focus borders & outlines
val HighslateOutline: Color get() = currentThemeStateBySelection.value.outline

// Typography Contrast
val TextPrimary: Color get() = currentThemeStateBySelection.value.onSurface
val TextSecondary: Color get() = currentThemeStateBySelection.value.onSurfaceVariant

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
