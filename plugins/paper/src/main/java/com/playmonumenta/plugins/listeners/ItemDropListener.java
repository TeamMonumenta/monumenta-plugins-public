package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.GameMode;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public final class ItemDropListener implements Listener {
	public static final String COMMAND = "disabledrop";
	public static final String ALIAS = "dd";

	public enum Mode {
		NONE(null, "None", item -> false),
		TIERED("DisableDropTiered", "Tiered items", item -> {
			Tier tier = ItemStatUtils.getTier(item);
			return tier != Tier.NONE && tier != Tier.CURRENCY && tier != Tier.KEYTIER;
		}),
		LORE("DisableDropLore", "Items with lore", item -> ItemUtils.hasLore(item) || ItemUtils.isShulkerBox(item.getType())),
		INTERESTING("DisableDropInteresting", "Interesting items", ItemUtils::isInteresting),
		ALL("DisableDropAll", "All items", item -> true),
		EQUIPPED("DisableDropEquipped", "Equipped items (hotbar + equipment)", item -> false);

		@Nullable
		private final String mTag;
		public final String mDisplayName;
		private final Predicate<ItemStack> mPredicate;

		Mode(@Nullable String tag, String displayName, Predicate<ItemStack> mPredicate) {
			mTag = tag;
			this.mDisplayName = displayName;
			this.mPredicate = mPredicate;
		}

		private boolean shouldDisablePickup(ItemStack item) {
			return mPredicate.test(item);
		}
	}

	public static Mode getPlayerMode(Player player) {
		return Arrays.stream(Mode.values())
			.filter(x -> x.mTag != null && ScoreboardUtils.checkTag(player, x.mTag))
			.findFirst()
			.orElse(Mode.NONE);
	}

	public static void setPlayerMode(Player player, Mode mode) {
		for (Mode x : Mode.values()) {
			if (x.mTag != null) {
				player.getScoreboardTags().remove(x.mTag);
			}
		}

		if (mode.mTag != null) {
			player.getScoreboardTags().add(mode.mTag);
		}
	}

	public ItemDropListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.disabledrop");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.executesPlayer((sender, args) -> {
				if (getPlayerMode(sender) == Mode.NONE) {
					sender.sendMessage(Component.text("You can no longer drop tiered items.", NamedTextColor.GOLD, TextDecoration.BOLD));
					setPlayerMode(sender, Mode.TIERED);
				} else {
					sender.sendMessage(Component.text("You can now drop all items.", NamedTextColor.GOLD, TextDecoration.BOLD));
					setPlayerMode(sender, Mode.NONE);
				}
			})
			.register();

		for (final var entry : List.of(
			Triple.of("none", Mode.NONE, "You can now drop all items."),
			Triple.of("lore", Mode.LORE, "You can no longer drop items with lore text."),
			Triple.of("interesting", Mode.INTERESTING, "You can no longer drop interesting items."),
			Triple.of("all", Mode.ALL, "You can no longer drop items."),
			Triple.of("equipped", Mode.EQUIPPED, "You can no longer drop items you have equipped."),
			Triple.of("tiered", Mode.TIERED, "You can no longer drop tiered items.")
		)) {
			new CommandAPICommand(COMMAND)
				.withPermission(perms)
				.withAliases(ALIAS)
				.withArguments(new LiteralArgument(entry.getLeft()))
				.executesPlayer((sender, args) -> {
					sender.sendMessage(Component.text(entry.getRight(), NamedTextColor.GOLD, TextDecoration.BOLD));
					setPlayerMode(sender, entry.getMiddle());
				})
				.register();
		}
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
			if (!(human instanceof Player player)
				|| player.getGameMode() == GameMode.CREATIVE) {
				return;
			}

			ItemStack item = event.getCurrentItem();
			if (event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
				item = player.getItemOnCursor();
			}
			if (item == null || item.getType().isAir()) {
				return;
			}

			Mode mode = getPlayerMode(player);

			if (mode.shouldDisablePickup(item)) {
				event.setCancelled(true);
			} else if (mode == Mode.EQUIPPED) {
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
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		// If the item wouldn't fit back into the inventory we need to throw it in order to not delete it
		ItemStack item = itemEntity.getItemStack();
		PlayerInventory playerInventory = player.getInventory();
		if (!InventoryUtils.canFitInInventory(item, playerInventory)) {
			return;
		}

		Mode mode = getPlayerMode(player);

		if (mode.shouldDisablePickup(item)) {
			event.setCancelled(true);
		} else if (mode == Mode.EQUIPPED) {
			int droppedSlotId = Plugin.getInstance().mTrackingManager.mPlayers.getDroppedSlotId(event);

			if (isEquipmentSlot(droppedSlotId)) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * @param slot a Slot ID
	 * @return Whether the given slot is an equipment slot, i.e. is either an armor or a hotbar slot
	 */
	private boolean isEquipmentSlot(int slot) {
		return (0 <= slot && slot <= 8) || (36 <= slot && slot <= 40);
	}
}
