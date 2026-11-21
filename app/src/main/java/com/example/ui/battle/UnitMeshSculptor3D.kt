package com.example.ui.battle

import com.example.model.UnitArchetype

/**
 * High-detail procedural 3D miniature sculptor for Warhammer 40K units.
 * Coordinate convention: +X = Forward, +Y = Up, +Z = Right, -Z = Left.
 * Uploaded once per archetype to static GPU VBOs for locked 60 FPS rendering.
 */
object UnitMeshSculptor3D {

    private const val E = MeshCatalog3D.EMISSIVE_ALPHA

    // Primaris Ultramarine Reference Palette (Macragge Blue, Imperial Gold, Black Iron, Crimson Wax, Parchment)
    private const val BLUE_R = 0.11f; private const val BLUE_G = 0.28f; private const val BLUE_B = 0.67f
    private const val BLUE_HI_R = 0.15f; private const val BLUE_HI_G = 0.36f; private const val BLUE_HI_B = 0.78f
    private const val BLUE_DK_R = 0.07f; private const val BLUE_DK_G = 0.18f; private const val BLUE_DK_B = 0.45f

    private const val GOLD_R = 0.85f; private const val GOLD_G = 0.65f; private const val GOLD_B = 0.25f
    private const val GOLD_DK_R = 0.64f; private const val GOLD_DK_G = 0.46f; private const val GOLD_DK_B = 0.16f

    private const val IRON_R = 0.09f; private const val IRON_G = 0.10f; private const val IRON_B = 0.13f
    private const val STEEL_R = 0.52f; private const val STEEL_G = 0.56f; private const val STEEL_B = 0.62f

    private const val WHITE_R = 0.92f; private const val WHITE_G = 0.93f; private const val WHITE_B = 0.96f
    private const val WAX_R = 0.72f; private const val WAX_G = 0.09f; private const val WAX_B = 0.11f
    private const val PARCH_R = 0.84f; private const val PARCH_G = 0.75f; private const val PARCH_B = 0.56f
    private const val CAPE_R = 0.56f; private const val CAPE_G = 0.09f; private const val CAPE_B = 0.12f

    fun build(archetype: UnitArchetype, variant: String = ""): MeshGeometry {
        val m = MeshAccumulator()
        when (archetype) {
            UnitArchetype.DREADNOUGHT -> buildUltramarineDreadnought(m)
            UnitArchetype.TERMINATOR -> buildAssaultTerminator(m, useLightningClaws = variant == "CLAWS")
            UnitArchetype.INCURSOR -> buildUltramarineIncursor(m)
            UnitArchetype.INTERCESSOR,
            UnitArchetype.HELLBLASTER,
            UnitArchetype.ASSAULT_MARINE,
            UnitArchetype.CAPTAIN -> buildPrimarisUltramarine(m, archetype)

            UnitArchetype.TERMAGANT -> buildTermagant(m)
            UnitArchetype.HORMAGAUNT -> buildHormagaunt(m)
            UnitArchetype.WARRIOR -> buildTyranidWarrior(m)
            UnitArchetype.VON_RYANS_LEAPER -> buildVonRyansLeaper(m)
            UnitArchetype.GENESTEALER -> buildGenestealer(m)
            UnitArchetype.CARNIFEX -> buildCarnifex(m)
            UnitArchetype.HIVE_TYRANT -> buildHiveTyrant(m)
        }
        return m.build()
    }

