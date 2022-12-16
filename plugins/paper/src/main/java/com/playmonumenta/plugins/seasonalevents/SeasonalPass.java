package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;

public class SeasonalPass {
	public static final int MP_PER_LEVEL = 75;
	public static final String PASS_MP_SCOREBOARD = "SeasonalEventMP";
	// Loot tables for rewards
	public static final String ITEM_SKIN_KEY = "epic:pass/metamorphosis_token";
	public static final String TREASURE_WHEEL_KEY = "epic:pass/treasure_wheel_token";
	public static final String RELIC_WHEEL_KEY = "epic:pass/relic_wheel_token";
	// Missions and rewards arrays
	public final LocalDateTime mPassStart;
	public String mName;
	public Material mDisplayItem;
	public NamedTextColor mNameColor;
	public int mNumberOfWeeks = 0;
	public int mTotalMp = 0;
	public final List<WeeklyMission> mMissions = new ArrayList<>();
	public final List<SeasonalReward> mRewards = new ArrayList<>();

	/**
	 * Loads a season pass for a given week from its two json files
	 *
	 * @param missionsJson The contents of a missions json file for this pass
	 * @param rewardsJson  The contents of a rewards json file for this pass
	 * @throws Exception if the season pass has an invalid file
	 */
	public SeasonalPass(String startDateString, JsonObject missionsJson, JsonObject rewardsJson) throws Exception {
		mPassStart = LocalDateTime.parse(startDateString + "T00:00:00");
		loadRewards(rewardsJson);
		loadMissions(missionsJson);
	}

