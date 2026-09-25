package com.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.game.GamePrefs;
import com.example.game.Mission;
import com.example.game.Sniper3DView;
import com.example.game.SoundManager;
import com.example.game.Weapon;

public class MainActivity extends Activity {

    private GamePrefs gamePrefs;
    private SoundManager soundManager;
    private Weapon selectedWeapon;

    // Track chosen 3D Environments and Modes
    private String activeEnvironment = "JUNGLE"; // "JUNGLE" or "INDOOR"
    private String activeMode = "PRACTICE";       // "PRACTICE" or "COMBAT"

    // Tracker for active tab in armory
    private String armoryActiveTabId = "m24";
    private boolean isReloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Persistent Configurations
        gamePrefs = new GamePrefs(this);
        soundManager = new SoundManager(this);
        
        selectedWeapon = Weapon.getWeaponById(gamePrefs.getSelectedWeaponId());
        
        // Infiltrate Main Menu Selection deck
        loadMenuScreen();
    }

    @Override
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        loadMenuScreen();
    }

    // ============================================================================
    // 1. SCREEN SWITCH CONTROLLERS
    // ============================================================================

    private void loadMenuScreen() {
        setContentView(R.layout.activity_main);

        // Environment Selectors
        final Button btnJungle = findViewById(R.id.btn_select_jungle);
        final Button btnIndoor = findViewById(R.id.btn_select_indoor);

        updateSelectionButtonState(btnJungle, btnIndoor, activeEnvironment.equals("JUNGLE"));
        btnJungle.setOnClickListener(v -> {
            activeEnvironment = "JUNGLE";
            updateSelectionButtonState(btnJungle, btnIndoor, true);
        });
        btnIndoor.setOnClickListener(v -> {
            activeEnvironment = "INDOOR";
            updateSelectionButtonState(btnJungle, btnIndoor, false);
        });

        // Mode Selectors
        final Button btnPractice = findViewById(R.id.btn_select_practice);
        final Button btnCombat = findViewById(R.id.btn_select_combat);

        updateSelectionButtonState(btnPractice, btnCombat, activeMode.equals("PRACTICE"));
        btnPractice.setOnClickListener(v -> {
            activeMode = "PRACTICE";
            updateSelectionButtonState(btnPractice, btnCombat, true);
        });
        btnCombat.setOnClickListener(v -> {
            activeMode = "COMBAT";
            updateSelectionButtonState(btnPractice, btnCombat, false);
        });

        // Main action triggers
        Button btnStart3D = findViewById(R.id.btn_menu_campaign);
        btnStart3D.setOnClickListener(v -> loadGame3DScreen());

        Button btnArmory = findViewById(R.id.btn_menu_armory);
        btnArmory.setOnClickListener(v -> {
            armoryActiveTabId = gamePrefs.getSelectedWeaponId();
            loadArmoryScreen();
        });
    }

    private void updateSelectionButtonState(Button btnActive, Button btnInactive, boolean isActive) {
        if (isActive) {
            btnActive.setBackgroundColor(getResources().getColor(R.color.tactical_green));
            btnActive.setTextColor(getResources().getColor(R.color.gunmetal_black));
            btnInactive.setBackgroundColor(getResources().getColor(R.color.gunmetal_gray));
            btnInactive.setTextColor(Color.WHITE);
        } else {
            btnInactive.setBackgroundColor(getResources().getColor(R.color.tactical_green));
            btnInactive.setTextColor(getResources().getColor(R.color.gunmetal_black));
            btnActive.setBackgroundColor(getResources().getColor(R.color.gunmetal_gray));
            btnActive.setTextColor(Color.WHITE);
        }
    }

    private void loadArmoryScreen() {
        setContentView(R.layout.layout_armory);

        final Weapon currentEquipped = Weapon.getWeaponById(gamePrefs.getSelectedWeaponId());
        final Weapon activeWeapon = Weapon.getWeaponById(armoryActiveTabId);
        final boolean isUnlocked = gamePrefs.isWeaponUnlocked(activeWeapon.id);

        ImageButton btnBack = findViewById(R.id.btn_armory_back);
        btnBack.setOnClickListener(v -> loadMenuScreen());

        TextView txtTitle = findViewById(R.id.txt_armory_title);
        txtTitle.setText(activeWeapon.name.toUpperCase());
        txtTitle.setTextColor(activeWeapon.colorHex);

        TextView txtDesc = findViewById(R.id.txt_armory_desc);
        txtDesc.setText(activeWeapon.description);

        TextView equippedTag = findViewById(R.id.txt_armory_equipped_tag);
        if (currentEquipped.id.equals(activeWeapon.id)) {
            equippedTag.setVisibility(View.VISIBLE);
        } else {
            equippedTag.setVisibility(View.GONE);
        }

        bindArmoryTab(R.id.tab_weapon_m24, "m24");
        bindArmoryTab(R.id.tab_weapon_awm, "awm");
        bindArmoryTab(R.id.tab_weapon_m200, "m200");
        bindArmoryTab(R.id.tab_weapon_barrett, "barrett");

        LinearLayout upgradesPanel = findViewById(R.id.layout_upgrades_panel);
        LinearLayout lockedPanel = findViewById(R.id.layout_locked_panel);
        Button btnEquip = findViewById(R.id.btn_armory_equip);

        if (isUnlocked) {
            upgradesPanel.setVisibility(View.VISIBLE);
            lockedPanel.setVisibility(View.GONE);
            btnEquip.setVisibility(View.VISIBLE);

            if (currentEquipped.id.equals(activeWeapon.id)) {
                btnEquip.setEnabled(false);
                btnEquip.setText("CURRENTLY EQUIPPED");
            } else {
                btnEquip.setEnabled(true);
                btnEquip.setText("EQUIP THIS SNIPER");
                btnEquip.setOnClickListener(v -> {
                    gamePrefs.setSelectedWeaponId(activeWeapon.id);
                    selectedWeapon = activeWeapon;
                    soundManager.playReload();
                    loadArmoryScreen(); // Refresh equipped state
                });
            }

            populateUpgradesSection(activeWeapon);

        } else {
            upgradesPanel.setVisibility(View.GONE);
            lockedPanel.setVisibility(View.VISIBLE);
            btnEquip.setVisibility(View.GONE);

            Button btnUnlock = findViewById(R.id.btn_armory_unlock);
            btnUnlock.setEnabled(true);
            btnUnlock.setOnClickListener(v -> {
                gamePrefs.unlockWeapon(activeWeapon.id);
                gamePrefs.setSelectedWeaponId(activeWeapon.id);
                selectedWeapon = activeWeapon;
                soundManager.playSuccess();
                loadArmoryScreen(); // Unlock Refresh
            });
        }
    }

    private void bindArmoryTab(int btnId, final String weaponId) {
        Button btn = findViewById(btnId);
        if (armoryActiveTabId.equals(weaponId)) {
            btn.setBackgroundColor(getResources().getColor(R.color.tactical_green));
            btn.setTextColor(getResources().getColor(R.color.gunmetal_black));
        } else {
            btn.setBackgroundColor(getResources().getColor(R.color.gunmetal_gray));
            btn.setTextColor(Color.WHITE);
        }
        btn.setOnClickListener(v -> {
            armoryActiveTabId = weaponId;
            loadArmoryScreen();
        });
    }

    private void populateUpgradesSection(final Weapon activeWeapon) {
        LinearLayout container = findViewById(R.id.container_upgrades);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String[] statTypes = {"damage", "zoom", "stability", "range"};
        String[] statLabels = {
            "DAMAGE IMPACT (+15% / LVL)",
            "SCOPE MAGNIFICATION (+2x / LVL)",
            "SWAY STABILITY (+12% / LVL)",
            "VELOCITY RANGE (+8% / LVL)"
        };

        for (int i = 0; i < statTypes.length; i++) {
            final String stat = statTypes[i];
            String label = statLabels[i];

            View row = inflater.inflate(R.layout.item_upgrade_row, container, false);

            TextView txtLabel = row.findViewById(R.id.txt_upgrade_label);
            txtLabel.setText(label);

            final int currentLevel = gamePrefs.getStatUpgradeLevel(activeWeapon.id, stat);

            fillUpgradeNotches(row, currentLevel);

            Button btnUpgrade = row.findViewById(R.id.btn_upgrade_action);
            TextView txtMaxed = row.findViewById(R.id.txt_upgrade_maxed);

            if (currentLevel < 5) {
                btnUpgrade.setText("UPGRADE SYSTEM");
                btnUpgrade.setEnabled(true);
                btnUpgrade.setOnClickListener(v -> {
                    gamePrefs.incrementStatUpgradeLevel(activeWeapon.id, stat);
                    soundManager.playSuccess();
                    loadArmoryScreen(); // Refresh upgrades
                });
                btnUpgrade.setVisibility(View.VISIBLE);
                txtMaxed.setVisibility(View.GONE);
            } else {
                btnUpgrade.setVisibility(View.GONE);
                txtMaxed.setVisibility(View.VISIBLE);
            }

            container.addView(row);
        }
    }

    private void fillUpgradeNotches(View row, int level) {
        int[] notchIds = {R.id.notch_1, R.id.notch_2, R.id.notch_3, R.id.notch_4, R.id.notch_5};
        for (int idx = 0; idx < 5; idx++) {
            View notch = row.findViewById(notchIds[idx]);
            if (idx < level) {
                notch.setBackgroundColor(getResources().getColor(R.color.tactical_green));
            } else {
                notch.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void loadGame3DScreen() {
        setContentView(R.layout.layout_game_3d);

        final Sniper3DView gameView = findViewById(R.id.sniper_game_view);
        final TextView txtWind = findViewById(R.id.txt_game_wind);
        final TextView txtDistance = findViewById(R.id.txt_game_distance);
        final ProgressBar progressBreath = findViewById(R.id.progress_breath);
        final Button btnBreath = findViewById(R.id.btn_game_breath);
        final SeekBar seekZoom = findViewById(R.id.seekbar_zoom);
        final TextView txtZoom = findViewById(R.id.txt_game_zoom);
        final TextView txtAmmo = findViewById(R.id.txt_game_ammo);
        final ImageButton btnReload = findViewById(R.id.btn_game_reload);
        final Button btnFire = findViewById(R.id.btn_game_fire);
        final LinearLayout reloadOverlay = findViewById(R.id.layout_reload_overlay);
        final LinearLayout alertLayout = findViewById(R.id.layout_game_alert);
        final TextView alertText = findViewById(R.id.txt_game_alert);
        ImageButton btnAbort = findViewById(R.id.btn_game_abort);

        // Bind Abort triggers
        btnAbort.setOnClickListener(v -> {
            gameView.stopLoop();
            loadMenuScreen();
        });

        // Set wind conditions dynamically based on Jungle vs Indoor
        float windSpeed = activeEnvironment.equals("JUNGLE") ? 5.0f : 1.2f;
        txtWind.setText("WIND: " + windSpeed + " mph EAST");
        txtDistance.setText("RNG: 250m");

        // Set Zoom seekbar slider range
        float maxZoom = selectedWeapon.getMaxZoom(gamePrefs.getStatUpgradeLevel(selectedWeapon.id, "zoom"));
        seekZoom.setMax((int) (maxZoom - 2f) * 10);
        seekZoom.setProgress((int) (gameView.getActiveZoom() - 2f) * 10);
        txtZoom.setText("ZOOM: " + String.format("%.1f", gameView.getActiveZoom()) + "x");
        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float calculatedZoom = 2.0f + (progress / 10f);
                gameView.setActiveZoom(calculatedZoom);
                txtZoom.setText("ZOOM: " + String.format("%.1f", calculatedZoom) + "x");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Breath trigger click bindings
        btnBreath.setOnClickListener(v -> {
            boolean currentHold = btnBreath.getText().toString().equals("STABILIZED");
            gameView.setHoldBreath(!currentHold);
        });

        // Reload triggers
        btnReload.setOnClickListener(v -> gameView.performReload());

        // Fire triggers
        btnFire.setOnClickListener(v -> gameView.fire3DWeapon());

        // Launch Game Loop View
        gameView.setCallback(new Sniper3DView.GameCallback() {
            @Override
            public void onAmmoCountChanged(int ammo) {
                txtAmmo.setText("AMMO: " + ammo + " / 5");
                btnFire.setEnabled(ammo > 0 && !isReloading);
            }

            @Override
            public void onReloadStatusChanged(boolean reloading) {
                isReloading = reloading;
                if (reloading) {
                    reloadOverlay.setVisibility(View.VISIBLE);
                    btnFire.setEnabled(false);
                } else {
                    reloadOverlay.setVisibility(View.GONE);
                    txtAmmo.setText("AMMO: " + 5 + " / 5");
                    btnFire.setEnabled(true);
                }
            }

            @Override
            public void onBreathChanged(float stamina, boolean stabilized) {
                progressBreath.setProgress((int) (stamina * 100));
                if (stabilized) {
                    btnBreath.setText("STABILIZED");
                    btnBreath.setBackgroundColor(getResources().getColor(R.color.tactical_green));
                    btnBreath.setTextColor(getResources().getColor(R.color.gunmetal_black));
                } else {
                    btnBreath.setText("HOLD BREATH");
                    btnBreath.setBackgroundColor(getResources().getColor(R.color.gunmetal_gray));
                    btnBreath.setTextColor(Color.WHITE);
                }
            }

            @Override
            public void onAlertStatusChanged(boolean alerted, float timeLeft) {
                if (alerted) {
                    alertLayout.setVisibility(View.VISIBLE);
                    alertText.setText("WARNING: TARGET FLIGHT IN: " + String.format("%.1f", timeLeft) + "s");
                } else {
                    alertLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLevelComplete(String outcome, int cashEarned) {
                loadScoreboardScreen(outcome, cashEarned);
            }
        });
        gameView.initGame(activeEnvironment, activeMode, gamePrefs, soundManager);
    }

    private void loadScoreboardScreen(final String outcome, final int cashEarned) {
        setContentView(R.layout.layout_scoreboard);

        TextView txtOutcome = findViewById(R.id.txt_scoreboard_outcome);
        TextView txtStatus = findViewById(R.id.txt_score_status);
        LinearLayout badgeBg = findViewById(R.id.layout_scoreboard_badge_bg);
        TextView badgeChar = findViewById(R.id.txt_scoreboard_badge_char);

        Button btnExit = findViewById(R.id.btn_score_exit);
        Button btnAction = findViewById(R.id.btn_score_action);

        boolean isSuccess = outcome.equals("HEADSHOT") || outcome.equals("BODYSHOT");

        // Format outcome strings
        if (outcome.equals("HEADSHOT")) {
            txtOutcome.setText("CRITICAL HEADSHOT KILL");
            txtOutcome.setTextColor(getResources().getColor(R.color.tactical_green));
        } else if (outcome.equals("BODYSHOT")) {
            txtOutcome.setText("BODYSHOT ELIMINATION");
            txtOutcome.setTextColor(getResources().getColor(R.color.tactical_green));
        } else if (outcome.equals("CIVILIAN_HIT")) {
            txtOutcome.setText("COLLATERAL CIVILIAN HIT");
            txtOutcome.setTextColor(getResources().getColor(R.color.laser_crimson));
        } else if (outcome.equals("ALERT_ESCAPED")) {
            txtOutcome.setText("TARGET ESCAPED SECTOR");
            txtOutcome.setTextColor(getResources().getColor(R.color.laser_crimson));
        } else {
            txtOutcome.setText("TACTICAL OPERATIVE MISS");
            txtOutcome.setTextColor(getResources().getColor(R.color.laser_crimson));
        }

        txtStatus.setText(isSuccess ? "SUCCESS" : "FAILED");
        txtStatus.setTextColor(isSuccess ? getResources().getColor(R.color.tactical_green) : getResources().getColor(R.color.laser_crimson));

        if (isSuccess) {
            badgeChar.setText("✓");
            badgeChar.setTextColor(getResources().getColor(R.color.tactical_green));
        } else {
            badgeChar.setText("⚠");
            badgeChar.setTextColor(getResources().getColor(R.color.laser_crimson));
        }

        btnExit.setOnClickListener(v -> loadMenuScreen());

        if (isSuccess) {
            btnAction.setText("NEXT THEATER");
            btnAction.setBackgroundColor(getResources().getColor(R.color.tactical_green));
            btnAction.setTextColor(getResources().getColor(R.color.gunmetal_black));
            btnAction.setOnClickListener(v -> {
                // Switch environmental theater after successful hit
                if (activeEnvironment.equals("JUNGLE")) {
                    activeEnvironment = "INDOOR";
                } else {
                    activeEnvironment = "JUNGLE";
                }
                loadMenuScreen();
            });
        } else {
            btnAction.setText("REDEPLOY NOW");
            btnAction.setBackgroundColor(getResources().getColor(R.color.laser_crimson));
            btnAction.setTextColor(Color.WHITE);
            btnAction.setOnClickListener(v -> loadGame3DScreen());
        }
    }
}