    private fun buildAssaultTerminator(m: MeshAccumulator, useLightningClaws: Boolean = false) {
        val s = 1.18f
        val helmW_R = 0.90f; val helmW_G = 0.91f; val helmW_B = 0.94f
        val shieldFieldR = 0.88f; val shieldFieldG = 0.85f; val shieldFieldB = 0.78f
        val hammerGrnR = 0.16f; val hammerGrnG = 0.43f; val hammerGrnB = 0.29f
        val hammerGrnDkR = 0.10f; val hammerGrnDkG = 0.28f; val hammerGrnDkB = 0.19f

        // 1. Dark Ash-Grey Rocky Tabletop Base Plinth + Skull/Rock Detail (as in photo)
        m.addCylinder(0f, 0.002f, 0f, 0.22f, 0.205f, 0.024f, 14, 0.11f, 0.11f, 0.12f)
        m.addCylinder(0f, 0.024f, 0f, 0.195f, 0.190f, 0.010f, 14, 0.27f, 0.28f, 0.30f)
        m.addOrientedEllipsoid(0.09f, 0.036f, 0.07f, 0.026f, 0.022f, 0.024f, latSegs = 3, lonSegs = 6, r = 0.72f, g = 0.68f, b = 0.60f)
        m.addOrientedBox(-0.08f, 0.035f, -0.06f, 0.055f, 0.022f, 0.050f, rotYDeg = 25f, r = 0.21f, g = 0.23f, b = 0.20f)
        val by = 0.034f

        // 2. Heroic Wide Indomitus Legs (Cylindrical Blue Greaves, Steel Ankle Struts, White Ω, Crux Kneepads)
        for (side in floatArrayOf(-1f, 1f)) {
            val isLeft = side < 0f
            val footX = if (isLeft) 0.042f * s else -0.025f * s
            val footZ = side * 0.106f * s
            val yawDeg = if (isLeft) -12f else 12f

            // Wide Sabaton Boot
            m.addOrientedBox(footX, by + 0.018f * s, footZ, 0.140f * s, 0.036f * s, 0.096f * s, rotYDeg = yawDeg, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedEllipsoid(footX + 0.018f * s, by + 0.030f * s, footZ, 0.078f * s, 0.046f * s, 0.056f * s, rotYDeg = yawDeg, phiEndDeg = 90f, latSegs = 4, lonSegs = 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)

            // Thick Flared Indomitus Greave
            val kneeX = footX * 0.55f + 0.012f * s
            val kneeY = by + 0.205f * s
            val kneeZ = footZ * 0.90f
            m.addOrientedCylinder(footX, by + 0.044f * s, footZ, kneeX, kneeY, kneeZ, 0.072f * s, 0.058f * s, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            // Outer Steel Hydraulic Piston Strut
            m.addOrientedCylinder(footX - 0.01f * s, by + 0.05f * s, footZ + side * 0.064f * s, kneeX - 0.01f * s, kneeY - 0.02f * s, kneeZ + side * 0.056f * s, 0.012f * s, 0.010f * s, 6, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            // White Omega (Ω) on lower shin front (matching photo!)
            m.addArcTube(footX + 0.056f * s, by + 0.115f * s, footZ, 0.018f * s, 0.018f * s, 0.0045f * s, rotXDeg = 90f, rotYDeg = 90f + yawDeg, startDeg = 145f, endDeg = 395f, arcSegs = 8, tubeSegs = 4, r = WHITE_R, g = WHITE_G, b = WHITE_B)

            // Heavy Dark-Trimmed Kneecap + White/Silver Skull-Cross Crux Badge
            m.addOrientedEllipsoid(kneeX + 0.040f * s, kneeY, kneeZ, 0.038f * s, 0.046f * s, 0.046f * s, rotYDeg = yawDeg, latSegs = 4, lonSegs = 8, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedEllipsoid(kneeX + 0.046f * s, kneeY, kneeZ, 0.032f * s, 0.038f * s, 0.038f * s, rotYDeg = yawDeg, latSegs = 4, lonSegs = 8, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
            m.addOrientedBox(kneeX + 0.072f * s, kneeY, kneeZ, 0.014f * s, 0.034f * s, 0.012f * s, rotYDeg = yawDeg, r = STEEL_R * 1.35f, g = STEEL_G * 1.35f, b = STEEL_B * 1.35f)
            m.addOrientedBox(kneeX + 0.072f * s, kneeY, kneeZ, 0.014f * s, 0.012f * s, 0.034f * s, rotYDeg = yawDeg, r = STEEL_R * 1.35f, g = STEEL_G * 1.35f, b = STEEL_B * 1.35f)
            m.addOrientedEllipsoid(kneeX + 0.078f * s, kneeY, kneeZ, 0.012f * s, 0.014f * s, 0.012f * s, latSegs = 3, lonSegs = 6, r = helmW_R, g = helmW_G, b = helmW_B)

            // Thigh Armor
            m.addOrientedCylinder(kneeX, kneeY + 0.01f * s, kneeZ, 0f, by + 0.32f * s, side * 0.076f * s, 0.056f * s, 0.064f * s, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        }

        // 3. Pelvis, Groin Plate, Ribbed Waist & Blue Hip Tassets
        m.addOrientedEllipsoid(0f, by + 0.315f * s, 0f, 0.088f * s, 0.062f * s, 0.112f * s, latSegs = 4, lonSegs = 8, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(0.082f * s, by + 0.295f * s, 0f, 0.036f * s, 0.092f * s, 0.072f * s, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        m.addOrientedEllipsoid(0.102f * s, by + 0.305f * s, 0f, 0.014f * s, 0.016f * s, 0.014f * s, latSegs = 3, lonSegs = 6, r = helmW_R, g = helmW_G, b = helmW_B)
        m.addOrientedCylinder(0f, by + 0.325f * s, 0f, 0f, by + 0.42f * s, 0f, 0.084f * s, 0.098f * s, 10, r = IRON_R, g = IRON_G, b = IRON_B)
        // Angled Blue Hip Tassets
        m.addOrientedBox(0.02f * s, by + 0.335f * s, -0.108f * s, 0.095f * s, 0.095f * s, 0.030f * s, rotXDeg = -14f, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        m.addOrientedBox(0.02f * s, by + 0.335f * s, 0.108f * s, 0.095f * s, 0.095f * s, 0.030f * s, rotXDeg = 14f, r = BLUE_R, g = BLUE_G, b = BLUE_B)

        // 4. Heavy Indomitus Chest Cuirass + Gold Winged Aquila + Heraldic Tilting Shield
        m.addOrientedEllipsoid(0.012f * s, by + 0.480f * s, 0f, 0.132f * s, 0.118f * s, 0.162f * s, latSegs = 6, lonSegs = 12, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        // Gold Winged Chest Aquila
        m.addOrientedEllipsoid(0.142f * s, by + 0.495f * s, 0f, 0.022f * s, 0.024f * s, 0.020f * s, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.128f * s, by + 0.506f * s, -0.058f * s, 0.024f * s, 0.034f * s, 0.088f * s, rotXDeg = 20f, rotYDeg = 18f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.128f * s, by + 0.506f * s, 0.058f * s, 0.024f * s, 0.034f * s, 0.088f * s, rotXDeg = -20f, rotYDeg = -18f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        // Small Heraldic Tilting Shield on upper-left chest corner (matching photo!)
        m.addOrientedBox(0.122f * s, by + 0.548f * s, -0.105f * s, 0.018f * s, 0.062f * s, 0.050f * s, rotYDeg = 24f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.129f * s, by + 0.556f * s, -0.108f * s, 0.012f * s, 0.024f * s, 0.020f * s, rotYDeg = 24f, r = WAX_R, g = WAX_G, b = WAX_B)
        m.addOrientedBox(0.129f * s, by + 0.540f * s, -0.098f * s, 0.012f * s, 0.024f * s, 0.020f * s, rotYDeg = 24f, r = helmW_R, g = helmW_G, b = helmW_B)

        // 5. Towering Indomitus Cowl & Back Exhaust Block (placed cleanly behind & framing the head)
        m.addOrientedEllipsoid(-0.065f * s, by + 0.560f * s, 0f, 0.095f * s, 0.125f * s, 0.148f * s, phiEndDeg = 92f, latSegs = 5, lonSegs = 10, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        // Raised Blue Cowl Collar Arch framing behind the White Helmet
        m.addArcTube(0.010f * s, by + 0.635f * s, 0f, 0.088f * s, 0.098f * s, 0.022f * s, rotZDeg = 82f, startDeg = -95f, endDeg = 95f, arcSegs = 12, tubeSegs = 6, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        // Rear Terminator Power Block & Twin Exhaust Vents
        m.addOrientedBox(-0.115f * s, by + 0.525f * s, 0f, 0.115f * s, 0.215f * s, 0.185f * s, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        m.addOrientedBox(-0.165f * s, by + 0.600f * s, -0.056f * s, 0.045f * s, 0.062f * s, 0.052f * s, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(-0.165f * s, by + 0.600f * s, 0.056f * s, 0.045f * s, 0.062f * s, 0.052f * s, r = IRON_R, g = IRON_G, b = IRON_B)

        // 6. Authentic Pure WHITE Indomitus Terminator Helmet (prominent, iconic wedge snout, dark rebreather grille)
        val hx = 0.055f * s
        val hy = by + 0.585f * s
        val whiteHelmetR = 0.96f; val whiteHelmetG = 0.97f; val whiteHelmetB = 0.99f
        val whiteShadeR = 0.82f; val whiteShadeG = 0.84f; val whiteShadeB = 0.88f

        // White Cranium / Top Rounded Helmet Dome
        m.addOrientedEllipsoid(hx, hy + 0.022f * s, 0f, 0.076f * s, 0.068f * s, 0.066f * s, latSegs = 6, lonSegs = 12, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)
        // Center Forehead Ridge running over top of cranium
        m.addOrientedBox(hx + 0.015f * s, hy + 0.086f * s, 0f, 0.072f * s, 0.016f * s, 0.016f * s, rotZDeg = -6f, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)
        // Reinforced Brow Visor Rim overhang
        m.addOrientedBox(hx + 0.054f * s, hy + 0.038f * s, 0f, 0.034f * s, 0.018f * s, 0.088f * s, rotZDeg = -14f, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)

        // Iconic Sloped White "Bulldog / Tusk" Snout Faceplate
        m.addOrientedBox(hx + 0.056f * s, hy - 0.008f * s, 0f, 0.050f * s, 0.052f * s, 0.076f * s, rotZDeg = -18f, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)
        // Flaring White Cheek Plates
        m.addOrientedBox(hx + 0.038f * s, hy - 0.012f * s, -0.040f * s, 0.046f * s, 0.044f * s, 0.022f * s, rotZDeg = -14f, rotYDeg = -14f, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)
        m.addOrientedBox(hx + 0.038f * s, hy - 0.012f * s, 0.040f * s, 0.046f * s, 0.044f * s, 0.022f * s, rotZDeg = -14f, rotYDeg = 14f, r = whiteHelmetR, g = whiteHelmetG, b = whiteHelmetB)

        // Gunmetal Iron Rebreather Grille & Steel Slits at front of snout
        m.addOrientedBox(hx + 0.080f * s, hy - 0.014f * s, 0f, 0.014f * s, 0.034f * s, 0.032f * s, rotZDeg = -18f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(hx + 0.086f * s, hy - 0.014f * s, 0f, 0.008f * s, 0.026f * s, 0.020f * s, rotZDeg = -18f, r = STEEL_R, g = STEEL_G, b = STEEL_B)

        // Twin Cylindrical Cheek Filter Canisters / Vents
        m.addOrientedCylinder(hx + 0.044f * s, hy - 0.028f * s, -0.042f * s, hx + 0.044f * s, hy - 0.028f * s, -0.056f * s, 0.016f * s, 0.014f * s, 8, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        m.addOrientedCylinder(hx + 0.044f * s, hy - 0.028f * s, 0.042f * s, hx + 0.044f * s, hy - 0.028f * s, 0.056f * s, 0.016f * s, 0.014f * s, 8, r = STEEL_R, g = STEEL_G, b = STEEL_B)

        // Sleek Miniature-Accurate Recessed Eye Sockets & Dark Visor Slits (matching photo, no glowing red distortion)
        m.addOrientedBox(hx + 0.058f * s, hy + 0.022f * s, -0.022f * s, 0.014f * s, 0.014f * s, 0.022f * s, rotXDeg = -10f, rotYDeg = 15f, r = 0.08f, g = 0.08f, b = 0.10f)
        m.addOrientedBox(hx + 0.058f * s, hy + 0.022f * s, 0.022f * s, 0.014f * s, 0.014f * s, 0.022f * s, rotXDeg = 10f, rotYDeg = -15f, r = 0.08f, g = 0.08f, b = 0.10f)
        m.addOrientedBox(hx + 0.063f * s, hy + 0.022f * s, -0.022f * s, 0.007f * s, 0.008f * s, 0.018f * s, rotXDeg = -10f, rotYDeg = 15f, r = 0.58f, g = 0.10f, b = 0.12f)
        m.addOrientedBox(hx + 0.063f * s, hy + 0.022f * s, 0.022f * s, 0.007f * s, 0.008f * s, 0.018f * s, rotXDeg = 10f, rotYDeg = -15f, r = 0.58f, g = 0.10f, b = 0.12f)

        // White Chin Collar & Neck Seal
        m.addOrientedBox(hx + 0.046f * s, hy - 0.040f * s, 0f, 0.034f * s, 0.020f * s, 0.046f * s, rotZDeg = -14f, r = whiteShadeR, g = whiteShadeG, b = whiteShadeB)
        m.addCylinder(hx - 0.01f * s, hy - 0.060f * s, 0f, 0.040f * s, 0.040f * s, 0.026f * s, 8, IRON_R, IRON_G, IRON_B)

        // 7. Massive Chunky Indomitus Shoulder Pauldrons (+Crux Terminatus on Right, White Ω + Purity Seal on Left)
        for (side in floatArrayOf(-1f, 1f)) {
            val pz = side * 0.188f * s
            val py = by + 0.555f * s
            m.addOrientedEllipsoid(0f, py, pz, 0.125f * s, 0.118f * s, 0.106f * s, rotXDeg = side * 12f, phiEndDeg = 112f, latSegs = 5, lonSegs = 12, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
            // Heavy Raised Steel/Gold Border Trim
            m.addArcTube(0f, py - 0.032f * s, pz, 0.122f * s, 0.104f * s, 0.016f * s, rotXDeg = side * 12f, startDeg = 0f, endDeg = 360f, arcSegs = 14, tubeSegs = 5, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        }
        // Left Pauldron (-Z): White Omega (Ω) + Purity Seal
        val lpz = -0.280f * s
        val lpy = by + 0.562f * s
        m.addArcTube(0f, lpy + 0.004f * s, lpz, 0.034f * s, 0.034f * s, 0.0085f * s, rotXDeg = 90f, startDeg = 145f, endDeg = 395f, arcSegs = 10, tubeSegs = 4, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(-0.030f * s, lpy - 0.018f * s, lpz, 0.022f * s, 0.010f * s, 0.014f * s, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(0.030f * s, lpy - 0.018f * s, lpz, 0.022f * s, 0.010f * s, 0.014f * s, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedCylinder(0.098f * s, by + 0.515f * s, -0.215f * s, 0.116f * s, by + 0.515f * s, -0.222f * s, 0.018f * s, 0.016f * s, 8, r = WAX_R, g = WAX_G, b = WAX_B)
        m.addOrientedBox(0.106f * s, by + 0.460f * s, -0.220f * s, 0.008f * s, 0.075f * s, 0.022f * s, rotZDeg = -8f, r = PARCH_R, g = PARCH_G, b = PARCH_B)

        // Right Pauldron (+Z): Sculpted Silver-Bone Crux Terminatus (Cross + Skull Emblem, matching photo!)
        val rpz = 0.286f * s
        val rpy = by + 0.562f * s
        m.addOrientedBox(0f, rpy, rpz, 0.024f * s, 0.088f * s, 0.018f * s, r = helmW_R * 0.85f, g = helmW_G * 0.85f, b = helmW_B * 0.85f)
        m.addOrientedBox(0f, rpy + 0.008f * s, rpz, 0.086f * s, 0.024f * s, 0.018f * s, r = helmW_R * 0.85f, g = helmW_G * 0.85f, b = helmW_B * 0.85f)
        m.addOrientedEllipsoid(0f, rpy + 0.008f * s, rpz + 0.010f * s, 0.020f * s, 0.022f * s, 0.018f * s, latSegs = 3, lonSegs = 6, r = helmW_R, g = helmW_G, b = helmW_B)

        if (useLightningClaws) {
            // 8 & 9 (ALT): TWIN LIGHTNING CLAWS (Left & Right Armored Power Claw Gauntlets + Quad Razor Talons)
            val clawEnergyR = 0.34f; val clawEnergyG = 0.90f; val clawEnergyB = 1.00f
            for (side in floatArrayOf(-1f, 1f)) {
                val isLeft = side < 0f
                val elX = if (isLeft) 0.045f * s else 0.055f * s
                val elY = by + (if (isLeft) 0.420f else 0.435f) * s
                val elZ = side * 0.225f * s
                val hdX = if (isLeft) 0.165f * s else 0.185f * s
                val hdY = by + (if (isLeft) 0.460f else 0.480f) * s
                val hdZ = side * 0.212f * s

                buildMarineArm(
                    m, s,
                    shX = 0f, shY = by + 0.525f * s, shZ = side * 0.195f * s,
                    elX = elX, elY = elY, elZ = elZ,
                    hdX = hdX, hdY = hdY, hdZ = hdZ
                )

                val yawSplay = side * -12f
                val pitchUp = if (isLeft) 14f else 18f

                // Heavy Indomitus Power-Claw Forearm Casing + Knuckle Guard Collar
                m.addOrientedBox(
                    hdX - 0.015f * s, hdY, hdZ,
                    0.115f * s, 0.072f * s, 0.088f * s,
                    rotYDeg = yawSplay, rotZDeg = pitchUp,
                    r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
                )
                m.addOrientedBox(
                    hdX + 0.036f * s, hdY + 0.008f * s, hdZ,
                    0.042f * s, 0.068f * s, 0.094f * s,
                    rotYDeg = yawSplay, rotZDeg = pitchUp,
                    r = IRON_R, g = IRON_G, b = IRON_B
                )
                m.addOrientedBox(
                    hdX - 0.042f * s, hdY + 0.004f * s, hdZ,
                    0.022f * s, 0.076f * s, 0.092f * s,
                    rotYDeg = yawSplay, rotZDeg = pitchUp,
                    r = GOLD_R, g = GOLD_G, b = GOLD_B
                )
                // Dorsal Glowing Plasma-Arc Power Cell Conduit on top of Gauntlet
                m.addOrientedBox(
                    hdX + 0.004f * s, hdY + 0.040f * s, hdZ,
                    0.068f * s, 0.022f * s, 0.058f * s,
                    rotYDeg = yawSplay, rotZDeg = pitchUp,
                    r = clawEnergyR, g = clawEnergyG, b = clawEnergyB, a = E
                )

                // 4 Parallel Curved Terminator Lightning Claw Blades extending forward
                val bladeZOffsets = floatArrayOf(-0.032f, -0.011f, 0.011f, 0.032f)
                val bladeLengths = floatArrayOf(0.185f, 0.215f, 0.215f, 0.185f)
                for (bIdx in bladeZOffsets.indices) {
                    val bz = hdZ + bladeZOffsets[bIdx] * s
                    val bLen = bladeLengths[bIdx] * s
                    val rootX = hdX + 0.048f * s
                    val rootY = hdY + 0.012f * s
                    val midX = rootX + bLen * 0.56f
                    val midY = rootY + 0.020f * s
                    val tipX = rootX + bLen
                    val tipY = rootY - 0.024f * s

                    // Steel metallic blade spine
                    m.addOrientedCylinder(
                        rootX, rootY, bz,
                        midX, midY, bz + side * 0.006f * s,
                        0.011f * s, 0.0085f * s, 6,
                        r = STEEL_R * 1.2f, g = STEEL_G * 1.2f, b = STEEL_B * 1.25f
                    )
                    // Razor tapered glowing electric-cyan talon tip
                    m.addOrientedCylinder(
                        midX, midY, bz + side * 0.006f * s,
                        tipX, tipY, bz + side * 0.014f * s,
                        0.0085f * s, 0.0018f * s, 6,
                        r = clawEnergyR, g = clawEnergyG, b = clawEnergyB, a = E
                    )
                    // Under-edge energy corona fin
                    m.addOrientedBox(
                        rootX + bLen * 0.44f, rootY + 0.004f * s, bz + side * 0.004f * s,
                        bLen * 0.72f, 0.014f * s, 0.005f * s,
                        rotYDeg = yawSplay, rotZDeg = 6f,
                        r = clawEnergyR, g = clawEnergyG, b = clawEnergyB, a = E
                    )
                }
            }
        } else {
            // 8. Left Arm (-Z) + Authentic Faceted Hexagonal STORM SHIELD (Gold Frame + White Field + Steel Cross & Skull)
            buildMarineArm(
                m, s,
                shX = 0f, shY = by + 0.525f * s, shZ = -0.195f * s,
                elX = 0.025f * s, elY = by + 0.415f * s, elZ = -0.225f * s,
                hdX = 0.115f * s, hdY = by + 0.405f * s, hdZ = -0.230f * s
            )
            val scx = 0.135f * s
            val scy = by + 0.415f * s
            val scz = -0.268f * s
            val shRotY = 38f // +38° around Y rotates shield face (+X) toward forward-left (+X, -Z)
            val shRad = Math.toRadians(shRotY.toDouble())
            val shCos = kotlin.math.cos(shRad).toFloat()
            val shSin = kotlin.math.sin(shRad).toFloat()
            fun shieldBox(lx: Float, ly: Float, lz: Float, sx: Float, sy: Float, sz: Float, rotXDeg: Float = 0f, r: Float, g: Float, b: Float) {
                val wx = scx + lx * shCos + lz * shSin
                val wy = scy + ly
                val wz = scz - lx * shSin + lz * shCos
                m.addOrientedBox(wx, wy, wz, sx, sy, sz, rotXDeg = rotXDeg, rotYDeg = shRotY, r = r, g = g, b = b)
            }
            // Hexagonal / Kite Shield White Field
            shieldBox(0f, 0f, 0f, 0.042f * s, 0.230f * s, 0.210f * s, r = shieldFieldR, g = shieldFieldG, b = shieldFieldB)
            shieldBox(0f, 0.112f * s, 0f, 0.042f * s, 0.145f * s, 0.145f * s, rotXDeg = 45f, r = shieldFieldR, g = shieldFieldG, b = shieldFieldB)
            shieldBox(0f, -0.112f * s, 0f, 0.042f * s, 0.145f * s, 0.145f * s, rotXDeg = 45f, r = shieldFieldR, g = shieldFieldG, b = shieldFieldB)
            // Thick Gold 6-Sided Kite Outer Perimeter Frame
            shieldBox(0.004f * s, 0f, -0.102f * s, 0.050f * s, 0.238f * s, 0.026f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            shieldBox(0.004f * s, 0f, 0.102f * s, 0.050f * s, 0.238f * s, 0.026f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            shieldBox(0.004f * s, 0.162f * s, -0.052f * s, 0.050f * s, 0.026f * s, 0.140f * s, rotXDeg = -45f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            shieldBox(0.004f * s, 0.162f * s, 0.052f * s, 0.050f * s, 0.026f * s, 0.140f * s, rotXDeg = 45f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            shieldBox(0.004f * s, -0.162f * s, -0.052f * s, 0.050f * s, 0.026f * s, 0.140f * s, rotXDeg = 45f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            shieldBox(0.004f * s, -0.162f * s, 0.052f * s, 0.050f * s, 0.026f * s, 0.140f * s, rotXDeg = -45f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            // Dark Steel/Silver Imperial Cross + Center Skull Boss flush on White Shield Face
            shieldBox(0.024f * s, 0f, 0f, 0.022f * s, 0.295f * s, 0.036f * s, r = STEEL_R * 0.72f, g = STEEL_G * 0.72f, b = STEEL_B * 0.72f)
            shieldBox(0.024f * s, 0.024f * s, 0f, 0.022f * s, 0.036f * s, 0.180f * s, r = STEEL_R * 0.72f, g = STEEL_G * 0.72f, b = STEEL_B * 0.72f)
            m.addOrientedEllipsoid(
                scx + 0.034f * s * shCos,
                scy + 0.024f * s,
                scz - 0.034f * s * shSin,
                0.024f * s, 0.028f * s, 0.024f * s,
                rotYDeg = shRotY, latSegs = 3, lonSegs = 6,
                r = STEEL_R * 1.25f, g = STEEL_G * 1.25f, b = STEEL_B * 1.25f
            )

            // 9. Right Arm (+Z) + Heavy FOREST-GREEN THUNDER HAMMER (matching photo!)
            buildMarineArm(
                m, s,
                shX = 0f, shY = by + 0.525f * s, shZ = 0.195f * s,
                elX = 0.035f * s, elY = by + 0.425f * s, elZ = 0.235f * s,
                hdX = 0.150f * s, hdY = by + 0.435f * s, hdZ = 0.220f * s
            )
            // Hammer Haft angled heroically upward-forward (+32° pitch)
            val hBaseX = 0.105f * s; val hBaseY = by + 0.340f * s; val hBaseZ = 0.225f * s
            val hTopX = 0.255f * s; val hTopY = by + 0.625f * s; val hTopZ = 0.215f * s
            m.addOrientedCylinder(hBaseX, hBaseY, hBaseZ, hTopX, hTopY, hTopZ, 0.017f * s, 0.018f * s, 8, r = IRON_R, g = IRON_G, b = IRON_B)
            // Gold pommel cap at bottom of haft
            m.addOrientedEllipsoid(hBaseX, hBaseY, hBaseZ, 0.024f * s, 0.024f * s, 0.024f * s, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            // Forest-Green Double-Faced Thunder Hammer Head + Dark Iron Collar + Steel Striking Faces
            val hHeadX = 0.240f * s; val hHeadY = by + 0.595f * s; val hHeadZ = 0.215f * s
            val hHeadRotZ = -28f
            val hRad = Math.toRadians(hHeadRotZ.toDouble())
            val hCos = kotlin.math.cos(hRad).toFloat()
            val hSin = kotlin.math.sin(hRad).toFloat()
            m.addOrientedBox(hHeadX, hHeadY, hHeadZ, 0.175f * s, 0.098f * s, 0.098f * s, rotZDeg = hHeadRotZ, r = hammerGrnR, g = hammerGrnG, b = hammerGrnB)
            m.addOrientedBox(hHeadX, hHeadY, hHeadZ, 0.068f * s, 0.108f * s, 0.106f * s, rotZDeg = hHeadRotZ, r = hammerGrnDkR, g = hammerGrnDkG, b = hammerGrnDkB)
            m.addOrientedBox(hHeadX + 0.092f * s * hCos, hHeadY + 0.092f * s * hSin, hHeadZ, 0.026f * s, 0.090f * s, 0.090f * s, rotZDeg = hHeadRotZ, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            m.addOrientedBox(hHeadX - 0.092f * s * hCos, hHeadY - 0.092f * s * hSin, hHeadZ, 0.026f * s, 0.090f * s, 0.090f * s, rotZDeg = hHeadRotZ, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            // Crimson Purity Seal + Parchment ribbon fluttering off hammer collar
            m.addOrientedCylinder(hHeadX - 0.01f * s, hHeadY - 0.055f * s, hHeadZ + 0.045f * s, hHeadX - 0.01f * s, hHeadY - 0.055f * s, hHeadZ + 0.058f * s, 0.015f * s, 0.013f * s, 8, r = WAX_R, g = WAX_G, b = WAX_B)
            m.addOrientedBox(hHeadX - 0.015f * s, hHeadY - 0.105f * s, hHeadZ + 0.054f * s, 0.018f * s, 0.068f * s, 0.006f * s, rotZDeg = -10f, r = PARCH_R, g = PARCH_G, b = PARCH_B)
        }
    }

    private fun buildPrimarisUltramarine(m: MeshAccumulator, archetype: UnitArchetype) {
        val s = if (archetype == UnitArchetype.CAPTAIN) 1.15f else 1.0f
        val isTerm = false
        val isCap = archetype == UnitArchetype.CAPTAIN

        // 1. Compact Scenic Battle Plinth under the miniature's boots
        val plinthR = if (isCap) 0.24f else 0.17f * s
        m.addCylinder(0f, 0.002f, 0f, plinthR, plinthR * 0.94f, 0.028f, 12, 0.14f, 0.11f, 0.10f)
        m.addOrientedEllipsoid(0.04f * s, 0.025f, -0.04f * s, 0.08f * s, 0.024f, 0.07f * s, latSegs = 3, lonSegs = 6, phiEndDeg = 90f, r = 0.22f, g = 0.16f, b = 0.14f)
        m.addOrientedBox(-0.07f * s, 0.032f, 0.06f * s, 0.05f * s, 0.030f, 0.06f * s, rotYDeg = 28f, r = 0.26f, g = 0.20f, b = 0.17f)

        val baseY = 0.030f

        // 2. Heroic Wide Stance Legs: Flared MK X Greaves, Sabatons, Gold Knee Poleyns & Thighs
        // Left Leg (-Z, stepped slightly forward +X)
        buildMarineLeg(m, s, baseY, footX = 0.042f * s, footZ = -0.088f * s, yawDeg = -10f, isLeft = true, isTerm = isTerm)
        // Right Leg (+Z, braced back)
        buildMarineLeg(m, s, baseY, footX = -0.025f * s, footZ = 0.090f * s, yawDeg = 12f, isLeft = false, isTerm = isTerm)

        // 3. Ribbed Dark Undersuit Pelvis / Waist + Armored Belt + Gold Buckle + Hanging Reliquary
        m.addOrientedEllipsoid(0f, baseY + 0.305f * s, 0f, 0.076f * s, 0.058f * s, 0.098f * s, latSegs = 4, lonSegs = 8, r = IRON_R, g = IRON_G, b = IRON_B)
        // Waist abdomen
        m.addOrientedCylinder(0f, baseY + 0.31f * s, 0f, 0f, baseY + 0.40f * s, 0f, 0.072f * s, 0.084f * s, 10, r = IRON_R, g = IRON_G, b = IRON_B)
        // Segmented Belt + side pouches
        m.addCylinder(0f, baseY + 0.335f * s, 0f, 0.082f * s, 0.084f * s, 0.034f * s, 10, 0.18f, 0.14f, 0.12f)
        m.addOrientedBox(0.084f * s, baseY + 0.352f * s, 0f, 0.026f * s, 0.036f * s, 0.046f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.01f * s, baseY + 0.350f * s, 0.092f * s, 0.050f * s, 0.046f * s, 0.032f * s, r = 0.16f, g = 0.12f, b = 0.10f)
        // Hanging Reliquary Shrine on front-left hip (as in photo)
        m.addOrientedBox(0.080f * s, baseY + 0.278f * s, -0.042f * s, 0.026f * s, 0.072f * s, 0.044f * s, rotZDeg = -8f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(0.092f * s, baseY + 0.285f * s, -0.042f * s, 0.012f * s, 0.045f * s, 0.030f * s, rotZDeg = -8f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.074f * s, baseY + 0.222f * s, -0.044f * s, 0.008f * s, 0.048f * s, 0.024f * s, rotZDeg = -10f, r = PARCH_R, g = PARCH_G, b = PARCH_B)

        // 4. Sculpted Chest Cuirass + Raised Gorget Collar + Imperial Gold Winged Aquila
        val chestR = if (isTerm) 0.122f * s else 0.104f * s
        val chestW = if (isTerm) 0.152f * s else 0.132f * s
        m.addOrientedEllipsoid(
            0.012f * s, baseY + 0.455f * s, 0f,
            chestR, 0.105f * s, chestW,
            latSegs = 6, lonSegs = 10,
            r = BLUE_R, g = BLUE_G, b = BLUE_B
        )
        // Upper chest armor shelf + raised collar gorget
        m.addArcTube(
            0.012f * s, baseY + 0.542f * s, 0f,
            0.078f * s, 0.088f * s, 0.020f * s,
            startDeg = -75f, endDeg = 75f, arcSegs = 8, tubeSegs = 5,
            r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
        )
        if (isTerm) {
            // Heavy Terminator Hooded Top Carapace
            m.addOrientedEllipsoid(
                -0.01f * s, baseY + 0.56f * s, 0f,
                0.125f * s, 0.115f * s, 0.145f * s,
                phiEndDeg = 80f, latSegs = 4, lonSegs = 10,
                r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
            )
        }
        // Gold Winged Chest Aquila + Center Skull (wings sweep upward and wrap back around cuirass)
        m.addOrientedEllipsoid(0.115f * s, baseY + 0.472f * s, 0f, 0.020f * s, 0.022f * s, 0.018f * s, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.105f * s, baseY + 0.484f * s, -0.048f * s, 0.022f * s, 0.032f * s, 0.072f * s, rotXDeg = 20f, rotYDeg = 18f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.105f * s, baseY + 0.484f * s, 0.048f * s, 0.022f * s, 0.032f * s, 0.072f * s, rotXDeg = -20f, rotYDeg = -18f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.095f * s, baseY + 0.466f * s, -0.044f * s, 0.018f * s, 0.018f * s, 0.056f * s, rotXDeg = 14f, rotYDeg = 18f, r = GOLD_DK_R, g = GOLD_DK_G, b = GOLD_DK_B)
        m.addOrientedBox(0.095f * s, baseY + 0.466f * s, 0.044f * s, 0.018f * s, 0.018f * s, 0.056f * s, rotXDeg = -14f, rotYDeg = -18f, r = GOLD_DK_R, g = GOLD_DK_G, b = GOLD_DK_B)

        // 5. Massive Domed Shoulder Pauldrons + Gold Trim + White Omega (Left) + Purity Seal
        buildPauldron(m, s, cy = baseY + 0.500f * s, cz = -0.150f * s, isLeft = true, hasOmega = true)
        buildPauldron(m, s, cy = baseY + 0.500f * s, cz = 0.150f * s, isLeft = false, hasOmega = false)

        // 6. Mk X Primaris Helmet (Blue Dome, Forehead Skull Stud, Crimson Eyes, Gunmetal Breather Grille)
        val helmY = if (isTerm) baseY + 0.585f * s else baseY + 0.602f * s
        val helmX = if (isTerm) 0.028f * s else 0.012f * s
        m.addCylinder(helmX, helmY - 0.052f * s, 0f, 0.042f * s, 0.040f * s, 0.045f * s, 8, IRON_R, IRON_G, IRON_B)
        m.addOrientedEllipsoid(helmX, helmY, 0f, 0.076f * s, 0.078f * s, 0.068f * s, latSegs = 5, lonSegs = 10, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        // Top helmet crest ridge
        m.addOrientedBox(helmX - 0.005f * s, helmY + 0.072f * s, 0f, 0.096f * s, 0.022f * s, 0.024f * s, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        // Silver forehead skull stud (exact match to photo!)
        m.addOrientedEllipsoid(helmX + 0.070f * s, helmY + 0.036f * s, 0f, 0.014f * s, 0.016f * s, 0.014f * s, latSegs = 3, lonSegs = 6, r = STEEL_R * 1.3f, g = STEEL_G * 1.3f, b = STEEL_B * 1.3f)
        // Glowing Crimson-Red Eye Lenses
        m.addOrientedBox(helmX + 0.066f * s, helmY + 0.008f * s, -0.024f * s, 0.022f * s, 0.016f * s, 0.026f * s, rotXDeg = -14f, rotYDeg = 18f, r = 0.98f, g = 0.22f, b = 0.16f, a = E)
        m.addOrientedBox(helmX + 0.066f * s, helmY + 0.008f * s, 0.024f * s, 0.022f * s, 0.016f * s, 0.026f * s, rotXDeg = 14f, rotYDeg = -18f, r = 0.98f, g = 0.22f, b = 0.16f, a = E)
        // Angled Gunmetal Respirator Faceplate + Vertical Slits + Ear Pods
        m.addOrientedBox(helmX + 0.056f * s, helmY - 0.026f * s, 0f, 0.048f * s, 0.044f * s, 0.056f * s, rotZDeg = -14f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(helmX + 0.078f * s, helmY - 0.026f * s, -0.012f * s, 0.010f * s, 0.032f * s, 0.008f * s, rotZDeg = -14f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        m.addOrientedBox(helmX + 0.078f * s, helmY - 0.026f * s, 0.012f * s, 0.010f * s, 0.032f * s, 0.008f * s, rotZDeg = -14f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        m.addOrientedCylinder(helmX, helmY - 0.012f * s, -0.068f * s, helmX, helmY - 0.012f * s, -0.078f * s, 0.024f * s, 0.020f * s, 8, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        m.addOrientedCylinder(helmX, helmY - 0.012f * s, 0.068f * s, helmX, helmY - 0.012f * s, 0.078f * s, 0.024f * s, 0.020f * s, 8, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)

        // 7. Mk X Power Backpack (or Assault Jump Pack)
        if (archetype == UnitArchetype.ASSAULT_MARINE) {
            // Jump Pack with Twin Winged Turbines & Glowing Amber Exhausts
            m.addOrientedBox(-0.115f * s, baseY + 0.48f * s, 0f, 0.11f * s, 0.19f * s, 0.17f * s, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
            for (side in floatArrayOf(-1f, 1f)) {
                m.addOrientedCylinder(-0.135f * s, baseY + 0.39f * s, side * 0.088f * s, -0.155f * s, baseY + 0.59f * s, side * 0.105f * s, 0.048f * s, 0.056f * s, 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)
                m.addOrientedBox(-0.155f * s, baseY + 0.58f * s, side * 0.125f * s, 0.08f * s, 0.09f * s, 0.016f * s, rotXDeg = side * 22f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
                m.addOrientedEllipsoid(-0.135f * s, baseY + 0.37f * s, side * 0.088f * s, 0.038f * s, 0.042f * s, 0.038f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.50f, b = 0.12f, a = E)
            }
        } else {
            // Standard Mk X Reactor Pack with Twin Angled V-Thruster Spheres
            m.addOrientedBox(-0.105f * s, baseY + 0.46f * s, 0f, 0.095f * s, 0.165f * s, 0.145f * s, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
            m.addOrientedEllipsoid(-0.115f * s, baseY + 0.555f * s, 0f, 0.045f * s, 0.045f * s, 0.045f * s, latSegs = 4, lonSegs = 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            m.addOrientedCylinder(-0.150f * s, baseY + 0.44f * s, 0f, -0.170f * s, baseY + 0.44f * s, 0f, 0.032f * s, 0.028f * s, 8, r = IRON_R, g = IRON_G, b = IRON_B)
            for (side in floatArrayOf(-1f, 1f)) {
                m.addOrientedCylinder(
                    -0.112f * s, baseY + 0.51f * s, side * 0.048f * s,
                    -0.124f * s, baseY + 0.585f * s, side * 0.118f * s,
                    0.026f * s, 0.032f * s, 8, r = BLUE_R, g = BLUE_G, b = BLUE_B
                )
                m.addOrientedEllipsoid(
                    -0.126f * s, baseY + 0.595f * s, side * 0.126f * s,
                    0.044f * s, 0.044f * s, 0.044f * s,
                    latSegs = 4, lonSegs = 8, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
                )
                m.addOrientedCylinder(
                    -0.126f * s, baseY + 0.595f * s, side * 0.126f * s,
                    -0.142f * s, baseY + 0.625f * s, side * 0.156f * s,
                    0.024f * s, 0.028f * s, 8, r = IRON_R, g = IRON_G, b = IRON_B
                )
            }
        }

        // 8. Arms & Iconic Weapons per Archetype
        when (archetype) {
            UnitArchetype.INTERCESSOR -> {
                // Left Arm: relaxed guard pose at hip with clenched blue gauntlet (matching photo)
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = -0.152f * s,
                    elX = -0.015f * s, elY = baseY + 0.375f * s, elZ = -0.172f * s,
                    hdX = 0.055f * s, hdY = baseY + 0.295f * s, hdZ = -0.155f * s
                )
                // Right Arm: holding Primaris Bolt Rifle angled down-forward (exact match to photo!)
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = 0.152f * s,
                    elX = -0.01f * s, elY = baseY + 0.375f * s, elZ = 0.168f * s,
                    hdX = 0.055f * s, hdY = baseY + 0.295f * s, hdZ = 0.165f * s
                )
                buildBoltRifle(m, s, rx = 0.115f * s, ry = baseY + 0.255f * s, rz = 0.175f * s, pitchDeg = -58f, isPlasma = false)
            }
            UnitArchetype.HELLBLASTER -> {
                // Left supporting arm reaching toward forward barrel
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = -0.152f * s,
                    elX = 0.05f * s, elY = baseY + 0.380f * s, elZ = -0.130f * s,
                    hdX = 0.175f * s, hdY = baseY + 0.355f * s, hdZ = 0.025f * s
                )
                // Right trigger arm holding Heavy Plasma Incinerator level-forward
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = 0.152f * s,
                    elX = -0.01f * s, elY = baseY + 0.385f * s, elZ = 0.165f * s,
                    hdX = 0.095f * s, hdY = baseY + 0.345f * s, hdZ = 0.115f * s
                )
                buildBoltRifle(m, s, rx = 0.195f * s, ry = baseY + 0.365f * s, rz = 0.095f * s, pitchDeg = -6f, isPlasma = true)
            }
            UnitArchetype.ASSAULT_MARINE -> {
                // Left Arm: Heavy Bolt Pistol aimed forward
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = -0.152f * s,
                    elX = 0.03f * s, elY = baseY + 0.40f * s, elZ = -0.165f * s,
                    hdX = 0.145f * s, hdY = baseY + 0.39f * s, hdZ = -0.145f * s
                )
                m.addOrientedBox(0.215f * s, baseY + 0.40f * s, -0.145f * s, 0.14f * s, 0.055f * s, 0.036f * s, r = IRON_R, g = IRON_G, b = IRON_B)
                m.addOrientedCylinder(0.27f * s, baseY + 0.415f * s, -0.145f * s, 0.31f * s, baseY + 0.415f * s, -0.145f * s, 0.014f * s, 0.014f * s, 6, r = STEEL_R, g = STEEL_G, b = STEEL_B)

                // Right Arm: Chainsword raised to strike
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = 0.152f * s,
                    elX = 0.04f * s, elY = baseY + 0.41f * s, elZ = 0.185f * s,
                    hdX = 0.145f * s, hdY = baseY + 0.43f * s, hdZ = 0.175f * s
                )
                m.addOrientedBox(0.235f * s, baseY + 0.525f * s, 0.175f * s, 0.055f * s, 0.28f * s, 0.028f * s, rotZDeg = -34f, r = CAPE_R, g = CAPE_G, b = CAPE_B)
                m.addOrientedBox(0.258f * s, baseY + 0.515f * s, 0.175f * s, 0.022f * s, 0.26f * s, 0.018f * s, rotZDeg = -34f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            }
            UnitArchetype.TERMINATOR -> {
                // Left Arm + Heavy Gold-Trimmed Storm Shield
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = -0.165f * s,
                    elX = 0.04f * s, elY = baseY + 0.39f * s, elZ = -0.195f * s,
                    hdX = 0.135f * s, hdY = baseY + 0.38f * s, hdZ = -0.195f * s
                )
                buildRelicShield(m, s, cx = 0.165f * s, cy = baseY + 0.36f * s, cz = -0.225f * s)

                // Right Arm + Glowing Thunder Hammer
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = 0.165f * s,
                    elX = 0.05f * s, elY = baseY + 0.40f * s, elZ = 0.195f * s,
                    hdX = 0.155f * s, hdY = baseY + 0.41f * s, hdZ = 0.185f * s
                )
                m.addOrientedCylinder(0.155f * s, baseY + 0.27f * s, 0.185f * s, 0.185f * s, baseY + 0.62f * s, 0.185f * s, 0.018f * s, 0.018f * s, 8, r = IRON_R, g = IRON_G, b = IRON_B)
                m.addOrientedBox(0.190f * s, baseY + 0.60f * s, 0.185f * s, 0.17f * s, 0.105f * s, 0.105f * s, rotZDeg = -8f, r = STEEL_R * 0.72f, g = STEEL_G * 0.72f, b = STEEL_B * 0.72f)
                m.addOrientedBox(0.190f * s, baseY + 0.60f * s, 0.185f * s, 0.185f * s, 0.042f * s, 0.115f * s, rotZDeg = -8f, r = 0.25f, g = 0.88f, b = 1.0f, a = E)
            }
            UnitArchetype.CAPTAIN -> {
                // Flowing Crimson Relic Cloak
                m.addOrientedBox(-0.145f * s, baseY + 0.28f * s, 0f, 0.032f * s, 0.45f * s, 0.24f * s, rotZDeg = 10f, r = CAPE_R, g = CAPE_G, b = CAPE_B)
                // Gold Iron Halo with crown rays behind helmet
                m.addArcTube(-0.02f * s, baseY + 0.665f * s, 0f, 0.095f * s, 0.095f * s, 0.013f * s, rotZDeg = 90f, startDeg = -135f, endDeg = 135f, arcSegs = 12, tubeSegs = 5, r = GOLD_R, g = GOLD_G, b = GOLD_B)
                // Left Arm + Relic Storm Shield
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = -0.158f * s,
                    elX = 0.04f * s, elY = baseY + 0.39f * s, elZ = -0.185f * s,
                    hdX = 0.135f * s, hdY = baseY + 0.38f * s, hdZ = -0.185f * s
                )
                buildRelicShield(m, s, cx = 0.165f * s, cy = baseY + 0.36f * s, cz = -0.215f * s)

                // Right Arm + Master-Crafted Glowing Power Sword
                buildMarineArm(
                    m, s,
                    shX = 0f, shY = baseY + 0.485f * s, shZ = 0.158f * s,
                    elX = 0.05f * s, elY = baseY + 0.40f * s, elZ = 0.185f * s,
                    hdX = 0.160f * s, hdY = baseY + 0.40f * s, hdZ = 0.175f * s
                )
                m.addOrientedBox(0.165f * s, baseY + 0.42f * s, 0.175f * s, 0.026f * s, 0.088f * s, 0.042f * s, rotZDeg = -25f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
                m.addOrientedBox(0.285f * s, baseY + 0.51f * s, 0.175f * s, 0.34f * s, 0.038f * s, 0.018f * s, rotZDeg = 28f, r = 0.32f, g = 0.92f, b = 1.0f, a = E)
            }
            else -> Unit
        }
    }

    private fun buildUltramarineIncursor(m: MeshAccumulator) {
        val s = 1.0f
        val skinR = 0.78f; val skinG = 0.56f; val skinB = 0.45f
        val skinShR = 0.64f; val skinShG = 0.44f; val skinShB = 0.36f
        val gunmetalR = 0.42f; val gunmetalG = 0.45f; val gunmetalB = 0.50f

        // 1. Dark Ash-Grey Tactical Base Plinth
        m.addCylinder(0f, 0.002f, 0f, 0.175f, 0.165f, 0.028f, 12, 0.13f, 0.13f, 0.14f)
        m.addOrientedEllipsoid(0.04f, 0.025f, -0.04f, 0.075f, 0.022f, 0.065f, latSegs = 3, lonSegs = 6, phiEndDeg = 90f, r = 0.22f, g = 0.22f, b = 0.24f)
        val by = 0.030f

        // 2. Sleek Phobos Mk X Stance Legs + Right Thigh Combat Knife Scabbard + Left Thigh Holster
        for (side in floatArrayOf(-1f, 1f)) {
            val isLeft = side < 0f
            val footX = if (isLeft) 0.045f * s else -0.025f * s
            val footZ = side * 0.092f * s
            val yawDeg = if (isLeft) -10f else 14f

            // Sabaton Boot
            m.addOrientedBox(footX, by + 0.015f * s, footZ, 0.122f * s, 0.030f * s, 0.078f * s, rotYDeg = yawDeg, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedEllipsoid(footX + 0.016f * s, by + 0.026f * s, footZ, 0.064f * s, 0.038f * s, 0.044f * s, rotYDeg = yawDeg, phiEndDeg = 90f, latSegs = 4, lonSegs = 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)

            // Slimmer Phobos Greave
            val kneeX = footX * 0.55f + 0.01f * s
            val kneeY = by + 0.195f * s
            val kneeZ = footZ * 0.92f
            m.addOrientedCylinder(footX, by + 0.040f * s, footZ, kneeX, kneeY, kneeZ, 0.052f * s, 0.043f * s, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            // Sleek Trimless Blue Phobos Kneepad (matching photo)
            m.addOrientedEllipsoid(kneeX + 0.032f * s, kneeY, kneeZ, 0.028f * s, 0.036f * s, 0.035f * s, rotYDeg = yawDeg, latSegs = 4, lonSegs = 8, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
            // Thigh Plate + Dark Undersuit Strap Bands
            val hipY = by + 0.305f * s
            val hipZ = side * 0.068f * s
            m.addOrientedCylinder(kneeX, kneeY + 0.01f * s, kneeZ, 0f, hipY, hipZ, 0.042f * s, 0.048f * s, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            m.addOrientedCylinder(kneeX * 0.45f, kneeY + 0.065f * s, (kneeZ + hipZ) * 0.5f, kneeX * 0.45f, kneeY + 0.082f * s, (kneeZ + hipZ) * 0.5f, 0.047f * s, 0.048f * s, 10, r = IRON_R, g = IRON_G, b = IRON_B)
        }
        // Right Hip/Thigh (+Z): Vertical Combat Knife in Scabbard (brass hilt/guard + dark sheath, matching photo!)
        m.addOrientedBox(-0.005f, by + 0.255f, 0.118f, 0.034f, 0.125f, 0.022f, rotZDeg = 6f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(-0.005f, by + 0.210f, 0.122f, 0.028f, 0.045f, 0.016f, rotZDeg = 6f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(-0.005f, by + 0.322f, 0.122f, 0.046f, 0.016f, 0.024f, rotZDeg = 6f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedCylinder(-0.005f, by + 0.330f, 0.122f, -0.012f, by + 0.382f, 0.122f, 0.012f, 0.011f, 6, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedEllipsoid(-0.013f, by + 0.386f, 0.122f, 0.015f, 0.014f, 0.015f, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        // Left Thigh (-Z): Dark Tactical Holster/Pouch
        m.addOrientedBox(0.012f, by + 0.270f, -0.116f, 0.056f, 0.110f, 0.030f, rotZDeg = -6f, r = IRON_R, g = IRON_G, b = IRON_B)

        // 3. Ribbed Waist, Tactical Belt & Sleek Phobos Cuirass (clean blue armor, no chest aquila)
        m.addOrientedEllipsoid(0f, by + 0.305f, 0f, 0.072f, 0.056f, 0.094f, latSegs = 4, lonSegs = 8, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedCylinder(0f, by + 0.310f, 0f, 0f, by + 0.400f, 0f, 0.068f, 0.080f, 10, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addCylinder(0f, by + 0.335f, 0f, 0.078f, 0.080f, 0.032f, 10, IRON_R, IRON_G, IRON_B)
        m.addOrientedBox(0.078f, by + 0.351f, 0f, 0.020f, 0.028f, 0.036f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        // Phobos Cuirass
        m.addOrientedEllipsoid(0.010f, by + 0.455f, 0f, 0.096f, 0.102f, 0.124f, latSegs = 6, lonSegs = 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        // Raised Phobos Collar Gorget + Left Collar Targeting Optic Pod (-Z beside head!)
        m.addArcTube(0.012f, by + 0.542f, 0f, 0.074f, 0.084f, 0.018f, startDeg = -85f, endDeg = 85f, arcSegs = 8, tubeSegs = 5, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        m.addOrientedCylinder(0.018f, by + 0.562f, -0.090f, 0.062f, by + 0.568f, -0.096f, 0.022f, 0.020f, 8, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedEllipsoid(0.064f, by + 0.568f, -0.096f, 0.010f, 0.014f, 0.014f, latSegs = 3, lonSegs = 6, r = STEEL_R, g = STEEL_G, b = STEEL_B)

        // 4. Bare Bald Head + Service Studs + Crimson Visor Goggles + Dark Half-Face Respirator Mask
        val hx = 0.015f
        val hy = by + 0.602f
        m.addCylinder(hx, hy - 0.050f, 0f, 0.040f, 0.038f, 0.042f, 8, IRON_R, IRON_G, IRON_B)
        // Exposed Weathered Bald Cranium
        m.addOrientedEllipsoid(hx, hy + 0.008f, 0f, 0.066f, 0.068f, 0.058f, latSegs = 5, lonSegs = 10, r = skinR, g = skinG, b = skinB)
        // Slight brow ridge shading + 2 Silver Forehead Service Studs
        m.addOrientedBox(hx + 0.050f, hy + 0.022f, 0f, 0.022f, 0.012f, 0.058f, r = skinShR, g = skinShG, b = skinShB)
        m.addOrientedEllipsoid(hx + 0.055f, hy + 0.042f, 0.018f, 0.006f, 0.006f, 0.006f, latSegs = 2, lonSegs = 5, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedEllipsoid(hx + 0.051f, hy + 0.043f, 0.030f, 0.006f, 0.006f, 0.006f, latSegs = 2, lonSegs = 5, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        // Dark Goggle Frame Band + Continuous Glowing Crimson Visor Slit
        m.addOrientedBox(hx + 0.042f, hy + 0.008f, 0f, 0.038f, 0.022f, 0.072f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(hx + 0.058f, hy + 0.008f, 0f, 0.014f, 0.010f, 0.058f, r = 0.96f, g = 0.16f, b = 0.14f, a = E)
        // Dark Iron Half-Face Respirator Mask + Twin Cheek Filter Canisters
        m.addOrientedBox(hx + 0.046f, hy - 0.026f, 0f, 0.048f, 0.042f, 0.056f, rotZDeg = -14f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(hx + 0.068f, hy - 0.026f, 0f, 0.012f, 0.028f, 0.018f, rotZDeg = -14f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        m.addOrientedCylinder(hx + 0.034f, hy - 0.028f, -0.028f, hx + 0.034f, hy - 0.028f, -0.046f, 0.016f, 0.014f, 8, r = STEEL_R * 0.85f, g = STEEL_G * 0.85f, b = STEEL_B * 0.85f)
        m.addOrientedCylinder(hx + 0.034f, hy - 0.028f, 0.028f, hx + 0.034f, hy - 0.028f, 0.046f, 0.016f, 0.014f, 8, r = STEEL_R * 0.85f, g = STEEL_G * 0.85f, b = STEEL_B * 0.85f)

        // 5. Asymmetrical Incursor Pauldrons:
        // Left Pauldron (-Z): Full Rounded Blue Shell + Gold Bottom Rim + White Omega (Ω)
        buildPauldron(m, s, cy = by + 0.500f, cz = -0.146f, isLeft = true, hasOmega = true)
        // Right Pauldron (+Z): Trimless Blue Phobos Shell + White Tactical Squad Chevron Arrow
        val rcy = by + 0.500f
        val rcz = 0.146f
        m.addOrientedEllipsoid(0f, rcy, rcz, 0.104f, 0.094f, 0.092f, rotXDeg = 14f, phiEndDeg = 104f, latSegs = 5, lonSegs = 12, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        val racZ = rcz + 0.082f
        val racY = rcy + 0.012f
        m.addOrientedBox(-0.014f, racY + 0.008f, racZ, 0.012f, 0.038f, 0.012f, rotZDeg = -32f, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(0.014f, racY + 0.008f, racZ, 0.012f, 0.038f, 0.012f, rotZDeg = 32f, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(0f, racY - 0.018f, racZ, 0.038f, 0.010f, 0.012f, r = WHITE_R, g = WHITE_G, b = WHITE_B)

        // 6. Phobos Power Pack + Towering Right-Shoulder MULTI-SPECTRUM ARRAY (Green Circle + Red Slit!)
        m.addOrientedBox(-0.102f, by + 0.460f, 0f, 0.090f, 0.158f, 0.138f, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        m.addOrientedEllipsoid(-0.112f, by + 0.550f, 0f, 0.042f, 0.042f, 0.042f, latSegs = 4, lonSegs = 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        for (side in floatArrayOf(-1f, 1f)) {
            m.addOrientedCylinder(-0.110f, by + 0.505f, side * 0.045f, -0.122f, by + 0.578f, side * 0.114f, 0.024f, 0.030f, 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            m.addOrientedEllipsoid(-0.124f, by + 0.588f, side * 0.122f, 0.042f, 0.042f, 0.042f, latSegs = 4, lonSegs = 8, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
            m.addOrientedCylinder(-0.124f, by + 0.588f, side * 0.122f, -0.140f, by + 0.618f, side * 0.150f, 0.022f, 0.026f, 8, r = IRON_R, g = IRON_G, b = IRON_B)
        }
        // Multi-Spectrum Array Articulated Stalk & Sensor Box over Right Shoulder (+Z)
        m.addOrientedCylinder(-0.085f, by + 0.565f, 0.056f, -0.052f, by + 0.720f, 0.072f, 0.014f, 0.012f, 6, r = IRON_R, g = IRON_G, b = IRON_B)
        val arrX = -0.032f; val arrY = by + 0.755f; val arrZ = 0.078f
        m.addOrientedBox(arrX, arrY, arrZ, 0.086f, 0.062f, 0.104f, rotZDeg = 6f, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        // Bright Emerald-Green Circular Optic Lens on Right-Front outer corner (+Z, +X)
        m.addOrientedCylinder(arrX + 0.015f, arrY - 0.008f, arrZ + 0.042f, arrX + 0.056f, arrY - 0.008f, arrZ + 0.042f, 0.026f, 0.024f, 10, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedEllipsoid(arrX + 0.058f, arrY - 0.008f, arrZ + 0.042f, 0.010f, 0.021f, 0.021f, latSegs = 4, lonSegs = 8, r = 0.12f, g = 0.94f, b = 0.36f, a = E)
        // Glowing Crimson-Red Rectangular Scanner Window beside the green lens
        m.addOrientedBox(arrX + 0.046f, arrY - 0.004f, arrZ - 0.018f, 0.012f, 0.026f, 0.046f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(arrX + 0.051f, arrY - 0.004f, arrZ - 0.018f, 0.008f, 0.018f, 0.038f, r = 0.98f, g = 0.16f, b = 0.14f, a = E)

        // 7. Two-Handed Arms Holding Occulus Bolt Carbine Horizontally Across Chest (exact Brother Remus pose!)
        // Right Arm (+Z): holding grip
        buildMarineArm(
            m, s,
            shX = 0f, shY = by + 0.485f, shZ = 0.148f,
            elX = 0.010f, elY = by + 0.370f, elZ = 0.178f,
            hdX = 0.145f, hdY = by + 0.352f, hdZ = 0.088f
        )
        // Left Arm (-Z): supporting forward under-barrel
        buildMarineArm(
            m, s,
            shX = 0f, shY = by + 0.485f, shZ = -0.148f,
            elX = 0.035f, elY = by + 0.365f, elZ = -0.162f,
            hdX = 0.172f, hdY = by + 0.342f, hdZ = -0.068f
        )

        // 8. Occulus Bolt Carbine (pointing left -Z and slightly forward +X, rotYDeg = 72°)
        val gx = 0.180f; val gy = by + 0.392f; val gz = 0.004f
        val gunYaw = 72f
        val gRad = Math.toRadians(gunYaw.toDouble())
        val gCos = kotlin.math.cos(gRad).toFloat()
        val gSin = kotlin.math.sin(gRad).toFloat()
        fun gunBox(lx: Float, ly: Float, lz: Float, sx: Float, sy: Float, sz: Float, rotZDeg: Float = 0f, r: Float, g: Float, b: Float, a: Float = 1f) {
            val wx = gx + lx * gCos + lz * gSin
            val wy = gy + ly
            val wz = gz - lx * gSin + lz * gCos
            m.addOrientedBox(wx, wy, wz, sx, sy, sz, rotYDeg = gunYaw, rotZDeg = rotZDeg, r = r, g = g, b = b, a = a)
        }
        // Heavy Weathered Gunmetal-Silver Carbine Receiver Body
        gunBox(0f, 0f, 0f, 0.235f, 0.072f, 0.044f, r = gunmetalR, g = gunmetalG, b = gunmetalB)
        // Dark ribbed forend + pistol grip
        gunBox(0.065f, -0.028f, 0f, 0.085f, 0.026f, 0.042f, r = IRON_R, g = IRON_G, b = IRON_B)
        gunBox(-0.085f, -0.048f, 0f, 0.038f, 0.056f, 0.032f, rotZDeg = 18f, r = IRON_R, g = IRON_G, b = IRON_B)
        // Curved Sickle Box Magazine underneath
        gunBox(-0.005f, -0.062f, 0f, 0.044f, 0.076f, 0.032f, rotZDeg = -14f, r = gunmetalR * 0.85f, g = gunmetalG * 0.85f, b = gunmetalB * 0.85f)
        // Large Boxy Occulus Targeter Optic Housing mounted on top of receiver (iconic Incursor silhouette!)
        gunBox(-0.012f, 0.064f, 0f, 0.115f, 0.056f, 0.052f, r = gunmetalR * 0.92f, g = gunmetalG * 0.92f, b = gunmetalB * 0.92f)
        gunBox(-0.012f, 0.036f, 0f, 0.135f, 0.018f, 0.038f, r = IRON_R, g = IRON_G, b = IRON_B)
        gunBox(0.046f, 0.066f, 0f, 0.012f, 0.034f, 0.034f, r = 0.95f, g = 0.18f, b = 0.16f, a = E)
        // Heavy Cylindrical Muzzle Brake extending along carbine forward (+lx -> -Z/+X in world)
        val mStartX = gx + 0.115f * gCos
        val mStartZ = gz - 0.115f * gSin
        val mEndX = gx + 0.185f * gCos
        val mEndZ = gz - 0.185f * gSin
        m.addOrientedCylinder(mStartX, gy + 0.006f, mStartZ, mEndX, gy + 0.006f, mEndZ, 0.022f, 0.020f, 8, r = STEEL_R, g = STEEL_G, b = STEEL_B)
    }

    private fun buildMarineLeg(
        m: MeshAccumulator,
        s: Float,
        baseY: Float,
        footX: Float,
        footZ: Float,
        yawDeg: Float,
        isLeft: Boolean,
        isTerm: Boolean
    ) {
        val sideSign = if (isLeft) -1f else 1f
        val greaveBotR = if (isTerm) 0.072f * s else 0.060f * s
        val greaveTopR = if (isTerm) 0.056f * s else 0.045f * s
        val thighR = if (isTerm) 0.062f * s else 0.049f * s

        // Armored Sabaton Boot + Rounded Toe Dome
        m.addOrientedBox(footX, baseY + 0.016f * s, footZ, 0.125f * s, 0.032f * s, 0.082f * s, rotYDeg = yawDeg, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedEllipsoid(
            footX + 0.018f * s, baseY + 0.028f * s, footZ,
            0.068f * s, 0.042f * s, 0.046f * s,
            rotYDeg = yawDeg, phiEndDeg = 90f, latSegs = 4, lonSegs = 8,
            r = BLUE_R, g = BLUE_G, b = BLUE_B
        )

        // Flared MK X Greave (Shin Armor flaring wider toward ankle!)
        val kneeX = footX * 0.55f + 0.01f * s
        val kneeY = baseY + 0.195f * s
        val kneeZ = footZ * 0.92f
        m.addOrientedCylinder(
            footX, baseY + 0.042f * s, footZ,
            kneeX, kneeY, kneeZ,
            greaveBotR, greaveTopR, segments = 10,
            r = BLUE_R, g = BLUE_G, b = BLUE_B
        )

        // Rounded Knee Cap (Poleyn) + Gold Arch Trim Rim
        m.addOrientedEllipsoid(
            kneeX + 0.034f * s, kneeY, kneeZ,
            0.032f * s, 0.040f * s, 0.038f * s,
            rotYDeg = yawDeg, latSegs = 4, lonSegs = 8,
            r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
        )
        m.addArcTube(
            kneeX + 0.034f * s, kneeY + 0.012f * s, kneeZ,
            0.034f * s, 0.038f * s, 0.008f * s,
            rotYDeg = yawDeg, rotZDeg = 68f,
            startDeg = -90f, endDeg = 90f, arcSegs = 8, tubeSegs = 4,
            r = GOLD_R, g = GOLD_G, b = GOLD_B
        )

        // Right Knee Gold Badge / Left Shin Purity Seal (matching photo!)
        if (!isLeft) {
            m.addOrientedEllipsoid(kneeX + 0.062f * s, kneeY - 0.004f * s, kneeZ, 0.012f * s, 0.016f * s, 0.016f * s, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        } else {
            // Crimson Purity Seal + Parchment on outer left greave
            m.addOrientedCylinder(footX + 0.02f * s, baseY + 0.125f * s, footZ - 0.048f * s, footX + 0.02f * s, baseY + 0.125f * s, footZ - 0.062f * s, 0.016f * s, 0.014f * s, 8, r = WAX_R, g = WAX_G, b = WAX_B)
            m.addOrientedBox(footX + 0.008f * s, baseY + 0.088f * s, footZ - 0.056f * s, 0.018f * s, 0.058f * s, 0.006f * s, rotXDeg = -8f, r = PARCH_R, g = PARCH_G, b = PARCH_B)
        }

        // Thigh Plate connecting knee to hip
        val hipX = 0f
        val hipY = baseY + 0.305f * s
        val hipZ = sideSign * 0.068f * s
        m.addOrientedCylinder(kneeX, kneeY + 0.01f * s, kneeZ, hipX, hipY, hipZ, thighR * 0.88f, thighR, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
    }

    private fun buildPauldron(
        m: MeshAccumulator,
        s: Float,
        cy: Float,
        cz: Float,
        isLeft: Boolean,
        hasOmega: Boolean
    ) {
        val side = if (isLeft) -1f else 1f
        val tiltX = side * 14f
        // Massive curved blue dome shell
        m.addOrientedEllipsoid(
            0f, cy, cz,
            0.108f * s, 0.098f * s, 0.096f * s,
            rotXDeg = tiltX, phiEndDeg = 108f, latSegs = 5, lonSegs = 12,
            r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B
        )
        // Thick Warm Gold Trim Rim around outer/bottom pauldron edge
        m.addArcTube(
            0f, cy - 0.028f * s, cz,
            0.106f * s, 0.094f * s, 0.015f * s,
            rotXDeg = tiltX,
            startDeg = 0f, endDeg = 360f, arcSegs = 14, tubeSegs = 5,
            r = GOLD_R, g = GOLD_G, b = GOLD_B
        )

        if (hasOmega) {
            // Crisp White Ultramarines Omega (Ω) on outer Left Pauldron (-Z)
            val oz = cz - 0.084f * s
            val oy = cy + 0.014f * s
            // U-arch rotated onto XY plane facing -Z, right-side-up Ω (145°..395°)
            m.addArcTube(
                0f, oy + 0.004f * s, oz,
                0.032f * s, 0.032f * s, 0.0085f * s,
                rotXDeg = 90f,
                startDeg = 145f, endDeg = 395f, arcSegs = 10, tubeSegs = 4,
                r = WHITE_R, g = WHITE_G, b = WHITE_B
            )
            // Flared Omega base feet
            m.addOrientedBox(-0.028f * s, oy - 0.016f * s, oz, 0.022f * s, 0.010f * s, 0.014f * s, r = WHITE_R, g = WHITE_G, b = WHITE_B)
            m.addOrientedBox(0.028f * s, oy - 0.016f * s, oz, 0.022f * s, 0.010f * s, 0.014f * s, r = WHITE_R, g = WHITE_G, b = WHITE_B)

            // Crimson Wax Purity Seal & Twin Cream Parchment Strips on front-left pauldron rim (matching photo!)
            m.addOrientedCylinder(
                0.088f * s, cy - 0.035f * s, cz - 0.032f * s,
                0.104f * s, cy - 0.035f * s, cz - 0.038f * s,
                0.018f * s, 0.016f * s, 8, r = WAX_R, g = WAX_G, b = WAX_B
            )
            m.addOrientedBox(0.095f * s, cy - 0.086f * s, cz - 0.034f * s, 0.008f * s, 0.072f * s, 0.022f * s, rotZDeg = -10f, r = PARCH_R, g = PARCH_G, b = PARCH_B)
        }
    }

    private fun buildMarineArm(
        m: MeshAccumulator,
        s: Float,
        shX: Float, shY: Float, shZ: Float,
        elX: Float, elY: Float, elZ: Float,
        hdX: Float, hdY: Float, hdZ: Float
    ) {
        // Dark elbow joint sphere
        m.addOrientedEllipsoid(elX, elY, elZ, 0.036f * s, 0.036f * s, 0.036f * s, latSegs = 3, lonSegs = 6, r = IRON_R, g = IRON_G, b = IRON_B)
        // Upper arm
        m.addOrientedCylinder(shX, shY, shZ, elX, elY, elZ, 0.044f * s, 0.038f * s, 8, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        // Flared forearm vambrace
        m.addOrientedCylinder(elX, elY, elZ, hdX, hdY, hdZ, 0.036f * s, 0.044f * s, 8, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        // Armored gauntlet fist
        m.addOrientedEllipsoid(hdX, hdY, hdZ, 0.034f * s, 0.034f * s, 0.032f * s, latSegs = 3, lonSegs = 6, r = BLUE_R, g = BLUE_G, b = BLUE_B)
    }

    private fun buildBoltRifle(
        m: MeshAccumulator,
        s: Float,
        rx: Float,
        ry: Float,
        rz: Float,
        pitchDeg: Float,
        isPlasma: Boolean
    ) {
        // Casing length along local X rotated by pitchDeg around Z
        val rad = Math.toRadians(pitchDeg.toDouble())
        val cx = kotlin.math.cos(rad).toFloat()
        val cy = kotlin.math.sin(rad).toFloat()
        // Main dark steel receiver body
        m.addOrientedBox(rx, ry, rz, 0.28f * s, 0.068f * s, 0.044f * s, rotZDeg = pitchDeg, r = IRON_R, g = IRON_G, b = IRON_B)
        // Gunmetal upper rail & scope
        m.addOrientedBox(rx - cy * 0.036f * s, ry + cx * 0.036f * s, rz, 0.22f * s, 0.022f * s, 0.036f * s, rotZDeg = pitchDeg, r = STEEL_R * 0.7f, g = STEEL_G * 0.7f, b = STEEL_B * 0.7f)
        m.addOrientedBox(rx - cy * 0.056f * s - cx * 0.02f * s, ry + cx * 0.056f * s - cy * 0.02f * s, rz, 0.09f * s, 0.024f * s, 0.024f * s, rotZDeg = pitchDeg, r = STEEL_R * 0.85f, g = STEEL_G * 0.85f, b = STEEL_B * 0.85f)
        // Vertical magazine
        m.addOrientedBox(rx + cy * 0.046f * s + cx * 0.04f * s, ry - cx * 0.046f * s + cy * 0.04f * s, rz, 0.042f * s, 0.075f * s, 0.032f * s, rotZDeg = pitchDeg + 12f, r = IRON_R * 1.2f, g = IRON_G * 1.2f, b = IRON_B * 1.2f)
        // Barrel shroud & muzzle
        val bStartX = rx + cx * 0.13f * s
        val bStartY = ry + cy * 0.13f * s
        val bEndX = rx + cx * 0.24f * s
        val bEndY = ry + cy * 0.24f * s
        m.addOrientedCylinder(bStartX, bStartY, rz, bEndX, bEndY, rz, 0.017f * s, 0.015f * s, 8, r = STEEL_R, g = STEEL_G, b = STEEL_B)

        if (isPlasma) {
            // Glowing cyan plasma containment coils along top of rifle
            m.addOrientedBox(rx - cy * 0.038f * s + cx * 0.03f * s, ry + cx * 0.038f * s + cy * 0.03f * s, rz, 0.15f * s, 0.036f * s, 0.040f * s, rotZDeg = pitchDeg, r = 0.20f, g = 0.88f, b = 1.0f, a = E)
        } else {
            // Crimson Wax Purity Seal + parchment strip on rifle casing (matching photo!)
            m.addOrientedCylinder(rx + cx * 0.06f * s, ry + cy * 0.06f * s, rz + 0.022f * s, rx + cx * 0.06f * s, ry + cy * 0.06f * s, rz + 0.032f * s, 0.014f * s, 0.012f * s, 8, r = WAX_R, g = WAX_G, b = WAX_B)
            m.addOrientedBox(rx + cx * 0.055f * s, ry + cy * 0.055f * s - 0.032f * s, rz + 0.028f * s, 0.016f * s, 0.052f * s, 0.006f * s, rotZDeg = -6f, r = PARCH_R, g = PARCH_G, b = PARCH_B)
        }
    }

    private fun buildRelicShield(m: MeshAccumulator, s: Float, cx: Float, cy: Float, cz: Float) {
        // Heavy curved Ultramarine Shield + Thick Gold Border + Gold Cross Boss
        m.addOrientedBox(cx, cy, cz, 0.044f * s, 0.34f * s, 0.22f * s, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        m.addOrientedBox(cx + 0.008f * s, cy + 0.165f * s, cz, 0.052f * s, 0.032f * s, 0.235f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(cx + 0.008f * s, cy - 0.165f * s, cz, 0.052f * s, 0.032f * s, 0.235f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(cx + 0.008f * s, cy, cz - 0.108f * s, 0.052f * s, 0.345f * s, 0.030f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(cx + 0.008f * s, cy, cz + 0.108f * s, 0.052f * s, 0.345f * s, 0.030f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(cx + 0.024f * s, cy, cz, 0.026f * s, 0.26f * s, 0.038f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(cx + 0.024f * s, cy + 0.03f * s, cz, 0.026f * s, 0.038f * s, 0.16f * s, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedEllipsoid(cx + 0.038f * s, cy + 0.03f * s, cz, 0.026f * s, 0.028f * s, 0.026f * s, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
    }

    private fun buildUltramarineDreadnought(m: MeshAccumulator) {
        // 1. Detailed Textured Warzone Base Plinth + Rusted Metal Plates & Crushed Enemy Debris
        m.addCylinder(0f, 0.002f, 0f, 0.38f, 0.36f, 0.04f, 16, 0.12f, 0.11f, 0.10f)
        m.addOrientedBox(0.04f, 0.018f, -0.05f, 0.24f, 0.016f, 0.18f, rotYDeg = 18f, r = 0.42f, g = 0.22f, b = 0.14f)
        m.addOrientedBox(-0.08f, 0.024f, 0.08f, 0.20f, 0.018f, 0.16f, rotYDeg = -24f, r = 0.36f, g = 0.18f, b = 0.11f)
        m.addOrientedEllipsoid(0.02f, 0.022f, 0.12f, 0.08f, 0.035f, 0.055f, latSegs = 3, lonSegs = 6, r = 0.65f, g = 0.12f, b = 0.12f)

        val by = 0.04f

        // 2. Heavy Mechanical Walker Legs (True Mecha Design - No Humanoid Shape)
        for (side in floatArrayOf(-1f, 1f)) {
            val lz = side * 0.175f
            // Wide Industrial Foot Sabaton (Heavy armored treads/toes)
            m.addOrientedBox(0.04f, by + 0.035f, lz, 0.28f, 0.07f, 0.18f, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedBox(0.14f, by + 0.05f, lz, 0.09f, 0.07f, 0.14f, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            // Heavy Lower Greave Hydraulic Armor Block
            m.addOrientedBox(0f, by + 0.18f, lz, 0.18f, 0.22f, 0.14f, r = BLUE_R, g = BLUE_G, b = BLUE_B)
            // External Hydraulic Piston Struts
            m.addOrientedCylinder(0.05f, by + 0.07f, lz + side * 0.06f, -0.05f, by + 0.28f, lz + side * 0.06f, 0.022f, 0.018f, 6, r = STEEL_R, g = STEEL_G, b = STEEL_B)
            // Heavy Knee Joint Housing & Armor Plate
            m.addOrientedBox(0.02f, by + 0.32f, lz, 0.14f, 0.12f, 0.16f, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedEllipsoid(0.06f, by + 0.32f, lz, 0.065f, 0.072f, 0.072f, latSegs = 4, lonSegs = 8, r = GOLD_R, g = GOLD_G, b = GOLD_B)
            // Upper Thigh Mecha Casing
            m.addOrientedCylinder(0.02f, by + 0.38f, lz, 0f, by + 0.52f, lz * 0.85f, 0.095f, 0.11f, 10, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        }

        // 3. Heavy Armored Waist / Hip Chassis Block
        m.addOrientedBox(0f, by + 0.53f, 0f, 0.34f, 0.14f, 0.32f, r = IRON_R, g = IRON_G, b = IRON_B)

        // 4. Massive Boxy Sarcophagus Torso (True Dreadnought Mecha Body)
        m.addOrientedBox(0f, by + 0.74f, 0f, 0.52f, 0.42f, 0.56f, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        // Upper Cowling Slope & Gold Trim
        m.addOrientedBox(0f, by + 0.97f, 0f, 0.48f, 0.05f, 0.52f, r = GOLD_R, g = GOLD_G, b = GOLD_B)

        // Central Gold Sarcophagus Door + Imperial Aquila + Glowing Cyan Optic Visor
        m.addOrientedBox(0.25f, by + 0.74f, 0f, 0.07f, 0.30f, 0.26f, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        m.addOrientedEllipsoid(0.27f, by + 0.78f, 0f, 0.038f, 0.042f, 0.038f, latSegs = 3, lonSegs = 6, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.27f, by + 0.80f, -0.09f, 0.032f, 0.036f, 0.14f, rotXDeg = 18f, rotYDeg = 16f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.27f, by + 0.80f, 0.09f, 0.032f, 0.036f, 0.14f, rotXDeg = -18f, rotYDeg = -16f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        // Glowing Cyan Optic Visor Slot
        m.addOrientedBox(0.288f, by + 0.79f, 0f, 0.036f, 0.052f, 0.16f, r = 0.24f, g = 0.90f, b = 1.0f, a = E)

        // 5. Twin Heavy Exhaust Stacks on Rear Cowling
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.16f
            m.addOrientedCylinder(-0.18f, by + 0.96f, sz, -0.24f, by + 1.24f, sz * 1.2f, 0.072f, 0.062f, 8, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedCylinder(-0.24f, by + 1.24f, sz * 1.2f, -0.26f, by + 1.30f, sz * 1.2f, 0.065f, 0.065f, 8, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        }

        // 6. Massive Left Shoulder Pauldron (+ Gold Trim + White Laurel Wreath Ω + Purity Seal)
        val lpsz = -0.38f
        val lpsy = by + 0.86f
        m.addOrientedBox(0.02f, lpsy, lpsz, 0.20f, 0.18f, 0.18f, rotXDeg = 16f, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        m.addArcTube(0.02f, lpsy - 0.042f, lpsz, 0.18f, 0.16f, 0.024f, rotXDeg = 16f, startDeg = 0f, endDeg = 360f, arcSegs = 14, tubeSegs = 5, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        // White Laurel Wreath + Omega (Ω) on Pauldron Face
        m.addArcTube(0.09f, lpsy + 0.02f, lpsz, 0.06f, 0.06f, 0.014f, rotXDeg = 90f, rotYDeg = 90f, startDeg = 140f, endDeg = 400f, arcSegs = 12, tubeSegs = 5, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(0.09f, lpsy - 0.03f, lpsz - 0.05f, 0.036f, 0.016f, 0.024f, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        m.addOrientedBox(0.09f, lpsy - 0.03f, lpsz + 0.05f, 0.036f, 0.016f, 0.024f, r = WHITE_R, g = WHITE_G, b = WHITE_B)
        // Purity Seal
        m.addOrientedCylinder(0.14f, lpsy - 0.04f, lpsz - 0.08f, 0.16f, lpsy - 0.04f, lpsz - 0.09f, 0.026f, 0.024f, 8, r = WAX_R, g = WAX_G, b = WAX_B)
        m.addOrientedBox(0.15f, lpsy - 0.13f, lpsz - 0.085f, 0.012f, 0.11f, 0.032f, rotZDeg = -8f, r = PARCH_R, g = PARCH_G, b = PARCH_B)

        // 7. Left Arm: Massive Articulated Siege Power Fist
        m.addOrientedBox(0.20f, by + 0.68f, -0.38f, 0.40f, 0.26f, 0.22f, r = BLUE_R, g = BLUE_G, b = BLUE_B)
        m.addOrientedBox(0.32f, by + 0.68f, -0.46f, 0.32f, 0.38f, 0.06f, r = GOLD_R, g = GOLD_G, b = GOLD_B)
        m.addOrientedBox(0.44f, by + 0.58f, -0.38f, 0.24f, 0.28f, 0.26f, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        for (fingerIdx in 0 until 4) {
            val fz = -0.48f + fingerIdx * 0.075f
            m.addOrientedBox(0.58f, by + 0.58f + (fingerIdx - 1.5f) * 0.03f, fz, 0.16f, 0.06f, 0.06f, r = IRON_R, g = IRON_G, b = IRON_B)
            m.addOrientedBox(0.67f, by + 0.58f + (fingerIdx - 1.5f) * 0.03f, fz, 0.07f, 0.05f, 0.05f, r = 0.22f, g = 0.88f, b = 1.0f, a = E)
        }

        // 8. Right Arm: Heavy Rotary Gatling Cannon + Ammunition Feed Box
        val rpsz = 0.38f
        val rpsy = by + 0.86f
        m.addOrientedBox(0.02f, rpsy, rpsz, 0.20f, 0.18f, 0.18f, rotXDeg = -16f, r = BLUE_HI_R, g = BLUE_HI_G, b = BLUE_HI_B)
        m.addArcTube(0.02f, rpsy - 0.042f, rpsz, 0.18f, 0.16f, 0.024f, rotXDeg = -16f, startDeg = 0f, endDeg = 360f, arcSegs = 14, tubeSegs = 5, r = GOLD_R, g = GOLD_G, b = GOLD_B)

        m.addOrientedBox(0.22f, by + 0.68f, 0.38f, 0.42f, 0.22f, 0.22f, r = IRON_R, g = IRON_G, b = IRON_B)
        m.addOrientedBox(0.25f, by + 0.85f, 0.38f, 0.20f, 0.16f, 0.18f, r = BLUE_DK_R, g = BLUE_DK_G, b = BLUE_DK_B)
        m.addOrientedCylinder(0.42f, by + 0.68f, 0.38f, 0.70f, by + 0.68f, 0.38f, 0.064f, 0.058f, 12, r = STEEL_R, g = STEEL_G, b = STEEL_B)
        for (b in 0 until 4) {
            val ba = (b * 90.0 * Math.PI / 180.0).toFloat()
            val bx = 0.42f
            val byOff = by + 0.68f + kotlin.math.sin(ba.toDouble()).toFloat() * 0.026f
            val bzOff = 0.38f + kotlin.math.cos(ba.toDouble()).toFloat() * 0.026f
            m.addOrientedCylinder(bx, byOff, bzOff, bx + 0.32f, byOff, bzOff, 0.016f, 0.016f, 6, r = IRON_R, g = IRON_G, b = IRON_B)
        }
    }

    // Hive Fleet Leviathan Tyranids Authentic Palette
    private val FLESH_R = 0.84f; private val FLESH_G = 0.72f; private val FLESH_B = 0.72f
    private val FLESH_SHADE_R = 0.68f; private val FLESH_SHADE_G = 0.50f; private val FLESH_SHADE_B = 0.54f
    private val CHITIN_R = 0.26f; private val CHITIN_G = 0.11f; private val CHITIN_B = 0.34f
    private val CHITIN_HI_R = 0.50f; private val CHITIN_HI_G = 0.20f; private val CHITIN_HI_B = 0.42f
    private val TALON_DK_R = 0.15f; private val TALON_DK_G = 0.08f; private val TALON_DK_B = 0.11f
    private val TALON_TIP_R = 0.64f; private val TALON_TIP_G = 0.14f; private val TALON_TIP_B = 0.18f
    private val TENDRIL_R = 0.92f; private val TENDRIL_G = 0.42f; private val TENDRIL_B = 0.18f

    private fun buildTermagant(m: MeshAccumulator) {
        val s = 0.92f
        // Compact desert/ash tactical base
        m.addCylinder(0f, 0.002f, 0f, 0.16f * s, 0.145f * s, 0.022f, 10, 0.13f, 0.10f, 0.12f)
        val by = 0.022f

        // Agile Sprinting Digitigrade Hind Legs (staggered stride)
        for (side in floatArrayOf(-1f, 1f)) {
            val z = side * 0.082f * s
            val fOff = if (side < 0f) 0.035f * s else -0.045f * s
            m.addOrientedCylinder(fOff - 0.02f * s, by + 0.018f * s, z, fOff - 0.07f * s, by + 0.115f * s, z, 0.022f * s, 0.028f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
            m.addOrientedCylinder(fOff - 0.07f * s, by + 0.115f * s, z, -0.02f * s, by + 0.21f * s, z * 0.78f, 0.030f * s, 0.042f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedEllipsoid(fOff, by + 0.014f * s, z, 0.046f * s, 0.018f * s, 0.030f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }

        // Low-slung Horizontal Ribbed Torso + Segmented Purple Chitin Dorsal Plates
        m.addOrientedEllipsoid(0.03f * s, by + 0.215f * s, 0f, 0.145f * s, 0.076f * s, 0.078f * s, rotZDeg = -14f, latSegs = 5, lonSegs = 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedEllipsoid(0.02f * s, by + 0.258f * s, 0f, 0.155f * s, 0.068f * s, 0.088f * s, rotZDeg = -14f, phiEndDeg = 95f, latSegs = 4, lonSegs = 10, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        // Edge-highlighted chitin ridge plates along spine
        for (i in 0 until 3) {
            val px = (-0.06f + i * 0.065f) * s
            m.addOrientedBox(px, by + 0.302f * s - i * 0.008f * s, 0f, 0.052f * s, 0.018f * s, 0.076f * s, rotZDeg = -16f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        }

        // Long Counter-Balancing Tail
        m.addOrientedCylinder(-0.09f * s, by + 0.19f * s, 0f, -0.23f * s, by + 0.16f * s, 0f, 0.034f * s, 0.022f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.23f * s, by + 0.16f * s, 0f, -0.34f * s, by + 0.21f * s, 0f, 0.022f * s, 0.008f * s, 6, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)

        // Swept Gaunt Head + Extended Chitin Head Crest + Bright Amber Eyes
        m.addOrientedEllipsoid(0.17f * s, by + 0.255f * s, 0f, 0.095f * s, 0.048f * s, 0.048f * s, rotZDeg = -8f, latSegs = 4, lonSegs = 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(0.125f * s, by + 0.295f * s, 0f, 0.11f * s, 0.024f * s, 0.068f * s, rotZDeg = 14f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedEllipsoid(0.22f * s, by + 0.262f * s, -0.028f * s, 0.014f * s, 0.011f * s, 0.011f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.62f, b = 0.12f, a = E)
        m.addOrientedEllipsoid(0.22f * s, by + 0.262f * s, 0.028f * s, 0.014f * s, 0.011f * s, 0.011f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.62f, b = 0.12f, a = E)

        // Symbiote Bio-Fleshborer gripped under thorax with both fore-arms + ammo-sac + dark organic barrel
        for (side in floatArrayOf(-1f, 1f)) {
            m.addOrientedCylinder(0.06f * s, by + 0.20f * s, side * 0.065f * s, 0.14f * s, by + 0.155f * s, side * 0.036f * s, 0.020f * s, 0.016f * s, 6, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        }
        m.addOrientedEllipsoid(0.16f * s, by + 0.155f * s, 0f, 0.115f * s, 0.042f * s, 0.044f * s, latSegs = 4, lonSegs = 8, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        m.addOrientedEllipsoid(0.14f * s, by + 0.115f * s, 0f, 0.048f * s, 0.032f * s, 0.034f * s, latSegs = 3, lonSegs = 6, r = TENDRIL_R * 0.78f, g = TENDRIL_G * 0.78f, b = TENDRIL_B * 0.78f)
        m.addOrientedCylinder(0.24f * s, by + 0.162f * s, 0f, 0.37f * s, by + 0.168f * s, 0f, 0.022f * s, 0.016f * s, 8, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
    }

    private fun buildHormagaunt(m: MeshAccumulator) {
        val s = 0.98f
        m.addCylinder(0f, 0.002f, 0f, 0.165f * s, 0.15f * s, 0.022f, 10, 0.13f, 0.09f, 0.13f)
        val by = 0.022f

        // Bounding Sprint Legs (Left foot pushing off turf, Right leg tucked high in mid-stride!)
        // Left leg grounded push-off
        m.addOrientedCylinder(-0.07f * s, by + 0.015f * s, -0.085f * s, -0.14f * s, by + 0.13f * s, -0.085f * s, 0.024f * s, 0.030f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.14f * s, by + 0.13f * s, -0.085f * s, -0.02f * s, by + 0.24f * s, -0.068f * s, 0.032f * s, 0.046f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedEllipsoid(-0.05f * s, by + 0.015f * s, -0.085f * s, 0.052f * s, 0.020f * s, 0.032f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        // Right leg tucked mid-leap forward
        m.addOrientedCylinder(0.05f * s, by + 0.095f * s, 0.085f * s, -0.04f * s, by + 0.175f * s, 0.085f * s, 0.024f * s, 0.032f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.04f * s, by + 0.175f * s, 0.085f * s, 0.02f * s, by + 0.25f * s, 0.068f * s, 0.032f * s, 0.046f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)

        // Forward-Lunging Sleek Torso + Sharp Swept Carapace
        m.addOrientedEllipsoid(0.045f * s, by + 0.26f * s, 0f, 0.15f * s, 0.072f * s, 0.076f * s, rotZDeg = 18f, latSegs = 5, lonSegs = 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedEllipsoid(0.035f * s, by + 0.295f * s, 0f, 0.16f * s, 0.062f * s, 0.084f * s, rotZDeg = 18f, phiEndDeg = 95f, latSegs = 4, lonSegs = 10, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)

        // Long Whip Tail arching behind
        m.addOrientedCylinder(-0.08f * s, by + 0.22f * s, 0f, -0.25f * s, by + 0.20f * s, 0f, 0.034f * s, 0.020f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.25f * s, by + 0.20f * s, 0f, -0.40f * s, by + 0.28f * s, 0f, 0.020f * s, 0.006f * s, 6, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)

        // Snarling Swept Predator Skull
        m.addOrientedEllipsoid(0.20f * s, by + 0.305f * s, 0f, 0.105f * s, 0.048f * s, 0.048f * s, rotZDeg = -6f, latSegs = 4, lonSegs = 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(0.155f * s, by + 0.345f * s, 0f, 0.12f * s, 0.024f * s, 0.064f * s, rotZDeg = 16f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        m.addOrientedEllipsoid(0.26f * s, by + 0.312f * s, -0.026f * s, 0.015f * s, 0.011f * s, 0.011f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.50f, b = 0.08f, a = E)
        m.addOrientedEllipsoid(0.26f * s, by + 0.312f * s, 0.026f * s, 0.015f * s, 0.011f * s, 0.011f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.50f, b = 0.08f, a = E)

        // Paired Long Razor Scything Talons Reaching Far Ahead to Slash!
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.088f * s
            m.addOrientedCylinder(0.10f * s, by + 0.27f * s, sz, 0.21f * s, by + 0.22f * s, sz * 1.32f, 0.026f * s, 0.020f * s, 6, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedCylinder(0.21f * s, by + 0.22f * s, sz * 1.32f, 0.42f * s, by + 0.32f * s, sz * 0.92f, 0.022f * s, 0.004f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
            // Secondary lower grasping talons
            m.addOrientedCylinder(0.08f * s, by + 0.21f * s, side * 0.068f * s, 0.19f * s, by + 0.16f * s, side * 0.09f * s, 0.018f * s, 0.005f * s, 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }
    }

    private fun buildTyranidWarrior(m: MeshAccumulator) {
        val s = 1.28f
        m.addCylinder(0f, 0.002f, 0f, 0.20f * s, 0.185f * s, 0.026f, 12, 0.13f, 0.10f, 0.14f)
        val by = 0.026f

        // Heavy Synapse Stalker Legs with Chitin Thigh & Shin Armor
        for (side in floatArrayOf(-1f, 1f)) {
            val z = side * 0.105f * s
            m.addOrientedCylinder(-0.02f * s, by + 0.02f * s, z, -0.075f * s, by + 0.16f * s, z, 0.034f * s, 0.042f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
            m.addOrientedCylinder(-0.075f * s, by + 0.16f * s, z, -0.01f * s, by + 0.29f * s, z * 0.85f, 0.045f * s, 0.056f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedBox(-0.03f * s, by + 0.20f * s, z + side * 0.03f * s, 0.062f * s, 0.11f * s, 0.036f * s, rotZDeg = 18f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
            m.addOrientedEllipsoid(0.01f * s, by + 0.018f * s, z, 0.064f * s, 0.024f * s, 0.044f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }

        // Tall Upright Armored Torso & Ribbed Exoskeleton
        m.addOrientedEllipsoid(0f, by + 0.36f * s, 0f, 0.115f * s, 0.145f * s, 0.115f * s, rotZDeg = -8f, latSegs = 5, lonSegs = 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(-0.025f * s, by + 0.38f * s, 0f, 0.135f * s, 0.24f * s, 0.20f * s, rotZDeg = -8f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        // Broad Armored Shoulder Carapace Plates + 3 Dorsal Spore Chimneys
        for (side in floatArrayOf(-1f, 1f)) {
            m.addOrientedBox(-0.01f * s, by + 0.46f * s, side * 0.14f * s, 0.13f * s, 0.065f * s, 0.11f * s, rotXDeg = side * 22f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
            m.addOrientedCylinder(-0.06f * s, by + 0.44f * s, side * 0.065f * s, -0.14f * s, by + 0.56f * s, side * 0.088f * s, 0.028f * s, 0.014f * s, 6, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        }
        m.addOrientedCylinder(-0.05f * s, by + 0.46f * s, 0f, -0.13f * s, by + 0.61f * s, 0f, 0.032f * s, 0.014f * s, 6, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)

        // Heavy Armored Tail
        m.addOrientedCylinder(-0.08f * s, by + 0.24f * s, 0f, -0.22f * s, by + 0.18f * s, 0f, 0.048f * s, 0.028f * s, 8, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedCylinder(-0.22f * s, by + 0.18f * s, 0f, -0.32f * s, by + 0.22f * s, 0f, 0.028f * s, 0.010f * s, 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)

        // Horned Warrior Crest Head + Mandibles + Glowing Violet Synapse Eyes
        m.addOrientedEllipsoid(0.085f * s, by + 0.495f * s, 0f, 0.088f * s, 0.062f * s, 0.066f * s, latSegs = 4, lonSegs = 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(0.055f * s, by + 0.545f * s, 0f, 0.145f * s, 0.038f * s, 0.140f * s, rotZDeg = 18f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        m.addOrientedCylinder(0.14f * s, by + 0.545f * s, 0f, 0.21f * s, by + 0.625f * s, 0f, 0.018f * s, 0.005f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        m.addOrientedEllipsoid(0.145f * s, by + 0.505f * s, -0.032f * s, 0.016f * s, 0.012f * s, 0.012f * s, latSegs = 3, lonSegs = 6, r = 0.85f, g = 0.35f, b = 1.0f, a = E)
        m.addOrientedEllipsoid(0.145f * s, by + 0.505f * s, 0.032f * s, 0.016f * s, 0.012f * s, 0.012f * s, latSegs = 3, lonSegs = 6, r = 0.85f, g = 0.35f, b = 1.0f, a = E)

        // Upper Arm Pair: Twin Massive Crimson-Hilt Tyranid BONESWORDS
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.145f * s
            m.addOrientedCylinder(0.03f * s, by + 0.44f * s, sz, 0.14f * s, by + 0.38f * s, sz * 1.2f, 0.032f * s, 0.026f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            // Serrated Chitin Bonesword Blade
            m.addOrientedBox(0.28f * s, by + 0.46f * s, sz * 1.15f, 0.34f * s, 0.052f * s, 0.020f * s, rotZDeg = 28f, rotYDeg = -side * 12f, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
            m.addOrientedBox(0.30f * s, by + 0.47f * s, sz * 1.15f, 0.32f * s, 0.018f * s, 0.022f * s, rotZDeg = 28f, rotYDeg = -side * 12f, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        }
        // Lower Arm Pair: Rendering Claws
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.115f * s
            m.addOrientedCylinder(0.04f * s, by + 0.31f * s, sz, 0.16f * s, by + 0.28f * s, sz * 1.25f, 0.024f * s, 0.016f * s, 6, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
            m.addOrientedCylinder(0.16f * s, by + 0.28f * s, sz * 1.25f, 0.26f * s, by + 0.32f * s, sz * 1.05f, 0.018f * s, 0.004f * s, 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }
    }

    private fun buildVonRyansLeaper(m: MeshAccumulator) {
        val s = 1.15f
        // 1. Scenic Tactical Rubble Base (matching Leviathan Leaper photo!)
        m.addCylinder(0f, 0.002f, 0f, 0.195f * s, 0.180f * s, 0.024f, 12, 0.13f, 0.10f, 0.09f)
        m.addOrientedBox(0.05f * s, 0.030f, -0.04f * s, 0.12f * s, 0.032f, 0.10f * s, rotYDeg = 18f, r = 0.28f, g = 0.26f, b = 0.24f)
        m.addOrientedEllipsoid(-0.07f * s, 0.028f, 0.06f * s, 0.06f * s, 0.022f, 0.05f * s, latSegs = 3, lonSegs = 6, r = 0.24f, g = 0.22f, b = 0.20f)
        val by = 0.036f

        // 2. Dynamic Coiled Digitigrade Hunter Legs + Segmented Carapace Shin Plates (left planted, right coiled in ambush stride)
        // Left Leg (-Z, forward planted on rock)
        m.addOrientedCylinder(0.05f * s, by + 0.015f * s, -0.090f * s, -0.035f * s, by + 0.145f * s, -0.102f * s, 0.026f * s, 0.034f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.035f * s, by + 0.145f * s, -0.102f * s, 0.015f * s, by + 0.260f * s, -0.075f * s, 0.036f * s, 0.048f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(-0.015f * s, by + 0.185f * s, -0.115f * s, 0.054f * s, 0.11f * s, 0.032f * s, rotZDeg = -22f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedEllipsoid(0.075f * s, by + 0.012f * s, -0.090f * s, 0.054f * s, 0.020f * s, 0.036f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)

        // Right Leg (+Z, coiled rear-up)
        m.addOrientedCylinder(-0.045f * s, by + 0.025f * s, 0.095f * s, -0.125f * s, by + 0.145f * s, 0.105f * s, 0.026f * s, 0.034f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
        m.addOrientedCylinder(-0.125f * s, by + 0.145f * s, 0.105f * s, -0.02f * s, by + 0.255f * s, 0.075f * s, 0.036f * s, 0.048f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(-0.07f * s, by + 0.19f * s, 0.118f * s, 0.056f * s, 0.11f * s, 0.032f * s, rotZDeg = 20f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedEllipsoid(-0.025f * s, by + 0.020f * s, 0.095f * s, 0.052f * s, 0.020f * s, 0.034f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)

        // 3. Crouched Agile Flesh Torso & Overlapping Layered Chitin Carapace + Magenta Ridges
        m.addOrientedEllipsoid(0.035f * s, by + 0.285f * s, 0f, 0.135f * s, 0.088f * s, 0.086f * s, rotZDeg = 16f, latSegs = 5, lonSegs = 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedEllipsoid(0.022f * s, by + 0.325f * s, 0f, 0.145f * s, 0.076f * s, 0.096f * s, rotZDeg = 16f, phiEndDeg = 102f, latSegs = 5, lonSegs = 10, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        // Stepped Leviathan Magenta Carapace Ridges
        for (i in 0 until 4) {
            val rx = (-0.06f + i * 0.055f) * s
            val ry = by + (0.365f + i * 0.012f) * s
            m.addOrientedBox(rx, ry, 0f, 0.048f * s, 0.020f * s, 0.112f * s - i * 0.008f * s, rotZDeg = 20f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        }

        // 4. Sweeping Curved Armored Bladed Tail (matching photo!)
        m.addOrientedCylinder(-0.08f * s, by + 0.255f * s, 0f, -0.22f * s, by + 0.195f * s, -0.02f * s, 0.040f * s, 0.026f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedCylinder(-0.22f * s, by + 0.195f * s, -0.02f * s, -0.34f * s, by + 0.145f * s, -0.04f * s, 0.026f * s, 0.016f * s, 8, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedCylinder(-0.34f * s, by + 0.145f * s, -0.04f * s, -0.43f * s, by + 0.215f * s, -0.02f * s, 0.016f * s, 0.004f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)

        // 5. Iconic Von Ryan's Leaper Stalker Skull: Swept Back Crest + ORANGE-CORAL Feeder Tendrils!
        val hx = 0.175f * s
        val hy = by + 0.345f * s
        m.addOrientedEllipsoid(hx, hy, 0f, 0.095f * s, 0.050f * s, 0.052f * s, rotZDeg = -14f, latSegs = 4, lonSegs = 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        // High Swept-Back Lictor/Leaper Chitin Cranial Crest
        m.addOrientedBox(hx - 0.028f * s, hy + 0.044f * s, 0f, 0.140f * s, 0.028f * s, 0.072f * s, rotZDeg = 22f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        m.addOrientedCylinder(hx - 0.01f * s, hy + 0.052f * s, 0f, hx - 0.09f * s, hy + 0.105f * s, 0f, 0.020f * s, 0.005f * s, 6, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        // Fierce Glowing Eyes
        m.addOrientedEllipsoid(hx + 0.055f * s, hy + 0.010f * s, -0.028f * s, 0.015f * s, 0.010f * s, 0.010f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.65f, b = 0.15f, a = E)
        m.addOrientedEllipsoid(hx + 0.055f * s, hy + 0.010f * s, 0.028f * s, 0.015f * s, 0.010f * s, 0.010f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.65f, b = 0.15f, a = E)

        // Signature Bright Orange-Coral Facial Feeder Tendrils (4-tendril undulating cluster hanging forward-down!)
        for (tIdx in 0 until 4) {
            val tz = (tIdx - 1.5f) * 0.022f * s
            val lenScale = if (tIdx == 1 || tIdx == 2) 1.0f else 0.78f
            val mx = hx + 0.078f * s
            val my = hy - 0.025f * s
            val midX = mx + 0.036f * s * lenScale
            val midY = my - 0.048f * s * lenScale
            val tipX = midX + 0.012f * s * lenScale
            val tipY = midY - 0.052f * s * lenScale
            m.addOrientedCylinder(mx, my, tz, midX, midY, tz * 1.2f, 0.014f * s, 0.010f * s, 6, r = TENDRIL_R, g = TENDRIL_G, b = TENDRIL_B)
            m.addOrientedCylinder(midX, midY, tz * 1.2f, tipX, tipY, tz * 0.85f, 0.010f * s, 0.004f * s, 6, r = TENDRIL_R * 1.05f, g = TENDRIL_G * 0.80f, b = TENDRIL_B * 0.75f)
        }

        // 6. Signature Giant Towering Upper Mantis Scything Talons (arching high above shoulders & sweeping down-forward!)
        for (side in floatArrayOf(-1f, 1f)) {
            val shZ = side * 0.088f * s
            val shY = by + 0.355f * s
            val elX = -0.02f * s
            val elY = by + 0.545f * s
            val elZ = side * 0.145f * s
            val wrX = 0.14f * s
            val wrY = by + 0.615f * s
            val wrZ = side * 0.135f * s
            val bladeTipX = 0.33f * s
            val bladeTipY = by + 0.455f * s
            val bladeTipZ = side * 0.095f * s

            // Muscular bone-pink upper mantis limb arching upward-back
            m.addOrientedCylinder(0.05f * s, shY, shZ, elX, elY, elZ, 0.026f * s, 0.022f * s, 6, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            // Raised chitin forearm segment
            m.addOrientedCylinder(elX, elY, elZ, wrX, wrY, wrZ, 0.024f * s, 0.026f * s, 6, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedEllipsoid(wrX, wrY, wrZ, 0.028f * s, 0.024f * s, 0.024f * s, latSegs = 3, lonSegs = 6, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
            // Towering Segmented Mantis Scythe Blade (obsidian base -> crimson tip)
            val midBladeX = (wrX + bladeTipX) * 0.5f + 0.04f * s
            val midBladeY = (wrY + bladeTipY) * 0.5f + 0.04f * s
            val midBladeZ = side * 0.118f * s
            m.addOrientedCylinder(wrX, wrY, wrZ, midBladeX, midBladeY, midBladeZ, 0.028f * s, 0.020f * s, 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
            m.addOrientedCylinder(midBladeX, midBladeY, midBladeZ, bladeTipX, bladeTipY, bladeTipZ, 0.020f * s, 0.004f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        }

        // 7. Lower Curved Razor Mantis Arms ready to strike
        for (side in floatArrayOf(-1f, 1f)) {
            val lshZ = side * 0.082f * s
            val lelX = 0.14f * s
            val lelY = by + 0.245f * s
            val lelZ = side * 0.132f * s
            val lTipX = 0.30f * s
            val lTipY = by + 0.195f * s
            val lTipZ = side * 0.085f * s
            m.addOrientedCylinder(0.08f * s, by + 0.285f * s, lshZ, lelX, lelY, lelZ, 0.022f * s, 0.018f * s, 6, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedCylinder(lelX, lelY, lelZ, lTipX, lTipY, lTipZ, 0.020f * s, 0.004f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        }
    }

    private fun buildGenestealer(m: MeshAccumulator) {
        val s = 1.05f
        // 1. Dark Sector Base Plinth
        m.addCylinder(0f, 0.002f, 0f, 0.175f * s, 0.16f * s, 0.024f, 10, 0.12f, 0.12f, 0.14f)
        val by = 0.024f

        val skinR = 0.88f; val skinG = 0.84f; val skinB = 0.82f
        val skinShR = 0.72f; val skinShG = 0.65f; val skinShB = 0.75f
        val purpleR = 0.42f; val purpleG = 0.18f; val purpleB = 0.58f
        val purpleHiR = 0.58f; val purpleHiG = 0.28f; val purpleHiB = 0.74f
        val tongueR = 0.92f; val tongueG = 0.26f; val tongueB = 0.38f
        val talonDkR = 0.22f; val talonDkG = 0.18f; val talonDkB = 0.22f
        val talonTipR = 0.12f; val talonTipG = 0.08f; val talonTipB = 0.10f

        // 2. Crouched Muscular Pale-Flesh Legs & Clawed Feet
        for (side in floatArrayOf(-1f, 1f)) {
            val z = side * 0.092f * s
            m.addOrientedCylinder(-0.02f * s, by + 0.018f * s, z, -0.085f * s, by + 0.135f * s, z, 0.028f * s, 0.036f * s, 8, r = skinShR, g = skinShG, b = skinShB)
            m.addOrientedCylinder(-0.085f * s, by + 0.135f * s, z, -0.01f * s, by + 0.25f * s, z * 0.82f, 0.036f * s, 0.046f * s, 8, r = skinR, g = skinG, b = skinB)
            m.addOrientedEllipsoid(0.01f * s, by + 0.016f * s, z, 0.054f * s, 0.020f * s, 0.038f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = talonDkR, g = talonDkG, b = talonDkB)
        }

        // 3. Hunched Prowling Torso & Layered Deep Purple Carapace Shell Plates
        m.addOrientedEllipsoid(0.035f * s, by + 0.265f * s, 0f, 0.130f * s, 0.085f * s, 0.088f * s, rotZDeg = 14f, latSegs = 5, lonSegs = 10, r = skinR, g = skinG, b = skinB)
        m.addOrientedEllipsoid(0.018f * s, by + 0.305f * s, 0f, 0.140f * s, 0.075f * s, 0.098f * s, rotZDeg = 14f, phiEndDeg = 100f, latSegs = 5, lonSegs = 10, r = purpleR, g = purpleG, b = purpleB)
        // Stepped Purple Dorsal Carapace Flared Shell Segments (matching photo!)
        for (i in 0 until 4) {
            val rx = (-0.055f + i * 0.050f) * s
            val ry = by + (0.355f + i * 0.012f) * s
            m.addOrientedBox(rx, ry, 0f, 0.052f * s, 0.022f * s, 0.125f * s - i * 0.008f * s, rotZDeg = 16f, r = purpleHiR, g = purpleHiG, b = purpleHiB)
        }

        // 4. Iconic Genestealer Skull: Sloped Head, Purple Ridges, Gaping Jaw + Long Hanging Pink Tongue & Glowing Eyes
        val hx = 0.165f * s
        val hy = by + 0.325f * s
        m.addOrientedEllipsoid(hx, hy, 0f, 0.092f * s, 0.052f * s, 0.054f * s, rotZDeg = -10f, latSegs = 4, lonSegs = 8, r = skinR, g = skinG, b = skinB)
        // Purple Head Crest Ridges
        m.addOrientedBox(hx - 0.02f * s, hy + 0.042f * s, 0f, 0.075f * s, 0.022f * s, 0.048f * s, rotZDeg = 18f, r = purpleR, g = purpleG, b = purpleB)
        m.addOrientedBox(hx + 0.015f * s, hy + 0.048f * s, 0f, 0.055f * s, 0.020f * s, 0.038f * s, rotZDeg = 12f, r = purpleHiR, g = purpleHiG, b = purpleHiB)
        // Glowing Eyes
        m.addOrientedEllipsoid(hx + 0.048f * s, hy + 0.012f * s, -0.026f * s, 0.014f * s, 0.010f * s, 0.010f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.55f, b = 0.15f, a = E)
        m.addOrientedEllipsoid(hx + 0.048f * s, hy + 0.012f * s, 0.026f * s, 0.014f * s, 0.010f * s, 0.010f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.55f, b = 0.15f, a = E)
        // Signature Long Hanging Pink/Purple Tongue
        m.addOrientedCylinder(hx + 0.06f * s, hy - 0.012f * s, 0f, hx + 0.115f * s, hy - 0.068f * s, 0f, 0.018f * s, 0.010f * s, 6, r = tongueR, g = tongueG, b = tongueB)
        m.addOrientedCylinder(hx + 0.115f * s, hy - 0.068f * s, 0f, hx + 0.145f * s, hy - 0.105f * s, 0f, 0.010f * s, 0.004f * s, 6, r = tongueR * 0.9f, g = tongueG * 0.8f, b = tongueB * 0.9f)

        // 5. 4 Wide-Spread Rending Claw Arms (Upper Mantis Sweep + Lower Reaching Claws)
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.115f * s
            // Upper Scything Rending Arms
            m.addOrientedCylinder(0.06f * s, by + 0.285f * s, sz, 0.17f * s, by + 0.355f * s, sz * 1.55f, 0.026f * s, 0.022f * s, 6, r = skinR, g = skinG, b = skinB)
            m.addOrientedCylinder(0.17f * s, by + 0.355f * s, sz * 1.55f, 0.31f * s, by + 0.315f * s, sz * 1.25f, 0.022f * s, 0.005f * s, 6, r = talonTipR, g = talonTipG, b = talonTipB)
            // Lower Reaching Rending Arms
            m.addOrientedCylinder(0.07f * s, by + 0.215f * s, sz * 0.88f, 0.19f * s, by + 0.185f * s, sz * 1.35f, 0.024f * s, 0.018f * s, 6, r = skinShR, g = skinShG, b = skinShB)
            m.addOrientedCylinder(0.19f * s, by + 0.185f * s, sz * 1.35f, 0.32f * s, by + 0.22f * s, sz * 1.05f, 0.018f * s, 0.005f * s, 6, r = talonDkR, g = talonDkG, b = talonDkB)
        }
    }

    private fun buildCarnifex(m: MeshAccumulator) {
        val s = 1.50f
        // Wide Heavy Monster Base
        m.addCylinder(0f, 0.002f, 0f, 0.26f * s, 0.245f * s, 0.028f, 14, 0.13f, 0.09f, 0.13f)
        val by = 0.028f

        // Squat Heavy Siege Pillars (thick legs)
        for (side in floatArrayOf(-1f, 1f)) {
            val z = side * 0.155f * s
            m.addOrientedCylinder(-0.02f * s, by + 0.02f * s, z, -0.07f * s, by + 0.145f * s, z, 0.048f * s, 0.056f * s, 10, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
            m.addOrientedCylinder(-0.07f * s, by + 0.145f * s, z, 0.01f * s, by + 0.26f * s, z * 0.85f, 0.060f * s, 0.074f * s, 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedBox(0.01f * s, by + 0.024f * s, z, 0.095f * s, 0.036f * s, 0.072f * s, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }

        // Massive Domed Armored Carapace & Broad Siege Thorax
        m.addOrientedEllipsoid(0.02f * s, by + 0.275f * s, 0f, 0.195f * s, 0.125f * s, 0.165f * s, rotZDeg = -10f, latSegs = 6, lonSegs = 12, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedEllipsoid(0f, by + 0.340f * s, 0f, 0.220f * s, 0.115f * s, 0.195f * s, rotZDeg = -12f, phiEndDeg = 104f, latSegs = 6, lonSegs = 12, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        // Stepped Magenta Dorsal Armor Ridges
        for (i in 0 until 3) {
            m.addOrientedBox((-0.09f + i * 0.07f) * s, by + (0.39f + i * 0.02f) * s, 0f, 0.085f * s, 0.028f * s, 0.22f * s - i * 0.03f * s, rotZDeg = -12f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        }

        // Dorsal Spine Banks Cluster (Bio-Weapon Pods)
        for (side in floatArrayOf(-1f, 1f)) {
            for (row in 0 until 2) {
                m.addOrientedCylinder(-0.06f * s + row * 0.06f * s, by + 0.40f * s, side * 0.08f * s, -0.10f * s + row * 0.05f * s, by + 0.54f * s, side * 0.125f * s, 0.024f * s, 0.007f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
            }
        }

        // Heavy Clubbed Ram Tail
        m.addOrientedCylinder(-0.14f * s, by + 0.22f * s, 0f, -0.32f * s, by + 0.16f * s, 0f, 0.062f * s, 0.036f * s, 8, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)
        m.addOrientedEllipsoid(-0.34f * s, by + 0.17f * s, 0f, 0.068f * s, 0.048f * s, 0.062f * s, latSegs = 4, lonSegs = 8, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)

        // Armored Battering Ram Head + Twin Curved Lower Tusks
        m.addOrientedEllipsoid(0.205f * s, by + 0.29f * s, 0f, 0.108f * s, 0.074f * s, 0.092f * s, latSegs = 4, lonSegs = 8, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        m.addOrientedEllipsoid(0.27f * s, by + 0.305f * s, -0.042f * s, 0.018f * s, 0.014f * s, 0.014f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.50f, b = 0.08f, a = E)
        m.addOrientedEllipsoid(0.27f * s, by + 0.305f * s, 0.042f * s, 0.018f * s, 0.014f * s, 0.014f * s, latSegs = 3, lonSegs = 6, r = 1.0f, g = 0.50f, b = 0.08f, a = E)
        for (side in floatArrayOf(-1f, 1f)) {
            m.addOrientedCylinder(0.23f * s, by + 0.24f * s, side * 0.056f * s, 0.35f * s, by + 0.32f * s, side * 0.078f * s, 0.022f * s, 0.005f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        }

        // Bulky Siege CRUSHING CLAWS
        for (side in floatArrayOf(-1f, 1f)) {
            val sz = side * 0.175f * s
            m.addOrientedCylinder(0.08f * s, by + 0.28f * s, sz, 0.21f * s, by + 0.23f * s, sz * 1.18f, 0.046f * s, 0.052f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedBox(0.28f * s, by + 0.22f * s, sz * 1.08f, 0.16f * s, 0.095f * s, 0.082f * s, rotYDeg = -side * 12f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
            // Twin Pincer Mandibles of Crushing Claw
            m.addOrientedCylinder(0.34f * s, by + 0.25f * s, sz * 1.08f, 0.45f * s, by + 0.26f * s, sz * 0.92f, 0.024f * s, 0.005f * s, 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
            m.addOrientedCylinder(0.34f * s, by + 0.19f * s, sz * 1.08f, 0.44f * s, by + 0.21f * s, sz * 0.96f, 0.020f * s, 0.005f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        }
    }

    private fun buildHiveTyrant(m: MeshAccumulator) {
        val s = 1.62f
        m.addCylinder(0f, 0.002f, 0f, 0.25f * s, 0.23f * s, 0.028f, 14, 0.14f, 0.08f, 0.16f)
        val by = 0.028f

        // Towering Sovereign Legs
        for (side in floatArrayOf(-1f, 1f)) {
            val z = side * 0.125f * s
            m.addOrientedCylinder(-0.02f * s, by + 0.02f * s, z, -0.075f * s, by + 0.17f * s, z, 0.036f * s, 0.046f * s, 8, r = FLESH_SHADE_R, g = FLESH_SHADE_G, b = FLESH_SHADE_B)
            m.addOrientedCylinder(-0.075f * s, by + 0.17f * s, z, 0f, by + 0.31f * s, z * 0.82f, 0.048f * s, 0.060f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
            m.addOrientedEllipsoid(0.015f * s, by + 0.018f * s, z, 0.070f * s, 0.024f * s, 0.045f * s, phiEndDeg = 90f, latSegs = 3, lonSegs = 6, r = TALON_DK_R, g = TALON_DK_G, b = TALON_DK_B)
        }

        // Upright Regnal Thorax & Chitin Ribcage
        m.addOrientedEllipsoid(0f, by + 0.40f * s, 0f, 0.120f * s, 0.165f * s, 0.125f * s, rotZDeg = -6f, latSegs = 5, lonSegs = 10, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(-0.02f * s, by + 0.42f * s, 0f, 0.14f * s, 0.26f * s, 0.21f * s, rotZDeg = -8f, r = CHITIN_R, g = CHITIN_G, b = CHITIN_B)

        // Towering Hive Tyrant Crown Crest + Psychic Glowing Horn Tips!
        val hx = 0.08f * s
        val hy = by + 0.56f * s
        m.addOrientedEllipsoid(hx, hy, 0f, 0.088f * s, 0.064f * s, 0.068f * s, latSegs = 4, lonSegs = 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(hx - 0.01f * s, hy + 0.055f * s, 0f, 0.16f * s, 0.045f * s, 0.15f * s, rotZDeg = 20f, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
        // Crown Horns with glowing psychic violet energy
        m.addOrientedCylinder(hx + 0.08f * s, hy + 0.05f * s, 0f, hx + 0.18f * s, hy + 0.18f * s, 0f, 0.022f * s, 0.006f * s, 6, r = 0.85f, g = 0.35f, b = 1.0f, a = E)
        m.addOrientedCylinder(hx + 0.04f * s, hy + 0.05f * s, -0.05f * s, hx + 0.08f * s, hy + 0.16f * s, -0.09f * s, 0.016f * s, 0.005f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        m.addOrientedCylinder(hx + 0.04f * s, hy + 0.05f * s, 0.05f * s, hx + 0.08f * s, hy + 0.16f * s, 0.09f * s, 0.016f * s, 0.005f * s, 6, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)
        m.addOrientedEllipsoid(hx + 0.068f * s, hy + 0.01f * s, -0.032f * s, 0.016f * s, 0.012f * s, 0.012f * s, latSegs = 3, lonSegs = 6, r = 0.85f, g = 0.35f, b = 1.0f, a = E)
        m.addOrientedEllipsoid(hx + 0.068f * s, hy + 0.01f * s, 0.032f * s, 0.016f * s, 0.012f * s, 0.012f * s, latSegs = 3, lonSegs = 6, r = 0.85f, g = 0.35f, b = 1.0f, a = E)

        // Sweeping Giant Winged Tyrant Bio-Wings
        for (side in floatArrayOf(-1f, 1f)) {
            m.addOrientedCylinder(-0.04f * s, by + 0.46f * s, side * 0.09f * s, -0.18f * s, by + 0.72f * s, side * 0.36f * s, 0.032f * s, 0.016f * s, 6, r = CHITIN_HI_R, g = CHITIN_HI_G, b = CHITIN_HI_B)
            m.addOrientedBox(-0.14f * s, by + 0.56f * s, side * 0.28f * s, 0.22f * s, 0.34f * s, 0.018f * s, rotXDeg = side * 26f, rotYDeg = side * 18f, r = 0.32f, g = 0.12f, b = 0.38f)
        }

        // Right Arm (+Z): Monstrous Bonesword; Left Arm (-Z): Coiled Lash Whip
        m.addOrientedCylinder(0.03f * s, by + 0.45f * s, 0.15f * s, 0.16f * s, by + 0.40f * s, 0.19f * s, 0.034f * s, 0.026f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addOrientedBox(0.31f * s, by + 0.50f * s, 0.18f * s, 0.38f * s, 0.060f * s, 0.022f * s, rotZDeg = 32f, r = TALON_TIP_R, g = TALON_TIP_G, b = TALON_TIP_B)

        m.addOrientedCylinder(0.03f * s, by + 0.45f * s, -0.15f * s, 0.15f * s, by + 0.38f * s, -0.19f * s, 0.032f * s, 0.024f * s, 8, r = FLESH_R, g = FLESH_G, b = FLESH_B)
        m.addArcTube(0.24f * s, by + 0.35f * s, -0.22f * s, 0.11f * s, 0.09f * s, 0.014f * s, rotXDeg = 22f, startDeg = -130f, endDeg = 130f, arcSegs = 10, tubeSegs = 5, r = TENDRIL_R, g = TENDRIL_G, b = TENDRIL_B)
    }
}
