package dev.welran.camera.camera_overhall.mixin.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import dev.welran.camera.camera_overhall.render.CameraViewPostPassAccess;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.renderer.PostPass;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(PostPass.class)
public abstract class CameraViewPostPassMixin implements CameraViewPostPassAccess {
    @Shadow @Final
    private Map<String, GpuBuffer> customUniforms;

    @Override
    public void camera_overhall$writeUniforms(
            Consumer<Std140SizeCalculator> layout,
            Consumer<Std140Builder> values
    ) {
        if (!customUniforms.containsKey("CameraView")) {
            return;
        }

        Std140SizeCalculator calculator = new Std140SizeCalculator();
        layout.accept(calculator);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, calculator.get());
            values.accept(builder);

            GpuBuffer replacement = RenderSystem.getDevice().createBuffer(
                    () -> "Camera Overhall post-effect parameters",
                    128,
                    builder.get()
            );

            GpuBuffer previous = customUniforms.put("CameraView", replacement);
            if (previous != null) {
                previous.close();
            }
        }
    }
}
