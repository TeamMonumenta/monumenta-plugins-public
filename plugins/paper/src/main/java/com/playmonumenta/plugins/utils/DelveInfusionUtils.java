package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.infusions.delves.Ardor;
import com.playmonumenta.plugins.enchantments.infusions.delves.Aura;
import com.playmonumenta.plugins.enchantments.infusions.delves.Carapace;
import com.playmonumenta.plugins.enchantments.infusions.delves.Choler;
import com.playmonumenta.plugins.enchantments.infusions.delves.Empowered;
import com.playmonumenta.plugins.enchantments.infusions.delves.Epoch;
import com.playmonumenta.plugins.enchantments.infusions.delves.Execution;
import com.playmonumenta.plugins.enchantments.infusions.delves.Expedite;
import com.playmonumenta.plugins.enchantments.infusions.delves.Mitosis;
import com.playmonumenta.plugins.enchantments.infusions.delves.Natant;
import com.playmonumenta.plugins.enchantments.infusions.delves.Nutriment;
import com.playmonumenta.plugins.enchantments.infusions.delves.Pennate;
import com.playmonumenta.plugins.enchantments.infusions.delves.Reflection;
import com.playmonumenta.plugins.enchantments.infusions.delves.Understanding;
import com.playmonumenta.plugins.enchantments.infusions.delves.Usurper;

public class DelveInfusionUtils {

	public static final int MAX_LEVEL = 4;
	public static final int[] MAT_DEPTHS_COST_PER_INFUSION = {2, 4, 8, 16};
	public static final int[] MAT_COST_PER_INFUSION = {4, 8, 16, 32};
	public static final int[] XP_COST_PER_LEVEL = {2920, 5345, 8670, 12895};
													//40, 50, 60, 70

	public static final NamespacedKey DEPTHS_MAT_LOOT_TABLE = NamespacedKey.fromString("epic:r2/depths/loot/voidstained_geode");

