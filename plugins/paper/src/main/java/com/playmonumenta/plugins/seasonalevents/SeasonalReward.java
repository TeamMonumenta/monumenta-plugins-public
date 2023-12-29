package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.plots.PlotBorderCustomInventory;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.seasonalevents.SeasonalPass.COSMETIC_COSTS;
import static com.playmonumenta.plugins.seasonalevents.SeasonalPass.ITEM_SKIN_KEY;
import static com.playmonumenta.plugins.seasonalevents.SeasonalPass.RELIC_WHEEL_KEY;
import static com.playmonumenta.plugins.seasonalevents.SeasonalPass.TREASURE_WHEEL_KEY;

public class SeasonalReward {

	public SeasonalRewardType mType;

	// String data- name of title, or parrot unlock, or ability skin name
	public @Nullable String mData;

	// Int data - for amounts such as number of loot spins
	public int mAmount = 1;
	// Cost to purchase after the pass is over; -1 cannot be purchased
	public int mCost = -1;

	// Fields for item display in GUI
	public @Nullable String mName;
	public @Nullable String mDescription;
	public @Nullable Material mDisplayItem;
	public @Nullable TextColor mNameColor;
	public @Nullable TextColor mDescriptionColor;
	public @Nullable ItemStack mLootTable;
	public @Nullable SeasonalReward mAltReward;