	/**
	 * Load reward objects from json parsing
	 */
	private void loadRewards(JsonObject data) throws Exception {
		JsonArray rewardParse = data.get("rewards").getAsJsonArray();
		for (JsonElement missionElement : rewardParse) {
			try {

				JsonObject toParse = missionElement.getAsJsonObject();

				SeasonalReward reward = new SeasonalReward();
				reward.mType = SeasonalRewardType.getRewardTypeSelection(toParse.get("type").getAsString());

				if (toParse.get("data") != null) {
					reward.mData = toParse.get("data").getAsString();
				}

				//Check if loot table is given
				if (toParse.get("loottable") != null) {
					String lootTable = toParse.get("loottable").getAsString();
					if (lootTable != null && lootTable.length() > 0) {
						LootContext context = new LootContext.Builder(Bukkit.getWorlds().get(0).getSpawnLocation()).build();
						LootTable rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTable));
						if (rewardTable != null) {
							Collection<ItemStack> loot = rewardTable.populateLoot(FastUtils.RANDOM, context);
							if (loot != null && loot.size() > 0) {
								for (ItemStack item : loot) {
									reward.mLootTable = item;
									mRewards.add(reward);
								}
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
					reward.mDisplayItem = Material.getMaterial(toParse.get("displayitem").getAsString());
				} else {
					reward.mDisplayItem = Material.CHEST;
				}
				if (toParse.get("namecolor") != null) {
					reward.mNameColor = NamedTextColor.NAMES.value(toParse.get("namecolor").getAsString());
				} else {
					reward.mNameColor = NamedTextColor.WHITE;
				}
				if (toParse.get("descriptioncolor") != null) {
					reward.mDescriptionColor = NamedTextColor.NAMES.value(toParse.get("descriptioncolor").getAsString());
				} else {
					reward.mDescriptionColor = NamedTextColor.GRAY;
				}

				mRewards.add(reward);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Load mission objects from json parsing
	 */
	private void loadMissions(JsonObject data) throws Exception {
		int numberOfWeeks = 0;
		String startDateStr = data.get("start_date").getAsString();
		mName = data.get("pass_name").getAsString();
		mDisplayItem = Material.getMaterial(data.get("pass_displayitem").getAsString());
		mNameColor = NamedTextColor.NAMES.value(data.get("pass_namecolor").getAsString());
		JsonArray missionParse = data.get("missions").getAsJsonArray();
		for (JsonElement missionElement : missionParse) {
			try {

				JsonObject toParse = missionElement.getAsJsonObject();

				WeeklyMission mission = new WeeklyMission();
				// Required fields
				mission.mType = WeeklyMissionType.getMissionTypeSelection(toParse.get("type").getAsString());
				mission.mWeek = toParse.get("week").getAsInt();
				mission.mMP = toParse.get("mp").getAsInt();
				mission.mAmount = toParse.get("amount").getAsInt();
				mission.mDescription = toParse.get("description").getAsString();

				// Optional fields

				if (toParse.get("content") != null) {
					JsonArray content = toParse.get("content").getAsJsonArray();
					List<MonumentaContent> contentList = new ArrayList<>();
					for (JsonElement con : content) {
						contentList.add(MonumentaContent.getContentSelection(con.getAsString()));
					}
					mission.mContent = contentList;
				}
				if (toParse.get("region") != null) {
					mission.mRegion = toParse.get("region").getAsInt();
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
								} else {
									MMLog.warning("[SeasonPass] loadMissions for " + startDateStr + " " + mName + ": No such modifier " + modName);
								}
							} else {
								MMLog.warning("[SeasonPass] loadMissions for " + startDateStr + " " + mName + ": Modifier ID is not string " + mod);
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
							} else {
								MMLog.warning("[SeasonPass] loadMissions for " + startDateStr + " " + mName + ": No such modifier " + modName);
							}
						} else {
							MMLog.warning("[SeasonPass] loadMissions for " + startDateStr + " " + mName + ": Modifier ID is not string " + modPrimitive);
						}
					}
				}
				mTotalMp += mission.mMP;
				if (mission.mWeek > numberOfWeeks) {
					numberOfWeeks = mission.mWeek;
				}
				mMissions.add(mission);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mNumberOfWeeks = numberOfWeeks;
	}

	/**
	 * Adds a certain amount of MP to the player
	 * If above the threshold for a new unlock, awards it to the player
	 */
	public void addMP(Player p, int amount) {
		if (!isActive()) {
			return;
		}
		int newMP = amount + getMP(p);
		//Check if they earned any new rewards from this
		int currentLevel = getLevelFromMP(getMP(p));
		int newLevel = getLevelFromMP(newMP);
		//Give any unclaimed rewards
		for (int i = currentLevel + 1; i <= newLevel; i++) {
			p.sendMessage(Component.text("You earned a new reward!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));

			animate(p);
			givePassReward(p, i);
		}

		// Special case - finished all missions
		if (newMP == mTotalMp && !CosmeticsManager.getInstance().playerHasCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME)) {
			p.sendMessage(Component.text("You finished all the missions!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			animate(p);
			CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME);
		}

		//Actually set the score
		ScoreboardUtils.setScoreboardValue(p.getName(), PASS_MP_SCOREBOARD, newMP);
	}

	/**
	 * This method is called before opening the GUI to view the battle pass and updates player MP
	 * with any new earned MP from completed missions
	 */
	public void updatePlayerPassProgress(Player p) {
		if (!isActive()) {
			return;
		}
		// Loop through our internal mission list and check scores
		int mpToAdd = 0;
		int missionCounter = 1;
		for (WeeklyMission mission : getActiveMissions()) {
			String scoreboard = SeasonalEventManager.MISSION_SCOREBOARD + missionCounter;
			missionCounter++;
			int playerScore = ScoreboardUtils.getScoreboardValue(p.getName(), scoreboard);
			if (playerScore < mission.mAmount) {
				continue;
			} else {
				// Add mp and mark mission as claimed by setting score to -1
				mpToAdd += mission.mMP;
				ScoreboardUtils.setScoreboardValue(p.getName(), scoreboard, -1);
			}
		}
		// Update MP with any earned
		addMP(p, mpToAdd);
	}

	/**
	 * This method is called every time a player logs in on a new week
	 */
	public void resetPlayerPassMissions(Player p) {
		if (!isActive()) {
			return;
		}
		// Loop through our internal mission list and check scores
		for (int missionCounter = 1; missionCounter <= getActiveMissions().size(); ++missionCounter) {
			String scoreboard = SeasonalEventManager.MISSION_SCOREBOARD + missionCounter;
			ScoreboardUtils.setScoreboardValue(p.getName(), scoreboard, 0);
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
	 * Runs through the missions list and returns all
	 * matching the current week number of the pass
	 */
	public List<WeeklyMission> getActiveMissions() {
		List<WeeklyMission> activeMissions = new ArrayList<>();
		// Determine what week it is since the pass start

		int week = getWeekOfPass();
		for (WeeklyMission mission : mMissions) {
			if (mission.mWeek == week) {
				activeMissions.add(mission);
			}
		}
		return activeMissions;
	}

	/**
	 * Debug method to get missions for given week
	 */
	public List<WeeklyMission> getMissionsInWeek(int week) {
		List<WeeklyMission> activeMissions = new ArrayList<>();
		for (WeeklyMission mission : mMissions) {
			if (mission.mWeek == week) {
				activeMissions.add(mission);
			}
		}
		return activeMissions;
	}

	/**
	 * Gives out reward for specific level depending
	 * on its type implementation
	 */
	public void givePassReward(Player p, int level) {
		if (!isActive()) {
			return;
		}
		SeasonalReward reward = mRewards.get(level - 1);
		int amount = 1;
		if (reward.mAmount != 0) {
			amount = reward.mAmount;
		}
		switch (reward.mType) {
			case ELITE_FINISHER:
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.ELITE_FINISHER, reward.mData);
				break;
			case TITLE:
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, reward.mData);
				break;
			case PLOT_BORDER:
				CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.PLOT_BORDER, reward.mData);
				break;
			case ITEM_SKIN:
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, ITEM_SKIN_KEY);
				}
				break;
			case LOOT_SPIN:
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, TREASURE_WHEEL_KEY);
				}
				break;
			case UNIQUE_SPIN:
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, RELIC_WHEEL_KEY);
				}
				break;
			case LOOT_TABLE:
				for (int i = 0; i < amount; i++) {
					givePlayerLootTable(p, reward.mData);
				}
				break;
			case SHULKER_BOX:
				ItemStack shulker = new ItemStack(Material.PURPLE_SHULKER_BOX, 1);
				InventoryUtils.giveItem(p, shulker);
				break;
			default:
				break;
		}
	}

	/**
	 * Helper method to give player loot from a table
	 */
	private void givePlayerLootTable(Player p, String lootTablePath) {
		if (!isActive()) {
			return;
		}
		if (lootTablePath == null || lootTablePath.length() == 0) {
			return;
		}
		LootContext context = new LootContext.Builder(p.getLocation()).build();
		LootTable rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTablePath));
		if (rewardTable != null) {
			Collection<ItemStack> loot = rewardTable.populateLoot(FastUtils.RANDOM, context);
			if (loot != null) {
				for (ItemStack item : loot) {
					InventoryUtils.giveItem(p, item);
				}
			}
		}
	}

	public int getMP(Player p) {
		if (!isActive()) {
			return 0;
		}
		return ScoreboardUtils.getScoreboardValue(p.getName(), PASS_MP_SCOREBOARD);
	}

	public void setMP(Player p, int amount) {
		//Debug method - only use for testing as it will not apply rewards!
		if (!isActive()) {
			return;
		}
		ScoreboardUtils.setScoreboardValue(p.getName(), PASS_MP_SCOREBOARD, amount);
	}

	public int getLevelFromMP(int mp) {
		if (!isActive()) {
			return 0;
		}
		return Math.min(mRewards.size(), mp / MP_PER_LEVEL);
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public void addWeeklyMissionProgress(Player p, WeeklyMission mission, int missionNumber, int amount) {
		String scoreboard = SeasonalEventManager.MISSION_SCOREBOARD + missionNumber;
		int score = ScoreboardUtils.getScoreboardValue(p.getName(), scoreboard);
		if (score == -1 || score >= mission.mAmount) {
			// Rewards already claimed - don't add progress for infinite mp
			return;
		}
		ScoreboardUtils.setScoreboardValue(p.getName(), scoreboard, Math.min(mission.mAmount, score + amount));
		if (score + amount >= mission.mAmount) {
			//Play an animation and notify player
			p.sendMessage(Component.text("You completed a weekly mission! Open the Seasonal Pass menu to claim your progress!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			animate(p);
		}
	}

	public void animate(Player player) {
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

	// Time Utils

	public int getDaysUntilMissionReset() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.getDaysLeftInWeeklyVersion() - 1;
	}

	public int getHoursUntilMissionReset() {
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
