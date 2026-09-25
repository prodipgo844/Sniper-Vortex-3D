package com.example.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;

public class SoundManager {
    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private Vibrator vibrator;

    private final int sampleRate = 22050;
    private short[] gunshotPcm;
    private short[] clickPcm;
    private short[] heartbeatPcm;
    private short[] hitPcm;
    private short[] successPcm;
    private short[] windPcm;
    private boolean isSynthesized = false;

    public SoundManager(Context context) {
        this.context = context;
        initVibrator();
        executor.submit(this::synthesizeAll);
    }

    private void initVibrator() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    vibrator = vibratorManager.getDefaultVibrator();
                }
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synthesizeAll() {
        Random rand = new Random();

        // 1. Gunshot: White noise + rapid downward sine sweep + long decay
        int gunshotSamples = (int) (sampleRate * 1.0);
        gunshotPcm = new short[gunshotSamples];
        for (int i = 0; i < gunshotSamples; i++) {
            double t = (double) i / sampleRate;
            double envelope = Math.exp(-4.5 * t);
            double freq = 280.0 * Math.exp(-25.0 * t) + 30.0;
            double sineVal = Math.sin(2.0 * Math.PI * freq * t);
            double noiseVal = rand.nextDouble() * 2.0 - 1.0;
            double noiseEnvelop = Math.exp(-30.0 * t);
            double combined = (sineVal * 0.7 * envelope) + (noiseVal * 0.4 * noiseEnvelop);
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            gunshotPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        // 2. Click (Reload Click: opening/closing chamber)
        int clickSamples = (int) (sampleRate * 0.35);
        clickPcm = new short[clickSamples];
        for (int i = 0; i < clickSamples; i++) {
            double t = (double) i / sampleRate;
            double combined = 0.0;
            if (t < 0.08) {
                double env = Math.exp(-80.0 * t);
                combined += (rand.nextDouble() * 2.0 - 1.0) * 0.5 * env;
            }
            if (t > 0.18 && t < 0.28) {
                double t2 = t - 0.18;
                double env = Math.exp(-80.0 * t2);
                combined += (rand.nextDouble() * 2.0 - 1.0) * 0.6 * env;
            }
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            clickPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        // 3. Heartbeat: Dual low frequencies thump (55Hz)
        int hbSamples = (int) (sampleRate * 0.55);
        heartbeatPcm = new short[hbSamples];
        for (int i = 0; i < hbSamples; i++) {
            double t = (double) i / sampleRate;
            double combined = 0.0;
            if (t < 0.15) {
                double env = Math.sin(Math.PI * t / 0.15);
                combined += Math.sin(2.0 * Math.PI * 55.0 * t) * 0.8 * env;
            }
            if (t > 0.22 && t < 0.37) {
                double t2 = t - 0.22;
                double env = Math.sin(Math.PI * t2 / 0.15);
                combined += Math.sin(2.0 * Math.PI * 52.0 * t2) * 0.6 * env;
            }
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            heartbeatPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        // 4. Hit impact steel cling (1350Hz + 1620Hz)
        int hitSamples = (int) (sampleRate * 0.25);
        hitPcm = new short[hitSamples];
        for (int i = 0; i < hitSamples; i++) {
            double t = (double) i / sampleRate;
            double env = Math.exp(-15.0 * t);
            double combined = (Math.sin(2.0 * Math.PI * 1350.0 * t) + Math.sin(2.0 * Math.PI * 1620.0 * t)) * 0.4 * env;
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            hitPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        // 5. Success chime: C5 to G5 note
        int successSamples = (int) (sampleRate * 0.4);
        successPcm = new short[successSamples];
        for (int i = 0; i < successSamples; i++) {
            double t = (double) i / sampleRate;
            double combined = 0.0;
            if (t < 0.12) {
                double env = Math.exp(-12.0 * t);
                combined += Math.sin(2.0 * Math.PI * 523.25 * t) * 0.5 * env;
            } else {
                double t2 = t - 0.12;
                double env = Math.exp(-8.0 * t2);
                combined += Math.sin(2.0 * Math.PI * 783.99 * t2) * 0.6 * env;
            }
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            successPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        // 6. Wind Ambient Sound (synthesized white-noise sweep)
        int windSamples = (int) (sampleRate * 1.5);
        windPcm = new short[windSamples];
        double filterState = 0.0;
        for (int i = 0; i < windSamples; i++) {
            double t = (double) i / sampleRate;
            double rawNoise = rand.nextDouble() * 2.0 - 1.0;
            // High-resonance bandpass filter simulation for howling wind effect
            double speedMod = 0.05 + 0.02 * Math.sin(2.0 * Math.PI * 0.5 * t);
            filterState = filterState * (1.0 - speedMod) + rawNoise * speedMod;
            double env = Math.sin(Math.PI * t / 1.5);
            double combined = filterState * 0.3 * env;
            if (combined < -1.0) combined = -1.0;
            if (combined > 1.0) combined = 1.0;
            windPcm[i] = (short) (combined * Short.MAX_VALUE);
        }

        isSynthesized = true;
    }

    /**
     * Plays a synthesized mono sound track with stereo volume mapping.
     */
    private void playPcm(short[] pcm, float volumeLeft, float volumeRight) {
        if (!isSynthesized || pcm == null) return;
        executor.submit(() -> {
            try {
                int minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                );
                int bufferSize = Math.max(minBufferSize, pcm.length * 4);

                AudioTrack audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();

                // Convert mono PCM array to stereo (interleaved Left/Right)
                short[] stereoPcm = new short[pcm.length * 2];
                for (int i = 0; i < pcm.length; i++) {
                    stereoPcm[i * 2] = (short) (pcm[i] * volumeLeft);      // Left Channel
                    stereoPcm[i * 2 + 1] = (short) (pcm[i] * volumeRight);  // Right Channel
                }

                audioTrack.write(stereoPcm, 0, stereoPcm.length);
                audioTrack.play();

                long durationMs = (pcm.length * 1000L) / sampleRate;
                Thread.sleep(durationMs + 100);
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Plays audio with realistic 3D spatial panning and distance-based volume attenuation.
     * 
     * @param pcm The synthesized PCM audio asset
     * @param baseVolume Overall audio scale
     * @param sourceX Vector X of the emitter
     * @param sourceY Vector Y of the emitter
     * @param sourceZ Vector Z of the emitter
     * @param cameraYaw Active camera yaw panning looking direction
     */
    public void playPcmSpatial(short[] pcm, float baseVolume, float sourceX, float sourceY, float sourceZ, float cameraYaw) {
        if (!isSynthesized || pcm == null) return;

        // 1. Calculate Euclidean distance from camera / emitter center
        float dist = (float) Math.sqrt(sourceX * sourceX + sourceY * sourceY + sourceZ * sourceZ);
        
        // 2. Compute dynamic volume attenuation based on inverse square approximation
        float referenceDist = 15.0f; // Distance threshold before decay starts
        float attenuation = referenceDist / Math.max(referenceDist, dist);
        float spatialVolume = baseVolume * attenuation;

        // 3. Compute left/right pan split using directional azimuth angle relative to camera orientation
        double azimuthRad = Math.atan2(sourceX, sourceZ) - Math.toRadians(cameraYaw);
        float sinAzimuth = (float) Math.sin(azimuthRad);

        // Standard equal-power cross-fade panning curves
        float panLeft = (float) Math.sin(Math.PI / 4.0 * (1.0 - sinAzimuth));
        float panRight = (float) Math.cos(Math.PI / 4.0 * (1.0 - sinAzimuth));

        playPcm(pcm, spatialVolume * panLeft, spatialVolume * panRight);
    }

    public void playGunshot() {
        // Gunshot emits directly from the muzzle tip (mono center close range)
        playPcm(gunshotPcm, 1.0f, 1.0f);
        triggerRecoilVibration();
    }

    public void playReload() {
        playPcm(clickPcm, 0.8f, 0.8f);
        triggerReloadVibration();
    }

    public void playHeartbeat() {
        playPcm(heartbeatPcm, 0.9f, 0.9f);
        triggerHeartbeatVibration();
    }

    /**
     * Triggers steel chime spatial click sound from the far target zone.
     */
    public void playSpatialHitImpact(float targetX, float targetY, float targetZ, float cameraYaw) {
        playPcmSpatial(hitPcm, 0.9f, targetX, targetY, targetZ, cameraYaw);
        triggerSuccessVibration();
    }

    /**
     * Audio feedback for environmental wind sweeps based on lateral speed.
     */
    public void playWindAmbient(float windSpeedMph, float cameraYaw) {
        float absoluteSpeed = Math.abs(windSpeedMph);
        float baseWindVolume = Math.min(0.8f, absoluteSpeed / 25.0f); // Higher windspeed = loader wind noise

        // Panning direction matches the wind source vector (positive = East wind, negative = West wind)
        float windXOffset = (windSpeedMph > 0) ? 50.0f : -50.0f;
        playPcmSpatial(windPcm, baseWindVolume, windXOffset, 0.0f, 20.0f, cameraYaw);
    }

    public void playSuccess() {
        playPcm(successPcm, 0.75f, 0.75f);
    }

    // ------------------------------------------------------------------------
    // HAPTIC RECOIL EFFECTS
    // ------------------------------------------------------------------------

    private void triggerRecoilVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 120, 50, 80},
                    new int[]{0, VibrationEffect.DEFAULT_AMPLITUDE, 0, 100},
                    -1
                ));
            } else {
                vibrator.vibrate(180);
            }
        }
    }

    private void triggerReloadVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 30, 150, 40},
                    new int[]{0, 80, 0, 110},
                    -1
                ));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    private void triggerHeartbeatVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 40, 100, 30},
                    new int[]{0, 60, 0, 40},
                    -1
                ));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    private void triggerSuccessVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, 120));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
}
