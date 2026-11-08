
package dev.welran.camera.camera_overhall.mixin.client;

import dev.welran.camera.camera_overhall.camera.CameraViewController;

import net.minecraft.client.gui.Gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class GuiHudMixin {

    @ModifyVariable(
            method = "extractRenderState",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private boolean camera_overhall$hideHudDuringCameraView(
            boolean shouldRenderLevel
    ) {
        if (CameraViewController.isActive()) {
            return false;
        }

        return shouldRenderLevel;
    }
}
