
package dev.welran.camera.camera_overhall;

import dev.welran.camera.camera_overhall.mixin.client.SkullBlockEntityAccessor;

import dev.welran.camera.camera_overhall.camera.CameraRegistry;
import dev.welran.camera.camera_overhall.camera.CameraEntry;
import dev.welran.camera.camera_overhall.camera.CameraScanner;
import dev.welran.camera.camera_overhall.camera.CameraViewController;
import dev.welran.camera.camera_overhall.screen.CameraViewScreen;
import dev.welran.camera.camera_overhall.config.CameraConfig;
import dev.welran.camera.camera_overhall.util.NameParser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class Camera_overhall implements ClientModInitializer {

    public static final String MOD_ID = "camera_overhall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Camera_overhall instance;

    private final CameraRegistry cameraRegistry = new CameraRegistry();
    private final CameraConfig config = new CameraConfig();

    private int tickCounter;

    @Override
    public void onInitializeClient() {

        instance = this;

        LOGGER.info("Initializing Camera Overhall (client-side)");

        config.load();
        cameraRegistry.clear();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            LOGGER.info("Camera Overhall: block clicked at {}",
                    hitResult.getBlockPos());

            BlockEntity blockEntity =
                    world.getBlockEntity(hitResult.getBlockPos());

            if (!(blockEntity instanceof SkullBlockEntity)) {
                LOGGER.info("Clicked block is not a SkullBlockEntity");
                return InteractionResult.PASS;
            }

            LOGGER.info("Clicked block is a skull");

            Component customName =
                    ((SkullBlockEntityAccessor) blockEntity)
                            .camera_overhall$getCustomName();

            if (customName == null) {
                LOGGER.warn("Skull custom name is NULL");
                return InteractionResult.PASS;
            }

            String skullName = customName.getString();

            LOGGER.info("Skull custom name: '{}'", skullName);

            var monitor = NameParser.parseMonitorName(skullName);

            if (monitor.isEmpty()) {
                LOGGER.warn(
                        "NameParser did not recognize monitor name: '{}'",
                        skullName
                );
                return InteractionResult.PASS;
            }

            Minecraft client = Minecraft.getInstance();
            String networkId = monitor.get().networkId();

            LOGGER.info("Monitor recognized. Network: '{}'", networkId);

            CameraScanner.scanLoadedWorld(client, cameraRegistry);

            List<CameraEntry> cameras =
                    cameraRegistry.getCameras(networkId);

            LOGGER.info("Found {} cameras in network '{}'",
                    cameras.size(), networkId);

            if (cameras.isEmpty()) {
                LOGGER.warn("No cameras found in network '{}'", networkId);
                return InteractionResult.SUCCESS;
            }

            String monitorKey =
                    world.dimension().identifier()
                            + "@"
                            + hitResult.getBlockPos().asLong();

            int rememberedIndex =
                    config.getLastSelectedCamera(monitorKey, -1);

            CameraEntry initialCamera = cameras.stream()
                    .filter(camera ->
                            camera.cameraIndex() == rememberedIndex)
                    .findFirst()
                    .orElseGet(() -> cameras.stream()
                            .min(java.util.Comparator.comparingInt(
                                    CameraEntry::cameraIndex
                            ))
                            .orElseThrow()
                    );

            LOGGER.info("Trying to start camera {}",
                    initialCamera.cameraIndex());

            if (CameraViewController.start(client, initialCamera)) {

                client.gui.setScreen(new CameraViewScreen(
                        networkId,
                        monitorKey,
                        initialCamera,
                        null
                ));

                config.saveLastSelectedCamera(
                        monitorKey,
                        initialCamera.cameraIndex()
                );

                LOGGER.info("Monitor GUI opened successfully");

            } else {
                LOGGER.error("CameraViewController.start() returned false");
            }

            return InteractionResult.SUCCESS;
        });

        // Обновление виртуальной камеры каждый клиентский тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            CameraViewController.tick();

            tickCounter++;

            if (tickCounter >= 20) {
                tickCounter = 0;
                CameraScanner.scanLoadedWorld(client, cameraRegistry);
            }
        });
    }

    public static Camera_overhall getInstance() {
        return instance;
    }

    public CameraRegistry getCameraRegistry() {
        return cameraRegistry;
    }

    public CameraConfig getConfig() {
        return config;
    }
}
