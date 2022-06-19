package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.Understanding;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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

public class DelveInfusionUtils {

	public static final int MAX_LEVEL = 4;
	public static final int[] MAT_DEPTHS_COST_PER_INFUSION = {2, 4, 8, 16};
	public static final int[] MAT_COST_PER_INFUSION = {3, 6, 12, 24};
	public static final int[] XP_COST_PER_LEVEL = {ExperienceUtils.LEVEL_40, ExperienceUtils.LEVEL_50, ExperienceUtils.LEVEL_60, ExperienceUtils.LEVEL_70};

	/**When set to true the refund function will return all the XP used for the infusion, when false only the 50% */
	public static final boolean FULL_REFUND = false;

	public static final NamespacedKey DEPTHS_MAT_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode");

	public enum DelveInfusionSelection {
		PENNATE("pennate", "Pennate", NamespacedKeyUtils.fromString("epic:r1/delves/white/auxiliary/delve_material")),
		CARAPACE("carapace", "Carapace", NamespacedKeyUtils.fromString("epic:r1/delves/orange/auxiliary/delve_material")),
		AURA("aura", "Aura", NamespacedKeyUtils.fromString("epic:r1/delves/magenta/auxiliary/delve_material")),
		EXPEDITE("expedite", "Expedite", NamespacedKeyUtils.fromString("epic:r1/delves/lightblue/auxiliary/delve_material")),
		CHOLER("choler", "Choler", NamespacedKeyUtils.fromString("epic:r1/delves/yellow/auxiliary/delve_material")),
		UNYIELDING("unyielding", "Unyielding", NamespacedKeyUtils.fromString("epic:r1/delves/willows/auxiliary/echoes_of_the_veil")),
		USURPER("usurper", "Usurper", NamespacedKeyUtils.fromString("epic:r1/delves/reverie/auxiliary/delve_material")),
		VENGEFUL("vengeful", "Vengeful", NamespacedKeyUtils.fromString("epic:r1/delves/rogue/persistent_parchment")),

		EMPOWERED("empowered", "Empowered", NamespacedKeyUtils.fromString("epic:r2/delves/lime/auxiliary/delve_material")),
		NUTRIMENT("nutriment", "Nutriment", NamespacedKeyUtils.fromString("epic:r2/delves/pink/auxiliary/delve_material")),
		EXECUTION("execution", "Execution", NamespacedKeyUtils.fromString("epic:r2/delves/gray/auxiliary/delve_material")),
		REFLECTION("reflection", "Reflection", NamespacedKeyUtils.fromString("epic:r2/delves/lightgray/auxiliary/delve_material")),
		MITOSIS("mitosis", "Mitosis", NamespacedKeyUtils.fromString("epic:r2/delves/cyan/auxiliary/delve_material")),
		ARDOR("ardor", "Ardor", NamespacedKeyUtils.fromString("epic:r2/delves/purple/auxiliary/delve_material")),
		EPOCH("epoch", "Epoch", NamespacedKeyUtils.fromString("epic:r2/delves/teal/auxiliary/delve_material")),
		NATANT("natant", "Natant", NamespacedKeyUtils.fromString("epic:r2/delves/shiftingcity/auxiliary/delve_material")),
		UNDERSTANDING("understanding", "Understanding", NamespacedKeyUtils.fromString("epic:r2/delves/forum/auxiliary/delve_material")),

		REFRESH("refresh", "Refresh", NamespacedKeyUtils.fromString("epic:r3/items/currency/silver_remnant")),
		SOOTHING("soothing", "Soothing", NamespacedKeyUtils.fromString("epic:r3/items/currency/sorceress_stave")),
		QUENCH("quench", "Quench", NamespacedKeyUtils.fromString("epic:r3/items/currency/fenian_flower")),
		GRACE("grace", "Grace", NamespacedKeyUtils.fromString("epic:r3/items/currency/iridium_catalyst")),

		REFUND("refund", "refund", null);

		private final String mLabel;
		private final String mEnchantName;
		private final @Nullable NamespacedKey mLootTable;

		DelveInfusionSelection(String label, String enchantName, @Nullable NamespacedKey lootTable) {
			mLabel = label;
			mEnchantName = enchantName;
			mLootTable = lootTable;
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

		public String getEnchantName() {
			return mEnchantName;
		}

		public @Nullable NamespacedKey getLootTable() {
			return mLootTable;
		}
	}

	public static void infuseItem(Player player, ItemStack item, DelveInfusionSelection selection) throws Exception {
		if (selection.equals(DelveInfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		}

		//Assume the player has already paid for this infusion
		int prevLvl = ItemStatUtils.getInfusionLevel(item, InfusionType.getInfusionType(selection.getEnchantName()));
		if (prevLvl > 0) {
			ItemStatUtils.removeInfusion(item, InfusionType.getInfusionType(selection.getEnchantName()), false);
		}
		ItemStatUtils.addInfusion(item, InfusionType.getInfusionType(selection.getEnchantName()), prevLvl + 1, player.getUniqueId());

		animate(player);
	}

	public static void refundInfusion(ItemStack item, Player player) {
		DelveInfusionSelection infusion = getCurrentInfusion(item);
		if (infusion == null) {
			return;
		}
		int level = getInfuseLevel(item) - 1;
		int levelXp = level;

		ItemStatUtils.removeInfusion(item, InfusionType.getInfusionType(infusion.getEnchantName()));
		ItemStatUtils.generateItemStats(item);

		List<ItemStack> mats = null;

		/* Audit */
		String matStr = "";
		int auditLevel = level + 1;

		while (level >= 0) {
			mats = getCurrenciesCost(item, infusion, level, player);
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
			mats.clear();
		}

		AuditListener.log("Delve infusion refund - player=" + player.getName() + " item='" + ItemUtils.getPlainName(item) + "' level=" + auditLevel + "' stack size=" + item.getAmount() + " mats=" + matStr);

		int xp = ExperienceUtils.getTotalExperience(player);
		for (int i = 0; i <= levelXp; i++) {
			xp += (FULL_REFUND ? XP_COST_PER_LEVEL[i] : XP_COST_PER_LEVEL[i] / 2) * item.getAmount();
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
			level += ItemStatUtils.getInfusionLevel(item, InfusionType.getInfusionType(d.getEnchantName()));
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
		delveMats.setAmount(MAT_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(delveMats);

		//Get depth mat loot table
		ItemStack depthMats = InventoryUtils.getItemFromLootTable(p, DEPTHS_MAT_LOOT_TABLE).clone();
		depthMats.setAmount(MAT_DEPTHS_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(depthMats);
		return cost;
	}


	// Caps level at MAX_LEVEL, and then adds the forum infusion bonus
	public static double getModifiedLevel(Plugin plugin, Player player, int level) {
		if (level == 0) {
			return 0;
		}
		return Math.min(MAX_LEVEL, level) + Math.min(plugin.mItemStatManager.getInfusionLevel(player, InfusionType.UNDERSTANDING), MAX_LEVEL) * Understanding.POINTS_PER_LEVEL;
	}


	public static @Nullable DelveInfusionSelection getCurrentInfusion(ItemStack item) {
		for (DelveInfusionSelection infusionSelection : DelveInfusionSelection.values()) {
			if (ItemStatUtils.getInfusionLevel(item, InfusionType.getInfusionType(infusionSelection.getEnchantName())) > 0) {
				return infusionSelection;
			}
		}
		return null;
	}

	public static int getInfusionLevel(ItemStack item, DelveInfusionSelection selection) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.getInfusionType(selection.getEnchantName()));
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
