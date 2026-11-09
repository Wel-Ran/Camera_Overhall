
package dev.welran.camera.camera_overhall.screen;

import dev.welran.camera.camera_overhall.Camera_overhall;
import dev.welran.camera.camera_overhall.camera.CameraEntry;
import dev.welran.camera.camera_overhall.camera.CameraViewController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.input.MouseButtonEvent;

public final class CameraViewScreen extends Screen {

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int PANEL_WIDTH = 130;
    private static final int PANEL_RIGHT_MARGIN = 4;

    private static final int BUTTON_WIDTH = 27;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 3;

    private static final int CAMERA_COLUMNS = 4;
    private static final int MAX_CAMERAS = 16;

    private static final int PANEL_PADDING = 7;

    private static final int ARROW_WIDTH = 20;
    private static final int ARROW_HEIGHT = 32;

    private static final float ANIMATION_SPEED = 0.20F;
    private static final float ANIMATION_EPSILON = 0.001F;

    private static final int INFO_PANEL_WIDTH = 125;
    private static final int INFO_PANEL_HEIGHT = 29;
    private static final int INFO_PANEL_CUT = 12;

    private static final int PANEL_OUTER = 0xFF000000;
    private static final int PANEL_BORDER = 0xFF555555;
    private static final int PANEL_INNER = 0xFF252525;
    private static final int PANEL_HIGHLIGHT = 0xFF777777;

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final String networkId;
    private final String monitorKey;
    private final Screen returnScreen;

    private CameraEntry currentCamera;

    private boolean panelOpen = false;

    private float panelProgress = 0.0F;
    private float animatedProgress = 0.0F;

    private Button arrowButton;
    private Button previousCameraButton;
    private Button nextCameraButton;

    private final List<Button> cameraButtons = new ArrayList<>();
    private final List<CameraEntry> availableCameras = new ArrayList<>();

    public CameraViewScreen(
            String networkId,
            String monitorKey,
            CameraEntry initialCamera,
            Screen returnScreen
    ) {
        super(Component.literal("Camera Overhall"));

        this.networkId = networkId;
        this.monitorKey = monitorKey;
        this.currentCamera = initialCamera;
        this.returnScreen = returnScreen;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean result = super.mouseReleased(event);

        clearFocus();

        return result;
    }

    @Override
    protected void init() {
        super.init();

        cameraButtons.clear();
        availableCameras.clear();

        availableCameras.addAll(getAvailableCameras());

        createArrowButton();
        createTopNavigationButtons();
        createCameraButtons();

        updateWidgetPositions();
    }

    private List<CameraEntry> getAvailableCameras() {
        return Camera_overhall.getInstance()
                .getCameraRegistry()
                .getCameras(networkId)
                .stream()
                .filter(camera -> camera.cameraIndex() >= 1
                        && camera.cameraIndex() <= 99)
                .sorted(Comparator.comparingInt(CameraEntry::cameraIndex))
                .limit(MAX_CAMERAS)
                .toList();
    }

    private void createArrowButton() {
        arrowButton = Button.builder(
                Component.literal("<"),
                ignored -> {
                    panelOpen = !panelOpen;
                    updateArrowLabel();
                }
        ).bounds(
                0,
                0,
                ARROW_WIDTH,
                ARROW_HEIGHT
        ).build();

        addRenderableWidget(arrowButton);
    }

    private void updateArrowLabel() {
        if (arrowButton != null) {
            arrowButton.setMessage(
                    Component.literal(panelOpen ? ">" : "<")
            );
        }
    }

    private void createTopNavigationButtons() {
        previousCameraButton = Button.builder(
                Component.literal("‹"),
                ignored -> cycleCamera(-1)
        ).bounds(0, 0, 18, 16).build();

        nextCameraButton = Button.builder(
                Component.literal("›"),
                ignored -> cycleCamera(1)
        ).bounds(0, 0, 18, 16).build();

        addRenderableWidget(previousCameraButton);
        addRenderableWidget(nextCameraButton);
    }

