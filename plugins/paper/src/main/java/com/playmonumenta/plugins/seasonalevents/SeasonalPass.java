package com.playmonumenta.plugins.seasonalevents;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.plots.PlotBorderCustomInventory;
import com.playmonumenta.plugins.seasonalevents.PlayerProgress.PassProgress;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

public class SeasonalPass {
	// cost of rewards, in Metamorphosis Tokens
	private static final int[] DUMMY_COSTS = {2, 3};
	private static final ImmutableMap<CosmeticType, int[]> COSMETIC_COSTS = ImmutableMap.of(
		CosmeticType.TITLE, new int[] {1, 1, 2, 2, 3, 3},
		CosmeticType.ELITE_FINISHER, new int[] {2, 3, 4},
		CosmeticType.PLOT_BORDER, new int[] {5}
	);

	public static final int MP_PER_LEVEL = 75;
	// Loot tables for rewards
	public static final String ITEM_SKIN_KEY = "epic:pass/metamorphosis_token";
	public static final String TREASURE_WHEEL_KEY = "epic:pass/treasure_wheel_token";
	public static final String RELIC_WHEEL_KEY = "epic:pass/relic_wheel_token";
	// Missions and rewards arrays
	public final LocalDateTime mPassStart;
	public String mName;
	public Material mDisplayItem;
	public TextColor mNameColor;
	public int mNumberOfWeeks = 0;
	public int mTotalMp = 0;
	public final Map<Integer, List<WeeklyMission>> mMissions = new HashMap<>();
	public final List<SeasonalReward> mRewards = new ArrayList<>();

	/**
	 * Loads a season pass for a given week from its two json files
	 *
	 * @param missionsJson The contents of a missions json file for this pass
	 * @param rewardsJson  The contents of a rewards json file for this pass
	 */
	public SeasonalPass(final CommandSender sender, LocalDateTime passStart, JsonObject missionsJson, JsonObject rewardsJson, boolean showWarnings) {
		mPassStart = passStart;
		loadRewards(sender, rewardsJson, showWarnings);
		loadMissions(sender, missionsJson, showWarnings);
	}

	/**
	 * Load reward objects from json parsing
	 */
	private void loadRewards(final CommandSender sender, JsonObject data, boolean showWarnings) {
		int dummiesSoFar = 0;
		Map<CosmeticType, Integer> rewardsSoFar = new HashMap<>();

		String startDateStr = data.get("start_date").getAsString();
		JsonArray rewardParse = data.get("rewards").getAsJsonArray();
		for (JsonElement missionElement : rewardParse) {
			try {

				JsonObject toParse = missionElement.getAsJsonObject();

				String rewardTypeStr = toParse.get("type").getAsString();
				SeasonalRewardType type = SeasonalRewardType.getRewardTypeSelection(rewardTypeStr);
				if (type == null) {
					if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": No such reward type " + rewardTypeStr, NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
					continue;
				}
				SeasonalReward reward = new SeasonalReward(type);

				if (toParse.get("data") != null) {
					reward.mData = toParse.get("data").getAsString();
				}

				//Check if loot table is given
				if (toParse.get("loottable") != null) {
					String lootTable = toParse.get("loottable").getAsString();
					if (lootTable != null && lootTable.length() > 0) {
						LootContext context = new LootContext.Builder(Bukkit.getWorlds().get(0).getSpawnLocation()).build();
						LootTable rewardTable;
						try {
							rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTable));
						} catch (IllegalArgumentException ex) {
							rewardTable = null;
						}
						if (rewardTable == null) {
							if (showWarnings) {
								sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
									+ ": No such loot table " + lootTable, NamedTextColor.RED)
									.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
							}
						} else {
							Iterator<ItemStack> loot = rewardTable.populateLoot(FastUtils.RANDOM, context).iterator();
							if (loot.hasNext()) {
								reward.mLootTable = loot.next();
								if (SeasonalRewardType.LOOT_TABLE.equals(reward.mType)
									&& reward.mLootTable != null
									&& reward.mLootTable.getItemMeta() instanceof SpawnEggMeta) {
									reward.mCost = DUMMY_COSTS[Math.min(dummiesSoFar, DUMMY_COSTS.length - 1)];
									dummiesSoFar++;
								}
								mRewards.add(reward);
								continue;
							}
						}
					}
				}

				if (toParse.get("name") != null) {
					reward.mName = toParse.get("name").getAsString();
				}

				if (toParse.get("description") != null) {
					reward.mDescription = toParse.get("description").getAsString();
				}

