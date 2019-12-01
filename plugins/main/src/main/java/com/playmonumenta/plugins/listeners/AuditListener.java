package com.playmonumenta.plugins.listeners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.Lootable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.AuditLogPacket;

public class AuditListener implements Listener {
	private final Plugin mPlugin;
	private final String mShardName;

	public AuditListener(@Nonnull Plugin plugin, @Nonnull String shardName) {
		mPlugin = plugin;
		mShardName = shardName;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void gamemode(PlayerGameModeChangeEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		GameMode curMode = player.getGameMode();
		GameMode newMode = event.getNewGameMode();
		if ((curMode.equals(GameMode.SURVIVAL) && newMode.equals(GameMode.ADVENTURE))
		    || (curMode.equals(GameMode.ADVENTURE) && newMode.equals(GameMode.SURVIVAL))) {
			// Don't log normal game mode changes
			return;
		}

		log("GameMode: " + player.getName() + " " + curMode.toString() + " -> " + newMode.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void death(PlayerDeathEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getEntity();
		if (!player.isOp()) {
			return;
		}

		log("Death: " + player.getName() + " " + event.getDeathMessage());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void command(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		String cmd = event.getMessage();
		if (cmd.startsWith("/questtrigger")
		    || cmd.startsWith("/msg")) {
			return;
		}

		log("Command: " + player.getName() + " " + cmd);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void block(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		if (!player.getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		log("PlaceDuplicateBlock: " + player.getName() + " " + getItemLogString(event.getItemInHand()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void creative(InventoryCreativeEvent event) {
		if (event.isCancelled()) {
			return;
		}

		if (event.getCurrentItem() != null && !event.getCurrentItem().getType().equals(Material.AIR)) {
			log("InventoryDestroy: " + event.getWhoClicked().getName() + " " + getItemLogString(event.getCurrentItem()));
		}
		if (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) {
			log("CreativeInventory: " + event.getWhoClicked().getName() + " " + getItemLogString(event.getCursor()));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void cloneClick(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}

		/* For some stupid reason UNKNOWN = clone stack with pick block key */
		if (event.getAction().equals(InventoryAction.UNKNOWN) && event.getCurrentItem() != null) {
			log("CloneItemStack: " + event.getWhoClicked().getName() + " " + getItemLogString(event.getCurrentItem()));
		}
	}

	/* Format:
	 * (stick 64 "Item Name" ["monumenta:loot_table", (x), (y), (z)])
	 */
	private String getItemLogString(@Nullable ItemStack item) {
		if (item == null) {
			return "";
		}

		String retStr = "(" + item.getType().toString().replace("minecraft:", "") + " " + Integer.toString(item.getAmount());
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				String displayName = meta.getDisplayName();
				if (displayName != null && !displayName.isEmpty()) {
					/* Has display name */
					retStr += " \"" + displayName + "\"";
				}

				if (meta instanceof BlockStateMeta) {
					retStr += " [";

					boolean elementAdded = false;

					/* Is a container of more things */
					BlockState state = ((BlockStateMeta)meta).getBlockState();
					if ((state instanceof Lootable) && ((Lootable)state).hasLootTable()) {
						retStr += ((Lootable)state).getLootTable().getKey().toString();
						elementAdded = true;
					}
					if (state instanceof CommandBlock) {
						retStr += ((CommandBlock)state).getCommand();
					}
					if (state instanceof Container) {
						for (ItemStack subItem : ((Container)state).getInventory().getContents()) {
							if (subItem != null) {
								if (elementAdded) {
									retStr += ", ";
								}
								retStr += getItemLogString(subItem);
								elementAdded = true;
							}
						}
					}
					if (state instanceof Sign) {
						for (String line : ((Sign)state).getLines()) {
							if (line != null && !line.isEmpty()) {
								if (elementAdded) {
									retStr += ", ";
								}
								retStr += "\"" + line + "\"";
								elementAdded = true;
							}
						}
					}

					retStr += "]";
				}
			}
		}
		return retStr + ")";
	}

	private void log(@Nonnull String message) {
		mPlugin.mSocketManager.sendPacket(new AuditLogPacket(mShardName + ": " + message));
	}
}
