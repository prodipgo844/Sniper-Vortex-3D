package com.example.game;

import java.util.ArrayList;
import java.util.List;

public class BallisticsEngine {

    // Physical and Environmental Constants
    private static final float GRAVITY = 9.80665f;      // m/s^2
    private static final float AIR_DENSITY = 1.225f;    // kg/m^3 (Standard sea-level ISA)
    
    // Bullet Parameters (.338 Lapua Magnum match bullet specifications)
    private static final float BULLET_MASS = 0.0162f;     // kg (250 grains)
    private static final float BULLET_DIAMETER = 0.00858f; // m (8.6mm)
    private static final float DRAG_COEFFICIENT = 0.25f;   // Cd (G1 drag profile index)
    
    private final float crossSectionalArea;

    public BallisticsEngine() {
        // Area = pi * r^2
        this.crossSectionalArea = (float) (Math.PI * Math.pow(BULLET_DIAMETER / 2.0f, 2));
    }

    /**
     * Represents the state of the bullet at any point during its flight trajectory.
     */
    public static class BulletState {
        public float x;          // Horizontal offset / Windage (meters)
        public float y;          // Vertical offset / Gravity drop (meters)
        public float z;          // Distance traveled along the line of sight (meters)
        public float vx;         // Velocity x (m/s)
        public float vy;         // Velocity y (m/s)
        public float vz;         // Velocity z (m/s)
        public float time;       // Elapsed travel time (seconds)
        public float totalSpeed; // Total scalar velocity (m/s)

        public BulletState(float x, float y, float z, float vx, float vy, float vz, float time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.time = time;
            this.totalSpeed = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
        }
    }

    /**
     * Simulates the complete bullet trajectory in 3D using Euler-Cromer numerical integration.
     * 
     * @param initialVelocity Initial muzzle velocity (m/s)
     * @param pitchDegrees Initial camera vertical elevation angle (degrees)
     * @param yawDegrees Initial camera horizontal panning angle (degrees)
     * @param windSpeedMph Cross-wind speed in miles per hour (positive = East wind, negative = West wind)
     * @param targetZDistance Distance to the target plane (meters)
     * @return Complete List of step-by-step BulletStates throughout the flight path.
     */
    public List<BulletState> simulateTrajectory(
            float initialVelocity, 
            float pitchDegrees, 
            float yawDegrees, 
            float windSpeedMph, 
            float targetZDistance
    ) {
        List<BulletState> trajectory = new ArrayList<>();

        // Convert angles to radians
        double pitchRad = Math.toRadians(pitchDegrees);
        double yawRad = Math.toRadians(yawDegrees);

        // Convert wind speed from mph to m/s
        float windSpeedMps = windSpeedMph * 0.44704f;

        // Initialize state vectors at muzzle mouth (0, 0, 0)
        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;
        float t = 0.0f;

        // Break initial scalar muzzle velocity into 3D Cartesian vectors
        float vz = (float) (initialVelocity * Math.cos(pitchRad) * Math.cos(yawRad));
        float vx = (float) (initialVelocity * Math.cos(pitchRad) * Math.sin(yawRad));
        float vy = (float) (initialVelocity * Math.sin(pitchRad));

        trajectory.add(new BulletState(x, y, z, vx, vy, vz, t));

        // High-precision simulation step (dt = 1 millisecond)
        float dt = 0.001f;

        while (z < targetZDistance && t < 5.0f) { // Stop on target plane or 5s max flight timeout
            
            // 1. Calculate scalar relative speed of bullet through air
            // Relative speed includes lateral wind velocity component
            float relVx = vx - windSpeedMps;
            float relVy = vy;
            float relVz = vz;
            float relativeSpeed = (float) Math.sqrt(relVx*relVx + relVy*relVy + relVz*relVz);

            // 2. Calculate Drag Force magnitude: Fd = 0.5 * rho * v^2 * Cd * A
            float dragForce = 0.5f * AIR_DENSITY * (float) Math.pow(relativeSpeed, 2) * DRAG_COEFFICIENT * crossSectionalArea;
            
            // 3. Calculate 3D Deceleration vectors (a = F / m)
            // Deceleration acts in the opposite direction of the relative velocity vector
            float axDrag = -(dragForce * (relVx / relativeSpeed)) / BULLET_MASS;
            float ayDrag = -(dragForce * (relVy / relativeSpeed)) / BULLET_MASS;
            float azDrag = -(dragForce * (relVz / relativeSpeed)) / BULLET_MASS;

            // 4. Update 3D Velocity vectors (v = v0 + a * dt)
            // Vertical axis vy includes continuous gravity acceleration downward
            vx += axDrag * dt;
            vy += (ayDrag - GRAVITY) * dt;
            vz += azDrag * dt;

            // 5. Update 3D Position vectors (s = s0 + v * dt)
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            t += dt;

            // Sample bullet state coordinates into trajectory log every 10 milliseconds to save memory
            if (Math.round(t * 1000f) % 10 == 0) {
                trajectory.add(new BulletState(x, y, z, vx, vy, vz, t));
            }
        }

        // Add final impact state precisely at target plane
        trajectory.add(new BulletState(x, y, z, vx, vy, vz, t));
        return trajectory;
    }

    /**
     * Utility method returning only the final impact state at the target plane.
     */
    public BulletState calculateImpact(
            float initialVelocity, 
            float pitchDegrees, 
            float yawDegrees, 
            float windSpeedMph, 
            float targetZDistance
    ) {
        List<BulletState> trajectory = simulateTrajectory(initialVelocity, pitchDegrees, yawDegrees, windSpeedMph, targetZDistance);
        return trajectory.get(trajectory.size() - 1);
    }

    /**
     * Hit hitbox verification check. Calculates intersection of bullet impact spot
     * against target spheres and rectangle boundaries.
     */
    public String verifyHit(
            BulletState impact, 
            float targetX, 
            float targetY, 
            float targetZ, 
            float scale
    ) {
        // Hitbox parameters scale down proportionally as distance increases
        float headRadius = 0.15f * scale; // ~15cm radius head
        float torsoWidth = 0.40f * scale; // ~40cm width body
        float torsoHeight = 0.90f * scale; // ~90cm height body

        // Coordinate offsets relative to target center
        float dx = impact.x - targetX;
        float dy = impact.y - targetY;

        // Head position offset vertically above torso center
        float headCenterY = targetY + (torsoHeight / 2.0f) + headRadius;

        // 1. Headshot check (Sphere intersection)
        float headDistanceSq = (float) (Math.pow(dx, 2) + Math.pow(impact.y - headCenterY, 2));
        if (headDistanceSq <= headRadius * headRadius) {
            return "HEADSHOT";
        }

        // 2. Bodyshot check (3D bounding box)
        if (impact.x >= targetX - torsoWidth / 2.0f && 
            impact.x <= targetX + torsoWidth / 2.0f &&
            impact.y >= targetY - torsoHeight / 2.0f && 
            impact.y <= targetY + torsoHeight / 2.0f) {
            return "BODYSHOT";
        }

        return "MISS";
    }
}
