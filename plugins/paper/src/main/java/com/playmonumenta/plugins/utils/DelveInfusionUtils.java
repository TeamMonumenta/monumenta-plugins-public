package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.ClassSelectionCustomInventory;
import com.playmonumenta.plugins.itemstats.infusions.Understanding;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class DelveInfusionUtils {

	public static final int MAX_LEVEL = 4;
	public static final int[] MAT_DEPTHS_COST_PER_INFUSION = {2, 4, 8, 16};
	public static final int[] MAT_COST_PER_INFUSION = {3, 6, 12, 24};
	public static final int[] XP_COST_PER_LEVEL = {ExperienceUtils.LEVEL_40, ExperienceUtils.LEVEL_50, ExperienceUtils.LEVEL_60, ExperienceUtils.LEVEL_70};

	/**When set to true the refund function will return all the XP used for the infusion, when false only the 75% */
	public static final boolean FULL_REFUND = false;
	public static final double REFUND_PERCENT = 0.75;

	public static final NamespacedKey DEPTHS_MAT_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode");

	public enum DelveInfusionSelection {
		PENNATE("pennate", InfusionType.PENNATE, NamespacedKeyUtils.fromString("epic:r1/delves/white/auxiliary/delve_material"), "White"),
		CARAPACE("carapace", InfusionType.CARAPACE, NamespacedKeyUtils.fromString("epic:r1/delves/orange/auxiliary/delve_material"), "Orange"),
		AURA("aura", InfusionType.AURA, NamespacedKeyUtils.fromString("epic:r1/delves/magenta/auxiliary/delve_material"), "Magenta"),
		EXPEDITE("expedite", InfusionType.EXPEDITE, NamespacedKeyUtils.fromString("epic:r1/delves/lightblue/auxiliary/delve_material"), "LightBlue"),
		CHOLER("choler", InfusionType.CHOLER, NamespacedKeyUtils.fromString("epic:r1/delves/yellow/auxiliary/delve_material"), "Yellow"),
		UNYIELDING("unyielding", InfusionType.UNYIELDING, NamespacedKeyUtils.fromString("epic:r1/delves/willows/auxiliary/echoes_of_the_veil"), "R1Bonus"),
		USURPER("usurper", InfusionType.USURPER, NamespacedKeyUtils.fromString("epic:r1/delves/reverie/auxiliary/delve_material"), "Corrupted"),
		VENGEFUL("vengeful", InfusionType.VENGEFUL, NamespacedKeyUtils.fromString("epic:r1/delves/rogue/persistent_parchment"), "RogFinished", "RogFinishedN", "RogFinishedC", "RogFinishedD"),

		EMPOWERED("empowered", InfusionType.EMPOWERED, NamespacedKeyUtils.fromString("epic:r2/delves/lime/auxiliary/delve_material"), "Lime"),
		NUTRIMENT("nutriment", InfusionType.NUTRIMENT, NamespacedKeyUtils.fromString("epic:r2/delves/pink/auxiliary/delve_material"), "Pink"),
		EXECUTION("execution", InfusionType.EXECUTION, NamespacedKeyUtils.fromString("epic:r2/delves/gray/auxiliary/delve_material"), "Gray"),
		REFLECTION("reflection", InfusionType.REFLECTION, NamespacedKeyUtils.fromString("epic:r2/delves/lightgray/auxiliary/delve_material"), "LightGray"),
		MITOSIS("mitosis", InfusionType.MITOSIS, NamespacedKeyUtils.fromString("epic:r2/delves/cyan/auxiliary/delve_material"), "Cyan"),
		ARDOR("ardor", InfusionType.ARDOR, NamespacedKeyUtils.fromString("epic:r2/delves/purple/auxiliary/delve_material"), "Purple"),
		EPOCH("epoch", InfusionType.EPOCH, NamespacedKeyUtils.fromString("epic:r2/delves/teal/auxiliary/delve_material"), "Teal"),
		NATANT("natant", InfusionType.NATANT, NamespacedKeyUtils.fromString("epic:r2/delves/shiftingcity/auxiliary/delve_material"), "Fred"),
		UNDERSTANDING("understanding", InfusionType.UNDERSTANDING, NamespacedKeyUtils.fromString("epic:r2/delves/forum/auxiliary/delve_material"), "Forum"),

		REFRESH("refresh", InfusionType.REFRESH, NamespacedKeyUtils.fromString("epic:r3/items/currency/silver_remnant"), "SKT", "SKTH"),
		SOOTHING("soothing", InfusionType.SOOTHING, NamespacedKeyUtils.fromString("epic:r3/items/currency/sorceress_stave"), "Blue"),
		QUENCH("quench", InfusionType.QUENCH, NamespacedKeyUtils.fromString("epic:r3/items/currency/fenian_flower"), ClassSelectionCustomInventory.R3_UNLOCK_SCOREBOARD),
		GRACE("grace", InfusionType.GRACE, NamespacedKeyUtils.fromString("epic:r3/items/currency/iridium_catalyst"), ClassSelectionCustomInventory.R3_UNLOCK_SCOREBOARD),
		GALVANIC("galvanic", InfusionType.GALVANIC, NamespacedKeyUtils.fromString("epic:r3/items/currency/corrupted_circuit"), "Portal"),
		DECAPITATION("decapitation", InfusionType.DECAPITATION, NamespacedKeyUtils.fromString("epic:r3/items/currency/shattered_mask"), "MasqueradersRuin"),
		FUELED("fueled", InfusionType.FUELED, NamespacedKeyUtils.fromString("epic:r3/items/currency/broken_god_gearframe"), "Brown"),

		REFUND("refund", null, null, (String[]) null);

		private final String mLabel;
		private final @Nullable InfusionType mInfusionType;
		private final @Nullable NamespacedKey mLootTable;
		private final @Nullable List<String> mScoreboard;

		DelveInfusionSelection(String label, @Nullable InfusionType infusionType, @Nullable NamespacedKey lootTable, @Nullable String... scoreboard) {
			mLabel = label;
			mInfusionType = infusionType;
			mLootTable = lootTable;
			mScoreboard = scoreboard == null ? null : Arrays.asList(scoreboard);
		}

		public static @Nullable DelveInfusionSelection getInfusionSelection(@Nullable String label) {
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

		public @Nullable InfusionType getInfusionType() {
			return mInfusionType;
		}

		public @Nullable NamespacedKey getLootTable() {
			return mLootTable;
		}

		public boolean isUnlocked(Player player) {
			return mScoreboard == null || mScoreboard.stream().anyMatch(s -> s == null || ScoreboardUtils.getScoreboardValue(player, s).orElse(0) >= 1);
		}
	}

	public static void infuseItem(Player player, ItemStack item, DelveInfusionSelection selection) {
		if (selection.equals(DelveInfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		}

		InfusionType infusionType = selection.getInfusionType();
		if (infusionType == null) {
			return;
		}

		//Assume the player has already paid for this infusion
		int prevLvl = ItemStatUtils.getInfusionLevel(item, infusionType);
		if (prevLvl > 0) {
			ItemStatUtils.removeInfusion(item, infusionType, false);
		}
		ItemStatUtils.addInfusion(item, infusionType, prevLvl + 1, player.getUniqueId());

		EntityUtils.fireworkAnimation(player);
	}

	public static void refundInfusion(ItemStack item, Player player) {
		DelveInfusionSelection infusion = getCurrentInfusion(item);
		if (infusion == null) {
			return;
		}
		InfusionType infusionType = infusion.getInfusionType();
		if (infusionType == null) {
			return;
		}

		int level = getInfuseLevel(item) - 1;
		int levelXp = level;

		ItemStatUtils.removeInfusion(item, infusionType);

		/* Audit */
		String matStr = "";
		int auditLevel = level + 1;

		while (level >= 0) {
			List<ItemStack> mats = getCurrenciesCost(item, infusion, level, player);
			level--;

			/* Audit */
			for (ItemStack it : mats) {
				if (it != null && it.getAmount() > 0) {
					if (!matStr.isEmpty()) {
						matStr += ",";
					}
					matStr += "'" + ItemUtils.getPlainName(it) + ":" + it.getAmount() + "'";
				}
			}

			giveMaterials(player, mats);
		}

		AuditListener.log("Delve infusion refund - player=" + player.getName() + " item='" + ItemUtils.getPlainName(item) + "' level=" + auditLevel + "' stack size=" + item.getAmount() + " mats=" + matStr);

		int xp = ExperienceUtils.getTotalExperience(player);
		for (int i = 0; i <= levelXp; i++) {
			xp += (int) (XP_COST_PER_LEVEL[i] * (FULL_REFUND ? 1 : REFUND_PERCENT) * item.getAmount());
		}
		ExperienceUtils.setTotalExperience(player, xp);
	}

	private static void giveMaterials(Player player, List<ItemStack> mats) {
		for (ItemStack item : mats) {
			InventoryUtils.giveItem(player, item);
		}
	}

	private static int getInfuseLevel(ItemStack item) {
		int level = 0;
		for (DelveInfusionSelection d : DelveInfusionSelection.values()) {
			level += ItemStatUtils.getInfusionLevel(item, d.getInfusionType());
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
		ItemStack delveMats = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(p, Objects.requireNonNull(selection.mLootTable)));
		delveMats.setAmount(MAT_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(delveMats);

		//Get depth mat loot table
		ItemStack depthMats = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(p, DEPTHS_MAT_LOOT_TABLE));
		depthMats.setAmount(MAT_DEPTHS_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(depthMats);
		return cost;
	}

	public static double getModifiedLevel(Plugin plugin, Player player, int level) {
		return getModifiedLevel(level, plugin.mItemStatManager.getInfusionLevel(player, InfusionType.UNDERSTANDING));
	}

	// Caps level at MAX_LEVEL, and then adds the forum infusion bonus
	public static double getModifiedLevel(int level, int understanding) {
		if (level == 0) {
			return 0;
		}
		return Math.min(MAX_LEVEL, level) + Math.min(understanding, MAX_LEVEL) * Understanding.POINTS_PER_LEVEL;
	}

	public static @Nullable DelveInfusionSelection getCurrentInfusion(ItemStack item) {
		for (DelveInfusionSelection infusionSelection : DelveInfusionSelection.values()) {
			if (ItemStatUtils.getInfusionLevel(item, infusionSelection.getInfusionType()) > 0) {
				return infusionSelection;
			}
		}
		return null;
	}

	public static int getInfusionLevel(ItemStack item, DelveInfusionSelection selection) {
		return ItemStatUtils.getInfusionLevel(item, selection.getInfusionType());
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
