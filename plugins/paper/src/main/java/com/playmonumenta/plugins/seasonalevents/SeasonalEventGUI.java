package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SeasonalEventGUI extends Gui {

	private static final int PREV_PAGE_LOC = 45;
	private static final int NEXT_PAGE_LOC = 53;
	private static final int SUMMARY_LOC = 0;
	private static final int WEEKLY_MISSION_LOC = 5;
	private static final int LEVELS_PER_PAGE = 5;
	private static final int LEVEL_PROGRESS_START = 18;
	private static final int REWARD_START = 27;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	private final Player mTargetPlayer;
	private final SeasonalPass mSeasonalPass;
	private int mCurrentPage;
	private final int mWeek;

	public SeasonalEventGUI(SeasonalPass seasonalPass, Player player) {
		this(seasonalPass, player, player, -1);
	}

	public SeasonalEventGUI(SeasonalPass seasonalPass, Player player, int week) {
		this(seasonalPass, player, player, week);
	}

	public SeasonalEventGUI(SeasonalPass seasonalPass, Player requestingPlayer, Player targetPlayer, int week) {
		super(requestingPlayer, 54, Component.text(seasonalPass.mName, seasonalPass.mNameColor));
		if (requestingPlayer == targetPlayer && seasonalPass.isActive()) {
			seasonalPass.updatePlayerPassProgress(targetPlayer);
		}
		int level = seasonalPass.getLevelFromMP(seasonalPass.getMP(targetPlayer));
		mSeasonalPass = seasonalPass;
		mCurrentPage = Math.max(1, ((level - 1) / 5) + 1);
		mWeek = week == -1 ? mSeasonalPass.getWeekOfPass() : week;
		mTargetPlayer = targetPlayer;
		setFiller(FILLER);
	}

	@Override
	protected void setup() {
		List<WeeklyMission> currentMissions;
		if (mWeek != -1) {
			currentMissions = mSeasonalPass.getMissionsInWeek(mWeek);
		} else {
			currentMissions = mSeasonalPass.getActiveMissions();
		}
		List<SeasonalReward> rewards = mSeasonalPass.mRewards;
		int playerMP = mSeasonalPass.getMP(mTargetPlayer);
		int playerLevel = mSeasonalPass.getLevelFromMP(playerMP);
		int mpToNextLevel = ((playerLevel + 1) * SeasonalPass.MP_PER_LEVEL) - playerMP;

		//Set up summary
		ItemStack passSummary = new ItemStack(Material.SOUL_LANTERN, Math.min(64, Math.max(1, playerLevel)));
		ItemMeta meta = passSummary.getItemMeta();
		meta.displayName(Component.text(mSeasonalPass.mName, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(String.format("Your Level: %d", playerLevel), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("Earned MP: %d", playerMP), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
		if (playerLevel < mSeasonalPass.mRewards.size()) {
			lore.add(Component.text(String.format("MP to Next Level: %d", mpToNextLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		}
		lore.add(Component.text(String.format("Pass ends in %dd %dh", mSeasonalPass.getDaysUntilPassEnd(), mSeasonalPass.getHoursUntilPassEnd()), NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));

		meta.lore(lore);
		passSummary.setItemMeta(meta);
		setItem(SUMMARY_LOC, passSummary);

		// Set up weekly mission display
		ItemStack missionSummary = new ItemStack(Material.CLOCK, 1);
		meta = missionSummary.getItemMeta();
		meta.displayName(Component.text("Weekly Missions", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		lore = new ArrayList<>();
		if (mSeasonalPass.isActive()) {
			lore.add(Component.text(String.format("Week %d", mWeek), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("Resets in %dd %dh", mSeasonalPass.getDaysUntilMissionReset(), mSeasonalPass.getHoursUntilMissionReset()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		} else {
			LocalDateTime now = DateUtils.localDateTime();
			LocalDateTime passStart = mSeasonalPass.mPassStart;
			LocalDateTime passEnd = passStart.plusWeeks(mSeasonalPass.mNumberOfWeeks).minusDays(1);
			if (now.isBefore(passStart)) {
				lore.add(Component.text(String.format("Week %d", mWeek), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text(String.format("Pass starts %s %d, %d",
					passStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
					passStart.getDayOfMonth(),
					passStart.getYear()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(String.format("Week %d", mWeek), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text(String.format("Pass ran from %s %d, %d",
					passStart.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
					passStart.getDayOfMonth(),
					passStart.getYear()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text(String.format("through %s %d, %d",
					passEnd.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
					passEnd.getDayOfMonth(),
					passEnd.getYear()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			}
		}
		meta.lore(lore);
		missionSummary.setItemMeta(meta);
		setItem(WEEKLY_MISSION_LOC, missionSummary);

		// Go to repurchase GUI button
		setItem(0, 2, ItemUtils.modifyMeta(new ItemStack(Material.CARTOGRAPHY_TABLE), m -> {
			m.displayName(Component.text("Old Season Passes", NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			m.lore(List.of(Component.text("Click to view old passes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("and purchase rewards from them.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		}))
			.onLeftClick(() -> {
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
				new SeasonalEventRepurchaseGUI(mPlayer).open();
			});

		// Individual weekly mission display
		for (int i = 0; i < Math.min(3, currentMissions.size()); i++) {
			try {
				WeeklyMission mission = currentMissions.get(i);
				String missionScoreboard = SeasonalEventManager.MISSION_SCOREBOARD + (i + 1);
				int progress = ScoreboardUtils.getScoreboardValue(mTargetPlayer.getName(), missionScoreboard);

				//Placeholder
				ItemStack missionItem = new ItemStack(Material.YELLOW_CONCRETE_POWDER, 1);
				if (progress == -1) {
					missionItem = new ItemStack(Material.GREEN_CONCRETE_POWDER, 1);
				}

				meta = missionItem.getItemMeta();
				meta.displayName(Component.text("Mission " + (i + 1), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				String missionDescription = mission.mDescription;
				if (missionDescription == null) {
					missionDescription = "Mission description not set";
				}
				GUIUtils.splitLoreLine(meta, missionDescription, NamedTextColor.RED, 30, false);
				lore = meta.lore();
				if (lore == null) {
					lore = new ArrayList<>();
				}

				if (progress == -1) {
					lore.add(Component.text("Progress: " + mission.mAmount + "/" + mission.mAmount, NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Reward: " + mission.mMP + "MP", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Completed", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("Progress: " + progress + "/" + mission.mAmount, NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Reward: " + mission.mMP + "MP", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("In Progress", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				}
				meta.lore(lore);
				missionItem.setItemMeta(meta);
				setItem(WEEKLY_MISSION_LOC + (i + 1), missionItem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Level progress display
		int startingLevel = ((mCurrentPage - 1) * LEVELS_PER_PAGE) + 1;
		for (int i = 0; i < LEVELS_PER_PAGE; i++) {
			try {
				int levelLocation = LEVEL_PROGRESS_START + (i * 2);
				int rewardLocation = REWARD_START + (i * 2);
				int level = startingLevel + i;
				int mP = level * SeasonalEventManager.MP_PER_LEVEL;

				//Level item
				ItemStack levelItem = new ItemStack(Material.RED_CONCRETE, level);
				if (level == playerLevel + 1) {
					levelItem = new ItemStack(Material.YELLOW_CONCRETE, level);
				} else if (level <= playerLevel) {
					levelItem = new ItemStack(Material.GREEN_CONCRETE, level);
				}

				meta = levelItem.getItemMeta();
				meta.displayName(Component.text("Level " + level, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				lore = new ArrayList<>();
				if (level == playerLevel + 1) {
					lore.add(Component.text("" + playerMP + "/" + mP + " MP", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("In Progress", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
				} else if (level <= playerLevel) {
					lore.add(Component.text("" + mP + "/" + mP + " MP", NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Obtained", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("" + playerMP + "/" + mP + " MP", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
					lore.add(Component.text("Locked", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
				}
				meta.lore(lore);
				levelItem.setItemMeta(meta);
				setItem(levelLocation, levelItem);

				//Reward item
				SeasonalReward reward = rewards.get(level - 1);
				// If we have a loot table already for the item, just show that
				if (reward.mLootTable != null) {
					setItem(rewardLocation, reward.mLootTable);
					continue;
				}
				Material displayItem = reward.mDisplayItem;
				if (displayItem == null) {
					displayItem = Material.STONE;
				}
				ItemStack rewardItem = new ItemStack(displayItem, 1);
				meta = rewardItem.getItemMeta();
				String rewardName = reward.mName;
				if (rewardName == null) {
					rewardName = "Reward name not set";
				}
				meta.displayName(Component.text(rewardName, reward.mNameColor).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				String description = reward.mDescription;
				if (description == null) {
					description = "Description not set";
				}
				NamedTextColor namedTextColor = reward.mDescriptionColor;
				if (namedTextColor == null) {
					namedTextColor = NamedTextColor.WHITE;
				}
				GUIUtils.splitLoreLine(meta, description, namedTextColor, 30, false);
				rewardItem.setItemMeta(meta);

				setItem(rewardLocation, rewardItem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Prev and next page buttons
		if (mCurrentPage > 1) {
			// Display prev page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			setItem(PREV_PAGE_LOC, pageItem)
				.onLeftClick(() -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
					mCurrentPage--;
					update();
				});
		}

		if (mCurrentPage < (mSeasonalPass.mRewards.size() / LEVELS_PER_PAGE)) {
			// Display next page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			setItem(NEXT_PAGE_LOC, pageItem)
				.onLeftClick(() -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
					mCurrentPage++;
					update();
				});
		}
	}
}