	public SeasonalReward(CommandSender sender,
	                      String startDateStr,
	                      Map<CosmeticType, Integer> rewardsSoFar,
	                      JsonElement rewardElement,
	                      boolean showWarnings) throws Exception {
		JsonObject toParse = rewardElement.getAsJsonObject();

		JsonElement altRewardElement = toParse.get("alt");
		if (altRewardElement != null) {
			mAltReward = new SeasonalReward(sender, startDateStr, rewardsSoFar, altRewardElement, showWarnings);
		}

		String rewardTypeStr = toParse.get("type").getAsString();
		SeasonalRewardType type = SeasonalRewardType.getRewardTypeSelection(rewardTypeStr);
		if (type == null) {
			throw new IgnoredEntryException("[SeasonPass] loadRewards for " + startDateStr
				+ ": No such reward type " + rewardTypeStr);
		}
		mType = type;

		if (toParse.get("data") != null) {
			mData = toParse.get("data").getAsString();
		}

		//Check if loot table is given
		if (toParse.get("loottable") != null
			|| type == SeasonalRewardType.LOOT_SPIN
			|| type == SeasonalRewardType.UNIQUE_SPIN
			|| type == SeasonalRewardType.ITEM_SKIN) {
			String lootTable = null;
			if (toParse.get("loottable") != null) {
				lootTable = toParse.get("loottable").getAsString();
			} else if (type == SeasonalRewardType.LOOT_SPIN) {
				lootTable = TREASURE_WHEEL_KEY;
			} else if (type == SeasonalRewardType.UNIQUE_SPIN) {
				lootTable = RELIC_WHEEL_KEY;
			} else if (type == SeasonalRewardType.ITEM_SKIN) {
				lootTable = ITEM_SKIN_KEY;
			}
			if (lootTable != null && !lootTable.isEmpty()) {
				LootContext context = new LootContext.Builder(Bukkit.getWorlds().get(0).getSpawnLocation()).build();
				LootTable rewardTable;
				try {
					rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTable));
				} catch (IllegalArgumentException ex) {
					rewardTable = null;
				}
				if (rewardTable == null) {
					throw new IgnoredEntryException("[SeasonPass] loadRewards for " + startDateStr
						+ ": No such loot table " + lootTable);
				} else {
					Iterator<ItemStack> loot = rewardTable.populateLoot(FastUtils.RANDOM, context).iterator();
					if (loot.hasNext()) {
						mLootTable = loot.next();
						return;
					}
				}
			}
		}

		if (toParse.get("name") != null) {
			mName = toParse.get("name").getAsString();
		}

		if (toParse.get("description") != null) {
			mDescription = toParse.get("description").getAsString();
		}

		if (toParse.get("amount") != null) {
			mAmount = toParse.get("amount").getAsInt();
		}
		if (toParse.get("displayitem") != null) {
			String displayItem = toParse.get("displayitem").getAsString();
			mDisplayItem = Material.getMaterial(displayItem);
			if (mDisplayItem == null) {
				mDisplayItem = Material.CHEST;
				if (showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": Invalid display item " + displayItem, NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
			}
		} else {
			mDisplayItem = Material.CHEST;
			if (showWarnings) {
				sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
						+ ": Display item not set", NamedTextColor.RED)
					.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
			}
		}
		if (toParse.get("namecolor") != null) {
			String nameColor = toParse.get("namecolor").getAsString();
			mNameColor = MessagingUtils.colorFromString(nameColor);
			if (mNameColor == null) {
				mNameColor = NamedTextColor.WHITE;
				if (showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": Invalid name color " + nameColor, NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
			}
		} else {
			mNameColor = NamedTextColor.WHITE;
		}
		if (toParse.get("descriptioncolor") != null) {
			String descriptionColor = toParse.get("descriptioncolor").getAsString();
			mDescriptionColor = MessagingUtils.colorFromString(descriptionColor);
			if (mDescriptionColor == null) {
				mDescriptionColor = NamedTextColor.GRAY;
				if (showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": Invalid description color " + descriptionColor, NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
			}
		} else {
			mDescriptionColor = NamedTextColor.GRAY;
		}

		CosmeticType cosmeticType;
		switch (mType) {
			case TITLE -> cosmeticType = CosmeticType.TITLE;
			case ELITE_FINISHER -> {
				if (showWarnings
					&& !EliteFinishers.getNameSet().contains(mData)) {
					sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": No such elite finisher " + mData, NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
				cosmeticType = CosmeticType.ELITE_FINISHER;
			}
			case PLOT_BORDER -> {
				if (showWarnings
					&& !PlotBorderCustomInventory.getCosmeticNameSet().contains(mData)) {
					sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
								+ ": No such plot border " + mData + " in the plot border GUI",
							NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
				cosmeticType = CosmeticType.PLOT_BORDER;
			}
			default -> cosmeticType = null;
		}
		if (cosmeticType != null) {
			int soFar = rewardsSoFar.merge(cosmeticType, 1, Integer::sum);
			int[] costs = COSMETIC_COSTS.get(cosmeticType);
			if (costs != null) {
				mCost = costs[Math.min(soFar - 1, costs.length - 1)];
			}
		}
	}

	public Component getDisplayItemName() {
		CosmeticType cosmeticType = switch (mType) {
			case TITLE -> CosmeticType.TITLE;
			case ELITE_FINISHER -> CosmeticType.ELITE_FINISHER;
			case PLOT_BORDER -> CosmeticType.PLOT_BORDER;
			default -> null;
		};

		if (mType == SeasonalRewardType.LOOT_TABLE
			&& mLootTable != null
			&& mLootTable.getItemMeta() instanceof SpawnEggMeta) {
			return mLootTable.displayName();
		} else if (mData != null
			&& cosmeticType != null) {
			String rewardName = mName;
			if (rewardName == null) {
				rewardName = "Name not set";
			}
			return Component.text(rewardName, mNameColor, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false);
		}

		if (mLootTable != null) {
			return mLootTable.displayName();
		} else {
			String rewardName = mName;
			if (rewardName == null) {
				rewardName = "Reward name not set";
			}
			return Component.text(rewardName, mNameColor, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false);
		}
	}

	public @Nullable SeasonalReward rewardToGive(Player p) {
		boolean giveAlt = false;
		switch (mType) {
			case ELITE_FINISHER -> giveAlt = CosmeticsManager.getInstance()
				.playerHasCosmetic(p, CosmeticType.ELITE_FINISHER, Objects.requireNonNull(mData));
			case TITLE -> giveAlt = CosmeticsManager.getInstance()
				.playerHasCosmetic(p, CosmeticType.TITLE, Objects.requireNonNull(mData));
			case PLOT_BORDER -> giveAlt = CosmeticsManager.getInstance()
				.playerHasCosmetic(p, CosmeticType.PLOT_BORDER, Objects.requireNonNull(mData));
			default -> {
			}
		}

		if (!giveAlt) {
			return this;
		} else if (mAltReward == null) {
			return null;
		} else {
			return mAltReward.rewardToGive(p);
		}
	}

	public void give(Player p) {
		int amount = mAmount;
		boolean giveAlt = false;
		switch (mType) {
			case ELITE_FINISHER -> {
				giveAlt = CosmeticsManager.getInstance()
					.playerHasCosmetic(p, CosmeticType.ELITE_FINISHER, Objects.requireNonNull(mData));
				if (!giveAlt) {
					CosmeticsManager.getInstance()
						.addCosmetic(p, CosmeticType.ELITE_FINISHER, Objects.requireNonNull(mData));
				}
			}
			case TITLE -> {
				giveAlt = CosmeticsManager.getInstance()
					.playerHasCosmetic(p, CosmeticType.TITLE, Objects.requireNonNull(mData));
				if (!giveAlt) {
					CosmeticsManager.getInstance()
						.addCosmetic(p, CosmeticType.TITLE, Objects.requireNonNull(mData));
				}
			}
			case PLOT_BORDER -> {
				giveAlt = CosmeticsManager.getInstance()
					.playerHasCosmetic(p, CosmeticType.PLOT_BORDER, Objects.requireNonNull(mData));
				if (!giveAlt) {
					CosmeticsManager.getInstance()
						.addCosmetic(p, CosmeticType.PLOT_BORDER, Objects.requireNonNull(mData));
				}
			}
			case ITEM_SKIN -> {
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, ITEM_SKIN_KEY);
				}
			}
			case LOOT_SPIN -> {
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, TREASURE_WHEEL_KEY);
				}
			}
			case UNIQUE_SPIN -> {
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, RELIC_WHEEL_KEY);
				}
			}
			case LOOT_TABLE -> {
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, Objects.requireNonNull(mData));
				}
			}
			case SHULKER_BOX -> {
				Material shulkerMaterial = mDisplayItem;
				if (shulkerMaterial == null) {
					shulkerMaterial = Material.PURPLE_SHULKER_BOX;
				}
				ItemStack shulker = new ItemStack(shulkerMaterial, 1);
				InventoryUtils.giveItem(p, shulker);
			}
			default -> {
			}
		}

		if (giveAlt && mAltReward != null) {
			mAltReward.give(p);
		}
	}

	/**
	 * Helper method to give player loot from a table
	 */
	private void givePlayerLootTable(Player p, String lootTablePath) {
		if (lootTablePath == null || lootTablePath.isEmpty()) {
			return;
		}
		LootContext context = new LootContext.Builder(p.getLocation()).build();
		LootTable rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTablePath));
		if (rewardTable != null) {
			Collection<ItemStack> loot = rewardTable.populateLoot(FastUtils.RANDOM, context);
			for (ItemStack item : loot) {
				InventoryUtils.giveItem(p, item);
			}
		}
	}

}
