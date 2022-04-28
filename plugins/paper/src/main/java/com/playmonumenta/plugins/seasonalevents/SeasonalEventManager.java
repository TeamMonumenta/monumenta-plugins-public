package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
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

public class SeasonalEventManager {
	public static final int LEVEL_COUNT = 25;
	public static final int MP_PER_LEVEL = 75;
	//Change this to the active pass name for each new pass
	public static final String PASS_NAME = "Anniversary Pass";
	public static final LocalDateTime PASS_START = LocalDateTime.of(2022, 4, 29, 3, 30);
	public static final int NUMBER_OF_WEEKS = 12;
	public static final int WEEKLY_MP = 225;
	public static final String PASS_MP_SCOREBOARD = "SeasonalEventMP";
	public static final String MISSION_SCOREBOARD = "WeeklyMission";
	public static final String MISSION_FILE_PATH = "/seasonalevents/missions.json";
	public static final String REWARD_FILE_PATH = "/seasonalevents/rewards.json";
	public static final String ALL_MISSIONS_TITLE_NAME = "Tryhard";
	// Loot tables for rewards
	public static final String ITEM_SKIN_KEY = "epic:pass/metamorphosis_token";
	public static final String TREASURE_WHEEL_KEY = "epic:pass/treasure_wheel_token";
	public static final String RELIC_WHEEL_KEY = "epic:pass/relic_wheel_token";
	// Missions and rewards arrays
	public static List<WeeklyMission> PASS_MISSIONS = new ArrayList<>();
	public static List<SeasonalReward> PASS_REWARDS = new ArrayList<>();

	static {
		try {
			PASS_REWARDS = loadRewards();
			PASS_MISSIONS = loadMissions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load reward objects from json parsing
	 */
	private static List<SeasonalReward> loadRewards() throws Exception {
		List<SeasonalReward> rewards = new ArrayList<>();

		String rewardsContent = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + REWARD_FILE_PATH);
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(rewardsContent, JsonObject.class);
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
									rewards.add(reward);
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

				rewards.add(reward);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rewards;
	}

	/**
	 * Load mission objects from json parsing
	 */
	private static List<WeeklyMission> loadMissions() throws Exception {
		List<WeeklyMission> missions = new ArrayList<>();

		String missionContent = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + MISSION_FILE_PATH);
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(missionContent, JsonObject.class);
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
				if (toParse.get("delvemodifier") != null) {
					// This compares using integers- in json need to list the number of the modifier, not the name!
					int delveModifier = toParse.get("delvemodifier").getAsInt();
					for (DelvesUtils.Modifier selection : DelvesUtils.Modifier.values()) {
						if (selection.getColumn() == delveModifier) {
							mission.mDelveModifier = selection;
						}
					}
				}
				missions.add(mission);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return missions;
	}

