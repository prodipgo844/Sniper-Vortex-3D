package com.example.game;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class GamePrefs {
    private static final String PREF_NAME = "SniperTacticalPrefs";
    private static final String KEY_CASH = "player_cash";
    private static final String KEY_SELECTED_WEAPON = "selected_weapon";
    private static final String KEY_COMPLETED_LEVELS = "completed_levels";
    
    private final SharedPreferences prefs;

    public GamePrefs(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize default cash on very first run
        if (!prefs.contains(KEY_CASH)) {
            prefs.edit().putInt(KEY_CASH, 1000).apply(); // $1000 starter credits
        }
    }

    public int getCash() {
        return prefs.getInt(KEY_CASH, 1000);
    }

    public void addCash(int amount) {
        prefs.edit().putInt(KEY_CASH, getCash() + amount).apply();
    }

    public boolean spendCash(int amount) {
        int current = getCash();
        if (current >= amount) {
            prefs.edit().putInt(KEY_CASH, current - amount).apply();
            return true;
        }
        return false;
    }

    public String getSelectedWeaponId() {
        return prefs.getString(KEY_SELECTED_WEAPON, "m24");
    }

    public void setSelectedWeaponId(String weaponId) {
        prefs.edit().putString(KEY_SELECTED_WEAPON, weaponId).apply();
    }

    public boolean isWeaponUnlocked(String weaponId) {
        if (weaponId.equals("m24")) return true; // starter unlocked
        return prefs.getBoolean("unlocked_" + weaponId, false);
    }

    public void unlockWeapon(String weaponId) {
        prefs.edit().putBoolean("unlocked_" + weaponId, true).apply();
    }

    public int getStatUpgradeLevel(String weaponId, String statType) {
        return prefs.getInt("upgrade_" + weaponId + "_" + statType, 1); // levels 1 to 5
    }

    public void incrementStatUpgradeLevel(String weaponId, String statType) {
        int current = getStatUpgradeLevel(weaponId, statType);
        if (current < 5) {
            prefs.edit().putInt("upgrade_" + weaponId + "_" + statType, current + 1).apply();
        }
    }

    public Set<String> getCompletedLevels() {
        return prefs.getStringSet(KEY_COMPLETED_LEVELS, new HashSet<>());
    }

    public void completeLevel(int levelId) {
        Set<String> completed = new HashSet<>(getCompletedLevels());
        completed.add(String.valueOf(levelId));
        prefs.edit().putStringSet(KEY_COMPLETED_LEVELS, completed).apply();
    }

    public boolean isLevelUnlocked(int levelId) {
        if (levelId == 1) return true;
        return getCompletedLevels().contains(String.valueOf(levelId - 1));
    }
}
