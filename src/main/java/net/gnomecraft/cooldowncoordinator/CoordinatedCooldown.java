package net.gnomecraft.cooldowncoordinator;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;

/**
 * Interface to enable automatic coordination of item movement cooldown.
 *
 * <p>Implement this interface to enable other implementing item movement block entities to notify yours
 * when they move items into yours in a manner which would trigger a cooldown in a vanilla hopper.
 *
 * <p><b>NOTE:</b> At this time, you must implement your Inventory in a vanilla-compliant manner.
 * The Fabric transfer API's hopper mixin will prevent notification by vanilla hoppers otherwise.
 * (F.e. you can {@code implement Inventory } to be compatible with both the transfer API and cooldown notification.)
 */
@SuppressWarnings("unused")
public interface CoordinatedCooldown {
    /**
     * This method will be called when an implementor of the interface should set a cooldown timer.  It is
     * effectively a private method for use by the CooldownCoordinator, and implementations may vary.  For these
     * reasons, there is no default implementation.
     *
     * <p>To mirror vanilla behavior, implementors should keep track of world.getTime() at each server tick()
     * in order to know if they have already been called for the currently processing tick.  In the event the
     * implementing block entity has already run the current tick, the cooldown timer should be reduced by 1 tick.
     *
     * <p>Notification is considered successful when this method is called, regardless of what the method does
     * with the notification.  Notification cannot be rejected and error conditions must be handled internally.
     *
     * <p>For example, here is what the implementation for vanilla hoppers would look like:
     * <pre>{@code
     * public class HopperBlockEntity extends LootableContainerBlockEntity implements Hopper, CoordinatedCooldown {
     *     private long lastTickTime;
     *     ...
     *
     *     public static void serverTick(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity) {
     *         blockEntity.lastTickTime = world.getTime();
     *         ...
     *     }
     *
     *     public void notifyCooldown() {
     *         if (!this.isDisabled()) {
     *             if (this.lastTickTime >= world.getTime()) {
     *                 this.setTransferCooldown(7);
     *             } else {
     *                 this.setTransferCooldown(8);
     *             }
     *             this.markDirty();
     *         }
     *     }
     * }
     * }</pre>
     */
    void notifyCooldown();

    /**
     * This method implements a Lithium API so we can be notified by Lithium's reimplementation of the
     * HopperBlockEntity code, which defeats mixins to a large portion of the vanilla Hopper code.
     *
     * <a href="https://github.com/CaffeineMC/lithium-fabric/issues/426">Lithium issue 426</a>
     *
     * @return boolean Whether Lithium should notify a CC implementor when a Hopper pushes to its inventory
     */
    default boolean canReceiveTransferCooldown() {
        // Let Lithium handle cooldown notification between hoppers in whatever manner it expects will work...
        return !(this instanceof HopperBlockEntity);
    }

    /**
     * This method implements a Lithium API so we can be notified by Lithium's reimplementation of the
     * HopperBlockEntity code, which defeats mixins to a large portion of the vanilla Hopper code.
     *
     * <a href="https://github.com/CaffeineMC/lithium-fabric/issues/426">Lithium issue 426</a>
     *
     * @param currentTime Game tick time at which Lithium is processing the notification
     */
    default void setTransferCooldown(long currentTime) {
        if (this instanceof BlockEntity blockEntity) {
            CooldownCoordinator.notify(blockEntity);
        }
    }
}