	public enum DelveInfusionSelection {
		//TODO put more delve infusions here! This is the only place in the file you need to update!
		PENNATE("pennate", Pennate.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/white/auxiliary/delve_material")),
		CARAPACE("carapace", Carapace.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/orange/auxiliary/delve_material")),
		AURA("aura", Aura.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/magenta/auxiliary/delve_material")),
		EXPEDITE("expedite", Expedite.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/lightblue/auxiliary/delve_material")),
		CHOLER("choler", Choler.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/yellow/auxiliary/delve_material")),
		USURPER("usurper", Usurper.PROPERTY_NAME, NamespacedKey.fromString("epic:r1/delves/reverie/auxiliary/delve_material")),

		EMPOWERED("empowered", Empowered.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/lime/auxiliary/delve_material")),
		NUTRIMENT("nutriment", Nutriment.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/pink/auxiliary/delve_material")),
		EXECUTION("execution", Execution.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/gray/auxiliary/delve_material")),
		REFLECTION("reflection", Reflection.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/lightgray/auxiliary/delve_material")),
		MITOSIS("mitosis", Mitosis.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/cyan/auxiliary/delve_material")),
		ARDOR("ardor", Ardor.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/purple/auxiliary/delve_material")),
		EPOCH("epoch", Epoch.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/teal/auxiliary/delve_material")),
		NATANT("natant", Natant.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/shiftingcity/auxiliary/delve_material")),
		UNDERSTANDING("understanding", Understanding.PROPERTY_NAME, NamespacedKey.fromString("epic:r2/delves/forum/auxiliary/delve_material")),

		REFUND("refund", "refund", null);

		private final String mLabel;
		private final String mEnchantName;
		private final NamespacedKey mLootTable;
		DelveInfusionSelection(String label, String enchantName, NamespacedKey lootTable) {
			mLabel = label;
			mEnchantName = enchantName;
			mLootTable = lootTable;
		}

		public static DelveInfusionSelection getInfusionSelection(String label) {
			if (label == null) {
				return null;
			}
			for (DelveInfusionSelection selection : DelveInfusionSelection.values()) {
				if (selection.getLabel().equals(label)) {
					return selection;
				}
			}
			return null;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getEnchantName() {
			return mEnchantName;
		}

		public NamespacedKey getLootTable() {
			return mLootTable;
		}
	}

	public static void infuseItem(Player player, ItemStack item, DelveInfusionSelection selection) throws Exception {
		if (selection.equals(DelveInfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		}

		//Assume the player has already paid for this infusion
		int prevLvl = InventoryUtils.getCustomEnchantLevel(item, selection.getEnchantName(), true);
		if (prevLvl > 0) {
			InventoryUtils.removeCustomEnchant(item, selection.getEnchantName());
		}

		String numeral = "";
		switch (prevLvl) {
			case 1:
				numeral = " II";
				break;
			case 2:
				numeral = " III";
				break;
			case 3:
				numeral = " IV";
				break;
			case 0:
				numeral = " I";
				break;
			default:
				throw new RuntimeException("Error loading the level");
		}

		ItemUtils.enchantifyItem(item, ChatColor.stripColor(selection.getEnchantName()) + numeral);
		animate(player);
	}

	public static void refundInfusion(ItemStack item, Player player) {
		DelveInfusionSelection infusion = getCurrentInfusion(item);
		int level = getInfuseLevel(item) - 1;

		InventoryUtils.removeCustomEnchant(item, infusion.getEnchantName());

		List<ItemStack> mats = null;
		while (level >= 0) {
			mats = getCurrenciesCost(item, infusion, level, player);
			level--;
			giveMaterials(player, mats);
			mats.clear();
		}

		int xp = ExperienceUtils.getTotalExperience(player);
		for (int i = 0; i < level; i++) {
			xp += XP_COST_PER_LEVEL[i] / 2;
		}
		ExperienceUtils.setTotalExperience(player, xp);
	}

	private static void giveMaterials(Player player, List<ItemStack> mats) {
		for (ItemStack item : mats) {
			InventoryUtils.giveItem(player, item);
		}
	}

	private static void animate(Player player) {
		Location loc = player.getLocation();
		Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
		fwBuilder.withColor(Color.RED, Color.GREEN, Color.BLUE);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(Plugin.getInstance(), 5);
	}

	private static int getInfuseLevel(ItemStack item) {
		int level = 0;
		for (DelveInfusionSelection d : DelveInfusionSelection.values()) {
			level += InventoryUtils.getCustomEnchantLevel(item, d.getEnchantName(), true);
		}
		return level;
	}

	public static boolean canPayInfusion(ItemStack item, DelveInfusionSelection selection, Player p) {

		if (selection == DelveInfusionSelection.REFUND || p.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		int targetLevel = getInfuseLevel(item);
		List<ItemStack> mats = getCurrenciesCost(item, selection, targetLevel, p);
		int playerXP = ExperienceUtils.getTotalExperience(p);

		if (playerXP < XP_COST_PER_LEVEL[targetLevel]) {
			return false;
		}

		PlayerInventory inventory = p.getInventory();

		for (ItemStack currencies : mats) {
			if (!inventory.containsAtLeast(currencies, currencies.getAmount())) {
				return false;
			}
		}

		return true;
	}

	public static boolean payInfusion(ItemStack item, DelveInfusionSelection selection, Player p) {
		//if the player is in creative -> free infusion
		if (selection == DelveInfusionSelection.REFUND || p.getGameMode() == GameMode.CREATIVE) {
			Plugin.getInstance().getLogger().warning("[Delve Infusion] Player: " + p.getName() + " infused an item while be on creative mode! InfusionType: " + selection.getLabel());
			return true;
		}
		int targetLevel = getInfuseLevel(item);
		List<ItemStack> mats = getCurrenciesCost(item, selection, targetLevel, p);

		int playerXP = ExperienceUtils.getTotalExperience(p);

		if (playerXP < XP_COST_PER_LEVEL[targetLevel]) {
			return false;
		} else {
			ExperienceUtils.setTotalExperience(p, playerXP - XP_COST_PER_LEVEL[targetLevel]);
		}

		PlayerInventory inventory = p.getInventory();

		for (ItemStack currencies : mats) {
			inventory.removeItem(currencies);
		}

		return true;
	}

	public static List<ItemStack> getCurrenciesCost(ItemStack item, DelveInfusionSelection selection, int level, Player p) {

		List<ItemStack> cost = new ArrayList<>();

		//Get delve mat loot table
		ItemStack delveMats = InventoryUtils.getItemFromLootTable(p, selection.mLootTable).clone();
		delveMats.setAmount(MAT_COST_PER_INFUSION[level]);
		cost.add(delveMats);

		//Get depth mat loot table
		ItemStack depthMats = InventoryUtils.getItemFromLootTable(p, DEPTHS_MAT_LOOT_TABLE).clone();
		depthMats.setAmount(MAT_DEPTHS_COST_PER_INFUSION[level]);
		cost.add(depthMats);
		return cost;
	}


	// Caps level at MAX_LEVEL, and then adds the forum infusion bonus
	public static double getModifiedLevel(Plugin plugin, Player player, int level) {
		return Math.min(MAX_LEVEL, level) + Math.min(plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Understanding.class), MAX_LEVEL) * Understanding.POINTS_PER_LEVEL;
	}


	public static DelveInfusionSelection getCurrentInfusion(ItemStack item) {
		for (DelveInfusionSelection infusionSelection : DelveInfusionSelection.values()) {
			if (InventoryUtils.getCustomEnchantLevel(item, infusionSelection.mEnchantName, true) > 0) {
				return infusionSelection;
			}
		}
		return null;
	}

	public static int getInfusionLevel(ItemStack item, DelveInfusionSelection selection) {
		return InventoryUtils.getCustomEnchantLevel(item, selection.mEnchantName, true);
	}

	public static int getExpLvlInfuseCost(ItemStack item) {
		int exp = XP_COST_PER_LEVEL[getInfuseLevel(item)];

		switch (exp) {
			case 1395:
				return 30;
			case 2920:
				return 40;
			case 5345:
				return 50;
			case 8670:
				return 60;
			case 12895:
				return 70;
			case 18020:
				return 80;
			default:
				return 0;
		}
	}
}
