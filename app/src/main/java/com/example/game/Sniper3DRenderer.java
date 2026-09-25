package com.example.game;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Sniper3DRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private final Random rand = new Random(42);

    // Environments & Game Modes
    private String activeEnvironment = "JUNGLE"; // "JUNGLE" or "INDOOR"
    private String activeMode = "PRACTICE"; // "PRACTICE" or "COMBAT"

    // Real-Time Camera angles (Yaw & Pitch)
    private float cameraYaw = 0.0f;   // Horizontal panning
    private float cameraPitch = 0.0f; // Vertical panning
    private float activeZoom = 4.0f;

    // Trajectory Targets
    private float targetPatrolX = 0.0f;
    private boolean isTargetDead = false;
    private boolean isTargetAlerted = false;

    // Bullet-Time trace coordinates
    private boolean isBulletTimeActive = false;
    private float bulletTimeProgress = 0f;

    // Math Matrix transformations
    private final float[] mModelMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];

    // GLSL Shader Programs
    private int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mLightDirHandle;
    private int mFogColorHandle;
    private int mFogDensityHandle;

    // Vertex Buffers for 3D Primitives
    private FloatBuffer cubeBuffer;
    private FloatBuffer treeConeBuffer;
    private FloatBuffer sphereBuffer;

    public Sniper3DRenderer(Context context) {
        this.context = context;
        initGeometries();
    }

    public void setGameState(String env, String mode, float zoom, float yaw, float pitch, 
                             float patrolX, boolean dead, boolean alerted, 
                             boolean bulletTime, float btProgress) {
        this.activeEnvironment = env;
        this.activeMode = mode;
        this.activeZoom = zoom;
        this.cameraYaw = yaw;
        this.cameraPitch = pitch;
        this.targetPatrolX = patrolX;
        this.isTargetDead = dead;
        this.isTargetAlerted = alerted;
        this.isBulletTimeActive = bulletTime;
        this.bulletTimeProgress = btProgress;
    }

    private void initGeometries() {
        // 1. Generate 3D Cube vertex array (used for walls, furniture, floor)
        float[] cubeCoords = {
            -0.5f,  0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f, // Front
             0.5f,  0.5f, -0.5f,  0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,  0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, // Back
            -0.5f,  0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, // Left
             0.5f,  0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f, -0.5f, // Right
            -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f, // Top
            -0.5f, -0.5f,  0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, -0.5f  // Bottom
        };
        cubeBuffer = allocateFloatBuffer(cubeCoords);

        // 2. Generate 3D Cone (used for foliage / tree tops)
        int coneSlices = 16;
        float[] coneCoords = new float[coneSlices * 3 * 3];
        int idx = 0;
        for (int i = 0; i < coneSlices; i++) {
            double angle1 = (i * 2 * Math.PI) / coneSlices;
            double angle2 = ((i + 1) * 2 * Math.PI) / coneSlices;
            
            // Base center point
            coneCoords[idx++] = 0.0f; coneCoords[idx++] = -0.5f; coneCoords[idx++] = 0.0f;
            coneCoords[idx++] = (float) Math.cos(angle1) * 0.5f; coneCoords[idx++] = -0.5f; coneCoords[idx++] = (float) Math.sin(angle1) * 0.5f;
            coneCoords[idx++] = (float) Math.cos(angle2) * 0.5f; coneCoords[idx++] = -0.5f; coneCoords[idx++] = (float) Math.sin(angle2) * 0.5f;
            
            // Tip point triangles
            coneCoords[idx++] = 0.0f; coneCoords[idx++] = 0.5f; coneCoords[idx++] = 0.0f;
            coneCoords[idx++] = (float) Math.cos(angle2) * 0.5f; coneCoords[idx++] = -0.5f; coneCoords[idx++] = (float) Math.sin(angle2) * 0.5f;
            coneCoords[idx++] = (float) Math.cos(angle1) * 0.5f; coneCoords[idx++] = -0.5f; coneCoords[idx++] = (float) Math.sin(angle1) * 0.5f;
        }
        treeConeBuffer = allocateFloatBuffer(coneCoords);

        // 3. Generate Sphere (used for heads of targets)
        int rings = 8;
        int sectors = 8;
        float[] sphereCoords = new float[rings * sectors * 6 * 3];
        idx = 0;
        for (int r = 0; r < rings; r++) {
            for (int s = 0; sectorIndex(s, sectors); s++) {
                double y1 = Math.cos((r * Math.PI) / rings);
                double y2 = Math.cos(((r + 1) * Math.PI) / rings);
                double r1 = Math.sin((r * Math.PI) / rings);
                double r2 = Math.sin(((r + 1) * Math.PI) / rings);

                double x1 = r1 * Math.cos((s * 2 * Math.PI) / sectors);
                double z1 = r1 * Math.sin((s * 2 * Math.PI) / sectors);
                double x2 = r1 * Math.cos(((s + 1) * 2 * Math.PI) / sectors);
                double z2 = r1 * Math.sin(((s + 1) * 2 * Math.PI) / sectors);

                double x3 = r2 * Math.cos((s * 2 * Math.PI) / sectors);
                double z3 = r2 * Math.sin((s * 2 * Math.PI) / sectors);
                double x4 = r2 * Math.cos(((s + 1) * 2 * Math.PI) / sectors);
                double z4 = r2 * Math.sin(((s + 1) * 2 * Math.PI) / sectors);

                sphereCoords[idx++] = (float) x1; sphereCoords[idx++] = (float) y1; sphereCoords[idx++] = (float) z1;
                sphereCoords[idx++] = (float) x3; sphereCoords[idx++] = (float) y2; sphereCoords[idx++] = (float) z3;
                sphereCoords[idx++] = (float) x2; sphereCoords[idx++] = (float) y1; sphereCoords[idx++] = (float) z2;

                sphereCoords[idx++] = (float) x2; sphereCoords[idx++] = (float) y1; sphereCoords[idx++] = (float) z2;
                sphereCoords[idx++] = (float) x3; sphereCoords[idx++] = (float) y2; sphereCoords[idx++] = (float) z3;
                sphereCoords[idx++] = (float) x4; sphereCoords[idx++] = (float) y2; sphereCoords[idx++] = (float) z4;
            }
        }
        sphereBuffer = allocateFloatBuffer(sphereCoords);
    }

    private boolean sectorIndex(int s, int max) {
        return s < max;
    }

    private FloatBuffer allocateFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.05f, 0.07f, 0.1f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Advanced Vertex shader supporting Directional Lighting and Fog passing
        String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "uniform vec4 uColor;" +
            "varying vec4 vColor;" +
            "varying float v_Dist;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vColor = uColor;" +
            "  v_Dist = gl_Position.z;" + // Pass distance for fog
            "}";

        // Advanced Fragment shader executing exponential depth fog mixed on sky color
        String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 vColor;" +
            "varying float v_Dist;" +
            "uniform vec4 u_FogColor;" +
            "uniform float u_FogDensity;" +
            "void main() {" +
            "  float fogFactor = 1.0 - exp(-u_Dist * u_FogDensity);" +
            "  fogFactor = clamp(fogFactor, 0.0, 1.0);" +
            "  gl_FragColor = mix(vColor, u_FogColor, fogFactor);" +
            "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        // Handle handles
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mFogColorHandle = GLES20.glGetUniformLocation(mProgram, "u_FogColor");
        mFogDensityHandle = GLES20.glGetUniformLocation(mProgram, "u_FogDensity");
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        // Apply dynamic Narrow Field of View (FoV) for real optical zoom lens effect!
        // Greater zoom reduces frustum window dimensions, magnification multiplies details.
        float frustumScale = 1.0f / activeZoom;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio * frustumScale, ratio * frustumScale, -frustumScale, frustumScale, 1.0f, 1000.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Render tick updater
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        // Adjust perspective frustum matching active zoom values
        float ratio = (float) GLES20.glGetAttribLocation(mProgram, "viewport") / 1080f; // placeholder ratio
        ratio = 16f/9f;
        float frustumScale = 2.0f / activeZoom;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio * frustumScale, ratio * frustumScale, -frustumScale, frustumScale, 1.0f, 1000.0f);

        // Set View Camera look matrix
        setupCamera();

        // Bind global fog values based on chosen environment
        float[] fogColor = {0.05f, 0.07f, 0.1f, 1.0f}; // Default dark gray
        float fogDensity = 0.003f; // Default low fog

        if (activeEnvironment.equals("JUNGLE")) {
            fogColor = new float[]{0.12f, 0.18f, 0.15f, 1.0f}; // Dense Jungle Green fog
            fogDensity = 0.012f; // Extreme thick distance occlusion
        } else {
            fogColor = new float[]{0.05f, 0.05f, 0.06f, 1.0f}; // Warm Indoor hallway fog
            fogDensity = 0.005f;
        }
        GLES20.glUniform4fv(mFogColorHandle, 1, fogColor, 0);
        GLES20.glUniform1f(mFogDensityHandle, fogDensity);

        // 1. DRAW ENVIRONMENTS
        if (activeEnvironment.equals("JUNGLE")) {
            drawForestJungleBackdrop();
        } else {
            drawIndoorCorridors();
        }

        // 2. DRAW TARGET/ENEMY CHARACTERS
        draw3DTargetCharacter();

        // 3. DRAW BULLET-TIME VECTOR CINEMATICS
        if (isBulletTimeActive) {
            draw3DBulletTracerTrace();
        }

        // 4. DRAW 3D SNIPER RIFLE SILHOUETTE
        draw3DSniperRifleModel();
    }

    private void setupCamera() {
        // Yaw & Pitch are represented in standard spherical orbital camera positions
        float distance = 1.0f;
        float yawRad = (float) Math.toRadians(cameraYaw);
        float pitchRad = (float) Math.toRadians(cameraPitch);

        // Position Look vector
        float lookZ = -(float) (distance * Math.cos(pitchRad) * Math.cos(yawRad));
        float lookX = (float) (distance * Math.cos(pitchRad) * Math.sin(yawRad));
        float lookY = (float) (distance * Math.sin(pitchRad));

        Matrix.setLookAtM(mViewMatrix, 0, 
            0.0f, 0.0f, 0.0f, // Eye center is standard camera focal point
            lookX, lookY, lookZ, // Look coordinates
            0.0f, 1.0f, 0.0f  // Up direction vector
        );
    }

    private void drawForestJungleBackdrop() {
        // Draw forest green terrain ground plane
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -50.0f, -100.0f);
        Matrix.scaleM(mModelMatrix, 0, 1000.0f, 1.0f, 1000.0f); // Massive ground block
        drawCube(mModelMatrix, new float[]{0.15f, 0.22f, 0.15f, 1.0f}); // Deep moss green

        // Draw multiple procedural green cone trees along the landscape
        rand.setSeed(12345); // Constant landscape placement
        for (int i = 0; i < 30; i++) {
            float tx = (rand.nextFloat() * 400.0f) - 200.0f;
            float tz = -(rand.nextFloat() * 400.0f + 50.0f);
            float ty = -10.0f;

            // Trunk (Brown Cylinder/Cube)
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, tx, ty - 5f, tz);
            Matrix.scaleM(mModelMatrix, 0, 2.0f, 15.0f, 2.0f);
            drawCube(mModelMatrix, new float[]{0.35f, 0.22f, 0.15f, 1.0f});

            // Foliage cone 1
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, tx, ty + 10f, tz);
            Matrix.scaleM(mModelMatrix, 0, 15.0f, 20.0f, 15.0f);
            drawCone(mModelMatrix, new float[]{0.12f, 0.32f, 0.18f, 1.0f});

            // Foliage cone 2 (Upper tip)
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, tx, ty + 20f, tz);
            Matrix.scaleM(mModelMatrix, 0, 10.0f, 15.0f, 10.0f);
            drawCone(mModelMatrix, new float[]{0.15f, 0.38f, 0.22f, 1.0f});
        }
    }

    private void drawIndoorCorridors() {
        // Draw floor gray tiles
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -12.0f, -100.0f);
        Matrix.scaleM(mModelMatrix, 0, 100.0f, 1.0f, 500.0f);
        drawCube(mModelMatrix, new float[]{0.25f, 0.25f, 0.28f, 1.0f});

        // Draw left Corridor wall block
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -25.0f, 10.0f, -100.0f);
        Matrix.scaleM(mModelMatrix, 0, 2.0f, 60.0f, 500.0f);
        drawCube(mModelMatrix, new float[]{0.35f, 0.35f, 0.35f, 1.0f});

        // Draw right Corridor wall block
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 25.0f, 10.0f, -100.0f);
        Matrix.scaleM(mModelMatrix, 0, 2.0f, 60.0f, 500.0f);
        drawCube(mModelMatrix, new float[]{0.35f, 0.35f, 0.35f, 1.0f});

        // Draw multiple indoor concrete pillar occlusions to hide targets
        for (int z = 1; z <= 4; z++) {
            float pz = -z * 80.0f;
            // Left pillar
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, -18.0f, 0.0f, pz);
            Matrix.scaleM(mModelMatrix, 0, 4.0f, 40.0f, 4.0f);
            drawCube(mModelMatrix, new float[]{0.5f, 0.5f, 0.52f, 1.0f});

            // Right pillar
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 18.0f, 0.0f, pz);
            Matrix.scaleM(mModelMatrix, 0, 4.0f, 40.0f, 4.0f);
            drawCube(mModelMatrix, new float[]{0.5f, 0.5f, 0.52f, 1.0f});
        }
    }

    private void draw3DTargetCharacter() {
        // Draw head sphere, torso, and limbs
        float tx = targetPatrolX;
        float ty = -6.0f;
        float tz = -250.0f; // Position at 250m depth in scene

        if (isTargetDead) {
            // Crumpled lying posture
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, tx + 6f, ty - 5f, tz);
            Matrix.scaleM(mModelMatrix, 0, 12.0f, 2.0f, 4.0f);
            drawCube(mModelMatrix, new float[]{0.25f, 0.25f, 0.25f, 1.0f}); // Crumpled Torso
            return;
        }

        // Draw Mannequin HEAD Sphere
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, tx, ty + 12f, tz);
        Matrix.scaleM(mModelMatrix, 0, 4.0f, 4.0f, 4.0f);
        drawSphere(mModelMatrix, activeMode.equals("COMBAT") ? new float[]{0.85f, 0.15f, 0.15f, 1.0f} : new float[]{0.85f, 0.65f, 0.35f, 1.0f});

        // Draw Torso cylinder block
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, tx, ty + 2f, tz);
        Matrix.scaleM(mModelMatrix, 0, 8.0f, 15.0f, 4.0f);
        drawCube(mModelMatrix, activeMode.equals("COMBAT") ? new float[]{0.15f, 0.35f, 0.85f, 1.0f} : new float[]{0.75f, 0.75f, 0.75f, 1.0f});

        // Limbs support pillars
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, tx - 3f, ty - 10f, tz);
        Matrix.scaleM(mModelMatrix, 0, 1.5f, 10.0f, 1.5f);
        drawCube(mModelMatrix, new float[]{0.5f, 0.5f, 0.5f, 1.0f});

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, tx + 3f, ty - 10f, tz);
        Matrix.scaleM(mModelMatrix, 0, 1.5f, 10.0f, 1.5f);
        drawCube(mModelMatrix, new float[]{0.5f, 0.5f, 0.5f, 1.0f});
    }

    private void draw3DBulletTracerTrace() {
        // Draw glowing golden bullet traveling through air
        float startZ = -5.0f;
        float endZ = -250.0f;
        float currentZ = startZ + (endZ - startZ) * bulletTimeProgress;

        float startX = 0f;
        float endX = targetPatrolX;
        float currentX = startX + (endX - startX) * bulletTimeProgress;

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, currentX, -2.0f, currentZ);
        Matrix.scaleM(mModelMatrix, 0, 0.6f, 0.6f, 5.0f); // Bullet cylinder scale
        drawCube(mModelMatrix, new float[]{1.0f, 0.84f, 0.0f, 1.0f}); // Glowing Gold Tracer
    }

    private void draw3DSniperRifleModel() {
        // Draw Sniper Rifle barrel block in front of camera
        Matrix.setIdentityM(mModelMatrix, 0);
        
        // Match camera spherical movements slightly for organic breathing lag
        float offsetBarrelX = 1.2f - (cameraYaw * 0.05f);
        float offsetBarrelY = -1.6f - (cameraPitch * 0.05f);
        float offsetBarrelZ = -4.0f;

        Matrix.translateM(mModelMatrix, 0, offsetBarrelX, offsetBarrelY, offsetBarrelZ);
        
        // Barrel Tube Cylinder
        Matrix.scaleM(mModelMatrix, 0, 0.25f, 0.25f, 6.0f);
        drawCube(mModelMatrix, new float[]{0.15f, 0.16f, 0.18f, 1.0f}); // Dark Gunmetal Steel
    }

    private void drawCube(float[] modelMatrix, float[] color) {
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, cubeBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    private void drawCone(float[] modelMatrix, float[] color) {
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, treeConeBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 16 * 6);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    private void drawSphere(float[] modelMatrix, float[] color) {
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, sphereBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 8 * 8 * 6);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
