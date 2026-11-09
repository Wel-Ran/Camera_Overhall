
package dev.welran.camera.camera_overhall.camera;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class CameraViewController {



    private static float focusProgress = 0.0F;
    private static float previousFocusProgress = 0.0F;

    private static final float FOCUS_SMOOTHING = 0.10F;



    private static ArmorStand cameraAnchor;
    private static Entity previousCameraEntity;
    private static CameraType previousCameraType;
    private static CameraEntry currentCamera;



    private static float targetYaw;
    private static float targetPitch;

    private static float currentYaw;
    private static float currentPitch;



    private static float zoomFactor = 1.0F;
    private static float targetZoomFactor = 1.0F;



    private  static  final float ROTATION_SMOOTHING = 0.25F;
    private  static  final  float ROTATION_HOLD_SP = 1.5F;
    private static  final float ROTATION_LIM = 33.5F;

    private static final float MIN_PITCH = -90.0F;
    private static final float MAX_PITCH = 90.0F;


    private static float baseYaw;
    private static float basePitch;

    private static final float MIN_ZOOM = 1.0F;
    private static final float MAX_ZOOM = 3.0F;


    private static boolean rotateUp;
    private static boolean rotateDown;
    private static boolean rotateLeft;
    private static boolean rotateRight;



    private static boolean eyeHeightRestorePending;

    private CameraViewController() {
    }



    public static boolean start(Minecraft client, CameraEntry entry) {
        if (client == null || client.level == null
                || client.player == null || entry == null) {
            return false;
        }

        if (cameraAnchor == null) {
            eyeHeightRestorePending = false;

            previousCameraEntity = client.getCameraEntity() != null
                    ? client.getCameraEntity()
                    : client.player;

            previousCameraType = client.options.getCameraType();

            cameraAnchor = new ArmorStand(
                    client.level,
                    entry.position().x,
                    entry.position().y,
                    entry.position().z
            );

            cameraAnchor.setInvisible(true);
            cameraAnchor.setNoGravity(true);

            client.options.setCameraType(CameraType.FIRST_PERSON);

            focusProgress = 0.0F;
            previousFocusProgress = 0.0F;
        }

        currentCamera = entry;

        focusProgress = 0.0F;
        previousFocusProgress = 0.0F;

        baseYaw = entry.yaw();
        basePitch = entry.pitch();

        targetYaw = entry.yaw();
        targetPitch = entry.pitch();

        currentYaw = entry.yaw();
        currentPitch = entry.pitch();

        zoomFactor = 1.0F;
        targetZoomFactor = 1.0F;

        clearRotationInput();

        cameraAnchor.setPos(
                entry.position().x,
                entry.position().y,
                entry.position().z
        );

        cameraAnchor.setYRot(entry.yaw());
        cameraAnchor.setXRot(entry.pitch());

        cameraAnchor.yHeadRot = entry.yaw();
        cameraAnchor.yHeadRotO = entry.yaw();

        cameraAnchor.yBodyRot = entry.yaw();
        cameraAnchor.yBodyRotO = entry.yaw();

        cameraAnchor.xo = cameraAnchor.getX();
        cameraAnchor.yo = cameraAnchor.getY();
        cameraAnchor.zo = cameraAnchor.getZ();

        cameraAnchor.yRotO = entry.yaw();
        cameraAnchor.xRotO = entry.pitch();

        if (client.getCameraEntity() != cameraAnchor) {
            client.setCameraEntity(cameraAnchor);
        }

        return true;
    }



    public static void stop(Minecraft client) {
        if (client == null || cameraAnchor == null) {
            return;
        }

        clearRotationInput();

        Entity restore = previousCameraEntity;

        if (restore == null || restore.isRemoved()
                || restore.level() != client.level) {
            restore = client.player;
        }

        if (restore != null) {
            restore.xo = restore.getX();
            restore.yo = restore.getY();
            restore.zo = restore.getZ();

            restore.yRotO = restore.getYRot();
            restore.xRotO = restore.getXRot();

            if (restore instanceof LivingEntity living) {
                living.yHeadRotO = living.yHeadRot;
                living.yBodyRotO = living.yBodyRot;
            }
        }

        client.setCameraEntity(restore);

        if (previousCameraType != null) {
            client.options.setCameraType(previousCameraType);
        }

        eyeHeightRestorePending = true;

        cameraAnchor = null;
        previousCameraEntity = null;
        previousCameraType = null;
        currentCamera = null;

        targetZoomFactor = 1.0F;
        zoomFactor = 1.0F;

        focusProgress = 0.0F;
        previousFocusProgress = 0.0F;
    }



    public static void tick() {
        if (cameraAnchor == null) {
            return;
        }

        previousFocusProgress = focusProgress;

        focusProgress += (1.0F - focusProgress) * FOCUS_SMOOTHING;

        if (focusProgress > 0.995F) {
            focusProgress = 1.0F;
        }

        cameraAnchor.xo = cameraAnchor.getX();
        cameraAnchor.yo = cameraAnchor.getY();
        cameraAnchor.zo = cameraAnchor.getZ();

        cameraAnchor.yRotO = cameraAnchor.getYRot();
        cameraAnchor.xRotO = cameraAnchor.getXRot();

        cameraAnchor.yHeadRotO = cameraAnchor.yHeadRot;
        cameraAnchor.yBodyRotO = cameraAnchor.yBodyRot;


        float yawDirection =
                (rotateRight ? 1.0F : 0.0F)
                        - (rotateLeft ? 1.0F : 0.0F);

        float pitchDirection =
                (rotateDown ? 1.0F : 0.0F)
                        - (rotateUp ? 1.0F : 0.0F);

        targetYaw = clamp(
                targetYaw + yawDirection * ROTATION_HOLD_SP,
                baseYaw - ROTATION_LIM,
                baseYaw + ROTATION_LIM

        );

        targetPitch = clamp(
                targetPitch + pitchDirection * ROTATION_HOLD_SP,
                Math.max(MIN_PITCH, basePitch - ROTATION_LIM),
                Math.min(MAX_PITCH, basePitch + ROTATION_LIM)
        );



        currentYaw += wrapDegrees(targetYaw - currentYaw)
                * ROTATION_SMOOTHING;

        currentPitch += (targetPitch - currentPitch)
                * ROTATION_SMOOTHING;



        zoomFactor += (targetZoomFactor - zoomFactor)
                * ROTATION_SMOOTHING;



        cameraAnchor.setYRot(currentYaw);
        cameraAnchor.setXRot(currentPitch);

        cameraAnchor.yHeadRot = currentYaw;
        cameraAnchor.yBodyRot = currentYaw;
    }



    public static float getFocusProgress(float partialTick) {
        partialTick = clamp(partialTick, 0.0F, 1.0F);

        return previousFocusProgress
                + (focusProgress - previousFocusProgress) * partialTick;
    }



    public static void setRotationInput(
            boolean up,
            boolean down,
            boolean left,
            boolean right
    ) {
        if (!isActive()) {
            clearRotationInput();
            return;
        }

        rotateUp = up;
        rotateDown = down;
        rotateLeft = left;
        rotateRight = right;
    }

    private static void clearRotationInput() {
        rotateUp = false;
        rotateDown = false;
        rotateLeft = false;
        rotateRight = false;
    }



    public static void rotate(int xDirection, int yDirection) {
        if (!isActive()) {
            return;
        }

        targetYaw = clamp(
                targetYaw + xDirection * ROTATION_HOLD_SP,
                baseYaw - ROTATION_LIM,
                baseYaw + ROTATION_LIM
        );

        targetPitch = clamp(
                targetPitch + yDirection * ROTATION_HOLD_SP,
                Math.max(MIN_PITCH, basePitch - ROTATION_LIM),
                Math.min(MAX_PITCH, basePitch + ROTATION_LIM)
        );
    }

    public static void resetRotation() {
        if (!isActive()) {
            return;
        }

        clearRotationInput();

        targetYaw = baseYaw;
        targetPitch = basePitch;
    }


    public static void adjustZoom(double amount) {
        if (!isActive()) {
            return;
        }

        targetZoomFactor = clamp(
                targetZoomFactor + (float) amount * 0.25F,
                MIN_ZOOM,
                MAX_ZOOM
        );
    }

    public static float getZoomFactor() {
        return zoomFactor;
    }


    private static float wrapDegrees(float angle) {
        angle %= 360.0F;

        if (angle >= 180.0F) {
            angle -= 360.0F;
        }

        if (angle < -180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isActive() {
        return cameraAnchor != null;
    }



    public static boolean consumeEyeHeightRestore() {
        if (!eyeHeightRestorePending) {
            return false;
        }

        eyeHeightRestorePending = false;
        return true;
    }



    public static boolean isViewingBlock(BlockPos pos) {
        return cameraAnchor != null
                && currentCamera != null
                && currentCamera.targetType() == CameraTargetType.BLOCK_ENTITY
                && currentCamera.blockPos() != null
                && currentCamera.blockPos().equals(pos);
    }
}
