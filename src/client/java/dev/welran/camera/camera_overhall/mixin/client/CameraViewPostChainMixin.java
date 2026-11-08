package dev.welran.camera.camera_overhall.mixin.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.welran.camera.camera_overhall.render.CameraViewPostChainAccess;
import dev.welran.camera.camera_overhall.render.CameraViewPostPassAccess;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Consumer;

@Mixin(PostChain.class)
public abstract class CameraViewPostChainMixin implements CameraViewPostChainAccess {
    @Shadow @Final
    private List<PostPass> passes;

    @Override
    public void camera_overhall$writeUniforms(
            Consumer<Std140SizeCalculator> layout,
            Consumer<Std140Builder> values
    ) {
        for (PostPass pass : passes) {
            ((CameraViewPostPassAccess) pass).camera_overhall$writeUniforms(layout, values);
        }
    }
}
