package net.gnomecraft.cooldowncoordinator.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.gnomecraft.cooldowncoordinator.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity extends RandomizableContainerBlockEntity implements CoordinatedCooldown {
    @Shadow
    private long tickedGameTime;
    @Shadow
    protected abstract boolean isOnCustomCooldown();
    @Shadow
    protected abstract void setCooldown(int transferCooldown);

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
     *     if (entity instanceof CoordinatedCooldown cooldownEntity) {
     *         cooldownEntity.notifyCooldown();
     *     }
     * }</pre>
     *
     * However, don't do that.  Just call {@code CooldownCoordinator.notify(targetEntity); } and let it notify.
     */
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void notifyCooldown() {
        // Insert implementation of the CoordinatedCooldown interface into HBE.
        // This mirrors what happens just after one hopper transfers to another.
        // See the example in the javadocs for CoordinatedCooldown.notifyCooldown()
        if (!this.isOnCustomCooldown()) {
            if (this.level != null && this.tickedGameTime >= this.level.getGameTime()) {
                this.setCooldown(7);
            } else {
                this.setCooldown(8);
            }
            this.setChanged();
        }

    }

    // This injection patches the traditional Inventory-based HopperBlockEntity code to call notify().
    @Inject(
            method = "tryMoveInItem",
			at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/Container;setChanged()V",
                    shift = At.Shift.BEFORE
            )
    )
    @SuppressWarnings("DiscouragedShift")
    private static void CooldownCoordinator$injectCoordinator(Container from, Container container, ItemStack stack, int slot, Direction side, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 1) boolean wasEmpty) {
        // wasEmpty indicates whether the destination inventory was empty before the hopper moved an item into it
        if (wasEmpty && container instanceof BlockEntity toEntity) {
            CooldownCoordinator.notify(toEntity);
        }
    }
}