    private void cycleCamera(int direction) {
        if (availableCameras.size() < 2 || currentCamera == null) {
            return;
        }

        int currentIndex = 0;

        for (int i = 0; i < availableCameras.size(); i++) {
            if (availableCameras.get(i).cameraIndex() == currentCamera.cameraIndex()) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = Math.floorMod(
                currentIndex + direction,
                availableCameras.size()
        );

        switchCamera(availableCameras.get(nextIndex));
    }

    private void createCameraButtons() {
        for (CameraEntry camera : availableCameras) {
            Button button = Button.builder(
                    Component.literal(
                            String.format("%02d", camera.cameraIndex())
                    ),
                    ignored -> switchCamera(camera)
            ).bounds(
                    0,
                    0,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT
            ).build();

            cameraButtons.add(button);
            addRenderableWidget(button);
        }
    }

    private float easeInOut(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private int getPanelX() {
        return width - PANEL_RIGHT_MARGIN
                - Math.round(PANEL_WIDTH * animatedProgress);
    }

    private int getPanelHeight() {
        int rows = (int) Math.ceil(
                (double) cameraButtons.size() / CAMERA_COLUMNS
        );

        return Math.max(
                115,
                75 + rows * (BUTTON_HEIGHT + BUTTON_GAP) + 12
        );
    }

    private int getPanelTop() {
        return (height - getPanelHeight()) / 2;
    }

    private void updateWidgetPositions() {
        int panelX = getPanelX();
        int panelTop = getPanelTop();
        int panelHeight = getPanelHeight();

        if (arrowButton != null) {
            arrowButton.setX(
                    Math.max(0, panelX - ARROW_WIDTH)
            );

            arrowButton.setY(
                    panelTop + panelHeight / 2 - ARROW_HEIGHT / 2
            );
        }

        int topCenter = width / 2;

        if (previousCameraButton != null) {
            previousCameraButton.setX(topCenter - 82);
            previousCameraButton.setY(6);
            previousCameraButton.active = availableCameras.size() > 1;
        }

        if (nextCameraButton != null) {
            nextCameraButton.setX(topCenter + 64);
            nextCameraButton.setY(6);
            nextCameraButton.active = availableCameras.size() > 1;
        }

        int startX = panelX + PANEL_PADDING;
        int startY = panelTop + 34;

        for (int i = 0; i < cameraButtons.size(); i++) {
            Button button = cameraButtons.get(i);

            int column = i % CAMERA_COLUMNS;
            int row = i / CAMERA_COLUMNS;

            button.setX(
                    startX + column * (BUTTON_WIDTH + BUTTON_GAP)
            );

            button.setY(
                    startY + row * (BUTTON_HEIGHT + BUTTON_GAP)
            );

            boolean isCurrent = currentCamera != null
                    && currentCamera.cameraIndex()
                    == availableCameras.get(i).cameraIndex();

            button.visible = animatedProgress >= 0.05F;

            button.active = panelOpen
                    && animatedProgress >= 0.99F
                    && !isCurrent;
        }
    }

    @Override
    public void tick() {
        super.tick();

        float target = panelOpen ? 1.0F : 0.0F;

        if (Math.abs(panelProgress - target) > ANIMATION_EPSILON) {
            panelProgress += (target - panelProgress) * ANIMATION_SPEED;

            if (Math.abs(target - panelProgress) < 0.005F) {
                panelProgress = target;
            }

            animatedProgress = easeInOut(panelProgress);
        }

        updateWidgetPositions();

    }

    private void switchCamera(CameraEntry camera) {
        if (currentCamera != null
                && currentCamera.cameraIndex() == camera.cameraIndex()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (!CameraViewController.start(client, camera)) {
            return;
        }

        currentCamera = camera;

        Camera_overhall.getInstance()
                .getConfig()
                .saveLastSelectedCamera(
                        monitorKey,
                        camera.cameraIndex()
                );

    }
    private void refreshButtons() {
        if (arrowButton != null) {
            arrowButton.setMessage(
                    Component.literal(panelOpen ? ">" : "<")
            );
        }

        if (previousCameraButton != null) {
            previousCameraButton.setMessage(Component.literal("‹"));
        }

        if (nextCameraButton != null) {
            nextCameraButton.setMessage(Component.literal("›"));
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (CameraViewController.isActive() && verticalAmount != 0.0D) {
            CameraViewController.adjustZoom(verticalAmount);
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawTopHousing(
            GuiGraphicsExtractor graphics,
            int centerX
    ) {
        final int housingHeight = 28;
        final int topHalfWidth = 110;
        final int bottomInset = 12;
        final int topY = 0;

        for (int row = 0; row < housingHeight; row++) {
            int inset = (bottomInset * row) / (housingHeight - 1);
            int halfWidth = topHalfWidth - inset;
            int y = topY + row;

            graphics.fill(
                    centerX - halfWidth,
                    y,
                    centerX + halfWidth,
                    y + 1,
                    PANEL_OUTER
            );

            if (row > 0 && row < housingHeight - 1) {
                graphics.fill(
                        centerX - halfWidth + 1,
                        y,
                        centerX + halfWidth - 1,
                        y + 1,
                        PANEL_HIGHLIGHT
                );
            }

            if (row > 1 && row < housingHeight - 2) {
                graphics.fill(
                        centerX - halfWidth + 2,
                        y,
                        centerX + halfWidth - 2,
                        y + 1,
                        PANEL_INNER
                );
            }
        }

        int bottomHalfWidth = topHalfWidth - bottomInset;

        graphics.fill(
                centerX - bottomHalfWidth + 2,
                topY + housingHeight - 2,
                centerX + bottomHalfWidth - 2,
                topY + housingHeight - 1,
                PANEL_BORDER
        );
    }

    private void drawInfoPanel(GuiGraphicsExtractor graphics) {
        int x = 0;
        int y = height - INFO_PANEL_HEIGHT;

        int right = x + INFO_PANEL_WIDTH;

        for (int row = 0; row < INFO_PANEL_HEIGHT; row++) {
            int cut = INFO_PANEL_CUT
                    - (INFO_PANEL_CUT * row / (INFO_PANEL_HEIGHT - 1));

            int rowRight = right - cut;

            graphics.fill(
                    x,
                    y + row,
                    rowRight,
                    y + row + 1,
                    PANEL_OUTER
            );

            if (row == 0) {
                graphics.fill(
                        x,
                        y + row,
                        rowRight,
                        y + row + 1,
                        0xFFB7B7B7
                );
            } else if (row < INFO_PANEL_HEIGHT - 1) {
                graphics.fill(
                        x + 1,
                        y + row,
                        x + 2,
                        y + row + 1,
                        0xFF777777
                );
            }

            if (row >= 2 && row < INFO_PANEL_HEIGHT - 2) {
                graphics.fill(
                        x + 2,
                        y + row,
                        rowRight - 2,
                        y + row + 1,
                        PANEL_INNER
                );
            }

            if (row == INFO_PANEL_HEIGHT - 1) {
                graphics.fill(
                        x + 2,
                        y + row,
                        rowRight,
                        y + row + 1,
                        0xFF303030
                );
            }
        }

        graphics.fill(
                x + 5,
                y + 4,
                right - INFO_PANEL_CUT - 5,
                y + 5,
                0xFF3A3A3A
        );

        graphics.fill(
                x + 7,
                y + 15,
                right - INFO_PANEL_CUT - 7,
                y + 16,
                PANEL_BORDER
        );
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        drawTopHousing(graphics, width / 2);

        int panelX = getPanelX();
        int panelRight = width + PANEL_WIDTH;
        int panelTop = getPanelTop();
        int panelBottom = panelTop + getPanelHeight();

        graphics.fill(
                panelX,
                panelTop,
                panelRight,
                panelBottom,
                0xFF080808
        );

        graphics.fill(
                panelX + 1,
                panelTop + 1,
                panelRight - 1,
                panelTop + 2,
                0xFFB7B7B7
        );

        graphics.fill(
                panelX + 1,
                panelTop + 1,
                panelX + 2,
                panelBottom - 1,
                0xFFB7B7B7
        );

        graphics.fill(
                panelX + 2,
                panelTop + 2,
                panelRight - 2,
                panelTop + 3,
                0xFF777777
        );

        graphics.fill(
                panelX + 2,
                panelTop + 2,
                panelX + 3,
                panelBottom - 2,
                0xFF777777
        );

        graphics.fill(
                panelX + 2,
                panelBottom - 2,
                panelRight - 1,
                panelBottom - 1,
                0xFF303030
        );

        graphics.fill(
                panelRight - 2,
                panelTop + 2,
                panelRight - 1,
                panelBottom - 1,
                0xFF303030
        );

        graphics.fill(
                panelX + 3,
                panelBottom - 3,
                panelRight - 2,
                panelBottom - 2,
                0xFF171717
        );

        graphics.fill(
                panelRight - 3,
                panelTop + 3,
                panelRight - 2,
                panelBottom - 2,
                0xFF171717
        );

        graphics.fill(
                panelX + 3,
                panelTop + 3,
                panelRight - 3,
                panelBottom - 3,
                0xFF292929
        );

        graphics.fill(
                panelX + 5,
                panelTop + 5,
                panelRight - 5,
                panelBottom - 5,
                0xFF242424
        );

        graphics.fill(
                panelX + 6,
                panelTop + 5,
                panelRight - 6,
                panelTop + 6,
                0xFF3A3A3A
        );

        graphics.centeredText(
                font,
                Component.literal("CAMS"),
                panelX + PANEL_WIDTH / 2,
                panelTop + 9,
                TEXT_COLOR
        );

        graphics.fill(
                panelX + 9,
                panelTop + 28,
                panelRight - 9,
                panelTop + 29,
                0xFF555555
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

        String cameraLabel = currentCamera == null
                ? "CAMERA UNAVAILABLE"
                : "CAMERA " + currentCamera.cameraIndex();

        graphics.centeredText(
                font,
                Component.literal(cameraLabel),
                width / 2,
                3,
                TEXT_COLOR
        );

        graphics.centeredText(
                font,
                Component.literal(
                        String.format(
                                "%.1fx",
                                CameraViewController.getZoomFactor()
                        )
                ),
                width / 2,
                15,
                TEXT_COLOR
        );

        LocalDateTime moscowTime = LocalDateTime.now(ZoneId.systemDefault());

        String clock = moscowTime.format(CLOCK_FORMAT);
        String date = moscowTime.format(DATE_FORMAT);

        drawInfoPanel(graphics);

        int panelCenterX = INFO_PANEL_WIDTH / 2 - INFO_PANEL_CUT / 2;
        int panelY = height - INFO_PANEL_HEIGHT;

        graphics.centeredText(
                font,
                Component.literal("Network: " + networkId),
                panelCenterX,
                panelY + 4,
                TEXT_COLOR
        );

        graphics.centeredText(
                font,
                Component.literal(date + "  " + clock),
                panelCenterX,
                panelY + 17,
                TEXT_COLOR
        );
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();

        CameraViewController.stop(client);

        client.gui.setScreen(returnScreen);
    }
}