				if (toParse.get("amount") != null) {
					reward.mAmount = toParse.get("amount").getAsInt();
				}
				if (toParse.get("displayitem") != null) {
					String displayItem = toParse.get("displayitem").getAsString();
					reward.mDisplayItem = Material.getMaterial(displayItem);
					if (reward.mDisplayItem == null) {
						reward.mDisplayItem = Material.CHEST;
						if (showWarnings) {
							sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
								+ ": Invalid display item " + displayItem, NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
					}
				} else {
					reward.mDisplayItem = Material.CHEST;
					if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
							+ ": Display item not set", NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
				}
				if (toParse.get("namecolor") != null) {
					String nameColor = toParse.get("namecolor").getAsString();
					reward.mNameColor = MessagingUtils.colorFromString(nameColor);
					if (reward.mNameColor == null) {
						reward.mNameColor = NamedTextColor.WHITE;
						if (showWarnings) {
							sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
								+ ": Invalid name color " + nameColor, NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
					}
				} else {
					reward.mNameColor = NamedTextColor.WHITE;
				}
				if (toParse.get("descriptioncolor") != null) {
					String descriptionColor = toParse.get("descriptioncolor").getAsString();
					reward.mDescriptionColor = MessagingUtils.colorFromString(descriptionColor);
					if (reward.mDescriptionColor == null) {
						reward.mDescriptionColor = NamedTextColor.GRAY;
						if (showWarnings) {
							sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
								+ ": Invalid description color " + descriptionColor, NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
					}
				} else {
					reward.mDescriptionColor = NamedTextColor.GRAY;
				}

				CosmeticType cosmeticType;
				switch (reward.mType) {
					case TITLE -> cosmeticType = CosmeticType.TITLE;
					case ELITE_FINISHER -> {
						if (showWarnings
							&& !EliteFinishers.getNameSet().contains(reward.mData)) {
							sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
									+ ": No such elite finisher " + reward.mData, NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
						cosmeticType = CosmeticType.ELITE_FINISHER;
					}
					case PLOT_BORDER -> {
						if (showWarnings
							&& !PlotBorderCustomInventory.getCosmeticNameSet().contains(reward.mData)) {
							sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr
										+ ": No such plot border " + reward.mData + " in the plot border GUI",
									NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
						cosmeticType = CosmeticType.PLOT_BORDER;
					}
					default -> cosmeticType = null;
				}
				if (cosmeticType != null) {
					int soFar = rewardsSoFar.merge(cosmeticType, 1, Integer::sum);
					int[] costs = COSMETIC_COSTS.get(cosmeticType);
					if (costs != null) {
						reward.mCost = costs[Math.min(soFar - 1, costs.length - 1)];
					}
				}

				mRewards.add(reward);
			} catch (Exception e) {
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
	}

	/**
	 * Load mission objects from json parsing
	 */
	private void loadMissions(final CommandSender sender, JsonObject data, boolean showWarnings) {
		int numberOfWeeks = 0;
		String startDateStr = data.get("start_date").getAsString();
		mName = data.get("pass_name").getAsString();
		mDisplayItem = Material.getMaterial(data.get("pass_displayitem").getAsString());
		String nameColorStr = data.get("pass_namecolor").getAsString();
		TextColor nameColor = MessagingUtils.colorFromString(nameColorStr);
		if (nameColor == null) {
			nameColor = NamedTextColor.GOLD;
			if (showWarnings) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ mName + ": No such name color " + nameColorStr, NamedTextColor.RED));
			}
		}
		mNameColor = nameColor;
		JsonArray missionParse = data.get("missions").getAsJsonArray();
		for (JsonElement missionElement : missionParse) {
			try {

				JsonObject toParse = missionElement.getAsJsonObject();

				WeeklyMission mission = new WeeklyMission();
				// Required fields
				String missionTypeStr = toParse.get("type").getAsString();
				mission.mType = WeeklyMissionType.getMissionTypeSelection(missionTypeStr);
				if (mission.mType == null && showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
						+ mName + ": No such mission type " + missionTypeStr, NamedTextColor.RED)
						.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
				}
				mission.mWeek = toParse.get("week").getAsInt();
				if (mission.mWeek <= 0 && showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
							+ mName + ": Mission week is <= 0: " + mission.mWeek, NamedTextColor.RED)
						.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
				}
				mission.mMP = toParse.get("mp").getAsInt();
				if (mission.mMP <= 0 && showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
							+ mName + ": Mission MP is <= 0: " + mission.mMP, NamedTextColor.RED)
						.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
				}
				mission.mAmount = toParse.get("amount").getAsInt();
				if (mission.mAmount <= 0 && showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
							+ mName + ": Mission Amount is <= 0: " + mission.mAmount, NamedTextColor.RED)
						.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
				}
				mission.mDescription = toParse.get("description").getAsString();

