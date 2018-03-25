package pe.project.listeners;

import java.util.List;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import pe.project.Constants;
import pe.project.Plugin;

public class PlayerListener implements Listener {
	Plugin mPlugin = null;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		if (mPlugin.mServerProperties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}

		/* Mark this player as inventory locked until their inventory data is applied */
		event.getPlayer().setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(mPlugin, true));

		new BukkitRunnable() {
			Integer tick = 0;
			@Override
			public void run() {
				if (++tick == 20) {
					Player player = event.getPlayer();

					mPlugin.mTrackingManager.addEntity(player);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		if (mPlugin.mServerProperties.getJoinMessagesEnabled() == false) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();

		mPlugin.mTrackingManager.removeEntity(player);
	}

	@EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
	public void PlayerTeleportEvent(PlayerTeleportEvent event) {
		Player player = event.getPlayer();

		// Only add the location to the back stack if the player didn't just use /back or /forward
		if (player.hasMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY)) {
			player.removeMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY, mPlugin);
		} else {
			// Get the stack of previous teleport locations
			Stack<Location> backStack = null;
			if (player.hasMetadata(Constants.PLAYER_BACK_STACK_METAKEY)) {
				List<MetadataValue> val = player.getMetadata(Constants.PLAYER_BACK_STACK_METAKEY);
				if (val != null && !val.isEmpty()) {
					backStack = (Stack<Location>)val.get(0).value();
				}
			}

			if (backStack == null) {
				backStack = new Stack<Location>();
			}

			backStack.push(event.getFrom());
			player.setMetadata(Constants.PLAYER_BACK_STACK_METAKEY, new FixedMetadataValue(mPlugin, backStack));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		ItemStack item = event.getItem();

		if (action == Action.RIGHT_CLICK_BLOCK &&
			item != null &&
			item.getType() == Material.FISHING_ROD) {
			event.setCancelled(true);
		}
	}
}