	/**
	 * Adds a certain amount of MP to the player
	 * If above the threshold for a new unlock, awards it to the player
	 */
	public static void addMP(Player p, int amount) {
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

		// Special case- finished all missions
		if (newMP == WEEKLY_MP * NUMBER_OF_WEEKS && !CosmeticsManager.getInstance().playerHasCosmetic(p, ALL_MISSIONS_TITLE_NAME, CosmeticType.TITLE)) {
			p.sendMessage(Component.text("You finished all the missions!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			animate(p);
			CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, ALL_MISSIONS_TITLE_NAME);
		}

		//Actually set the score
		ScoreboardUtils.setScoreboardValue(p.getName(), PASS_MP_SCOREBOARD, newMP);
	}

	/**
	 * This method is called before opening the GUI to view the battle pass and updates player MP
	 * with any new earned MP from completed missions
	 */
	public static void updatePlayerPassProgress(Player p) {
		// Loop through our internal mission list and check scores
		int mpToAdd = 0;
		int missionCounter = 1;
		for (WeeklyMission mission : getActiveMissions()) {
			String scoreboard = MISSION_SCOREBOARD + missionCounter;
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

	public static int getWeekOfPass() {
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));
		return Math.max(1, 1 + (int) ChronoUnit.WEEKS.between(PASS_START, currentTime));
	}

	/**
	 * Runs through the missions list and returns all
	 * matching the current week number of the pass
	 */
	public static List<WeeklyMission> getActiveMissions() {
		List<WeeklyMission> activeMissions = new ArrayList<>();
		// Determine what week it is since the pass start

		int week = getWeekOfPass();
		for (WeeklyMission mission : PASS_MISSIONS) {
			if (mission.mWeek == week) {
				activeMissions.add(mission);
			}
		}
		return activeMissions;
	}

	/**
	 * Debug method to get missions for given week
	 */
	public static List<WeeklyMission> getMissionsInWeek(int week) {
		List<WeeklyMission> activeMissions = new ArrayList<>();
		for (WeeklyMission mission : PASS_MISSIONS) {
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
	public static void givePassReward(Player p, int level) {
		SeasonalReward reward = PASS_REWARDS.get(level - 1);
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
	private static void givePlayerLootTable(Player p, String lootTablePath) {
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

	public static int getMP(Player p) {
		return ScoreboardUtils.getScoreboardValue(p.getName(), PASS_MP_SCOREBOARD);
	}

	public static void setMP(Player p, int amount) {
		//Debug method- only use for testing as it will not apply rewards!
		ScoreboardUtils.setScoreboardValue(p.getName(), PASS_MP_SCOREBOARD, amount);
	}

	public static int getLevelFromMP(int mp) {
		return Math.min(LEVEL_COUNT, mp / MP_PER_LEVEL);
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public static void addWeeklyMissionProgress(Player p, WeeklyMission mission, int missionNumber, int amount) {
		String scoreboard = MISSION_SCOREBOARD + missionNumber;
		int score = ScoreboardUtils.getScoreboardValue(p.getName(), scoreboard);
		if (score == -1 || score >= mission.mAmount) {
			// Rewards already claimed- don't add progress for infinite mp
			return;
		}
		ScoreboardUtils.setScoreboardValue(p.getName(), scoreboard, Math.min(mission.mAmount, score + amount));
		if (score + amount >= mission.mAmount) {
			//Play an animation and notify player
			p.sendMessage(Component.text("You completed a weekly mission! Open the Seasonal Pass menu to claim your progress!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			animate(p);
		}
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

	// Time Utils

	public static int getDaysUntilMissionReset() {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(PASS_START.getYear(), PASS_START.getMonthValue() - 1, PASS_START.getDayOfMonth(),
			PASS_START.getHour(), PASS_START.getMinute(), PASS_START.getSecond());
		calendar.add(Calendar.DAY_OF_MONTH, 7 * getWeekOfPass());
		LocalDateTime endOfWeek = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC"));
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));

		return (int) ChronoUnit.DAYS.between(currentTime, endOfWeek);
	}

	public static int getHoursUntilMissionReset() {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(PASS_START.getYear(), PASS_START.getMonthValue() - 1, PASS_START.getDayOfMonth(),
			PASS_START.getHour(), PASS_START.getMinute(), PASS_START.getSecond());
		calendar.add(Calendar.DAY_OF_MONTH, 7 * getWeekOfPass());
		LocalDateTime endOfWeek = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC"));
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));

		return (int) ChronoUnit.HOURS.between(currentTime, endOfWeek) % 24;
	}

	public static int getDaysUntilPassEnd() {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(PASS_START.getYear(), PASS_START.getMonthValue() - 1, PASS_START.getDayOfMonth(),
			PASS_START.getHour(), PASS_START.getMinute(), PASS_START.getSecond());
		calendar.add(Calendar.DAY_OF_MONTH, 7 * NUMBER_OF_WEEKS);
		LocalDateTime endOfWeek = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC"));
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));

		return (int) ChronoUnit.DAYS.between(currentTime, endOfWeek);
	}

	public static int getHoursUntilPassEnd() {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(PASS_START.getYear(), PASS_START.getMonthValue() - 1, PASS_START.getDayOfMonth(),
			PASS_START.getHour(), PASS_START.getMinute(), PASS_START.getSecond());
		calendar.add(Calendar.DAY_OF_MONTH, 7 * NUMBER_OF_WEEKS);
		LocalDateTime endOfWeek = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.of("UTC"));
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));

		return (int) ChronoUnit.HOURS.between(currentTime, endOfWeek) % 24;
	}
}
