package com.example.game;

import java.util.List;

public class PhysicsEngine {

    /**
     * Hitbox types supported by our sniper operations.
     */
    public enum HitboxType {
        HEAD,
        TORSO,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG,
        CIVILIAN,
        OBSTACLE,
        NONE
    }

    /**
     * Defines a 3D bounding box for hierarchical target hitboxes.
     */
    public static class BoundingBox {
        public float minX, maxX;
        public float minY, maxY;
        public float minZ, maxZ;
        public HitboxType type;
        public float scoreMultiplier;

        public BoundingBox(float minX, float maxX, float minY, float maxY, float minZ, float maxZ, HitboxType type, float scoreMultiplier) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.type = type;
            this.scoreMultiplier = scoreMultiplier;
        }

        /**
         * Verifies if a point lies precisely inside this 3D axis-aligned bounding box.
         */
        public boolean contains(float x, float y, float z) {
            return x >= minX && x <= maxX &&
                   y >= minY && y <= maxY &&
                   z >= minZ && z <= maxZ;
        }

        /**
         * Ray-AABB intersection algorithm to check if a segment intersects the box.
         */
        public boolean intersectsSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;

            float tmin = Float.NEGATIVE_INFINITY;
            float tmax = Float.POSITIVE_INFINITY;

            if (Math.abs(dx) > 1e-6) {
                float tx1 = (minX - x1) / dx;
                float tx2 = (maxX - x1) / dx;
                tmin = Math.max(tmin, Math.min(tx1, tx2));
                tmax = Math.min(tmax, Math.max(tx1, tx2));
            } else if (x1 < minX || x1 > maxX) {
                return false;
            }

            if (Math.abs(dy) > 1e-6) {
                float ty1 = (minY - y1) / dy;
                float ty2 = (maxY - y1) / dy;
                tmin = Math.max(tmin, Math.min(ty1, ty2));
                tmax = Math.min(tmax, Math.max(ty1, ty2));
            } else if (y1 < minY || y1 > maxY) {
                return false;
            }

            if (Math.abs(dz) > 1e-6) {
                float tz1 = (minZ - z1) / dz;
                float tz2 = (maxZ - z1) / dz;
                tmin = Math.max(tmin, Math.min(tz1, tz2));
                tmax = Math.min(tmax, Math.max(tz1, tz2));
            } else if (z1 < minZ || z1 > maxZ) {
                return false;
            }

            return tmax >= tmin && tmax >= 0.0f && tmin <= 1.0f;
        }
    }

    /**
     * Defines a 3D bounding sphere, mostly used for Head hitboxes.
     */
    public static class BoundingSphere {
        public float centerX, centerY, centerZ;
        public float radius;
        public HitboxType type;
        public float scoreMultiplier;

        public BoundingSphere(float centerX, float centerY, float centerZ, float radius, HitboxType type, float scoreMultiplier) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.type = type;
            this.scoreMultiplier = scoreMultiplier;
        }

        public boolean contains(float x, float y, float z) {
            float dx = x - centerX;
            float dy = y - centerY;
            float dz = z - centerZ;
            return (dx*dx + dy*dy + dz*dz) <= radius * radius;
        }

        /**
         * Swept-sphere segment intersection check to prevent high-speed tunneling.
         */
        public boolean intersectsSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float lenSq = dx*dx + dy*dy + dz*dz;

            if (lenSq < 1e-6) {
                return contains(x1, y1, z1);
            }

            // Projection factor t along the segment
            float t = ((centerX - x1) * dx + (centerY - y1) * dy + (centerZ - z1) * dz) / lenSq;
            t = Math.max(0.0f, Math.min(1.0f, t));

            // Closest point on the segment
            float projX = x1 + t * dx;
            float projY = y1 + t * dy;
            float projZ = z1 + t * dz;

            float distSq = (projX - centerX)*(projX - centerX) + 
                           (projY - centerY)*(projY - centerY) + 
                           (projZ - centerZ)*(projZ - centerZ);

            return distSq <= radius * radius;
        }
    }

    public static class CollisionResult {
        public HitboxType hitType = HitboxType.NONE;
        public float multiplier = 0.0f;
        public float hitX, hitY, hitZ;
        public boolean isCollided = false;
    }

    /**
     * Swept intersection check along the entire computed ballistic trajectory.
     * Prevents bullet "tunneling" through thin geometry at high speeds.
     */
    public CollisionResult detectCollisions(
            List<BallisticsEngine.BulletState> trajectory,
            float targetX,
            float targetY,
            float targetZ,
            float scale
    ) {
        CollisionResult result = new CollisionResult();
        if (trajectory == null || trajectory.size() < 2) {
            return result;
        }

        // Generate hierarchical hitbox list dynamically relative to the moving target
        float headRad = 0.15f * scale;
        float torsoW = 0.40f * scale;
        float torsoH = 0.90f * scale;
        float limbW = 0.12f * scale;
        float limbH = 0.40f * scale;

        // Bounding volumes around the primary character skeleton
        BoundingSphere headVolume = new BoundingSphere(
            targetX, 
            targetY + (torsoH / 2.0f) + headRad, 
            targetZ, 
            headRad, 
            HitboxType.HEAD, 
            2.5f
        );

        BoundingBox torsoVolume = new BoundingBox(
            targetX - torsoW/2.0f, targetX + torsoW/2.0f,
            targetY - torsoH/2.0f, targetY + torsoH/2.0f,
            targetZ - 0.2f, targetZ + 0.2f,
            HitboxType.TORSO,
            1.0f
        );

        // Swept checking through adjacent nodes along the trajectory path
        for (int i = 0; i < trajectory.size() - 1; i++) {
            BallisticsEngine.BulletState current = trajectory.get(i);
            BallisticsEngine.BulletState next = trajectory.get(i + 1);

            // 1. Check headshot collision
            if (headVolume.intersectsSegment(current.x, current.y, current.z, next.x, next.y, next.z)) {
                result.isCollided = true;
                result.hitType = HitboxType.HEAD;
                result.multiplier = headVolume.scoreMultiplier;
                result.hitX = next.x;
                result.hitY = next.y;
                result.hitZ = next.z;
                return result;
            }

            // 2. Check torso/body collision
            if (torsoVolume.intersectsSegment(current.x, current.y, current.z, next.x, next.y, next.z)) {
                result.isCollided = true;
                result.hitType = HitboxType.TORSO;
                result.multiplier = torsoVolume.scoreMultiplier;
                result.hitX = next.x;
                result.hitY = next.y;
                result.hitZ = next.z;
                return result;
            }
        }

        return result;
    }
}
