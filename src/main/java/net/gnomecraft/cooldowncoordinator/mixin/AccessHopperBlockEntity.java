package net.gnomecraft.cooldowncoordinator.mixin;

import net.minecraft.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HopperBlockEntity.class)
public interface AccessHopperBlockEntity {
    @Accessor("lastTickTime")
    long getLastTickTime();

    @Accessor("transferCooldown")
    void setTransferCooldown(int transferCooldown);

    @Invoker("isDisabled")
    boolean callIsDisabled();
}