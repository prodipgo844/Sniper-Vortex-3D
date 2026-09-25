package com.example.game;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import java.util.List;

public class Sniper3DView extends GLSurfaceView {

    public interface GameCallback {
        void onAmmoCountChanged(int ammo);
        void onReloadStatusChanged(boolean reloading);
        void onBreathChanged(float stamina, boolean stabilized);
        void onAlertStatusChanged(boolean alerted, float timeLeft);
        void onLevelComplete(String outcome, int cashEarned);
    }

    private GameCallback gameCallback;
    private final Sniper3DRenderer renderer;
    private final BallisticsEngine ballisticsEngine = new BallisticsEngine();
    private final SniperCameraController cameraController = new SniperCameraController();
    private final PhysicsEngine physicsEngine = new PhysicsEngine();
    private SoundManager soundManager;
    private GamePrefs gamePrefs;

    // Environmental states
    private String activeEnv = "JUNGLE";
    private String activeMode = "PRACTICE";

    // Camera angles (yaw, pitch) and Zoom
    private float cameraYaw = 0.0f;
    private float cameraPitch = 0.0f;
    private float activeZoom = 4.0f;

    // Breathing sway
    private float swayX = 0.0f;
    private float swayY = 0.0f;
    private float breathStamina = 1.0f;
    private boolean isHoldingBreath = false;

    // Bullet physics & Clips
    private int ammoCount = 5;
    private boolean isReloading = false;
    private int shotsFired = 0;

    // Targets & AI Movement
    private float targetPatrolX = 0.0f;
    private boolean isTargetDead = false;
    private boolean isTargetAlerted = false;
    private float alertTimerSeconds = 5.0f;

    // Cinematic Bullet-Time Tracer
    private boolean isBulletTimeActive = false;
    private float bulletTimeProgress = 0.0f;
    private String bulletTimeOutcome = "MISS";

