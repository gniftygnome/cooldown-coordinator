package net.gnomecraft.cooldowncoordinator.mixin;

import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.gnomecraft.cooldowncoordinator.CoordinatedCooldown;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity {
    @Inject(
			at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Inventory;markDirty()V",
                    shift = At.Shift.BEFORE
            ),
            method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void injectCoordinator(Inventory from, Inventory to, ItemStack stack, int slot, Direction side, CallbackInfoReturnable<ItemStack> cir, ItemStack itemStack, boolean bl, boolean bl2) {
        // bl2 indicates whether the destination inventory was empty before the hopper moved an item into it
        if (bl2 && to instanceof CoordinatedCooldown) {
            CooldownCoordinator.LOGGER.debug("Notifying BE: " + to);
            ((CoordinatedCooldown) to).notifyCooldown();
        }
    }
}