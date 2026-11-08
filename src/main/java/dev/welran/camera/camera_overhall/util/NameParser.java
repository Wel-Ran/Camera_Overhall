package dev.welran.camera.camera_overhall.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class NameParser {
    private static final String NETWORK = "([\\p{L}\\p{N}][\\p{L}\\p{N}._-]{0,63})";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;


    private static final Pattern CAMERA_SPACED = Pattern.compile(
            "^\\s*(?:camera|cam|камера)\\s*[:#/_-]?\\s*" + NETWORK + "\\s+(\\d{1,4})\\s*$", FLAGS
    );
    private static final Pattern CAMERA_SEPARATED = Pattern.compile(
            "^\\s*(?:camera|cam|камера)\\s*[:#/_-]\\s*" + NETWORK + "\\s*[:/#_-]\\s*(\\d{1,4})\\s*$", FLAGS
    );
    private static final Pattern MONITOR = Pattern.compile(
            "^\\s*(?:monitor|mon|монитор)\\s*[:#/_-]?\\s*" + NETWORK + "\\s*$", FLAGS
    );

    private NameParser() {
    }

    public static Optional<ParsedCamera> parseCameraName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = CAMERA_SPACED.matcher(rawName);
        if (!matcher.matches()) {
            matcher = CAMERA_SEPARATED.matcher(rawName);
        }
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ParsedCamera(
                    matcher.group(1).toLowerCase(Locale.ROOT),
                    Integer.parseInt(matcher.group(2))
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<ParsedMonitor> parseMonitorName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = MONITOR.matcher(rawName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedMonitor(matcher.group(1).toLowerCase(Locale.ROOT)));
    }

    public record ParsedCamera(String networkId, int cameraIndex) {
    }

    public record ParsedMonitor(String networkId) {
    }
}
