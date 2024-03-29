package net.gnomecraft.cooldowncoordinator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CooldownCoordinator implements ModInitializer {
    public static final String modId = "cooldown-coordinator";
    public static final Logger LOGGER = LoggerFactory.getLogger(modId);

    @Override
    public void onInitialize() {
        LOGGER.info("Cooldown Coordinator library loaded.");
    }

    /**
     * Participating item transferring entities should call this method with the destination block entity
     * <b>only</b> when the destination inventory was previously empty and <b>after</b> transferring the item(s).
     *
     * <p>In addition to notifying the target block entity when item(s) are pushed, a complete cooldown
     * implementation must also set its own cooldown timer once it has pushed or pulled items.  The local part
     * of the cooldown is outside the scope of this mod at this time.
     *
     * <p>Calls should only be made in the server context and will silently fail to notify in the client context.
     *
     * <p>For example, implementing code might look something like this:
     * <pre>{@code
     *     private boolean push(World world, BlockPos pos, BlockState state, MyEntity entity) {
     *         Direction facing = state.get(MyBlock.FACING);
     *         BlockEntity targetEntity = world.getBlockEntity(pos.offset(facing));
     *         ItemStorage sourceStorage = ItemStorage.SIDED.find(world, pos, state, entity, facing);
     *         ItemStorage targetStorage = ItemStorage.SIDED.find(world, pos.offset(facing), facing.getOpposite());
     *
     *         if (sourceStorage != null && targetStorage != null) {
     *             boolean targetEmpty = CooldownCoordinator.isItemStorageEmpty(targetStorage);
     *
     *             if (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0) {
     *                 if (targetEmpty && targetEntity != null) {
     *                     CooldownCoordinator.notify(targetEntity);
     *                 }
     *                 this.setCooldown(8);
     *                 this.markDirty();
     *
     *                 return true;
     *             }
     *         }
     *
     *         return false;
     *     }
     * }</pre>
     *
     * @param entity The block entity we may notify of coordinated cooldown
     * @return A boolean indicating whether the target was notified
     */
    public static boolean notify(@Nullable BlockEntity entity) {
        if (entity == null) {
            return false;
        }
        World world = entity.getWorld();
        if (world == null || world.isClient()) {
            return false;
        }

        if (entity instanceof CoordinatedCooldown coordinatedEntity) {
            CooldownCoordinator.LOGGER.debug("Notifying BE: " + entity);
            coordinatedEntity.notifyCooldown();
            return true;
        } else {
            CooldownCoordinator.LOGGER.debug("Cannot notify BE: " + entity);
            return false;
        }
    }

    /**
     * Helper method to check whether a Storage is empty.
     *
     * @param storage Storage to evaluate for emptiness
     * @return True if storage contains only blank Variants and false if not
     */
    public static boolean isStorageEmpty(@Nullable Storage<?> storage) {
        if (storage == null) {
            return true;
        }

        return StorageUtil.findStoredResource(storage) == null;
    }

    /**
     * Helper method to check whether ItemVariant Storage is empty.
     *
     * DEPRECATED in favor of {@code boolean isStorageEmpty(@Nullable Storage<?> storage) }
     *
     * @param storage ItemVariant Storage to evaluate for emptiness
     * @return True if storage contains only blank ItemVariants and false if not
     */
    @Deprecated(forRemoval = true)
    public static boolean isItemStorageEmpty(@Nullable Storage<ItemVariant> storage) {
        return CooldownCoordinator.isStorageEmpty(storage);
    }

    /**
     * Helper method to check whether an Inventory is empty.
     *
     * @param inventory Inventory to evaluate for emptiness
     * @return True if inventory contains only empty ItemStacks and false if not
     */
    public static boolean isInventoryEmpty(@Nullable Inventory inventory) {
        if (inventory == null) {
            return true;
        }

        for (int slot = 0; slot < inventory.size(); ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}