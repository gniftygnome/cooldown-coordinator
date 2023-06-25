package net.gnomecraft.cooldowncoordinator.mixin;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlockEntity.class)
public abstract class MixinHopperBlockEntity extends LootableContainerBlockEntity implements CoordinatedCooldown {

    @Shadow private long lastTickTime;
    @Shadow protected abstract boolean isDisabled();
    @Shadow protected abstract void setTransferCooldown(int transferCooldown);

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
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void CooldownCoordinator$injectCoordinator(Inventory from, Inventory to, ItemStack stack, int slot, Direction side, CallbackInfoReturnable<ItemStack> cir, ItemStack itemStack, boolean bl, boolean bl2) {
        // bl2 indicates whether the destination inventory was empty before the hopper moved an item into it
        if (bl2 && to instanceof BlockEntity toEntity) {
            CooldownCoordinator.notify(toEntity);
        }
    }

    // This injection bypasses the Fabric transfer API's implementation of HopperBLockEntity's insert() method.
    // Ultimately this is basically code that should go in the transfer API when/if this becomes part of the API.
    @Inject(
            at = @At(value = "HEAD"),
            method = "insert(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/inventory/Inventory;)Z",
            locals = LocalCapture.NO_CAPTURE,
            cancellable = true
    )
    @SuppressWarnings("UnstableApiUsage")
    private static void CooldownCoordinator$bypassFAPIStorageInsert(World world, BlockPos pos, BlockState state, Inventory inventory, CallbackInfoReturnable<Boolean> cir) {
        /*
         * Copy of HopperBlockEntity.getOutputInventory() which is captured by HopperBlockEntityMixin.hookInsert()
         */
        Direction facing = state.get(HopperBlock.FACING);
        Inventory targetInventory = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));

        /*
         * Reimplementation of the transfer API's HopperBlockEntityMixin.hookInsert()
         */
        // Let vanilla handle the transfer if it found an inventory.
        if (targetInventory != null) return;

        // Otherwise inject our transfer logic.
        Direction direction = state.get(HopperBlock.FACING);
        BlockPos targetPos = pos.offset(direction);
        Storage<ItemVariant> target = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        boolean targetEmpty = StorageUtil.findStoredResource(target) == null;

        if (target != null) {
            long moved = StorageUtil.move(
                    InventoryStorage.of(inventory, direction),
                    target,
                    iv -> true,
                    1,
                    null
            );

            if (moved == 1) {
                if (targetEmpty) {
                    CooldownCoordinator.notify(world.getBlockEntity(targetPos));
                }

                cir.setReturnValue(true);
            }
        }

        /*
         * Bypass the rest of HopperBlockEntity.insert() regardless.
         * This is necessary (for performance) to prevent the transfer API from retrying the move
         * we just tried.  There may be some mod compatibility consequences of this ... I am not
         * certain exactly why the transfer API elects not to return false here itself.
         */
        cir.setReturnValue(false);
    }
}