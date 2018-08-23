package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Stack;

public abstract class TeleportBase extends AbstractPlayerCommand {

    public TeleportBase(String name, String description, Plugin plugin) {
        super(name, description, plugin);
    }

    /**
     * Get the stack of previous teleport locations
     *
     * @param player
     * @return
     */
    protected Stack<Location> getForwardStack(final Player player) {
        return getStack(player, Constants.PLAYER_FORWARD_STACK_METAKEY);
    }

    /**
     * Get the stack of previous /forward locations and push the target location to it
     *
     * @param player
     * @return
     */
    protected Stack<Location> getBackStack(final Player player) {
        return getStack(player, Constants.PLAYER_BACK_STACK_METAKEY);
    }

    @SuppressWarnings("unchecked")
    private Stack<Location> getStack(final Player player, final String metadataKey) {
        final List<MetadataValue> metadata = player.getMetadata(metadataKey);
        return metadata.isEmpty() ? new Stack<>() : (Stack<Location>) metadata.get(0).value();
    }

    /**
     * Pop items off the stack, adding popped elements to the opposite stack
     *
     * @param player
     * @param numSteps
     * @param pushTo
     * @param popFrom
     * @return
     */
    protected Location getTarget(final Player player, final int numSteps, final Stack<Location> pushTo, final Stack<Location> popFrom) {
        Location target = player.getLocation();

        for (int i = 0; i < numSteps; i++) {
            pushTo.push(target);
            target = popFrom.pop();

            if (popFrom.empty()) {
                break;
            }
        }

        return target;
    }

    /**
     * Set the status to indicate that the next teleport shouldn't be added to the list
     *
     * @param player
     */
    protected void skipBackAdd(final Player player) {
        player.setMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY, new FixedMetadataValue(mPlugin, true));
    }

    /**
     * Save updated stacks
     *
     * @param player
     * @param forwardStack
     * @param backStack
     */
    protected void saveUpdatedStacks(final Player player, final Stack<Location> forwardStack, final Stack<Location> backStack) {
        player.setMetadata(Constants.PLAYER_FORWARD_STACK_METAKEY, new FixedMetadataValue(mPlugin, forwardStack));
        player.setMetadata(Constants.PLAYER_BACK_STACK_METAKEY, new FixedMetadataValue(mPlugin, backStack));
    }
}
