package com.playmonumenta.plugins.utils;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.infusions.Acumen;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.enchantments.infusions.Perspicacity;
import com.playmonumenta.plugins.enchantments.infusions.Tenacity;
import com.playmonumenta.plugins.enchantments.infusions.Vigor;
import com.playmonumenta.plugins.enchantments.infusions.Vitality;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class InfusionUtils {

	/**When set to true the refund function will return all the XP used for the infusion, when false only the 50% */
	public static final boolean FULL_REFUND = false;

	public enum InfusionSelection {
		ACUMEN("acumen", Acumen.PROPERTY_NAME),
		FOCUS("focus", Focus.PROPERTY_NAME),
		PERSPICACITY("perspicacity", Perspicacity.PROPERTY_NAME),
		TENACITY("tenacity", Tenacity.PROPERTY_NAME),
		VIGOR("vigor", Vigor.PROPERTY_NAME),
		VITALITY("vitality", Vitality.PROPERTY_NAME),
		REFUND("refund", "refund"),
		SPEC_REFUND("special", "special");

		private final String mLabel;
		private final String mEnchantName;

		InfusionSelection(String label, String enchantName) {
			mLabel = label;
			mEnchantName = enchantName;
		}

		public static @Nullable InfusionSelection getInfusionSelection(@Nullable String label) {
			if (label == null) {
				return null;
			}
			for (InfusionSelection selection : InfusionSelection.values()) {
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
	}

	public static void refundInfusion(ItemStack item, Player player) throws WrapperCommandSyntaxException {
		ItemRegion region = ItemUtils.getItemRegion(item);
		int refundMaterials = 0;

		//Calculate refund amount
		// First level is free and we calculate based on the level below current.
		int infuseLevel = getInfuseLevel(item) - 2;
		int costMult = getCostMultiplierWithCheck(item);
		while (infuseLevel >= 0) {
			refundMaterials += (costMult * Math.pow(2, infuseLevel));
			infuseLevel--;
		}

		int level = getInfuseLevel(item);

		AuditListener.log("Infusion refund - player=" + player.getName() + " item='" + ItemUtils.getPlainName(item) + "' level=" + level);

		//Remove the infusion enchants from the item
		for (InfusionSelection sel : InfusionSelection.values()) {
			InventoryUtils.removeCustomEnchant(item, sel.getEnchantName());
		}
		if (refundMaterials > 0) {
			giveMaterials(player, region, refundMaterials);
		}

		int xp = ExperienceUtils.getTotalExperience(player);
		int refundXP = 0;

		switch (ItemUtils.getItemTier(item)) {
			case MEME:
			case UNCOMMON:
			case ENHANCED_UNCOMMON:
			case UNIQUE:
			case UNIQUE_EVENT:
			case RARE:
			case PATRON_MADE:
				switch (level) {
					case 1:
						refundXP = (1395);
						break;
					case 2:
						refundXP = (1395 + 2920);
						break;
					case 3:
						refundXP = (1395 + 2920 + 5345);
						break;
					case 4:
						refundXP = (1395 + 2920 + 5345 + 8670);
						break;
					default:
					case 0:
						break;
				}
				break;
			case RELIC:
			case ARTIFACT:
			case ENHANCED_RARE:
				switch (level) {
					case 1:
						refundXP = (2920);
						break;
					case 2:
						refundXP = (2920 + 5345);
						break;
					case 3:
						refundXP = (2920 + 5345 + 8670);
						break;
					case 4:
						refundXP = (2920 + 5345 + 8670 + 12895);
						break;
					default:
					case 0:
						break;
				}
				break;
			case EPIC:
				switch (level) {
					case 1:
						refundXP = (5345);
						break;
					case 2:
						refundXP = (5345 + 8670);
						break;
					case 3:
						refundXP = (5345 + 8670 + 12895);
						break;
					case 4:
						refundXP = (5345 + 8670 + 12895 + 18020);
						break;
					default:
					case 0:
						break;
				}
				break;
			default:
				CommandAPI.fail("Invalid item. Item must be infused!");
		}

		refundXP = (FULL_REFUND ? refundXP : refundXP / 2);
		ExperienceUtils.setTotalExperience(player, xp + refundXP);
	}

	private static void giveMaterials(Player player, ItemRegion region, int refundMaterials) throws WrapperCommandSyntaxException {
		NamespacedKey key;
		if (region.equals(ItemRegion.KINGS_VALLEY)) {
			key = NamespacedKey.fromString("epic:r1/items/currency/pulsating_gold");
		} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
			key = NamespacedKey.fromString("epic:r2/items/currency/pulsating_emerald");
		} else {
			CommandAPI.fail("Item must have a Region tag!");
			return;
		}
		LootTable lt = Bukkit.getLootTable(key);
		LootContext.Builder builder = new LootContext.Builder(player.getLocation());
		LootContext context = builder.build();
		Collection<ItemStack> items = lt.populateLoot(FastUtils.RANDOM, context);
		ItemStack materials;
		if (items.size() > 0) {
			materials = items.iterator().next();
		} else {
			CommandAPI.fail("ERROR while refunding infusion (failed to get loot table). Please contact a moderator if you see this message!");
			return;
		}
		Item eItem = player.getWorld().dropItemNaturally(player.getLocation(), materials.add(refundMaterials - 1));
		eItem.setPickupDelay(0);
	}

	public static void animate(Player player) {
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

	public static int calcInfuseCost(ItemStack item) throws WrapperCommandSyntaxException {
		// First level is free
		int infuseLvl = getInfuseLevel(item) - 1;
		int cost = getCostMultiplierWithCheck(item);
		// Special case for first level
		if (infuseLvl == -1) {
			cost = 0;
		} else if (infuseLvl <= 2) {
			cost *= Math.pow(2, infuseLvl);
		} else {
			cost = 99999999;
			CommandAPI.fail("Items may only be infused 4 times!");
		}
		return cost;
	}

	public static int getInfuseLevel(ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, Acumen.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Focus.PROPERTY_NAME, true)
			+ InventoryUtils.getCustomEnchantLevel(item, Perspicacity.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Tenacity.PROPERTY_NAME, true)
			+ InventoryUtils.getCustomEnchantLevel(item, Vigor.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Vitality.PROPERTY_NAME, true);
	}

	private static int getCostMultiplierWithCheck(ItemStack item) throws WrapperCommandSyntaxException {
		int mult = getCostMultiplier(item);
		if (mult < 0) {
			CommandAPI.fail("Invalid item tier. Only Uncommon and higher tiered items are able to be infused!");
			return 99999999;
		}
		return mult;
	}

	/**
	 * Gets the infusion cost multiplier for the given item, or -1 if the item is not of a tier that can be infused.
	 */
	public static int getCostMultiplier(ItemStack item) {
		switch (ItemUtils.getItemTier(item)) {
			case MEME:
			case UNCOMMON:
			case ENHANCED_UNCOMMON:
			case UNIQUE:
			case UNIQUE_EVENT:
			case RARE:
			case PATRON_MADE:
				return 2;
			case RELIC:
			case ARTIFACT:
			case ENHANCED_RARE:
				return 3;
			case EPIC:
				return 6;
			default:
				return -1;
		}
	}

	private static int getExpInfuseCost(ItemStack item) throws WrapperCommandSyntaxException {
		int costMult = getCostMultiplierWithCheck(item);
		int level = getInfuseLevel(item);
		switch (costMult) {
			case 2:
				switch (level) {
					case 0:
					// Infuse Level 0 Rare
						return 1395;
						// Exp Level 30
					case 1:
					// Infuse Level 1 Rare
						return 2920;
						// Exp Level 40
					case 2:
					// Infuse Level 2 Rare
						return 5345;
						// Exp Level 50
					case 3:
					// Infuse Level 3 Rare
						return 8670;
						// Exp Level 60
					default:
					// Infuse Level 4 Rare
						CommandAPI.fail("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
						return 99999999;
						// Exp Level 9000 (but not really)
				}
			case 3:
				switch (level) {
					case 0:
					// Infuse Level 0 Artifact
						return 2920;
						// Exp Level 40
					case 1:
					// Infuse Level 1 Artifact
						return 5345;
						// Exp Level 50
					case 2:
					// Infuse Level 2 Artifact
						return 8670;
						// Exp Level 60
					case 3:
					// Infuse Level 3 Artifact
						return 12895;
						// Exp Level 70
					default:
					// Infuse Level 4 Artifact
						CommandAPI.fail("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
						return 99999999;
						// Exp Level 9000 (but not really)
				}
			case 6:
				switch (level) {
					case 0:
					// Infuse Level 0 Epic
						return 5345;
						// Exp Level 50
					case 1:
					// Infuse Level 1 Epic
						return 8670;
						// Exp Level 60
					case 2:
					// Infuse Level 2 Epic
						return 12895;
						// Exp Level 70
					case 3:
					// Infuse Level 3 Epic
						return 18020;
						// Exp Level 80
					default:
					// Infuse Level 4 Epic
						CommandAPI.fail("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
						return 99999999;
						// Exp Level 9000 (but not really)
				}
			default:
			// Infuse level What even happened?
				CommandAPI.fail("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
				return 99999999;
				// Exp Level How did you hit this code?
		}
	}

	public static InfusionSelection getCurrentInfusion(ItemStack item) {

		if (InventoryUtils.getCustomEnchantLevel(item, Acumen.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.ACUMEN;
		}

		if (InventoryUtils.getCustomEnchantLevel(item, Focus.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.FOCUS;
		}

		if (InventoryUtils.getCustomEnchantLevel(item, Perspicacity.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.PERSPICACITY;
		}

		if (InventoryUtils.getCustomEnchantLevel(item, Tenacity.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.TENACITY;
		}

		if (InventoryUtils.getCustomEnchantLevel(item, Vigor.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.VIGOR;
		}

		if (InventoryUtils.getCustomEnchantLevel(item, Vitality.PROPERTY_NAME, true) > 0) {
			return InfusionSelection.VITALITY;
		}

		return InfusionSelection.REFUND;
	}

	public static boolean infuseItem(ItemStack item, InfusionSelection selection) {
		if (!getCurrentInfusion(item).equals(selection) && getInfuseLevel(item) > 0) {
			return false;
		}

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
				return false;
		}

		String enchantment = selection.getEnchantName() + numeral;

		try {
			ItemUtils.enchantifyItem(item, enchantment);
		} catch (Exception e) {
			return false;
		}

		return true;
	}


	public static boolean isInfusionable(ItemStack item) {
		if (item == null) {
			return false;
		}

		if (item.getAmount() != 1) {
			return false;
		}

		ItemRegion region = ItemUtils.getItemRegion(item);
		if (region != ItemRegion.KINGS_VALLEY && region != ItemRegion.CELSIAN_ISLES) {
			return false;
		}

		switch (ItemUtils.getItemTier(item)) {
			case MEME:
			case UNCOMMON:
			case ENHANCED_UNCOMMON:
			case UNIQUE:
			case UNIQUE_EVENT:
			case RARE:
			case PATRON_MADE:
			case RELIC:
			case ARTIFACT:
			case ENHANCED_RARE:
			case EPIC:
				break;
			default:
				return false;
		}

		return true;
	}

	public static int getExpLvlInfuseCost(ItemStack item) {
		int exp = -10;
		try {
			exp = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return -1;
		}

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

	public static boolean canPayExp(Player player, ItemStack item) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		int expCost;
		int currentExp;

		try {
			expCost = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		currentExp = ExperienceUtils.getTotalExperience(player);

		if (currentExp < expCost) {
			return false;
		}

		return true;
	}

	public static boolean canPayPulsating(Player player, ItemStack item) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		ItemStack currency = null;

		if (ItemUtils.getItemRegion(item) == ItemRegion.CELSIAN_ISLES) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKey.fromString("epic:r2/items/currency/pulsating_emerald"));
		}

		if (ItemUtils.getItemRegion(item) == ItemRegion.KINGS_VALLEY) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKey.fromString("epic:r1/items/currency/pulsating_gold"));
		}

		if (currency == null) {
			//something went wrong
			return false;
		}

		int amount;

		try {
			amount = calcInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		currency.setAmount(amount);

		if (!player.getInventory().containsAtLeast(currency, amount)) {
			return false;
		}

		return true;
	}

	public static boolean canPayInfusion(Player player, ItemStack item) {
		return canPayExp(player, item) && canPayPulsating(player, item);
	}

	public static boolean payInfusion(Player player, ItemStack item) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			Plugin.getInstance().getLogger().warning("[Infusion] Player: " + player.getName() + " infused an item while be on creative mode!");
			return true;
		}

		//currency
		ItemStack currency = null;
		if (ItemUtils.getItemRegion(item) == ItemRegion.CELSIAN_ISLES) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKey.fromString("epic:r2/items/currency/pulsating_emerald"));
		}

		if (ItemUtils.getItemRegion(item) == ItemRegion.KINGS_VALLEY) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKey.fromString("epic:r1/items/currency/pulsating_gold"));
		}

		if (currency == null) {
			//something went wrong
			return false;
		}

		int amount;
		try {
			amount = calcInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		currency.setAmount(amount);
		player.getInventory().removeItem(currency);

		//exp
		int expCost;
		int currentExp;

		try {
			expCost = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		currentExp = ExperienceUtils.getTotalExperience(player);

		currentExp = currentExp - expCost;

		if (currentExp < 0) {
			return false;
		}

		ExperienceUtils.setTotalExperience(player, currentExp);

		return true;
	}

}
