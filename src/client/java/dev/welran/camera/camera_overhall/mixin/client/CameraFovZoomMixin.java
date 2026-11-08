package dev.welran.camera.camera_overhall.mixin.client;

import dev.welran.camera.camera_overhall.camera.CameraViewController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraFovZoomMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void camera_overhall$applyOpticalZoom(
            float partialTicks,
            CallbackInfoReturnable<Float> cir
    ) {
        if (CameraViewController.isActive()) {
            cir.setReturnValue(cir.getReturnValueF() / CameraViewController.getZoomFactor());
        }
    }
}
