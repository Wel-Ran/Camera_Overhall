
package dev.welran.camera.camera_overhall.screen;

import dev.welran.camera.camera_overhall.Camera_overhall;
import dev.welran.camera.camera_overhall.camera.CameraEntry;
import dev.welran.camera.camera_overhall.camera.CameraViewController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

public final class MonitorScreen extends Screen {

    private final String networkId;
    private boolean openingAttempted;

    public MonitorScreen(String networkId) {
        super(Component.literal("Монитор: " + networkId));
        this.networkId = networkId;
    }

    @Override
    protected void init() {
        super.init();

        if (!openingAttempted) {
            openingAttempted = true;
            openLastCamera();
        }
    }

    private void openLastCamera() {
        List<CameraEntry> cameras = Camera_overhall.getInstance()
                .getCameraRegistry()
                .getCameras(networkId)
                .stream()
                .sorted(Comparator.comparingInt(CameraEntry::cameraIndex))
                .toList();

        if (cameras.isEmpty()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Закрыть"),
                    ignored -> this.onClose()
            ).bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20).build());

            return;
        }

        int firstIndex = cameras.getFirst().cameraIndex();

        int savedIndex = Camera_overhall.getInstance()
                .getConfig()
                .getLastSelectedCamera(networkId, firstIndex);

        CameraEntry selectedCamera = cameras.stream()
                .filter(camera -> camera.cameraIndex() == savedIndex)
                .findFirst()
                .orElse(cameras.getFirst());

        Minecraft client = Minecraft.getInstance();

        if (!CameraViewController.start(client, selectedCamera)) {
            Camera_overhall.LOGGER.warn(
                    "Could not start camera {} in network '{}'",
                    selectedCamera.cameraIndex(),
                    networkId
            );
            return;
        }


        client.gui.setScreen(new CameraViewScreen(
                networkId,
                networkId,
                selectedCamera,
                null
        ));

        Camera_overhall.LOGGER.info(
                "Automatically opened camera {} in network '{}'",
                selectedCamera.cameraIndex(),
                networkId
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(
                this.font,
                Component.literal("Камеры сети не найдены"),
                this.width / 2,
                this.height / 2 - 10,
                0xFFFFFF
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
