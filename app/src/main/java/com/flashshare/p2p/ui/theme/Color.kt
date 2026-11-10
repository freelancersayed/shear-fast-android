package com.flashshare.p2p.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// BondhuShare Harmonious Design System
// Deep Midnight Navy + Mint Emerald + Electric Azure
// ==========================================

// 1. Canvas & Surface Hierarchy
val AppBackground = Color(0xFF0A111F)       // Deep Midnight Navy canvas (consistent everywhere)
val BackgroundDark = Color(0xFF0A111F)      // Backward-compatible alias
val SurfaceElevated = Color(0xFF132035)     // Card & container surface
val SurfaceDark = Color(0xFF132035)         // Backward-compatible alias
val CardBackground = Color(0xFF132035)      // Backward-compatible alias
val CardBorder = Color(0xFF1F3354)          // Subtle structural border
val CardBorderHighlight = Color(0xFF2E4B7A) // Highlighted border on active state

// 2. Action & Brand Accents
val AccentMint = Color(0xFF00D29E)          // Vibrant Mint Emerald (Primary Action: Send)
val AccentMintText = Color(0xFF03241C)      // Ultra-high contrast dark text for Mint button
val AccentAzure = Color(0xFF2563EB)         // Deep Electric Azure (Secondary Action: Receive)
val AccentAzureLight = Color(0xFF38BDF8)    // Sky Azure for active states & highlights

// 3. Functional Status Colors
val SuccessEmerald = Color(0xFF10B981)
val SuccessSubtle = Color(0x2610B981)
val ErrorRose = Color(0xFFF43F5E)
val ErrorSubtle = Color(0x26F43F5E)
val WarningAmber = Color(0xFFF59E0B)

// 4. Clean Typography Palette
val TextTitle = Color(0xFFF8FAFC)           // Pure crisp white
val TextBody = Color(0xFF94A3B8)            // Soft slate silver
val TextMuted = Color(0xFF64748B)           // Subdued steel grey
val TextSubtle = Color(0xFF475569)          // Low-priority hint text

// 5. Navigation Bar Palette
val BottomBarBackground = Color(0xFF0D1627) // Elevated dark frosted dock
val BottomBarBorder = Color(0xFF1B2B45)     // Top border line
val BottomBarIconActive = Color(0xFF00D29E) // Radiant Mint for active tab
val BottomBarIconInactive = Color(0xFF64748B) // Subdued silver for inactive tab

// 6. Legacy / Compatibility Aliases
val CorporateBlue = Color(0xFF0A111F)       // Harmonized with AppBackground
val BrandBlue = Color(0xFF2563EB)
val BrandBlueHover = Color(0xFF1D4ED8)
val BrandBlueLight = Color(0xFF38BDF8)
val BrandBlueSubtle = Color(0x262563EB)
val ButtonAquaGreen = Color(0xFF00D29E)
val ButtonSlateGrey = Color(0xFF2563EB)
val BottomBarMutedTeal = Color(0xFF0D1627)