    // 60fps Physics Update Loop
    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private float gameTimeSeconds = 0f;
    private boolean isLoopRunning = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLoopRunning) return;
            updatePhysics();
            
            // Pass updated coordinates to OpenGL Renderer
            renderer.setGameState(
                activeEnv, activeMode, activeZoom,
                cameraYaw + swayX, cameraPitch + swayY,
                targetPatrolX, isTargetDead, isTargetAlerted,
                isBulletTimeActive, bulletTimeProgress
            );
            requestRender(); // Force repaint 3D Frame

            tickHandler.postDelayed(this, 16); // ~60fps
        }
    };

    // Touch Drag trackers
    private float lastTouchX;
    private float lastTouchY;

    public Sniper3DView(Context context) {
        super(context);
        renderer = new Sniper3DRenderer(context);
        initView();
    }

    public Sniper3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new Sniper3DRenderer(context);
        initView();
    }

    private void initView() {
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); // Continuously redraw at 60 FPS for smooth 3D updates
    }

    public void setCallback(GameCallback callback) {
        this.gameCallback = callback;
    }

    public void initGame(String env, String mode, GamePrefs prefs, SoundManager sounds) {
        this.activeEnv = env;
        this.activeMode = mode;
        this.gamePrefs = prefs;
        this.soundManager = sounds;

        // Reset variables
        cameraController.setAngles(0.0f, 0.0f);
        this.cameraYaw = 0.0f;
        this.cameraPitch = 0.0f;
        this.activeZoom = 4.0f;
        this.isHoldingBreath = false;
        this.breathStamina = 1.0f;
        this.isTargetDead = false;
        this.isTargetAlerted = false;
        this.alertTimerSeconds = 5.0f;
        this.ammoCount = 5;
        this.isReloading = false;
        this.isBulletTimeActive = false;
        this.bulletTimeProgress = 0f;
        this.targetPatrolX = 0f;
        this.shotsFired = 0;
        this.gameTimeSeconds = 0f;

        if (gameCallback != null) {
            gameCallback.onAmmoCountChanged(ammoCount);
            gameCallback.onReloadStatusChanged(isReloading);
            gameCallback.onBreathChanged(breathStamina, isHoldingBreath);
            gameCallback.onAlertStatusChanged(isTargetAlerted, alertTimerSeconds);
        }

        startLoop();
    }

    public void startLoop() {
        if (!isLoopRunning) {
            isLoopRunning = true;
            tickHandler.post(tickRunnable);
        }
    }

    public void stopLoop() {
        isLoopRunning = false;
        tickHandler.removeCallbacks(tickRunnable);
    }

    public float getActiveZoom() {
        return activeZoom;
    }

    public void setActiveZoom(float zoom) {
        this.activeZoom = zoom;
    }

    public void setHoldBreath(boolean hold) {
        if (hold && breathStamina > 0.15f && !isReloading) {
            isHoldingBreath = true;
        } else {
            isHoldingBreath = false;
        }
    }

    public void performReload() {
        if (isReloading || ammoCount == 5) return;
        isReloading = true;
        isHoldingBreath = false;
        if (gameCallback != null) {
            gameCallback.onReloadStatusChanged(isReloading);
        }
        soundManager.playReload();

        // 1.6s reload timer
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ammoCount = 5;
            isReloading = false;
            if (gameCallback != null) {
                gameCallback.onAmmoCountChanged(ammoCount);
                gameCallback.onReloadStatusChanged(isReloading);
            }
        }, 1600);
    }

    private void updatePhysics() {
        float delta = 0.016f; // ~16ms tick
        gameTimeSeconds += delta;

        // 1. Stamina & Sway Update
        // Stability based on active weapon upgrades
        float stability = 0.5f; // Base stability (upgrades are bound through GamePrefs)
        if (gamePrefs != null) {
            String selectedId = gamePrefs.getSelectedWeaponId();
            stability = Weapon.getWeaponById(selectedId).getStability(gamePrefs.getStatUpgradeLevel(selectedId, "stability"));
        }

        if (isHoldingBreath) {
            breathStamina = Math.max(0f, breathStamina - 0.25f * delta);
            if (breathStamina <= 0f) {
                isHoldingBreath = false;
            }
            if (Math.floor(gameTimeSeconds * 1.5) > Math.floor((gameTimeSeconds - delta) * 1.5)) {
                soundManager.playHeartbeat();
            }
        } else {
            breathStamina = Math.min(1.0f, breathStamina + 0.15f * delta);
        }

        if (gameCallback != null) {
            gameCallback.onBreathChanged(breathStamina, isHoldingBreath);
        }

        // Play ambient wind sound periodically during active surveillance
        if (!isTargetDead && !isBulletTimeActive) {
            if (Math.floor(gameTimeSeconds / 1.8) > Math.floor((gameTimeSeconds - delta) / 1.8)) {
                float windSpeed = activeEnv.equals("JUNGLE") ? 5.0f : 1.0f;
                soundManager.playWindAmbient(windSpeed, cameraYaw);
            }
        }

        // Lissajous breathing sway offsets
        float swayDampener = (1.1f - stability);
        float breathMult = isHoldingBreath ? 0.05f : 1.0f;
        swayX = (float) (Math.sin(gameTimeSeconds * 1.6f) * 1.5f * swayDampener * breathMult);
        swayY = (float) (Math.cos(gameTimeSeconds * 0.9f) * 1.0f * swayDampener * breathMult);

        // 2. Target Patrol AI Update
        if (!isTargetDead) {
            if (isTargetAlerted) {
                alertTimerSeconds = Math.max(0f, alertTimerSeconds - delta);
                if (gameCallback != null) {
                    gameCallback.onAlertStatusChanged(isTargetAlerted, alertTimerSeconds);
                }
                if (alertTimerSeconds <= 0f) {
                    stopLoop();
                    if (gameCallback != null) {
                        gameCallback.onLevelComplete("ALERT_ESCAPED", 0);
                    }
                }
                targetPatrolX += 1.5f * 5.0f * delta; // Quick escape flight
            } else {
                if (activeMode.equals("COMBAT")) {
                    targetPatrolX = (float) Math.sin(gameTimeSeconds * 1.8f) * 20.0f; // Pacing back and forth
                } else {
                    targetPatrolX = 0f; // Fixed target
                }
            }
        }

        // 3. Bullet-Time flight animation ticker
        if (isBulletTimeActive) {
            bulletTimeProgress += 1.0f / 90f; // drains over 90 frames
            if (bulletTimeProgress >= 1.0f) {
                isBulletTimeActive = false;
                stopLoop();
                resolve3DImpact();
            }
        }
    }

    private void resolve3DImpact() {
        int reward = 0;
        if (bulletTimeOutcome.equals("HEADSHOT") || bulletTimeOutcome.equals("BODYSHOT")) {
            isTargetDead = true;
            soundManager.playSpatialHitImpact(targetPatrolX, 0f, 250f, cameraYaw);
            
            // Score cash based on targets and distance multiplier!
            int baseReward = activeMode.equals("COMBAT") ? 1200 : 400;
            reward = bulletTimeOutcome.equals("HEADSHOT") ? (int) (baseReward * 1.25) : baseReward;
            
            if (gamePrefs != null) {
                gamePrefs.addCash(reward);
                gamePrefs.completeLevel(activeEnv.equals("JUNGLE") ? 1 : 2); // Save level clearance index
            }
        }

        if (gameCallback != null) {
            gameCallback.onLevelComplete(bulletTimeOutcome, reward);
        }
    }

    public void fire3DWeapon() {
        if (isReloading || ammoCount <= 0 || isBulletTimeActive) return;

        shotsFired++;
        ammoCount--;
        if (gameCallback != null) {
            gameCallback.onAmmoCountChanged(ammoCount);
        }
        soundManager.playGunshot();

        // 3D Angle deviation due to gravity & cross-wind
        float currentYawAngle = cameraYaw + swayX;
        float currentPitchAngle = cameraPitch + swayY;

        // Gravity bullet drop & wind deflection using math
        float distanceToTarget = 250.0f; // Target is positioned at 250m depth
        float muzzleVelocity = 850.0f;
        float windSpeed = activeEnv.equals("JUNGLE") ? 5.0f : 1.0f; // Low wind indoor

        if (gamePrefs != null) {
            String selId = gamePrefs.getSelectedWeaponId();
            muzzleVelocity = Weapon.getWeaponById(selId).getVelocity(gamePrefs.getStatUpgradeLevel(selId, "range"));
        }

        // Run full dynamic Euler simulation to target depth to get complete trajectory path
        List<BallisticsEngine.BulletState> trajectory = ballisticsEngine.simulateTrajectory(
            muzzleVelocity, currentPitchAngle, currentYawAngle, windSpeed, distanceToTarget
        );

        // Target actual linear coordinates
        float targetX = targetPatrolX;
        float targetY = 0f; // head is vertically offset above 0

        // Hit hitbox evaluation using swept segment collision tests across all simulated steps
        PhysicsEngine.CollisionResult collision = physicsEngine.detectCollisions(
            trajectory, targetX, targetY, distanceToTarget, 20.0f // Scale factor
        );

        if (collision.isCollided) {
            if (collision.hitType == PhysicsEngine.HitboxType.HEAD) {
                bulletTimeOutcome = "HEADSHOT";
            } else {
                bulletTimeOutcome = "BODYSHOT";
            }
            // Trigger Bullet-Time slow motion movie
            isHoldingBreath = false;
            isBulletTimeActive = true;
            bulletTimeProgress = 0f;
        } else {
            bulletTimeOutcome = "MISS";
            // Missed! alert AI
            if (activeMode.equals("COMBAT")) {
                isTargetAlerted = true;
            }
            if (ammoCount <= 0) {
                performReload();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isBulletTimeActive) return true;
        
        // Delegate dragging math and constraints to the camera controller
        cameraController.onTouchEvent(event, activeZoom);
        this.cameraYaw = cameraController.getYaw();
        this.cameraPitch = cameraController.getPitch();
        return true;
    }
}
