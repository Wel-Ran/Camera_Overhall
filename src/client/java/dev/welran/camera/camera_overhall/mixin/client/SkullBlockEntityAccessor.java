package dev.welran.camera.camera_overhall.mixin.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityAccessor {
    @Accessor("customName")
    Component camera_overhall$getCustomName();
}
