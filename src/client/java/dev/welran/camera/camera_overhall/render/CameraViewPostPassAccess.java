package dev.welran.camera.camera_overhall.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;

import java.util.function.Consumer;

public interface CameraViewPostPassAccess {
    void camera_overhall$writeUniforms(
            Consumer<Std140SizeCalculator> layout,
            Consumer<Std140Builder> values
    );
}
