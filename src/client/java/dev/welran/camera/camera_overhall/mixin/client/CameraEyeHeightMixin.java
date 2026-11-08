
package dev.welran.camera.camera_overhall.mixin.client;

import dev.welran.camera.camera_overhall.camera.CameraViewController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraEyeHeightMixin {

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    private Entity entity;

    @Inject(method = "tick", at = @At("TAIL"))
    private void camera_overhall$restoreEyeHeight(CallbackInfo ci) {

        if (CameraViewController.isActive()) {
            eyeHeight = 0.0F;
            eyeHeightOld = 0.0F;
            return;
        }

        if (CameraViewController.consumeEyeHeightRestore()) {
            if (entity != null) {
                float targetHeight = entity.getEyeHeight();

                eyeHeight = targetHeight;
                eyeHeightOld = targetHeight;
            }
        }
    }
}
