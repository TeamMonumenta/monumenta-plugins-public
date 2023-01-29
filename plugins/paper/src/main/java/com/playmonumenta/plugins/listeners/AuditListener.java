package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
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
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.jetbrains.annotations.Nullable;



public class AuditListener implements Listener {
	private static final List<Pattern> IGNORED_COMMAND_REGEX = Arrays.asList(
		// ScriptedQuests
		exactOptionalArguments("(scriptedquests:)?questtrigger"),
		exactOptionalArguments("(minecraft:)?clickable"),
		exactOptionalArguments("(minecraft:)?leaderboard(?! update)"),
		exactOptionalArguments("(minecraft:)?waypoint"),

		// MonumentaNetworkChat
		exactOptionalArguments("help"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) help"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) listplayers"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) join"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) leave"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) pause"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) unpause"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) player"),
		exactOptionalArguments("(minecraft:)?(ch|chat|networkchat) say"),
		exactOptionalArguments("(minecraft:)?(global|g)"),
		exactOptionalArguments("(minecraft:)?(guildchat|gc)"),
		exactOptionalArguments("(minecraft:)?(local|l)"),
		exactOptionalArguments("(minecraft:)?(worldchat|wc)"),
		exactOptionalArguments("(minecraft:)?(party|p)"),
		exactOptionalArguments("(minecraft:)?(pausechat|pc)"),
		exactOptionalArguments("(minecraft:)?me"),
		exactOptionalArguments("(minecraft:)?(msg|tell|w)"),
		exactOptionalArguments("(minecraft:)?r"),
		exactOptionalArguments("(minecraft:)?(teammsg|tm)"),
		exactOptionalArguments("lfg"),
		exactOptionalArguments("m"),
		exactOptionalArguments("mh"),
		exactOptionalArguments("tr"),
		exactOptionalArguments("join"),
		exactOptionalArguments("leave"),
		exactOptionalArguments("ignore"),
		exactOptionalArguments("unignore"),

		// CoreProtect
		exactOptionalArguments("(coreprotect:)?co i(nspect)?"),
		exactOptionalArguments("(coreprotect:)?co l(ookup)?"),
		exactOptionalArguments("(coreprotect:)?co near"),

		// FAWE
		exactOptionalArguments("/calc"), // "//calc"

		// Common commands
		exactOptionalArguments(String.format(
			"(%s|%s)",
			JunkItemListener.COMMAND,
			JunkItemListener.ALIAS
		)),
		exactOptionalArguments("(minecraft:)?(blockinteractions|bi)"),
		exactOptionalArguments("(minecraft:)?(disabledrop|dd)"),
		exactOptionalArguments("(minecraft:)?glowing"),
		exactNoArguments("(minecraft:)?grave list"),
		exactOptionalArguments("(minecraft:)?particles"),
		exactOptionalArguments("(playerstats|ps)"),
		exactOptionalArguments("peb"),
		exactOptionalArguments("(minecraft:)?race leaderboard"),
		exactOptionalArguments("(minecraft:)?(rocketjump|rj)"),
		exactOptionalArguments("(minecraft:)?toggleswap"),
		exactOptionalArguments("(minecraft:)?toggleworldnames"),
		exactOptionalArguments("(minecraft:)?(virtualfirmament|vf)"),
		exactOptionalArguments("(minecraft:)?(viewcharms|vc)"),
		exactOptionalArguments("(minecraft:)?plot access (help|add|remove|info$)"),
		exactOptionalArguments("(spark:)?tps")
	);

	private final Map<HumanEntity, ItemStack> mLastCreativeDestroy = new HashMap<>();
	private final Logger mLogger;
	private static @Nullable AuditListener INSTANCE = null;

	public AuditListener(Logger logger) {
		mLogger = logger;
		INSTANCE = this;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void death(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// TODO Update MessageUtils.plain() to accept a language file to translate with
		logDeath("<" + player.getWorld().getName() + ">" + " Death: " + player.getName() + " " + event.getDeathMessage());

		checkDestroy(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void block(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (!player.getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		log("+PlaceDuplicateBlock: " + player.getName() + " " + getItemLogString(event.getItemInHand()));

		checkDestroy(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void creative(InventoryCreativeEvent event) {
		HumanEntity player = event.getWhoClicked();

		event.getCursor();
		if (!event.getCursor().getType().equals(Material.AIR)) {
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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void cloneClick(InventoryClickEvent event) {
		if (!event.getWhoClicked().getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		/* For some stupid reason UNKNOWN = clone stack with pick block key */
		if ((event.getAction().equals(InventoryAction.UNKNOWN) || event.getAction().equals(InventoryAction.CLONE_STACK)) && event.getCurrentItem() != null) {
			log("+CloneItem: " + event.getWhoClicked().getName() + " " + getItemLogString(event.getCurrentItem()));
		}

		/* Don't checkDestroy() here - this event fires every time InventoryCreativeEvent fires */
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryClose(InventoryCloseEvent event) {
		checkDestroy(event.getPlayer());
	}

	/* Format:
	 * (stick 64 "Item Name" ["monumenta:loot_table", (x), (y), (z)])
	 */
	public static String getItemLogString(@Nullable ItemStack item) {
		if (item == null) {
			return "";
		}

		StringBuilder retStr = new StringBuilder("(" + item.getType().toString().replace("minecraft:", "") + " " + item.getAmount());
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				Component displayName = meta.displayName();
				if (displayName != null) {
					/* Has display name */
					retStr.append(" \"").append(MessagingUtils.plainText(displayName)).append("\"");
				}

				if (meta instanceof BlockStateMeta) {
					retStr.append(" [");

					boolean elementAdded = false;

					/* Is a container of more things */
					BlockState state = ((BlockStateMeta)meta).getBlockState();
					if (state instanceof Lootable lootable) {
						if (lootable.hasLootTable()) {
							@Nullable LootTable lootTable = lootable.getLootTable();
							assert lootTable != null;
							retStr.append(lootTable.getKey());
							elementAdded = true;
						}
					}
					if (state instanceof CommandBlock) {
						retStr.append(((CommandBlock) state).getCommand());
					}
					if (state instanceof Container) {
						for (ItemStack subItem : ((Container)state).getInventory().getContents()) {
							if (subItem != null) {
								if (elementAdded) {
									retStr.append(", ");
								}
								retStr.append(getItemLogString(subItem));
								elementAdded = true;
							}
						}
					}
					if (state instanceof Sign) {
						for (Component comp : ((Sign)state).lines()) {
							String line = MessagingUtils.plainText(comp);
							if (!line.isEmpty()) {
								if (elementAdded) {
									retStr.append(", ");
								}
								retStr.append("\"").append(line).append("\"");
								elementAdded = true;
							}
						}
					}

					retStr.append("]");
				}
			}
		}
		return retStr + ")";
	}

	public static void logSevere(String message) {
		if (INSTANCE != null) {
			INSTANCE.mLogger.info("Audit | " + message);
			MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(createLocationData() + " " + message);
		}
	}

	public static void log(String message) {
		if (INSTANCE != null) {
			INSTANCE.mLogger.info("Audit | " + message);
			MonumentaNetworkRelayIntegration.sendAuditLogMessage(createLocationData() + " " + message);
		}
	}

	public static void logDeath(String message) {
		if (INSTANCE != null) {
			INSTANCE.mLogger.info("Audit | " + createLocationData() + " " + message);
			MonumentaNetworkRelayIntegration.sendDeathAuditLogMessage(createLocationData() + " " + message);
		}
	}

	public static String createLocationData() {
		String playerShard = PlaceholderAPI.setPlaceholders(null, "%network-relay_shard%");
		return "<" + playerShard + ">";
	}

	private void checkDestroy(HumanEntity player) {
		ItemStack lastItem = mLastCreativeDestroy.get(player);
		if (lastItem != null) {
			log("-DeleteItem: " + player.getName() + " " + getItemLogString(lastItem));
			mLastCreativeDestroy.remove(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		// Don't log normal game mode changes, eg changing zones
		GameMode oldGameMode = player.getGameMode();
		GameMode newGameMode = event.getNewGameMode();
		if (
			(
				GameMode.SURVIVAL.equals(oldGameMode)
					|| GameMode.ADVENTURE.equals(oldGameMode)
			) && (
				GameMode.SURVIVAL.equals(newGameMode)
					|| GameMode.ADVENTURE.equals(newGameMode)
			)
		) {
			return;
		}

		log(String.format(
			"GameMode: %s | %s -> %s",
			player.getName(),
			oldGameMode,
			newGameMode
		));

		checkDestroy(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		String command = event.getMessage();
		for (Pattern pattern : IGNORED_COMMAND_REGEX) {
			if (pattern.matcher(command).find()) {
				return;
			}
		}

		log(String.format(
			"Command: %s | %s",
			player.getName(),
			command
		));
	}

	/*
	 * Returns a regex pattern for the specified exact command
	 * (eg "r" won't match "restart" as well),
	 * without arguments after it.
	 */
	private static Pattern exactNoArguments(String command) {
		return Pattern.compile("^/" + command + "$", Pattern.CASE_INSENSITIVE);
	}

	/*
	 * Returns a regex pattern for the specified exact command
	 * (eg "r" won't match "restart" as well),
	 * either with or without arguments after it.
	 */
	private static Pattern exactOptionalArguments(String command) {
		return Pattern.compile("^/" + command + "($| )", Pattern.CASE_INSENSITIVE);
	}
}
