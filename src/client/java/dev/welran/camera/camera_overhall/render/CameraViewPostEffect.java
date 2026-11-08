
package dev.welran.camera.camera_overhall.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.welran.camera.camera_overhall.camera.CameraViewController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public final class CameraViewPostEffect {

    private static final Identifier EFFECT_ID =
            Identifier.fromNamespaceAndPath("camera_overhall", "camera_view");

    private static final CrossFrameResourcePool RESOURCE_POOL =
            new CrossFrameResourcePool(3);

    private CameraViewPostEffect() {
    }

    public static void render(Minecraft client, float partialTick) {
        if (client == null || !CameraViewController.isActive()) {
            return;
        }

        PostChain chain = client.getShaderManager().getPostChain(
                EFFECT_ID,
                LevelTargetBundle.MAIN_TARGETS
        );

        if (chain == null) {
            return;
        }

        float focus = CameraViewController.getFocusProgress(partialTick);
        float zoom = CameraViewController.getZoomFactor();

        final float distortionTime = client.level != null
                ? (client.level.getGameTime() + partialTick) / 20.0F
                : 0.0F;

        ((CameraViewPostChainAccess) chain).camera_overhall$writeUniforms(
                size -> size.putFloat()
                        .putFloat()
                        .putFloat()
                        .putFloat()
                        .putFloat(),

                values -> values
                        .putFloat(1.0F)
                        .putFloat(0.30F)
                        .putFloat(focus)
                        .putFloat(zoom)
                        .putFloat(distortionTime)
        );

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        chain.process(mainTarget, RESOURCE_POOL);
    }
}
