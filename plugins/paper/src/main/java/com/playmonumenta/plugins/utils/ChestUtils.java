package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ChestUtils {

	public static final int CHEST_LUCK_RADIUS = 128;
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

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle").orElse(0);
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.playersInRange(player.getLocation(), CHEST_LUCK_RADIUS, true).size();

			setPlayerLuckLevel(player, plugin);

			if (player.getEquipment() != null &&
			    player.getEquipment().getItemInMainHand() != null &&
			    player.getEquipment().getItemInMainHand().getType() == Material.COMPASS) {
				if (playerCount == 1) {
					MessagingUtils.sendActionBarMessage(player, playerCount + " player in range!");
				} else {
					MessagingUtils.sendActionBarMessage(player, playerCount + " players in range!");
				}
			}
		}
	}

	public static void generateContainerLootWithScaling(Player player, Block block, Plugin plugin) {
		BlockState blockState = block.getState();
		if (blockState instanceof Container) {
			Inventory inventory = ((Container) blockState).getInventory();
			if (inventory instanceof DoubleChestInventory) {
				generateContainerLootWithScaling(player, ((DoubleChestInventory) inventory).getLeftSide(), plugin);
				generateContainerLootWithScaling(player, ((DoubleChestInventory) inventory).getRightSide(), plugin);
			} else {
				generateContainerLootWithScaling(player, inventory, plugin);
			}
		}
	}

	private static void generateContainerLootWithScaling(Player player, Inventory inventory, Plugin plugin) {
		if (inventory.getHolder() instanceof Lootable lootable) {

			LootTable lootTable = lootable.getLootTable();
			if (lootTable != null) {

				setPlayerLuckLevel(player, plugin);
				LootContext.Builder builder = new LootContext.Builder(player.getLocation());
				if (player.hasPotionEffect(PotionEffectType.LUCK)) {
					builder.luck(player.getPotionEffect(PotionEffectType.LUCK).getAmplifier() + 1);
				}
				LootContext context = builder.build();
				Collection<ItemStack> popLoot = lootTable.populateLoot(FastUtils.RANDOM, context);

				inventory.clear();
				ChestUtils.generateLootInventory(popLoot, inventory, player);

			}
		}
	}

	public static int setPlayerLuckLevel(Player player, Plugin plugin) {
		int playerCount = PlayerUtils.playersInRange(player.getLocation(), CHEST_LUCK_RADIUS, true).size();
		double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, playerCount)];
		int luckLevel = (int) bonusItems;

		if (FastUtils.RANDOM.nextDouble() < bonusItems - luckLevel) {
			luckLevel++;
		}

		if (luckLevel > 0) {
			plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
			                                3, luckLevel - 1, true, false));
		}
		return luckLevel;
	}

	public static void generateLootInventory(Collection<ItemStack> populatedLoot, Inventory inventory, Player player) {
		ArrayList<ItemStack> lootList = new ArrayList<>();
		for (ItemStack i : populatedLoot) {
			if (i == null) {
				i = new ItemStack(Material.AIR);
			}
			lootList.add(i);
		}

		List<Integer> freeSlots = new ArrayList<>(27);
		for (int i = 0; i < 27; i++) {
		    freeSlots.add(i);
		}
		Collections.shuffle(freeSlots);

		ArrayDeque<Integer> slotsWithMultipleItems = new ArrayDeque<>();
		for (ItemStack lootItem : lootList) {
			if (freeSlots.size() == 0) {
				Plugin.getInstance().getLogger().severe("Tried to overfill container for player " + player.getName() + " at inventory " + inventory.getType().toString() + " at location " + player.getLocation().toString());
				player.sendMessage("Tried to overfill this container! Please report this");
				break;
			}
			int slot = freeSlots.remove(0);
			inventory.setItem(slot, lootItem);
			if (lootItem.getAmount() > 1) {
				slotsWithMultipleItems.add(slot);
			}
		}

		while (freeSlots.size() > 1 && slotsWithMultipleItems.size() > 0) {
		    int splitslot = slotsWithMultipleItems.getFirst();
		    int slot = freeSlots.remove(0);

		    ItemStack toSplitItem = inventory.getItem(splitslot);
		    ItemStack splitItem = toSplitItem.clone();
		    int amountToSplit = toSplitItem.getAmount() / 2;

		    toSplitItem.setAmount(toSplitItem.getAmount() - amountToSplit);
		    splitItem.setAmount(amountToSplit);
		    inventory.setItem(slot, splitItem);

		    if (amountToSplit > 1) {
		        slotsWithMultipleItems.add(slot);
		    }
		}
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest)block.getState());
	}

	public static boolean isEmpty(Chest chest) {
		for (ItemStack slot : chest.getInventory()) {
			if (slot != null) {
				return false;
			}
		}
		return true;
	}


}
