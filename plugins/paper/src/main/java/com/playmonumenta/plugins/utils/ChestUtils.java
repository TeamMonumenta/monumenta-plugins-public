package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelveLootTableGroup;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.LootTableManager;
import com.playmonumenta.plugins.managers.LootboxManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ChestUtils {
	public static final NamespacedKey NON_LOOT_LIMITED
		= Objects.requireNonNull(NamespacedKey.fromString("monumenta:non_looting_limited"));
	public static final String LOG_SCROLLS_PERMISSION = "monumenta.log.skrscrolls"; // dictates if SKR scrolls are logged or not when obtained
	public static final boolean LOG_SCROLL_FRAGMENTS = false;

	private static final double[] BONUS_ITEMS = {
		0, // Dummy value, this is a player count indexed array
		0.5,
		1.7,
		2.6,
		3.3,
		3.8,
		4.2,
		4.4,
		4.5
	};
	private static final String CHEST_SOLO_LOOT_EFFECT_NAME = "LootboxSoloLootSlownessEffect";

	private static boolean lootTableInventoryHasBonusRolls(Inventory inventory) {
		if (!(inventory.getHolder() instanceof Lootable lootable)) {
			return false;
		}
		LootTable lootTable = lootable.getLootTable();
		if (lootTable == null) {
			return false;
		}
		LootTableManager.LootTableEntry lootEntry = LootTableManager.getLootTableEntry(lootTable.getKey());
		return lootEntry != null && lootEntry.hasBonusRolls();
	}

	public static void generateContainerLootWithScaling(Player player, Block block, Plugin plugin) {
		if (block.getState() instanceof Container container) {
			Inventory inventory = container.getInventory();
			if (inventory instanceof DoubleChestInventory doubleChest) {
				boolean forceLootShare = lootTableInventoryHasBonusRolls(doubleChest.getLeftSide());
				generateContainerLootWithScaling(player, doubleChest.getLeftSide(), plugin);
				generateContainerLootWithScaling(player, doubleChest.getRightSide(), forceLootShare, plugin);
			} else {
				generateContainerLootWithScaling(player, inventory, plugin);
			}
		}
	}

	private static void generateContainerLootWithScaling(Player player, Inventory inventory, Plugin plugin) {
		generateContainerLootWithScaling(player, inventory, false, plugin);
	}

	private static void generateContainerLootWithScaling(Player player, Inventory inventory, boolean forceLootshare, Plugin plugin) {
		if (!(inventory.getHolder() instanceof Lootable lootable)) {
			return;
		}

		LootTable lootTable = lootable.getLootTable();
		if (lootTable == null) {
			return;
		}

		/* Figure out what the luck level should be and which players contribute to scaling */
		int luckAmount; // The amount of luck the loot table should be rolled with
		List<Player> nearbyPlayers = Collections.singletonList(player); // All players that may receive loot slices

		boolean isInLootroom = ZoneUtils.hasZoneProperty(inventory.getLocation() != null ? inventory.getLocation() : player.getLocation(), ZoneUtils.ZoneProperty.LOOTROOM);
		LootTableManager.LootTableEntry lootEntry = LootTableManager.getLootTableEntry(lootTable.getKey());
		if (lootEntry == null) {
			// This loot table doesn't exist, likely an error
			MMLog.severe("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which wasn't loaded by LootTableManager");
			luckAmount = 0;
		} else if (isInLootroom) {
			// Loot scaling is disabled (dungeon loot rooms)
			luckAmount = 0;
		} else if (!lootEntry.hasBonusRolls()) {
			// This chest doesn't have bonus rolls, don't apply luck
			MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which did not have scaling enabled");
			luckAmount = 0;
		} else {
			// Loot scaling is enabled
			MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which was scaled & distributed");

			// Get all players in range
			nearbyPlayers = PlayerUtils.playersInLootScalingRange(player, false);

			// This should at minimum be one since there should always be one player (the person who opened the chest)
			int otherPlayersMultiplier = nearbyPlayers.size();

			MMLog.fine("Lootable seed: " + lootable.getSeed());
			// Loot table seed set and use the seed for number of players
			if (lootable.getSeed() > 0 && lootable.getSeed() < 50) {
				otherPlayersMultiplier = (int) lootable.getSeed();
				MMLog.fine("Chest loot was already generated due to a spawner being broken with seed " + otherPlayersMultiplier);
			}

			double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, otherPlayersMultiplier)];
			MMLog.fine("Lootscaling for " + nearbyPlayers.size() + " players: " + bonusItems);
			luckAmount = (int) bonusItems;

			// Account for fractions of extra items with random roll
			if (FastUtils.RANDOM.nextDouble() < bonusItems - luckAmount) {
				luckAmount++;
			}
		}

		// Actually roll the loot into a collection of items
		LootContext.Builder builder = new LootContext.Builder(player.getLocation());
		builder.luck(luckAmount);
		LootContext context = builder.build();
		Collection<ItemStack> popLoot = lootTable.populateLoot(FastUtils.RANDOM, context);
		// Clear the original chest (vanilla behavior, loot table overrides whatever is in the chest, doesn't add to it
		inventory.clear();

		if (forceLootshare && !isInLootroom) {
			nearbyPlayers = PlayerUtils.playersInLootScalingRange(player, false);
		}

		// Divide the items up into fractional buckets for all the players
		List<List<ItemStack>> itemBuckets = LootboxManager.distributeLootToBuckets(new ArrayList<>(popLoot), nearbyPlayers.size());

		// The orig container starts with the first bucket of items
		// Need to make a copy here because the buckets lists are not modifiable
		List<ItemStack> itemsForOrigContainer = new ArrayList<>();

		Set<String> lootBoxPlayers = new TreeSet<>();
		Set<String> noSharePlayers = new TreeSet<>();
		// if lootbox isn't enabled for this shard or
		// if the opener is the only player, don't bother trying to give them a lootshare
		boolean useLootboxOnSelf = player.getScoreboardTags().contains("UseLootboxOnSelf")
			&& LootboxManager.hasEpicLootbox(player.getInventory());
		if (ServerProperties.getLootBoxEnabled() &&
			!(nearbyPlayers.size() == 1 && nearbyPlayers.contains(player))) {
			int bucketIdx = 0; // Start at 0
			for (Player other : nearbyPlayers) {
				//if not tagged, don't give lootshare to the opener
				if (other.getUniqueId().equals(player.getUniqueId()) && !useLootboxOnSelf) {
					itemsForOrigContainer.addAll(itemBuckets.get(bucketIdx));
					bucketIdx++;
					continue;
				}
				// otherwise give lootshare to players
				@Nullable List<ItemStack> rejectedItems = LootboxManager.giveShareToPlayer(new ArrayList<>(itemBuckets.get(bucketIdx)), other);
				// if tagged, close the chest
				if (other.getUniqueId().equals(player.getUniqueId()) && useLootboxOnSelf) {
					plugin.mEffectManager.addEffect(other, CHEST_SOLO_LOOT_EFFECT_NAME,
						new PercentSpeed(30, -0.8, CHEST_SOLO_LOOT_EFFECT_NAME).displays(false));
					new BukkitRunnable() {
						@Override
						public void run() {
							player.closeInventory();
						}
					}.runTaskLater(Plugin.getInstance(), 1);
				}
				// rejectedItems will be null if player has no lootbox
				// if rejectedItems is empty or has items, that means player has a lootbox
				if (rejectedItems == null) {
					// Failed to give this slice to the other player (no lootbox or lootbox is full)
					// Put all these items in the orig container
					itemsForOrigContainer.addAll(itemBuckets.get(bucketIdx));
					noSharePlayers.add(other.getName());
				} else {
					// Put rejected items in original container
					if (!rejectedItems.isEmpty()) {
						itemsForOrigContainer.addAll(rejectedItems);
					}
					lootBoxPlayers.add(other.getName());
				}
				bucketIdx++;
			}
		} else {
			// Add all items to chest if lootboxes are not enabled or there is only one player opening the chest
			for (List<ItemStack> items : itemBuckets) {
				itemsForOrigContainer.addAll(items);
			}
		}

		if (!lootBoxPlayers.isEmpty()) {
			// Sound to indicate loot was split
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.2f, 1.4f);

			StringJoiner otherPlayersJoiner = new StringJoiner(", ");
			for (String other : lootBoxPlayers) {
				otherPlayersJoiner.add(other);
			}
			StringJoiner noSharePlayerJoiner = new StringJoiner(", ");
			for (String noShare : noSharePlayers) {
				noSharePlayerJoiner.add(noShare);
			}
			Component lootboxPlayers = Component.text(lootBoxPlayers.size()
						+ " nearby player" + (lootBoxPlayers.size() == 1 ? "" : "s"),
					NamedTextColor.GOLD)
				.hoverEvent(Component.text(otherPlayersJoiner.toString()));

			Component noSharePlayerComponent;
			if (noSharePlayers.isEmpty()) {
				noSharePlayerComponent = Component.empty();
			} else {
				StringJoiner noShareJoiner = new StringJoiner(", ");
				for (String other : noSharePlayers) {
					noShareJoiner.add(other);
				}
				noSharePlayerComponent = Component.text(noSharePlayers.size()
							+ " player" + (noSharePlayers.size() == 1 ? "" : "s") + " got no share",
						NamedTextColor.RED)
					.hoverEvent(Component.text(noSharePlayerJoiner.toString()));
				noSharePlayerComponent = Component.text(", and ", NamedTextColor.GOLD)
					.append(noSharePlayerComponent);
			}

			Component lootDistributedMessage = Component.text("Loot distributed to ", NamedTextColor.GOLD)
				.append(lootboxPlayers)
				.append(noSharePlayerComponent);

			if (player.getScoreboardTags().contains("ActionBarLootbox")) {
				player.sendActionBar(lootDistributedMessage);
			} else {
				lootDistributedMessage = lootDistributedMessage.hoverEvent(Component.text(otherPlayersJoiner.toString()));
				player.sendMessage(lootDistributedMessage);
			}
		}

		// Logger for SKR Scrolls
		if (player.hasPermission(LOG_SCROLLS_PERMISSION)) {
			int fragmentCount = 0;
			for (ItemStack thisItem : popLoot) {
				if (testForScroll(thisItem)) {
					AuditListener.logPlayer("[Scroll Logger] Player " + player.getName() + " found a SKR Scroll (" + ItemUtils.getPlainNameIfExists(thisItem) + ") in a placed chest with loot table " + lootTable + ".");
					break;
				} else if (LOG_SCROLL_FRAGMENTS && InventoryUtils.testForItemWithName(thisItem, "Remnant", false) &&
					thisItem.getType().name().contains("FLINT")) {
					fragmentCount++;
				}
			}
			if (fragmentCount >= 1) {
				// Temp log for scroll fragments
				AuditListener.logPlayer("[Scroll Fragment Logger] Player " + player.getName() + " found " + fragmentCount + " SKR scroll fragments in a placed chest with loot table " + lootTable + ".");
			}
		}

		// Put the remainder of the loot in the original container
		generateLootInventory(itemsForOrigContainer, inventory, player, true);

		// warning on build server
		if (!Plugin.IS_PLAY_SERVER) {
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1, 1);
			player.sendMessage(Component.text("Loot table rolled!", NamedTextColor.RED).decorate(TextDecoration.BOLD)
				.append(Component.text(" (this message is only shown on the build server)", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
		}
	}

	public static void generateLootInventory(Collection<ItemStack> populatedLoot, Inventory inventory, Player player, boolean randomlyDistribute) {
		// consolidation
		Inventory tempFakeInventory = Bukkit.createInventory(null, 27);
		for (ItemStack lootItem : populatedLoot) {
			tempFakeInventory.addItem(lootItem);
		}
		List<ItemStack> lootList = Arrays.stream(tempFakeInventory.getContents())
			.filter((item) -> item != null && !item.getType().isAir()).toList();
		tempFakeInventory.clear();

		List<Integer> freeSlots = new ArrayList<>(27);
		for (int i = 0; i < 27; i++) {
			freeSlots.add(i);
		}
		if (randomlyDistribute) {
			Collections.shuffle(freeSlots);
		}

		MMLog.finer("generateLootInventory: Started with " + lootList.size() + " items and randomlyDistribute=" + randomlyDistribute);
		ArrayDeque<Integer> slotsWithMultipleItems = new ArrayDeque<>();
		boolean skrScrolls = false;
		for (ItemStack lootItem : lootList) {
			if (freeSlots.isEmpty()) {
				Plugin.getInstance().getLogger().severe("Tried to overfill container for player " + player.getName() + " at inventory " + inventory.getType() + " at location " + player.getLocation());
				player.sendMessage("Tried to overfill this container! Please report this");
				break;
			}
			int slot = freeSlots.remove(0);
			inventory.setItem(slot, lootItem);
			if (!skrScrolls) {
				if (testForScroll(lootItem)) {
					skrScrolls = true; // SKR Scroll found, alert the player later!
				}
			}
			if (MMLog.isLevelEnabled(Level.FINER)) { // Performance optimization to avoid calling lootItem.toString() when this log level is disabled
				MMLog.finer("generateLootInventory: Putting item in slot " + slot + ": " + lootItem.toString());
			}
			if (lootItem.getAmount() > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + slot + " to multiple items list");
				slotsWithMultipleItems.add(slot);
			}
		}

		while (randomlyDistribute && freeSlots.size() > 1 && !slotsWithMultipleItems.isEmpty()) {
			int splitSlot = slotsWithMultipleItems.remove();
			int slot = freeSlots.remove(0);

			ItemStack toSplitItem = inventory.getItem(splitSlot);
			if (toSplitItem == null) {
				continue;
			}
			ItemStack splitItem = toSplitItem.clone();
			int amountToSplit = toSplitItem.getAmount() / 2;
			int amountRemaining = toSplitItem.getAmount() - amountToSplit;

			if (MMLog.isLevelEnabled(Level.FINER)) {
				MMLog.finer("generateLootInventory: Splitting item type " + toSplitItem.getType() +
					" with count " + toSplitItem.getAmount() + " in slot " + splitSlot +
					" into count " + amountRemaining + " and " + amountToSplit + " in slot " + slot);
			}

			toSplitItem.setAmount(amountRemaining);
			splitItem.setAmount(amountToSplit);
			inventory.setItem(slot, splitItem);

			if (amountToSplit > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + slot + " to multiple items list");
				slotsWithMultipleItems.add(slot);
			}
			if (amountRemaining > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + splitSlot + " to multiple items list");
				slotsWithMultipleItems.add(splitSlot);
			}
		}
		// Alert the player if a SKR scroll was detected.
		if (skrScrolls) {
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute at %1$s as %1$s run function monumenta:skr/scroll_drop".formatted(player.getName()));
		}
	}

	// Test for an SKR Scroll
	public static boolean testForScroll(ItemStack item) {
		return (InventoryUtils.testForItemWithName(item, "Remnant Scroll", false) &&
			item.getType().name().contains("SMITHING_TEMPLATE"));
	}

	public static boolean isUnscaledChest(Block block) {
		return block.getState() instanceof Chest chest && chest.getLootTable() != null &&
			chest.getSeed() == 0 && isChestBlockEmpty(chest);
	}

	public static boolean isAstrableChest(Chest chest) {
		return DelveLootTableGroup.hasDelveableLootTable(chest) && isChestBlockEmpty(chest);
	}

	/**
	 * Checks if the inventory of this chest block is empty (i.e. only checks one half of a double chest)
	 *
	 * @see #isEmpty(Chest)
	 */
	public static boolean isChestBlockEmpty(Chest chest) {
		return Arrays.stream(chest.getBlockInventory().getContents()).allMatch(ItemUtils::isNullOrAir);
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest) block.getState());
	}

	/**
	 * Checks if the inventory of this entire chest is empty (i.e. checks both halves of a double chest)
	 *
	 * @see #isChestBlockEmpty(Chest)
	 */
	public static boolean isEmpty(Chest chest) {
		return Arrays.stream(chest.getInventory().getContents()).allMatch(ItemUtils::isNullOrAir);
	}

	public static boolean isChestWithLootTable(Block block) {
		Material type = block.getType();
		return (type == Material.CHEST || type == Material.TRAPPED_CHEST)
			&& block.getState() instanceof Chest chest
			&& (chest.hasLootTable() || (chest.getInventory() instanceof DoubleChestInventory doubleChestInventory
			&& (((Chest) doubleChestInventory.getLeftSide().getHolder()).hasLootTable() || ((Chest) doubleChestInventory.getRightSide().getHolder()).hasLootTable())));
	}

	public static ItemStack giveChestWithLootTable(String lootTable, String chestName, @Nullable String chestNameColor, List<Component> lore) {
		ItemStack chest = new ItemStack(Material.CHEST);
		if (chest.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof Chest chestMeta) {
			Component name = Component.text(chestName).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true);
			if (chestNameColor != null) {
				name = name.color(TextColor.fromHexString(chestNameColor));
			}
			blockMeta.displayName(name);
			chestMeta.setLootTable(Bukkit.getLootTable(NamespacedKey.fromString(lootTable)));
			blockMeta.setBlockState(chestMeta);
			blockMeta.lore(lore);
			chest.setItemMeta(blockMeta);
		}
		ItemUtils.setPlainTag(chest);

		return chest;
	}

	public static boolean isNonLootLimitedChest(Block block) {
		if (block.getState() instanceof PersistentDataHolder persistentDataHolder) {
			return Boolean.TRUE.equals(
				persistentDataHolder
					.getPersistentDataContainer()
					.get(NON_LOOT_LIMITED, PersistentDataType.BOOLEAN)
			);
		}

		return false;
	}

	public static void setNonLootLimitedChest(Block block, boolean value) {
		if (block instanceof PersistentDataHolder persistentDataHolder) {
			PersistentDataContainer dataContainer = persistentDataHolder.getPersistentDataContainer();
			if (value) {
				dataContainer.set(NON_LOOT_LIMITED, PersistentDataType.BOOLEAN, true);
			} else {
				dataContainer.remove(NON_LOOT_LIMITED);
			}
		}
	}

}
