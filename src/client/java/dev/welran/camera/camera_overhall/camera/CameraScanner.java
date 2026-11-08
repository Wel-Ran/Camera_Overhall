package dev.welran.camera.camera_overhall.camera;

import dev.welran.camera.camera_overhall.mixin.client.SkullBlockEntityAccessor;

import dev.welran.camera.camera_overhall.util.NameParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;


public final class CameraScanner {
    private static final long STALE_AFTER_TICKS = 40L;
    private static final int MAX_CHUNK_SCAN_RADIUS = 8;

    private CameraScanner() {
    }

    public static void scanLoadedWorld(Minecraft client, CameraRegistry registry) {
        if (client == null || client.level == null || client.player == null || registry == null) {
            return;
        }


        long currentTick = System.nanoTime() / 50_000_000L;

        scanEntityCameras(client, registry, currentTick);
        scanSkullBlockCameras(client, registry, currentTick);
        registry.purgeStaleEntries(currentTick, STALE_AFTER_TICKS);
    }

    private static void scanEntityCameras(Minecraft client, CameraRegistry registry, long currentTick) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            ItemStack helmetStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
            if (helmetStack.isEmpty()) {
                continue;
            }

            Component customName = helmetStack.getCustomName();
            if (customName == null) {
                continue;
            }

            Optional<NameParser.ParsedCamera> parsed = NameParser.parseCameraName(customName.getString());
            if (parsed.isEmpty()) {
                continue;
            }

            NameParser.ParsedCamera camera = parsed.get();
            Vec3 cameraPosition = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D);
            CameraEntry entry = new CameraEntry(
                    camera.networkId(),
                    camera.cameraIndex(),
                    CameraTargetType.ENTITY_EQUIPMENT,
                    null,
                    entity.getUUID(),
                    cameraPosition,
                    livingEntity.getYHeadRot(),
                    livingEntity.getXRot(),
                    currentTick
            );
            registry.registerOrUpdate(entry);
        }
    }

    private static void scanSkullBlockCameras(Minecraft client, CameraRegistry registry, long currentTick) {
        BlockPos playerPosition = BlockPos.containing(client.player.position());
        int centerChunkX = playerPosition.getX() >> 4;
        int centerChunkZ = playerPosition.getZ() >> 4;
        int scanRadius = Math.max(2, Math.min(client.options.getEffectiveRenderDistance(), MAX_CHUNK_SCAN_RADIUS));

        for (int chunkX = centerChunkX - scanRadius; chunkX <= centerChunkX + scanRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - scanRadius; chunkZ <= centerChunkZ + scanRadius; chunkZ++) {
                LevelChunk chunk = client.level.getChunkSource().getChunk(
                        chunkX, chunkZ, ChunkStatus.FULL, false
                );
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof SkullBlockEntity)) {
                        continue;
                    }

                    Component customName = ((SkullBlockEntityAccessor) blockEntity).camera_overhall$getCustomName();
                    if (customName == null) {
                        continue;
                    }

                    Optional<NameParser.ParsedCamera> parsed = NameParser.parseCameraName(customName.getString());
                    if (parsed.isEmpty()) {
                        continue;
                    }

                    NameParser.ParsedCamera camera = parsed.get();
                    BlockPos blockPos = blockEntity.getBlockPos();
                    BlockState state = blockEntity.getBlockState();
                    float yaw = 0.0F;
                    if (state.getBlock() instanceof WallSkullBlock) {
                        yaw = state.getValue(WallSkullBlock.FACING).toYRot();
                    } else if (state.hasProperty(SkullBlock.ROTATION)) {
                        yaw = RotationSegment.convertToDegrees(
                                state.getValue(SkullBlock.ROTATION)
                        ) - 180.0F;
                    }


                    double yawRadians = Math.toRadians(yaw);
                    double forwardX = -Math.sin(yawRadians);
                    double forwardZ = Math.cos(yawRadians);
                    Vec3 cameraPosition = Vec3.atCenterOf(blockPos)
                            .add(forwardX * 0.42D, 0.0D, forwardZ * 0.42D);

                    CameraEntry entry = new CameraEntry(
                            camera.networkId(),
                            camera.cameraIndex(),
                            CameraTargetType.BLOCK_ENTITY,
                            blockPos,
                            null,
                            cameraPosition,
                            yaw,
                            0.0F,
                            currentTick
                    );
                    registry.registerOrUpdate(entry);
                }
            }
        }
    }
}
