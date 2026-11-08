package dev.welran.camera.camera_overhall.camera;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record CameraCandidate(
        String networkId,
        int cameraIndex,
        CameraTargetType targetType,
        BlockPos blockPos,
        UUID entityUuid,
        Vec3 position,
        float yaw,
        float pitch
) {
}
