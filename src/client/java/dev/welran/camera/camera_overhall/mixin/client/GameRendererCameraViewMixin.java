package dev.welran.camera.camera_overhall.mixin.client;

import dev.welran.camera.camera_overhall.camera.CameraViewController;
import dev.welran.camera.camera_overhall.render.CameraViewPostEffect;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GameRenderer.class)
public abstract class GameRendererCameraViewMixin {
    @Shadow @Final
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;blitEntityOutline()V",
                    shift = At.Shift.AFTER
            )
    )
    private void camera_overhall$applyCameraImageEffect(CallbackInfo callbackInfo) {
        if (CameraViewController.isActive()) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            CameraViewPostEffect.render(minecraft, partialTick);
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void camera_overhall$hidePlayerHands(CallbackInfo callbackInfo) {
        if (CameraViewController.isActive()) {
            callbackInfo.cancel();
        }
    }
}
