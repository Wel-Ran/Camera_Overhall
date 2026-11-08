package dev.welran.camera.camera_overhall.mixin.client;

import dev.welran.camera.camera_overhall.camera.CameraViewController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LocalPlayerCameraRenderMixin {

    @Shadow @Final
    private LevelRenderer levelRenderer;

    @Inject(
            method = "extractVisibleEntities",
            at = @At("TAIL")
    )
    private void camera_overhall$renderLocalPlayer(
            Camera camera,
            Frustum frustum,
            DeltaTracker deltaTracker,
            LevelRenderState output,
            CallbackInfo ci
    ) {
        if (!CameraViewController.isActive()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null || client.level == null) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(
                !client.level.tickRateManager().isEntityFrozen(player)
        );

        EntityRenderState playerState =
                levelRenderer.entityRenderDispatcher()
                        .extractEntity(player, partialTick);

        if (playerState != null) {
            output.entityRenderStates.add(playerState);
        }
    }
}