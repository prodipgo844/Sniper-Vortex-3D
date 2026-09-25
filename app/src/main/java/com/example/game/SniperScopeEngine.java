package com.example.game;

public class SniperScopeEngine {
    private static final double GRAVITY = 9.81;
    private static final double PIXELS_PER_METER_REFERENCE = 120.0;
    private static final double WIND_DRIFT_FACTOR = 1.8;

    public static class BallisticResult {
        public final float driftX;
        public final float dropY;
        public final float flightTime;

        public BallisticResult(float driftX, float dropY, float flightTime) {
            this.driftX = driftX;
            this.dropY = dropY;
            this.flightTime = flightTime;
        }
    }

    public BallisticResult calculateTrajectory(
            float distance,
            float windSpeedMph,
            float muzzleVelocity,
            float scopeZoom
    ) {
        // 1. Flight time (t = d / v)
        float flightTime = distance / muzzleVelocity;

        // 2. Vertical gravity drop: y = 0.5 * g * t^2
        double gravityDropMeters = 0.5 * GRAVITY * Math.pow(flightTime, 2.0);

        // 3. Lateral wind deflection: x = windMps * t * factor
        double windSpeedMps = windSpeedMph * 0.44704;
        double windDriftMeters = windSpeedMps * flightTime * WIND_DRIFT_FACTOR;

        // Scale physical meters to screen pixels based on zoom amplification
        float dropPixels = (float) (gravityDropMeters * PIXELS_PER_METER_REFERENCE * scopeZoom);
        float driftPixels = (float) (windDriftMeters * PIXELS_PER_METER_REFERENCE * scopeZoom);

        return new BallisticResult(driftPixels, dropPixels, flightTime);
    }

    public static class SwayResult {
        public final float swayX;
        public final float swayY;

        public SwayResult(float swayX, float swayY) {
            this.swayX = swayX;
            this.swayY = swayY;
        }
    }

    public SwayResult calculateSway(
            float timeSeconds,
            float stability,
            boolean isHoldingBreath
    ) {
        float baseFreqX = 1.6f;
        float baseFreqY = 0.9f;
        float baseAmpX = 45.0f;
        float baseAmpY = 30.0f;

        float stabilityDampener = (1.1f - stability);
        if (stabilityDampener < 0.1f) stabilityDampener = 0.1f;
        if (stabilityDampener > 1.0f) stabilityDampener = 1.0f;

        float breathMultiplier = isHoldingBreath ? 0.06f : 1.0f;

        float finalAmpX = baseAmpX * stabilityDampener * breathMultiplier;
        float finalAmpY = baseAmpY * stabilityDampener * breathMultiplier;

        float freqMultiplier = isHoldingBreath ? 0.4f : 1.0f;
        float freqX = baseFreqX * freqMultiplier;
        float freqY = baseFreqY * freqMultiplier;

        float swayX = (float) (Math.sin(timeSeconds * freqX) * finalAmpX);
        float swayY = (float) (Math.cos(timeSeconds * freqY) * finalAmpY);

        return new SwayResult(swayX, swayY);
    }
}
