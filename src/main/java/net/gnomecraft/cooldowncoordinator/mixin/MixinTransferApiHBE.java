package net.gnomecraft.cooldowncoordinator.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.mixin.transfer.HopperBlockEntityMixin;
import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/*
 * This Mixin Squared mixin modifies Fabric Transfer API's HopperBlockEntity mixin to add our logic
 * which notifies CooldownCoordinator whenever a HopperBlockEntity or extender thereof uses FTAPI
 * to move item(s) into a non-Inventory block entity.
 */
@Mixin(value = HopperBlockEntityMixin.class, priority = 1500)
public final class MixinTransferApiHBE {
    @TargetHandler(
            mixin = "com.bawnorton.mixin.TargetClassMixin",
            name = "hookInsert"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/transfer/v1/storage/StorageUtil;move(Lnet/fabricmc/fabric/api/transfer/v1/storage/Storage;Lnet/fabricmc/fabric/api/transfer/v1/storage/Storage;Ljava/util/function/Predicate;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J"
            )
    )
    @SuppressWarnings("unused")
    private long CooldownCoordinator$extendFAPIStorageInsert(Storage<ItemVariant> from, Storage<ItemVariant> to, Predicate<ItemVariant> filter, long maxAmount, TransactionContext transaction, Operation<Long> operation, World world, @Local(ordinal = 0) BlockPos targetPos) {
        // Was the target empty before we moved something to it?
        boolean targetEmpty = StorageUtil.findStoredResource(to) == null;

        // Now go ahead and do the move as defined by FTAPI's mixin.
        long moved = operation.call(from, to, filter, maxAmount, transaction);

        // If something was moved, notify CooldownCoordinator.
        if (targetEmpty && moved > 0) {
            CooldownCoordinator.notify(world.getBlockEntity(targetPos));
        }

        // Return what the original operation returned.
        return moved;
    }
}