				// Optional fields

				if (toParse.get("content") != null) {
					JsonArray content = toParse.get("content").getAsJsonArray();
					List<MonumentaContent> contentList = new ArrayList<>();
					for (JsonElement con : content) {
						String contentStr = con.getAsString();
						MonumentaContent monumentaContent = MonumentaContent.getContentSelection(contentStr);
						if (monumentaContent == null) {
							if (showWarnings) {
								sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
									+ " " + mName + ": No such content " + contentStr, NamedTextColor.RED)
									.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
							}
							continue;
						}
						contentList.add(monumentaContent);
					}
					mission.mContent = contentList;
				}
				if (toParse.get("region") != null) {
					mission.mRegion = toParse.get("region").getAsInt();
					if (showWarnings && !MonumentaContent.ALL_CONTENT_REGION_INDEXES.contains(mission.mRegion)) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + mName + ": Region not used in Monumenta content: " + mission.mRegion,
								NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
				}
				if (toParse.get("delvepoints") != null) {
					mission.mDelvePoints = toParse.get("delvepoints").getAsInt();
				}
				if (toParse.get("modifierrank") != null) {
					mission.mModifierRank = toParse.get("modifierrank").getAsInt();
				}
				if (toParse.get("rotatingamount") != null) {
					mission.mRotatingModifiersAmount = toParse.get("rotatingamount").getAsInt();
				}
				if (toParse.get("delvemodifier") != null) {
					// This compares using integers - in json need to list the number of the modifier, not the name!
					if (toParse.get("delvemodifier") instanceof JsonArray mods) {
						List<DelvesModifier> modList = new ArrayList<>();
						for (JsonElement mod : mods) {
							JsonPrimitive modPrimitive = mod.getAsJsonPrimitive();
							if (modPrimitive.isString()) {
								String modName = modPrimitive.getAsString();
								DelvesModifier modifier = DelvesModifier.fromName(modName);
								if (modifier != null) {
									modList.add(modifier);
								} else if (showWarnings) {
									sender.sendMessage(Component.text("[SeasonPass] loadMissions for "
										+ startDateStr + " " + mName + ": No such modifier " + modName,
										NamedTextColor.RED)
										.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
								}
							} else if (showWarnings) {
								sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
									+ " " + mName + ": Modifier ID is not string " + mod, NamedTextColor.RED)
									.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
							}
						}
						mission.mDelveModifiers = modList;
					} else {
						JsonPrimitive modPrimitive = toParse.get("delvemodifier").getAsJsonPrimitive();
						if (modPrimitive.isString()) {
							String modName = modPrimitive.getAsString();
							DelvesModifier modifier = DelvesModifier.fromName(modName);
							if (modifier != null) {
								List<DelvesModifier> modList = new ArrayList<>();
								modList.add(modifier);
								mission.mDelveModifiers = modList;
							} else if (showWarnings) {
								sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
									+ " " + mName + ": No such modifier " + modName, NamedTextColor.RED)
									.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
							}
						} else if (showWarnings) {
							sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + mName + ": Modifier ID is not string " + modPrimitive, NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
					}
				}
				mTotalMp += mission.mMP;
				if (mission.mWeek > numberOfWeeks) {
					numberOfWeeks = mission.mWeek;
				}
				mMissions.computeIfAbsent(mission.mWeek, k -> new ArrayList<>()).add(mission);
			} catch (Exception e) {
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
		mNumberOfWeeks = numberOfWeeks;
	}

	/**
	 * Claim the rewards previously earned for the current pass
	 */
	public void claimMP(Player p) {
		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			return;
		}
		PassProgress passProgress = playerProgress.getPassProgress(this);
		if (passProgress == null) {
			return;
		}

		//Check if they earned any new rewards from this
		int claimedMP = passProgress.getClaimedPoints();
		int newMP = passProgress.getMissionPoints();
		int claimedLevel = getLevelFromMP(claimedMP);
		int newLevel = getLevelFromMP(newMP);
		//Give any unclaimed rewards
		for (int i = claimedLevel + 1; i <= newLevel; i++) {
			p.sendMessage(Component.text("You earned a new reward!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));

			EntityUtils.fireworkAnimation(p);
			givePassReward(p, i);
		}

		// Special case - finished all missions
		if (newMP == mTotalMp && !CosmeticsManager.getInstance().playerHasCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME)) {
			p.sendMessage(Component.text("You finished all the missions!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			EntityUtils.fireworkAnimation(p);
			CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME);
		}

		// Update claimed MP (unless this would allow re-claiming rewards)
		if (newMP > claimedMP) {
			passProgress.setClaimedPoints(newMP);
		}
	}

	/**
	 * Note that this can give weeks outside the range of the season pass
	 *
	 * @return week number within or beyond a season pass
	 */
	public int getWeekOfPass() {
		LocalDateTime currentTime = DateUtils.localDateTime();
		return getWeekOfPass(currentTime);
	}

	public int getWeekOfPass(LocalDateTime localDateTime) {
		return (int) (DateUtils.getWeeklyVersion(localDateTime) - DateUtils.getWeeklyVersion(mPassStart) + 1);
	}

	/**
	 * Returns true if this is the current season pass, else false.
	 */
	public boolean isActive() {
		int weekNumber = getWeekOfPass();
		return 1 <= weekNumber && weekNumber <= mNumberOfWeeks;
	}

	public boolean isActive(LocalDateTime localDateTime) {
		int weekNumber = getWeekOfPass(localDateTime);
		return 1 <= weekNumber && weekNumber <= mNumberOfWeeks;
	}

	/**
	 * Returns all missions for the current week number of the pass
	 */
	public List<WeeklyMission> getActiveMissions() {
		return getMissionsInWeek(getWeekOfPass());
	}

	/**
	 * Method to get missions for given week
	 */
	public List<WeeklyMission> getMissionsInWeek(int week) {
		return mMissions.getOrDefault(week, new ArrayList<>());
	}

	/**
	 * Gives out reward for specific level depending
	 * on its type implementation
	 */
	public void givePassReward(Player p, int level) {
		SeasonalReward reward = mRewards.get(level - 1);
		int amount = 1;
		if (reward.mAmount != 0) {
			amount = reward.mAmount;
		}
		switch (reward.mType) {
			case ELITE_FINISHER ->
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.ELITE_FINISHER, Objects.requireNonNull(reward.mData));
			case TITLE ->
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, Objects.requireNonNull(reward.mData));
			case PLOT_BORDER ->
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.PLOT_BORDER, Objects.requireNonNull(reward.mData));
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
					givePlayerLootTable(p, Objects.requireNonNull(reward.mData));
				}
			}
			case SHULKER_BOX -> {
				Material shulkerMaterial = reward.mDisplayItem;
				if (shulkerMaterial == null) {
					shulkerMaterial = Material.PURPLE_SHULKER_BOX;
				}
				ItemStack shulker = new ItemStack(shulkerMaterial, 1);
				InventoryUtils.giveItem(p, shulker);
			}
			default -> {
			}
		}
	}

	/**
	 * Helper method to give player loot from a table
	 */
	private void givePlayerLootTable(Player p, String lootTablePath) {
		if (lootTablePath == null || lootTablePath.length() == 0) {
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

	public int getMP(Player p) {
		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			return 0;
		}
		PassProgress passProgress = playerProgress.getPassProgress(this);
		if (passProgress == null) {
			return 0;
		}
		return passProgress.getMissionPoints();
	}

	public int getLevelFromMP(int mp) {
		return Math.min(mRewards.size(), mp / MP_PER_LEVEL);
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public void addWeeklyMissionProgress(Player p, int missionNumber, int amount) {
		LocalDateTime now = DateUtils.localDateTime();
		int missionIndex = missionNumber - 1;
		int week = getWeekOfPass();
		int numWeeklyMissions = mMissions.getOrDefault(week, List.of()).size();
		if (missionIndex < 0 || missionIndex >= numWeeklyMissions) {
			return;
		}

		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			MMLog.warning("No season pass player progress found for " + p.getName());
			return;
		}
		int missionProgress = playerProgress.getPassMissionProgress(now, missionIndex).orElse(0);
		if (missionProgress < 0) {
			// Mission already completed
			return;
		}
		missionProgress = playerProgress.addPassMissionProgress(now, missionIndex, amount);
		if (missionProgress < 0) {
			//Play an animation and notify player
			p.sendMessage(Component.text(
					"You completed a weekly mission! Open the Seasonal Pass menu to claim your progress!",
					NamedTextColor.GOLD,
					TextDecoration.BOLD));
			EntityUtils.fireworkAnimation(p);
		}
	}

	// Time Utils

	public int getDaysUntilMissionEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.getDaysLeftInWeeklyVersion() - 1;
	}

	public int getHoursUntilMissionEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewWeek(ChronoUnit.HOURS) % 24;
	}

	public int getDaysUntilPassEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewWeek(ChronoUnit.DAYS) + 7 * (mNumberOfWeeks - getWeekOfPass());
	}

	public int getHoursUntilPassEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewDay(ChronoUnit.HOURS);
	}

	@Override
	public String toString() {
		return mPassStart.toLocalDate().toString() + ": " + mName;
	}
}
