package com.example.game;

import java.util.ArrayList;
import java.util.List;

public class Weapon {
    public final String id;
    public final String name;
    public final String description;
    public final int basePrice;
    public final float baseDamage;
    public final float baseMaxZoom;
    public final float baseStability;
    public final float baseVelocity;
    public final int colorHex;

    public Weapon(String id, String name, String description, int basePrice, 
                  float baseDamage, float baseMaxZoom, float baseStability, 
                  float baseVelocity, int colorHex) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.baseDamage = baseDamage;
        this.baseMaxZoom = baseMaxZoom;
        this.baseStability = baseStability;
        this.baseVelocity = baseVelocity;
        this.colorHex = colorHex;
    }

    public static final List<Weapon> ALL_WEAPONS = new ArrayList<>();
    static {
        ALL_WEAPONS.add(new Weapon(
            "m24",
            "M24 Sniper Weapon System",
            "Military-grade bolt-action sniper. Highly accurate starter rifle. Reliable in standard conditions.",
            0,
            50f,
            4.0f,
            0.40f,
            700f,
            0xFF5D6B54 // Olive Drab
        ));
        ALL_WEAPONS.add(new Weapon(
            "awm",
            "AWM Dragon Hawk",
            "Legendary sniper rifle chambered in .338 Lapua Magnum. Superior zoom and devastating knockdown.",
            1200,
            85f,
            8.0f,
            0.55f,
            850f,
            0xFF354B45 // Deep Teal Camo
        ));
        ALL_WEAPONS.add(new Weapon(
            "m200",
            "CheyTac M200 Intervention",
            "The king of long-range performance. Features advanced stability dampers and extreme flat trajectory.",
            3500,
            110f,
            12.0f,
            0.75f,
            1000f,
            0xFF4A4B54 // Urban Gray
        ));
        ALL_WEAPONS.add(new Weapon(
            "barrett",
            "Barrett M82 .50 Cal",
            "Semi-automatic anti-materiel cannon. Explodes targets on impact. Heavy recoil but unmatched raw fire power.",
            6500,
            175f,
            10.0f,
            0.45f,
            900f,
            0xFF2B2B2C // Matte Obsidian Black
        ));
    }

    public static Weapon getWeaponById(String id) {
        for (Weapon w : ALL_WEAPONS) {
            if (w.id.equals(id)) return w;
        }
        return ALL_WEAPONS.get(0);
    }

    public static int getUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 250;
            case 2: return 500;
            case 3: return 900;
            case 4: return 1500;
            default: return 0;
        }
    }

    public float getDamage(int level) {
        return baseDamage * (1.0f + (level - 1) * 0.15f);
    }

    public float getMaxZoom(int level) {
        return baseMaxZoom + (level - 1) * 2.0f;
    }

    public float getStability(int level) {
        float stab = baseStability + (level - 1) * 0.10f;
        if (stab < 0.1f) return 0.1f;
        if (stab > 0.98f) return 0.98f;
        return stab;
    }

    public float getVelocity(int level) {
        return baseVelocity * (1.0f + (level - 1) * 0.08f);
    }
}
