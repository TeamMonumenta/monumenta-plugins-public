package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;

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

	private static final String PULSATING_GOLD = ChatColor.GOLD + "" + ChatColor.BOLD + "Pulsating Gold";
	private static final String PULSATING_GOLD_BAR = ChatColor.GOLD + "" + ChatColor.BOLD + "Pulsating Gold Bar";
	private static final String PULSATING_EMERALD = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pulsating Emerald";
	private static final String PULSATING_EMERALD_BLOCK = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pulsating Emerald Block";

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

		public static InfusionSelection getInfusionSelection(String label) {
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

	public static void doInfusion(CommandSender sender, Player player, ItemStack item, List<ItemFrame> paymentFrames, InfusionSelection selection) throws WrapperCommandSyntaxException {
		if (selection.equals(InfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		} else if (selection.equals(InfusionSelection.SPEC_REFUND)) {
			specialRefund(item, player);
			return;
		}

		//If item is not being refunded, check if cost adjust lore text exists, if so remove it to prevent abuse
		List<String> newLore = new ArrayList<>();
		for (String line : item.getLore()) {
			if (!line.contains("PRE COST ADJUST")) {
				newLore.add(line);
			}
		}
		item.setLore(newLore);
		ItemUtils.setPlainLore(item);

		ItemRegion region = ItemUtils.getItemRegion(item);
		int payment = calcPaymentValue(paymentFrames, region);
		int cost = calcInfuseCost(item);
		if (cost < 0) {
			CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			return;
		}

		if (item.getAmount() > 1) {
			CommandAPI.fail("Only one item can be infused at a time!");
			return;
		}

		if (payment == cost) {
			if (ExperienceUtils.getTotalExperience(player) >= getExpInfuseCost(item)) {
				//Infusion accepted
				for (ItemFrame frame : paymentFrames) {
					ItemStack frameItem = frame.getItem();
					if (frameItem == null || frameItem.getItemMeta() == null ||
							frameItem.getItemMeta().getDisplayName() == null) {
						continue;
					}
					if (region.equals(ItemRegion.KINGS_VALLEY)) {
						if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD)) {
							//Clear item frame contents
							frame.setItem(null);
						} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD_BAR)) {
							//Clear item frame contents
							frame.setItem(null);
						}
					} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
						if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD)) {
							//Clear item frame contents
							frame.setItem(null);
						} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD_BLOCK)) {
							//Clear item frame contents
							frame.setItem(null);
						}
					}
				}

				int newXP = ExperienceUtils.getTotalExperience(player) - getExpInfuseCost(item);
				ExperienceUtils.setTotalExperience(player, newXP);

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
						CommandAPI.fail("ERROR while assigning infusion level. Please contact a moderator if you see this message!");
				}
				CommandUtils.enchantify(sender, player, ChatColor.stripColor(selection.getEnchantName()) + numeral);

				animate(player);
			} else {
				CommandAPI.fail("You don't have enough exp to infuse that item!");
			}
		} else {
			if (region.equals(ItemRegion.KINGS_VALLEY)) {
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Gold into the 8 item frames!");
			} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Emeralds into the 8 item frames!");
			} else {
				CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			}
		}
	}

	public static void freeInfusion(CommandSender sender, Player player, InfusionSelection selection, int level) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (selection.equals(InfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		} else if (selection.equals(InfusionSelection.SPEC_REFUND)) {
			specialRefund(item, player);
			return;
		}

		List<String> newLore = new ArrayList<>();
		if (item.getLore() != null) {
			for (String line : item.getLore()) {
				if (!line.contains("PRE COST ADJUST")) {
					newLore.add(line);
				}
			}
			item.setLore(newLore);
			ItemUtils.setPlainLore(item);
		}


		if (calcInfuseCost(item) < 0) {
			CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			return;
		}

		if (item.getAmount() > 1) {
			CommandAPI.fail("Only one item can be infused at a time!");
			return;
		}

		String numeral = "";
		switch (level) {
			case 1:
				numeral = " I";
				break;
			case 2:
				numeral = " II";
				break;
			case 3:
				numeral = " III";
				break;
			case 4:
				numeral = " IV";
				break;

			default:
				CommandAPI.fail("Not a valid level!");
		}
		CommandUtils.enchantify(sender, player, ChatColor.stripColor(selection.getEnchantName()) + numeral);
	}

	/*
	 * Special Refunds for items that were infused prior to 4/2/2020 cost changes
	 * Items must be marked with 'PRE-UPDATE' lore text to be eligible.
	 * Running this command will grant the difference in pulsating materials to the player vs old costs
	 */
	private static void specialRefund(ItemStack item, Player player) throws WrapperCommandSyntaxException {
		//Remove the lore text marker from the item
		boolean isPreUpdate = false;
		List<String> newLore = new ArrayList<>();
		for (String line : item.getLore()) {
			if (!line.contains("PRE COST ADJUST")) {
				newLore.add(line);
			} else {
				isPreUpdate = true;
			}
		}
		item.setLore(newLore);
		ItemUtils.setPlainLore(item);

		if (isPreUpdate) {
			ItemRegion region = ItemUtils.getItemRegion(item);
			int refundMaterials = 0;

			//Determine old cost multiplier
			int oldMult = 0;
			switch (ItemUtils.getItemTier(item)) {
				case MEME:
				case UNCOMMON:
				case ENHANCED_UNCOMMON:
				case UNIQUE:
				case UNIQUE_EVENT:
				case RARE:
				case PATRON_MADE:
					oldMult = 1;
					break;
				case RELIC:
				case ARTIFACT:
				case ENHANCED_RARE:
					oldMult = 2;
					break;
				case EPIC:
					oldMult = 4;
					break;
				default:
					CommandAPI.fail("Invalid item tier. Only Uncommon and higher tiered items are able to be infused!");
			}

			//Calc old value
			int infuseLevel = getInfuseLevel(item) - 1;
			int oldValue = 0;
			while (infuseLevel >= 0) {
				oldValue += (oldMult * Math.pow(2, infuseLevel));
				infuseLevel--;
			}

			//Calc new value [first level free]
			infuseLevel = getInfuseLevel(item) - 2;
			int newValue = 0;
			while (infuseLevel >= 0) {
				newValue += (getCostMultiplier(item) * Math.pow(2, infuseLevel));
				infuseLevel--;
			}

			//Calc and give difference
			refundMaterials = oldValue - newValue;
			giveMaterials(player, region, refundMaterials);
		} else {
			CommandAPI.fail("This item does not have the 'PRE COST ADJUST' lore text so it is not eligible for a refund.");
		}
	}

	public static void refundInfusion(ItemStack item, Player player) throws WrapperCommandSyntaxException {
		ItemRegion region = ItemUtils.getItemRegion(item);
		int refundMaterials = 0;

		//Calculate refund amount
		// First level is free and we calculate based on the level below current.
		int infuseLevel = getInfuseLevel(item) - 2;
		int costMult = getCostMultiplier(item);
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
						ExperienceUtils.setTotalExperience(player, xp + (1395 / 2));
						break;
					case 2:
						ExperienceUtils.setTotalExperience(player, xp + ((1395 + 2920) / 2));
						break;
					case 3:
						ExperienceUtils.setTotalExperience(player, xp + ((1395 + 2920 + 5345) / 2));
						break;
					case 4:
						ExperienceUtils.setTotalExperience(player, xp + ((1395 + 2920 + 5345 + 8670) / 2));
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
						ExperienceUtils.setTotalExperience(player, xp + (2920 / 2));
						break;
					case 2:
						ExperienceUtils.setTotalExperience(player, xp + ((2920 + 5345) / 2));
						break;
					case 3:
						ExperienceUtils.setTotalExperience(player, xp + ((2920 + 5345 + 8670) / 2));
						break;
					case 4:
						ExperienceUtils.setTotalExperience(player, xp + ((2920 + 5345 + 8670 + 12895) / 2));
						break;
					default:
					case 0:
						break;
				}
				break;
			case EPIC:
				switch (level) {
					case 1:
						ExperienceUtils.setTotalExperience(player, xp + (5345 / 2));
						break;
					case 2:
						ExperienceUtils.setTotalExperience(player, xp + ((5345 + 8670) / 2));
						break;
					case 3:
						ExperienceUtils.setTotalExperience(player, xp + ((5345 + 8670 + 12895) / 2));
						break;
					case 4:
						ExperienceUtils.setTotalExperience(player, xp + ((5345 + 8670 + 12895 + 18020) / 2));
						break;
					default:
					case 0:
						break;
				}
				break;
			default:
				CommandAPI.fail("Invalid item. Item must be infused!");
		}
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
		int cost = getCostMultiplier(item);
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

	private static int getCostMultiplier(ItemStack item) throws WrapperCommandSyntaxException {
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
				CommandAPI.fail("Invalid item tier. Only Uncommon and higher tiered items are able to be infused!");
				return 99999999;
		}
	}

	private static int calcPaymentValue(List<ItemFrame> paymentFrames, ItemRegion region) {
		int payment = 0;
		for (ItemFrame iframe : paymentFrames) {
			ItemStack frameItem = iframe.getItem();
			if (frameItem == null || frameItem.getItemMeta() == null ||
					frameItem.getItemMeta().getDisplayName() == null) {
				continue;
			}
			if (region.equals(ItemRegion.KINGS_VALLEY)) {
				if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD)) {
					payment += 1;
				} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD_BAR)) {
					payment += 8;
				}
			} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
				if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD)) {
					payment += 1;
				} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD_BLOCK)) {
					payment += 8;
				}
			}
		}
		return payment;
	}

	private static int getExpInfuseCost(ItemStack item) throws WrapperCommandSyntaxException {
		int costMult = getCostMultiplier(item);
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

		//If item is not being refunded, check if cost adjust lore text exists, if so remove it to prevent abuse
		List<String> newLore = new ArrayList<>();
		for (String line : item.getLore()) {
			if (!line.contains("PRE COST ADJUST")) {
				newLore.add(line);
			}
		}
		item.setLore(newLore);
		ItemUtils.setPlainLore(item);

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
