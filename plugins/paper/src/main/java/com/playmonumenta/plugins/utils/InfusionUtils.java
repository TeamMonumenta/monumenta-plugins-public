package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class InfusionUtils {

	/**
	 * When set to true the refund function will return all the XP used for the infusion, when false only the 75%
	 */
	public static final boolean FULL_REFUND = false;
	public static final double REFUND_PERCENT = 0.75;
	public static final String PULSATING_GOLD = "epic:r1/items/currency/pulsating_gold";
	public static final String PULSATING_EMERALD = "epic:r2/items/currency/pulsating_emerald";
	public static final String PULSATING_DIAMOND = "epic:r3/items/currency/pulsating_diamond";

	public enum InfusionSelection {
		ACUMEN("acumen", InfusionType.ACUMEN),
		FOCUS("focus", InfusionType.FOCUS),
		PERSPICACITY("perspicacity", InfusionType.PERSPICACITY),
		TENACITY("tenacity", InfusionType.TENACITY),
		VIGOR("vigor", InfusionType.VIGOR),
		VITALITY("vitality", InfusionType.VITALITY),
		REFUND("refund", null),
		SPEC_REFUND("special", null);

		private final String mLabel;
		private final @Nullable InfusionType mInfusionType;

		InfusionSelection(String label, @Nullable InfusionType infusionType) {
			mLabel = label;
			mInfusionType = infusionType;
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

		public @Nullable InfusionType getInfusionType() {
			return mInfusionType;
		}
	}

	public static void refundInfusion(ItemStack item, Player player, Plugin plugin) throws WrapperCommandSyntaxException {
		Region region = ItemStatUtils.getRegion(item);
		int refundMaterials = 0;

		//Calculate refund amount
		// First level is free and we calculate based on the level below current.
		int infuseLevel = getInfuseLevel(item) - 1;
		int costMult = getCostMultiplierWithCheck(item);
		while (infuseLevel > 0) {
			refundMaterials += (costMult * (int) Math.pow(2, infuseLevel - 1)) * item.getAmount();
			infuseLevel--;
		}

		int level = getInfuseLevel(item);

		AuditListener.log("Infusion refund - player=" + player.getName() + " item='" + ItemUtils.getPlainName(item) + "' level=" + level + "' stack size=" + item.getAmount());

		//Remove the infusion enchants from the item
		for (InfusionSelection sel : InfusionSelection.values()) {
			InfusionType infusionType = sel.getInfusionType();
			if (infusionType != null) {
				ItemStatUtils.removeInfusion(item, infusionType, false);
			}
		}
		ItemStatUtils.generateItemStats(item);
		if (refundMaterials > 0 && region != null) {
			giveMaterials(player, region, refundMaterials);
		}

		int xp = ExperienceUtils.getTotalExperience(player);
		int refundXP = 0;

		if (region == Region.VALLEY || region == Region.ISLES) {
			switch (ItemStatUtils.getTier(item)) {
				case UNCOMMON:
				case UNIQUE:
				case EVENT:
				case RARE:
				case PATRON:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_30;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						default:
						case 0:
							break;
					}
					break;
				case ARTIFACT:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_40;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
							break;
						default:
						case 0:
							break;
					}
					break;
				case EPIC:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_50;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70 + ExperienceUtils.LEVEL_80;
							break;
						default:
						case 0:
							break;
					}
					break;
				default:
					CommandAPI.fail("Invalid item. Item must be infused!");
			}
		} else if (region == Region.RING) {
			// All Ring items has same infusion price, Artifact level.
			switch (level) {
				case 1:
					refundXP = ExperienceUtils.LEVEL_40;
					break;
				case 2:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
					break;
				case 3:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
					break;
				case 4:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
					break;
				default:
				case 0:
					break;
			}
		}

		refundXP = (int) (refundXP * (FULL_REFUND ? 1 : REFUND_PERCENT) * item.getAmount());
		ExperienceUtils.setTotalExperience(player, xp + refundXP);
	}

	private static void giveMaterials(Player player, Region region, int refundMaterials) throws WrapperCommandSyntaxException {
		ItemStack stack;
		if (region.equals(Region.VALLEY)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_GOLD));
		} else if (region.equals(Region.ISLES)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_EMERALD));
		} else if (region.equals(Region.RING)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_DIAMOND));
		} else {
			CommandAPI.fail("Item must have a Region tag!");
			return;
		}
		if (stack != null) {
			stack.setAmount(refundMaterials);
			InventoryUtils.giveItem(player, stack);
			return;
		}
		CommandAPI.fail("ERROR while refunding infusion (failed to get loot table). Please contact a moderator if you see this message!");
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

	public static int calcInfuseCost(Plugin plugin, Player player, ItemStack item) throws WrapperCommandSyntaxException {
		// First level is free
		int infuseLvl = getInfuseLevel(item);
		int cost = getCostMultiplierWithCheck(item);
		// Special case for first level
		if (infuseLvl == 0) {
			cost = 0;
		} else if (infuseLvl <= 3) {
			cost *= (int)Math.pow(2, infuseLvl - 1);
		} else {
			cost = 99999999;
			CommandAPI.fail("Items may only be infused 4 times!");
		}
		return cost;
	}

	public static int getInfuseLevel(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.ACUMEN) + ItemStatUtils.getInfusionLevel(item, InfusionType.FOCUS)
			+ ItemStatUtils.getInfusionLevel(item, InfusionType.PERSPICACITY) + ItemStatUtils.getInfusionLevel(item, InfusionType.TENACITY)
			+ ItemStatUtils.getInfusionLevel(item, InfusionType.VIGOR) + ItemStatUtils.getInfusionLevel(item, InfusionType.VITALITY);
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
		if (ItemStatUtils.getRegion(item) == Region.RING) {
			return 3;
		}

		switch (ItemStatUtils.getTier(item)) {
			case UNCOMMON:
			case UNIQUE:
			case EVENT:
			case RARE:
			case PATRON:
				return 2;
			case ARTIFACT:
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
						return ExperienceUtils.LEVEL_30;
					case 1:
					// Infuse Level 1 Rare
						return ExperienceUtils.LEVEL_40;
					case 2:
					// Infuse Level 2 Rare
						return ExperienceUtils.LEVEL_50;
					case 3:
					// Infuse Level 3 Rare
						return ExperienceUtils.LEVEL_60;
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
						return ExperienceUtils.LEVEL_40;
					case 1:
					// Infuse Level 1 Artifact
						return ExperienceUtils.LEVEL_50;
					case 2:
					// Infuse Level 2 Artifact
						return ExperienceUtils.LEVEL_60;
					case 3:
					// Infuse Level 3 Artifact
						return ExperienceUtils.LEVEL_70;
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
						return ExperienceUtils.LEVEL_50;
					case 1:
					// Infuse Level 1 Epic
						return ExperienceUtils.LEVEL_60;
					case 2:
					// Infuse Level 2 Epic
						return ExperienceUtils.LEVEL_70;
					case 3:
					// Infuse Level 3 Epic
						return ExperienceUtils.LEVEL_80;
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

	public static InfusionSelection getCurrentInfusion(Plugin plugin, Player player, ItemStack item) {

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.ACUMEN) > 0) {
			return InfusionSelection.ACUMEN;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.FOCUS) > 0) {
			return InfusionSelection.FOCUS;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.PERSPICACITY) > 0) {
			return InfusionSelection.PERSPICACITY;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.TENACITY) > 0) {
			return InfusionSelection.TENACITY;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.VIGOR) > 0) {
			return InfusionSelection.VIGOR;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.VITALITY) > 0) {
			return InfusionSelection.VITALITY;
		}

		return InfusionSelection.REFUND;
	}

	public static boolean infuseItem(Plugin plugin, Player player, ItemStack item, InfusionSelection selection) {
		if (!getCurrentInfusion(plugin, player, item).equals(selection) && getInfuseLevel(item) > 0) {
			return false;
		}

		InfusionType infusionType = selection.getInfusionType();
		if (infusionType == null) {
			return false;
		}

		int prevLvl = ItemStatUtils.getInfusionLevel(item, infusionType);
		ItemStatUtils.addInfusion(item, infusionType, prevLvl + 1, player.getUniqueId());

		return true;
	}


	public static boolean isInfusionable(ItemStack item) {
		if (item == null) {
			return false;
		}

		if (item.getAmount() != 1) {
			return false;
		}

		Region region = ItemStatUtils.getRegion(item);
		if (region != Region.VALLEY && region != Region.ISLES && region != Region.RING) {
			return false;
		}

		switch (ItemStatUtils.getTier(item)) {
			case UNCOMMON:
			case UNIQUE:
			case EVENT:
			case RARE:
			case PATRON:
			case ARTIFACT:
			case EPIC:
			case LEGENDARY:
				break;
			default:
				return false;
		}

		return true;
	}

	public static int getExpLvlInfuseCost(Plugin plugin, Player player, ItemStack item) {
		int exp;
		try {
			exp = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return -1;
		}

		switch (exp) {
			case ExperienceUtils.LEVEL_30:
				return 30;
			case ExperienceUtils.LEVEL_40:
				return 40;
			case ExperienceUtils.LEVEL_50:
				return 50;
			case ExperienceUtils.LEVEL_60:
				return 60;
			case ExperienceUtils.LEVEL_70:
				return 70;
			case ExperienceUtils.LEVEL_80:
				return 80;
			default:
				return 0;
		}
	}

	public static boolean canPayExp(Plugin plugin, Player player, ItemStack item) {
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

	public static boolean canPayPulsating(Plugin plugin, Player player, ItemStack item) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		ItemStack currency = null;

		if (ItemStatUtils.getRegion(item) == Region.RING) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_DIAMOND));
		}

		if (ItemStatUtils.getRegion(item) == Region.ISLES) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_EMERALD));
		}

		if (ItemStatUtils.getRegion(item) == Region.VALLEY) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_GOLD));
		}

		if (currency == null) {
			//something went wrong
			return false;
		}

		int amount;

		try {
			amount = calcInfuseCost(plugin, player, item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		currency.setAmount(amount);

		if (!player.getInventory().containsAtLeast(currency, amount)) {
			return false;
		}

		return true;
	}

	public static boolean canPayInfusion(Plugin plugin, Player player, ItemStack item) {
		return canPayExp(plugin, player, item) && canPayPulsating(plugin, player, item);
	}

	public static boolean payInfusion(Plugin plugin, Player player, ItemStack item) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			Plugin.getInstance().getLogger().warning("[Infusion] Player: " + player.getName() + " infused an item while be on creative mode!");
			return true;
		}

		//currency
		ItemStack currency = null;
		if (ItemStatUtils.getRegion(item) == Region.RING) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_DIAMOND));
		} else if (ItemStatUtils.getRegion(item) == Region.ISLES) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_EMERALD));
		} else if (ItemStatUtils.getRegion(item) == Region.VALLEY) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_GOLD));
		}

		if (currency == null) {
			//something went wrong
			return false;
		}

		int amount;
		try {
			amount = calcInfuseCost(plugin, player, item);
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
