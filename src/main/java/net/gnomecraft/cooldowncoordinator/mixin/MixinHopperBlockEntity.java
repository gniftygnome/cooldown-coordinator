package net.gnomecraft.cooldowncoordinator.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.gnomecraft.cooldowncoordinator.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity extends LootableContainerBlockEntity implements CoordinatedCooldown {
    @Shadow
    private long lastTickTime;
    @Shadow
    protected abstract boolean isDisabled();
    @Shadow
    protected abstract void setTransferCooldown(int transferCooldown);

    protected MixinHopperBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    /**
     * The notifyCooldown() method of the CoordinatedCooldown interface does not have a default implementation.
     * Because of this, despite our use of interface injection, {@code ((HopperBlockEntity) entity).notifyCooldown() }
     * will not compile.  If you need to call this method directly it is recommended to cast the HopperBlockEntity
     * to CoordinatedCooldown:
     *
     * <pre>{@code
     *     if (entity instanceof CoordinatedCooldown) {
     *         ((CoordinatedCooldown) entity).notifyCooldown();
     *     }
     * }</pre>
     *
     * However, don't do that.  Just call {@code CooldownCoordinator.notify(targetEntity); } and let it notify.
     */
    @Override
    public void notifyCooldown() {
        // Insert implementation of the CoordinatedCooldown interface into HBE.
        // This mirrors what happens just after one hopper transfers to another.
        // See the example in the javadocs for CoordinatedCooldown.notifyCooldown()
        if (!this.isDisabled()) {
            if (this.world != null && this.lastTickTime >= this.world.getTime()) {
                this.setTransferCooldown(7);
            } else {
                this.setTransferCooldown(8);
            }
            this.markDirty();
        }

    }

    // This injection patches the traditional Inventory-based HopperBlockEntity code to call notify().
    @Inject(
			at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Inventory;markDirty()V",
                    shift = At.Shift.BEFORE
            ),
            method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void CooldownCoordinator$injectCoordinator(Inventory from, Inventory to, ItemStack stack, int slot, Direction side, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 1) boolean bl2) {
        // bl2 indicates whether the destination inventory was empty before the hopper moved an item into it
        if (bl2 && to instanceof BlockEntity toEntity) {
            CooldownCoordinator.notify(toEntity);
        }
    }
}