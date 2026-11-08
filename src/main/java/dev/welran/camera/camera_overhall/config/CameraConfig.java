
package dev.welran.camera.camera_overhall.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public final class CameraConfig {
    private static final String FILE_NAME = "camera_overhall.properties";

    private static final String KEY_PREFIX = "lastCamera.";
    private static final String ROTATION_PREFIX = "cameraRotation.";

    private final Map<String, Integer> lastSelectedCameras =
            new ConcurrentHashMap<>();

    private final Map<String, CameraRotation> cameraRotations =
            new ConcurrentHashMap<>();

    private final Path configPath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(FILE_NAME);

    public void saveLastSelectedCamera(String monitorKey, int cameraIndex) {
        if (monitorKey == null || monitorKey.isBlank() || cameraIndex < 0) {
            return;
        }

        lastSelectedCameras.put(monitorKey, cameraIndex);
        save();
    }

    public int getLastSelectedCamera(String monitorKey, int defaultIndex) {
        if (monitorKey == null || monitorKey.isBlank()) {
            return defaultIndex;
        }

        return lastSelectedCameras.getOrDefault(monitorKey, defaultIndex);
    }


    public void saveCameraRotation(
            String networkId,
            int cameraIndex,
            float yaw,
            float pitch
    ) {
        if (networkId == null || networkId.isBlank() || cameraIndex < 0) {
            return;
        }

        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return;
        }

        String cameraKey = makeCameraKey(networkId, cameraIndex);

        cameraRotations.put(
                cameraKey,
                new CameraRotation(yaw, pitch)
        );

        save();
    }


    public CameraRotation getCameraRotation(
            String networkId,
            int cameraIndex,
            float defaultYaw,
            float defaultPitch
    ) {
        if (networkId == null || networkId.isBlank() || cameraIndex < 0) {
            return new CameraRotation(defaultYaw, defaultPitch);
        }

        String cameraKey = makeCameraKey(networkId, cameraIndex);

        return cameraRotations.getOrDefault(
                cameraKey,
                new CameraRotation(defaultYaw, defaultPitch)
        );
    }

    private String makeCameraKey(String networkId, int cameraIndex) {
        return networkId + "#" + cameraIndex;
    }

    private String makeRotationPropertyKey(String networkId, int cameraIndex) {
        String encodedNetwork = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(networkId.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return ROTATION_PREFIX + encodedNetwork + "." + cameraIndex;
    }

    public void load() {
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);

            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);

                if (key.startsWith(KEY_PREFIX)) {
                    String monitorKey = key.substring(KEY_PREFIX.length());

                    try {
                        int cameraIndex = Integer.parseInt(value);

                        if (!monitorKey.isBlank() && cameraIndex >= 0) {
                            lastSelectedCameras.put(monitorKey, cameraIndex);
                        }
                    } catch (NumberFormatException ignored) {
                    }

                    continue;
                }

                if (key.startsWith(ROTATION_PREFIX)) {
                    loadCameraRotation(key, value);
                }
            }
        } catch (IOException exception) {
            System.err.println(
                    "[Camera Overhall] Could not load camera config: "
                            + exception.getMessage()
            );
        }
    }

    private void loadCameraRotation(String key, String value) {
        try {
            String encodedPart = key.substring(ROTATION_PREFIX.length());
            int separator = encodedPart.lastIndexOf('.');

            if (separator <= 0) {
                return;
            }

            String encodedNetwork = encodedPart.substring(0, separator);
            int cameraIndex = Integer.parseInt(
                    encodedPart.substring(separator + 1)
            );

            String networkId = new String(
                    Base64.getUrlDecoder().decode(encodedNetwork),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            String[] angles = value.split(",");

            if (cameraIndex < 0 || angles.length != 2) {
                return;
            }

            float yaw = Float.parseFloat(angles[0]);
            float pitch = Float.parseFloat(angles[1]);

            if (!networkId.isBlank()
                    && Float.isFinite(yaw)
                    && Float.isFinite(pitch)) {
                cameraRotations.put(
                        makeCameraKey(networkId, cameraIndex),
                        new CameraRotation(yaw, pitch)
                );
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void save() {
        Properties properties = new Properties();

        lastSelectedCameras.forEach((monitorKey, cameraIndex) ->
                properties.setProperty(
                        KEY_PREFIX + monitorKey,
                        Integer.toString(cameraIndex)
                )
        );

        cameraRotations.forEach((cameraKey, rotation) -> {
            int separator = cameraKey.lastIndexOf('#');

            if (separator <= 0) {
                return;
            }

            String networkId = cameraKey.substring(0, separator);

            try {
                int cameraIndex = Integer.parseInt(
                        cameraKey.substring(separator + 1)
                );

                String propertyKey = makeRotationPropertyKey(
                        networkId,
                        cameraIndex
                );

                properties.setProperty(
                        propertyKey,
                        rotation.yaw() + "," + rotation.pitch()
                );
            } catch (NumberFormatException ignored) {
            }
        });

        try {
            Files.createDirectories(configPath.getParent());

            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "Camera Overhall settings");
            }
        } catch (IOException exception) {
            System.err.println(
                    "[Camera Overhall] Could not save camera config: "
                            + exception.getMessage()
            );
        }
    }

    public record CameraRotation(float yaw, float pitch) {
    }
}
