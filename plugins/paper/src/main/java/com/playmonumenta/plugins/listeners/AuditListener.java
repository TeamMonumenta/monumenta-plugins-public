package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.Lootable;

import com.playmonumenta.plugins.network.SocketManager;
import com.playmonumenta.plugins.packets.AuditLogPacket;

public class AuditListener implements Listener {
	private final Map<HumanEntity, ItemStack> mLastCreativeDestroy = new HashMap<HumanEntity, ItemStack>();
	private final Logger mLogger;

	public AuditListener(Logger logger) {
		mLogger = logger;
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

		checkDestroy(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void death(PlayerDeathEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getEntity();

		log("Death: " + player.getName() + " " + event.getDeathMessage());

		checkDestroy(player);
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
		if (cmd.startsWith("/questtrigger ")
		    || cmd.startsWith("/clickable ")
		    || cmd.startsWith("/r ")
		    || cmd.startsWith("/vtell ")
		    || cmd.startsWith("/pickup ")
		    || cmd.startsWith("/vmessage ")
		    || cmd.startsWith("/vwhisper ")
		    || cmd.startsWith("/peb")
		    || cmd.startsWith("/msg ")) {
			return;
		}

		log("Command: " + player.getName() + " " + cmd);

		checkDestroy(player);
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

		log("+PlaceDuplicateBlock: " + player.getName() + " " + getItemLogString(event.getItemInHand()));

		checkDestroy(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void creative(InventoryCreativeEvent event) {
		if (event.isCancelled()) {
			return;
		}

		HumanEntity player = event.getWhoClicked();

		if (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) {
			ItemStack lastItem = mLastCreativeDestroy.get(player);
			ItemStack newItem = event.getCursor();
			if (lastItem != null) {
				if (lastItem.isSimilar(newItem)) {
					/* These are the same item, but with potentially different counts */
					if (lastItem.getAmount() == newItem.getAmount()) {
						/* Exactly the same item */
						mLastCreativeDestroy.remove(player);
					} else if (lastItem.getAmount() > newItem.getAmount()) {
						/* Just decrease the stack size of the removed amount and hang onto it */
						lastItem.setAmount(lastItem.getAmount() - newItem.getAmount());
					} else {
						/* Same item, but we added more than we removed (combined stacks) */
						ItemStack logItem = newItem.clone();
						logItem.setAmount(logItem.getAmount() - lastItem.getAmount());
						log("+CreateItem: " + player.getName() + " " + getItemLogString(logItem));
						mLastCreativeDestroy.remove(player);
					}
				} else {
					/* These are totally different items - log both destroy and create */
					log("-DeleteItem: " + player.getName() + " " + getItemLogString(lastItem));
					log("+CreateItem: " + player.getName() + " " + getItemLogString(newItem));
					mLastCreativeDestroy.remove(player);
				}
			} else {
				/* There was no last item - this item was created */
				log("+CreateItem: " + player.getName() + " " + getItemLogString(newItem));
			}
		}
		if (event.getCurrentItem() != null && !event.getCurrentItem().getType().equals(Material.AIR)) {
			/* Make sure we're not overwriting the previous removed item without logging it */
			checkDestroy(player);
			mLastCreativeDestroy.put(player, event.getCurrentItem().clone());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void cloneClick(InventoryClickEvent event) {
		if (event.isCancelled() || !event.getWhoClicked().getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		/* For some stupid reason UNKNOWN = clone stack with pick block key */
		if ((event.getAction().equals(InventoryAction.UNKNOWN) || event.getAction().equals(InventoryAction.CLONE_STACK)) && event.getCurrentItem() != null) {
			log("+CloneItem: " + event.getWhoClicked().getName() + " " + getItemLogString(event.getCurrentItem()));
		}

		/* Don't checkDestroy() here - this event fires every time InventoryCreativeEvent fires */
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void inventoryClose(InventoryCloseEvent event) {
		checkDestroy(event.getPlayer());
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

	/* Checks that a destroyed item gets logged if the player doesn't place it again immediately */
	private void checkDestroy(HumanEntity player) {
		ItemStack lastItem = mLastCreativeDestroy.get(player);
		if (lastItem != null) {
			log("-DeleteItem: " + player.getName() + " " + getItemLogString(lastItem));
			mLastCreativeDestroy.remove(player);
		}
	}

	private void log(@Nonnull String message) {
		mLogger.info("Audit:" + message);

		SocketManager.sendPacket(new AuditLogPacket(message));
	}
}
