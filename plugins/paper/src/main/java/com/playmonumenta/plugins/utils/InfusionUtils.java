package com.playmonumenta.plugins.utils;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.infusions.Acumen;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.enchantments.infusions.Perspicacity;
import com.playmonumenta.plugins.enchantments.infusions.Tenacity;
import com.playmonumenta.plugins.enchantments.infusions.Vigor;
import com.playmonumenta.plugins.enchantments.infusions.Vitality;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import io.github.jorelali.commandapi.api.CommandAPI;

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
		REFUND("refund", "refund");

		private final String mLabel;
		private final String mEnchantName;
		InfusionSelection(String label, String enchantName) {
			mLabel = label;
			mEnchantName = enchantName;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getEnchantName() {
			return mEnchantName;
		}
	}

	public static void doInfusion(CommandSender sender, Player player, ItemStack item, List<ItemFrame> paymentFrames, InfusionSelection selection) throws CommandSyntaxException {
		if (selection.equals(InfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		}
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
			if (ExperienceUtils.getTotalExperience(player) >= getExpInfuseCost(getCostMultiplier(item))) {
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

				int newXP = ExperienceUtils.getTotalExperience(player) - getExpInfuseCost(getCostMultiplier(item));
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
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Gold into the 4 item frames!");
			} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Emeralds into the 4 item frames!");
			} else {
				CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			}
		}
	}

	private static void refundInfusion(ItemStack item, Player player) throws CommandSyntaxException {
		ItemRegion region = ItemUtils.getItemRegion(item);
		int refundMaterials = 0;

		//Calculate refund amount
		int infuseLevel = getInfuseLevel(item) - 1;
		while (infuseLevel >= 0) {
			refundMaterials += (getCostMultiplier(item) * Math.pow(2, infuseLevel));
			infuseLevel--;
		}

		//Remove the infusion enchants from the item
		for (InfusionSelection sel : InfusionSelection.values()) {
			InventoryUtils.removeCustomEnchant(item, sel.getEnchantName());
		}
		NamespacedKey key;
		if (region.equals(ItemRegion.KINGS_VALLEY)) {
			key = new NamespacedKey("epic", "r1/items/currency/pulsating_gold");
		} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
			key = new NamespacedKey("epic", "r2/items/currency/pulsating_emerald");
		} else {
			CommandAPI.fail("Item must have a Region tag!");
			return;
		}
		LootTable lt = Bukkit.getLootTable(key);
		LootContext.Builder builder = new LootContext.Builder(player.getLocation());
		LootContext context = builder.build();
		Collection<ItemStack> items = lt.populateLoot(new Random(), context);
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

	private static int calcInfuseCost(ItemStack item) throws CommandSyntaxException {
		int infuseLvl = getInfuseLevel(item);
		int cost = getCostMultiplier(item);
		if (infuseLvl <= 3) {
			cost *= Math.pow(2, infuseLvl);
		} else {
			CommandAPI.fail("Items may only be infused 4 times!");
		}
		return cost;
	}

	private static int getInfuseLevel(ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, Acumen.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Focus.PROPERTY_NAME, true)
		 		+ InventoryUtils.getCustomEnchantLevel(item, Perspicacity.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Tenacity.PROPERTY_NAME, true)
				+ InventoryUtils.getCustomEnchantLevel(item, Vigor.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Vitality.PROPERTY_NAME, true);
	}

	private static int getCostMultiplier(ItemStack item) throws CommandSyntaxException {
		switch (ItemUtils.getItemTier(item)) {
			case UNCOMMON:
			case ENHANCED_UNCOMMON:
			case UNIQUE:
			case UNIQUE_EVENT:
			case RARE:
			case PATRON_MADE:
				return 1;
			case RELIC:
			case ARTIFACT:
			case ENHANCED_RARE:
				return 2;
			case EPIC:
				return 4;
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

	private static int getExpInfuseCost(int scoreMult) throws CommandSyntaxException {
		switch (scoreMult) {
			case 1:
				return 8670;
			case 2:
				return 18020;
			case 4:
				return 30970;
			default:
				CommandAPI.fail("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
				return 99999999;
		}
	}
}
