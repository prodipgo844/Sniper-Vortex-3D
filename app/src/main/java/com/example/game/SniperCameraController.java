package com.example.game;

import android.view.MotionEvent;

public class SniperCameraController {

    // Current Camera Spherical Look Angles (Degrees)
    private float yaw = 0.0f;
    private float pitch = 0.0f;

    // Field of View Look Limits (Simulates scope mechanism stops)
    private final float minYaw;
    private final float maxYaw;
    private final float minPitch;
    private final float maxPitch;

    // Touch tracker coordinates
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    /**
     * Initializes scope camera rotation controller with default mechanical stops.
     * Prevents excessive twisting to simulate a realistic sniper rifle tripod/bipod mount.
     */
    public SniperCameraController() {
        // Limit yaw sweep to 45 degrees left/right, and pitch to 25 degrees up/down
        this.minYaw = -45.0f;
        this.maxYaw = 45.0f;
        this.minPitch = -25.0f;
        this.maxPitch = 25.0f;
    }

    /**
     * Custom mechanical scope bounds.
     */
    public SniperCameraController(float minYaw, float maxYaw, float minPitch, float maxPitch) {
        this.minYaw = minYaw;
        this.maxYaw = maxYaw;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setAngles(float yaw, float pitch) {
        this.yaw = clamp(yaw, minYaw, maxYaw);
        this.pitch = clamp(pitch, minPitch, maxPitch);
    }

    /**
     * Processes Android MotionEvents and updates yaw/pitch spherical offsets.
     * 
     * @param event The touch MotionEvent
     * @param currentZoom Active zoom level (used to scale down drag speed for high precision)
     * @return true if the camera angles changed as a result of the touch move
     */
    public boolean onTouchEvent(MotionEvent event, float currentZoom) {
        float tx = event.getX();
        float ty = event.getY();
        boolean angleChanged = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = tx;
                lastTouchY = ty;
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) break;

                float dx = tx - lastTouchX;
                float dy = ty - lastTouchY;

                // 1. Calculate zoom-scaled sensitivity
                // Finer magnification levels scale down touch speeds to allow precise crosshair alignment
                float baseSensitivity = 0.08f;
                float zoomScaleFactor = 1.0f / Math.max(1.0f, currentZoom);
                float finalSensitivity = baseSensitivity * zoomScaleFactor;

                float proposedYaw = yaw - (dx * finalSensitivity);
                float proposedPitch = pitch + (dy * finalSensitivity);

                // 2. Clamp look angles strictly inside scope mechanical limits
                float clampedYaw = clamp(proposedYaw, minYaw, maxYaw);
                float clampedPitch = clamp(proposedPitch, minPitch, maxPitch);

                if (clampedYaw != yaw || clampedPitch != pitch) {
                    yaw = clampedYaw;
                    pitch = clampedPitch;
                    angleChanged = true;
                }

                lastTouchX = tx;
                lastTouchY = ty;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return angleChanged;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
