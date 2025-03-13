package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.itemstats.enums.PickupFilterResult;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public final class JunkItemListener implements Listener {
	public static final String COMMAND = "pickup";
	public static final String ALIAS = "pu";

	private static final String PICKUP_MIN_OBJ_NAME = "PickupMin";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 17;

	public record PlayerSetting(Mode mode, int threshold) {
		public enum Mode {
			ALL(null, "All"),
			TIERED(PickupFilterResult.TIERED_TAG, "Tiered"),
			LORE(PickupFilterResult.LORE_TAG, "Lore"),
			INTERESTING(PickupFilterResult.INTERESTING_TAG, "Interesting");

			@Nullable
			private final String mTag;
			public final String mDisplayName;

			Mode(@Nullable String tag, String mDisplayName) {
				this.mTag = tag;
				this.mDisplayName = mDisplayName;
			}
		}

		public PlayerSetting mode(Mode mode) {
			return new PlayerSetting(mode, threshold);
		}

		public PlayerSetting threshold(int threshold) {
			return new PlayerSetting(mode, threshold);
		}

		public static PlayerSetting get(Player player) {
			final var mode = Arrays.stream(Mode.values())
				.filter(x -> x.mTag != null && ScoreboardUtils.checkTag(player, x.mTag))
				.findFirst()
				.orElse(Mode.ALL);

			return new JunkItemListener.PlayerSetting(mode, ScoreboardUtils.getScoreboardValue(player, PICKUP_MIN_OBJ_NAME).orElse(0));
		}

		public void set(Player player) {
			for (Mode value : Mode.values()) {
				if (value.mTag != null) {
					player.removeScoreboardTag(value.mTag);
				}
			}

			if (mode.mTag != null) {
				player.addScoreboardTag(mode.mTag);
			}

			ScoreboardUtils.setScoreboardValue(player, PICKUP_MIN_OBJ_NAME, threshold);
		}
	}

	private record Entry(String lit, PlayerSetting.Mode mode, String message) {
	}

	public JunkItemListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.executesPlayer((sender, args) -> {
				var setting = PlayerSetting.get(sender);

				if (setting.mode() != PlayerSetting.Mode.ALL) {
					sender.sendMessage(Component.text("You will now pick up all items.", NamedTextColor.GOLD, TextDecoration.BOLD));
					setting = setting.mode(PlayerSetting.Mode.ALL);
				} else {
					sender.sendMessage(Component.text("You will no longer pick up uninteresting items.", NamedTextColor.GOLD, TextDecoration.BOLD));
					setting = setting.mode(PlayerSetting.Mode.INTERESTING);
				}

				setting.set(sender);
			})
			.register();

		for (final var entry : List.of(
			new Entry("all", PlayerSetting.Mode.ALL, "You will now pick up all items."),
			new Entry("tiered", PlayerSetting.Mode.TIERED, "You will now only pick up items with a tier."),
			new Entry("lore", PlayerSetting.Mode.LORE, "You will now only pick up items with lore text."),
			new Entry("interesting", PlayerSetting.Mode.INTERESTING, "You will no longer pick up uninteresting items.")
		)) {
			new CommandAPICommand(COMMAND)
				.withPermission(perms)
				.withAliases(ALIAS)
				.withArguments(new LiteralArgument(entry.lit()))
				.executesPlayer((sender, args) -> {
					PlayerSetting.get(sender).mode(entry.mode()).set(sender);
					sender.sendMessage(Component.text(entry.message(), NamedTextColor.GOLD, TextDecoration.BOLD));
				})
				.register();
		}

		// Sets PickupMin, but does not change pickup status
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("threshold"), new IntegerArgument("count"))
			.executesPlayer((sender, args) -> {
				int count = Objects.requireNonNull(args.getUnchecked("count"));
				PlayerSetting.get(sender).threshold(count).set(sender);
				sender.sendMessage(Component.text("Threshold to pick up uninteresting items set to " + count + ".", NamedTextColor.GOLD, TextDecoration.BOLD));
			})
			.register();

	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void pickupItem(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player) {
			Item entity = event.getItem();
			ItemStack item = entity.getItemStack();
			if (item.getType().isAir()) {
				return;
			}

			PlayerSetting setting = PlayerSetting.get(player);

			if (setting.mode() == PlayerSetting.Mode.ALL) {
				return;
			}

			// Allow collection of valuable player-dropped items
			if (GraveManager.isThrownItem(entity)) {
				return;
			}

			if (pickupFilter(player, setting, item)) {
				event.setCancelled(true);
			}
		}
	}

	public boolean pickupFilter(Player player, ItemStack item) {
		return pickupFilter(player, PlayerSetting.get(player), item);
	}

	/**
	 * This checks if the item can be picked up or not by this player.
	 *
	 * @return True if pickup should be cancelled, false if not
	 */
	public boolean pickupFilter(Player player, PlayerSetting setting, ItemStack item) {
		PlayerInventory inventory = player.getInventory();

		// Allow collection of any items on the hotbar
		for (int i = 0; i <= 8; i++) {
			ItemStack hotbarItem = inventory.getItem(i);
			if (hotbarItem != null && hotbarItem.isSimilar(item)) {
				// This is the same as something on the player's hotbar, definitely don't want to cancel pickup
				return false;
			}
		}

		if (setting.threshold() <= 0) {
			setting = setting.threshold(JUNK_ITEM_SIZE_THRESHOLD);
			setting.set(player);
		}

		// If the stack size is at least the specified size, bypass restrictions
		if (PickupFilterResult.getPickupCount(item) >= setting.threshold()) {
			return false;
		}

		PickupFilterResult filterResult = PickupFilterResult.getFilterResult(item);

		return switch (setting.mode()) {
			case TIERED -> !PickupFilterResult.TIERED.equals(filterResult);
			case LORE -> !filterResult.mTags.contains(PickupFilterResult.LORE_TAG);
			case INTERESTING -> !filterResult.mTags.contains(PickupFilterResult.INTERESTING_TAG);
			default -> false;
		};
	}
}
