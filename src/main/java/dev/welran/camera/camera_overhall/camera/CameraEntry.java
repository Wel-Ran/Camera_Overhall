package dev.welran.camera.camera_overhall.camera;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record CameraEntry(
        String networkId,
        int cameraIndex,
        CameraTargetType targetType,
        BlockPos blockPos,
        UUID entityUuid,
        Vec3 position,
        float yaw,
        float pitch,
        long lastSeenTick
) {
    public String getUniqueId() {
        if (targetType == CameraTargetType.BLOCK_ENTITY && blockPos != null) {
            return "block_" + blockPos.getX() + "_" + blockPos.getY() + "_" + blockPos.getZ();
        }
        if (targetType == CameraTargetType.ENTITY_EQUIPMENT && entityUuid != null) {
            return "entity_" + entityUuid;
        }
        return "unknown_" + networkId + "_" + cameraIndex;
    }
}
