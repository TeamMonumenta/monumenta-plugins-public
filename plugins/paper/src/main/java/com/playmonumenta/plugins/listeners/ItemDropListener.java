package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemTier;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public final class ItemDropListener implements Listener {
	public static final String COMMAND = "disabledrop";
	public static final String ALIAS = "dd";

	private static final String TIERED_TAG = "DisableDropTiered";
	private static final String LORE_TAG = "DisableDropLore";
	private static final String INTERESTING_TAG = "DisableDropInteresting";
	private static final String ALL_TAG = "DisableDropAll";
	private static final String HOLDING_TAG = "DisableDropHolding";
	private static final String EQUIPPED_TAG = "DisableDropEquipped";

	private final Set<UUID> mTieredPlayers = new HashSet<>();
	private final Set<UUID> mLorePlayers = new HashSet<>();
	private final Set<UUID> mInterestingPlayers = new HashSet<>();
	private final Set<UUID> mAllPlayers = new HashSet<>();
	private final Set<UUID> mHoldingPlayers = new HashSet<>();
	private final Set<UUID> mEquippedPlayers = new HashSet<>();

	public ItemDropListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.disabledrop");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.executes((sender, args) -> {
				playerToggle(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("none"))
			.executes((sender, args) -> {
				enable(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("lore"))
			.executes((sender, args) -> {
				disableLore(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("interesting"))
			.executes((sender, args) -> {
				disableInteresting(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("all"))
			.executes((sender, args) -> {
				disableAll(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("holding"))
			.executes((sender, args) -> {
				disableHolding(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("equipped"))
			.executes((sender, args) -> {
				disableEquipped(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("tiered"))
			.executes((sender, args) -> {
				disableTiered(sender);
			})
			.register();
	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (hasTag(player)) {
			enable(player);
		} else {
			disableTiered(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Set<String> tags = player.getScoreboardTags();
		if (tags != null) {
			UUID uuid = player.getUniqueId();
			if (tags.contains(TIERED_TAG)) {
				mTieredPlayers.add(uuid);
			} else if (tags.contains(LORE_TAG)) {
				mLorePlayers.add(uuid);
			} else if (tags.contains(INTERESTING_TAG)) {
				mInterestingPlayers.add(uuid);
			} else if (tags.contains(ALL_TAG)) {
				mAllPlayers.add(uuid);
			} else if (tags.contains(HOLDING_TAG)) {
				mHoldingPlayers.add(uuid);
			} else if (tags.contains(EQUIPPED_TAG)) {
				mEquippedPlayers.add(uuid);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void quit(PlayerQuitEvent event) {
		removeFromSets(event.getPlayer());
	}

	// Handles dropping from an open inventory via drop key or dragging an item outside the open inventory window
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {

		if (event.getClick() == ClickType.DROP
			|| event.getClick() == ClickType.CONTROL_DROP
			|| event.getAction() == InventoryAction.DROP_ALL_CURSOR
			|| event.getAction() == InventoryAction.DROP_ALL_SLOT
			|| event.getAction() == InventoryAction.DROP_ONE_CURSOR
			|| event.getAction() == InventoryAction.DROP_ONE_SLOT) {

			HumanEntity human = event.getWhoClicked();
			if (!(human instanceof Player)) {
				return;
			}
			Player player = (Player) human;
			if (player.getGameMode() == GameMode.CREATIVE) {
				return;
			}

			ItemStack item = event.getCurrentItem();
			if (event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
				item = player.getItemOnCursor();
			}
			if (item == null || item.getType().isAir()) {
				return;
			}

			UUID uuid = player.getUniqueId();
			if (isDropDisabled(uuid, item)) {
				event.setCancelled(true);
			} else if (mHoldingPlayers.contains(uuid)) {
				if (player.getInventory().equals(event.getClickedInventory()) && event.getSlot() == player.getInventory().getHeldItemSlot()) {
					event.setCancelled(true);
				}
			} else if (mEquippedPlayers.contains(uuid)) {
				if (player.getInventory().equals(event.getClickedInventory()) && isEquipmentSlot(event.getSlot())) {
					event.setCancelled(true);
				}
			}
		}
	}

	// Handles every drop event, but badly - items don't get put back in the proper slots when this event is cancelled.
	// Events already handled & cancelled by other handlers won't reach this code, so those will be handled nicely.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {

		Player player = event.getPlayer();
		Item itemEntity = event.getItemDrop();
		if (player == null || itemEntity == null || player.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		// If the item wouldn't fit back into the inventory we need to throw it in order to not delete it
		ItemStack item = itemEntity.getItemStack();
		PlayerInventory playerInventory = player.getInventory();
		if (!InventoryUtils.canFitInInventory(item, playerInventory)) {
			return;
		}

		UUID uuid = player.getUniqueId();
		if (isDropDisabled(uuid, item)) {
			event.setCancelled(true);
		} else if (mHoldingPlayers.contains(uuid)) {
			int heldSlotId = playerInventory.getHeldItemSlot();
			int droppedSlotId = Plugin.getInstance().mTrackingManager.mPlayers.getDroppedSlotId(event);

			if (heldSlotId == droppedSlotId) {
				event.setCancelled(true);
			}
		} else if (mEquippedPlayers.contains(uuid)) {
			int droppedSlotId = Plugin.getInstance().mTrackingManager.mPlayers.getDroppedSlotId(event);

			if (isEquipmentSlot(droppedSlotId)) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Checks if dropping the given item is not allowed. Can not and does not check {@link #mHoldingPlayers} and {@link #mEquippedPlayers}.
	 */
	private boolean isDropDisabled(UUID uuid, ItemStack item) {
		if (mAllPlayers.contains(uuid)) {
			return true;
		} else if (mTieredPlayers.contains(uuid)) {
			return ItemUtils.getItemTier(item) != ItemTier.UNKNOWN;
		} else if (mLorePlayers.contains(uuid)) {
			return ItemUtils.hasLore(item) || ItemUtils.isShulkerBox(item.getType());
		} else if (mInterestingPlayers.contains(uuid)) {
			return ItemUtils.isInteresting(item);
		}
		return false;
	}

	/**
	 * @param slot a Slot ID
	 * @return Whether the given slot is an equipment slot, i.e. is either an armor or a hotbar slot
	 */
	private boolean isEquipmentSlot(int slot) {
		return 0 <= slot && slot <= 8 || 36 <= slot && slot <= 40;
	}

	private void enable(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can now drop all items.");
	}

	private void disableLore(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(LORE_TAG);
		mLorePlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop items with lore text.");
	}

	private void disableInteresting(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(INTERESTING_TAG);
		mInterestingPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop interesting items.");
	}

	private void disableAll(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(ALL_TAG);
		mAllPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop items.");
	}

	private void disableHolding(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(HOLDING_TAG);
		mHoldingPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop the item you are holding.");
	}

	private void disableEquipped(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(EQUIPPED_TAG);
		mEquippedPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop items you have equipped.");
	}

	private void disableTiered(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(TIERED_TAG);
		mTieredPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop tiered items.");
	}

	private boolean hasTag(Player player) {
		Set<String> tags = player.getScoreboardTags();
		return tags.contains(TIERED_TAG) || tags.contains(LORE_TAG) || tags.contains(HOLDING_TAG) || tags.contains(EQUIPPED_TAG) || tags.contains(INTERESTING_TAG) || tags.contains(ALL_TAG);
	}

	private void remove(Player player) {
		player.removeScoreboardTag(LORE_TAG);
		player.removeScoreboardTag(INTERESTING_TAG);
		player.removeScoreboardTag(ALL_TAG);
		player.removeScoreboardTag(HOLDING_TAG);
		player.removeScoreboardTag(EQUIPPED_TAG);
		player.removeScoreboardTag(TIERED_TAG);
		removeFromSets(player);
	}

	private void removeFromSets(Player player) {
		UUID uuid = player.getUniqueId();
		mLorePlayers.remove(uuid);
		mInterestingPlayers.remove(uuid);
		mAllPlayers.remove(uuid);
		mHoldingPlayers.remove(uuid);
		mEquippedPlayers.remove(uuid);
		mTieredPlayers.remove(uuid);
	}
}
