package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SeasonalEventGUI extends CustomInventory {

	private static final int PREV_PAGE_LOC = 45;
	private static final int NEXT_PAGE_LOC = 53;
	private static final int SUMMARY_LOC = 0;
	private static final int WEEKLY_MISSION_LOC = 5;
	private static final int LEVELS_PER_PAGE = 5;
	private static final int LEVEL_PROGRESS_START = 18;
	private static final int REWARD_START = 27;
	private static final int MISSIONS_PER_WEEK = 3;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private int mCurrentPage = 1;
	private int mWeek;

	public SeasonalEventGUI(Player player) {
		this(player, player, -1);
	}

	public SeasonalEventGUI(Player player, int week) {
		this(player, player, week);
	}

	public SeasonalEventGUI(Player requestingPlayer, Player targetPlayer, int week) {
		super(requestingPlayer, 54, SeasonalEventManager.PASS_NAME);
		SeasonalEventManager.updatePlayerPassProgress(targetPlayer);
		int level = SeasonalEventManager.getLevelFromMP(SeasonalEventManager.getMP(targetPlayer));
		mCurrentPage = Math.max(1, ((level - 1) / 5) + 1);
		mWeek = week;
		setUpPass(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() == mInventory) {
			//Attempt to switch page if clicked page
			ItemStack item = event.getCurrentItem();
			if (item == null || item.getType() == null) {
				return;
			}
			Player p = (Player) event.getWhoClicked();
			if (event.getSlot() == NEXT_PAGE_LOC && item.getType() == Material.ARROW) {
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mCurrentPage++;
				setUpPass(p);
			}

			if (event.getSlot() == PREV_PAGE_LOC && item.getType() == Material.ARROW) {
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mCurrentPage--;
				setUpPass(p);
			}
		}
	}

	public Boolean setUpPass(Player targetPlayer) {
		List<WeeklyMission> currentMissions = SeasonalEventManager.getActiveMissions();
		if (mWeek != -1) {
			currentMissions = SeasonalEventManager.getMissionsInWeek(mWeek);
		}
		List<SeasonalReward> rewards = SeasonalEventManager.PASS_REWARDS;
		int playerLevel = SeasonalEventManager.getLevelFromMP(SeasonalEventManager.getMP(targetPlayer));
		int playerMP = SeasonalEventManager.getMP(targetPlayer);
		int mpToNextLevel = ((playerLevel + 1) * SeasonalEventManager.MP_PER_LEVEL) - playerMP;

		//Set inventory to filler to start
		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		//Set up summary
		ItemStack passSummary = new ItemStack(Material.SOUL_LANTERN, Math.min(64, Math.max(1, playerLevel)));
		ItemMeta meta = passSummary.getItemMeta();
		meta.displayName(Component.text(SeasonalEventManager.PASS_NAME, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(String.format("Your Level: %d", playerLevel), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("Earned MP: %d", playerMP), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
		if (playerLevel < SeasonalEventManager.LEVEL_COUNT) {
			lore.add(Component.text(String.format("MP to Next Level: %d", mpToNextLevel), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		}
		lore.add(Component.text(String.format("Pass ends in %dd %dh", SeasonalEventManager.getDaysUntilPassEnd(), SeasonalEventManager.getHoursUntilPassEnd()), NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));

		meta.lore(lore);
		passSummary.setItemMeta(meta);
		mInventory.setItem(SUMMARY_LOC, passSummary);

		// Set up weekly mission display
		ItemStack missionSummary = new ItemStack(Material.CLOCK, 1);
		meta = missionSummary.getItemMeta();
		meta.displayName(Component.text("Weekly Missions", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		lore = new ArrayList<>();
		lore.add(Component.text(String.format("Week %d", SeasonalEventManager.getWeekOfPass()), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("Resets in %dd %dh", SeasonalEventManager.getDaysUntilMissionReset(), SeasonalEventManager.getHoursUntilMissionReset()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		missionSummary.setItemMeta(meta);
		mInventory.setItem(WEEKLY_MISSION_LOC, missionSummary);

		// Individual weekly mission display
		for (int i = 0; i < MISSIONS_PER_WEEK; i++) {
			try {
				WeeklyMission mission = currentMissions.get(i);
				String missionScoreboard = SeasonalEventManager.MISSION_SCOREBOARD + (i + 1);
				int progress = ScoreboardUtils.getScoreboardValue(targetPlayer.getName(), missionScoreboard);

				//Placeholder
				ItemStack missionItem = new ItemStack(Material.YELLOW_CONCRETE_POWDER, 1);
				if (progress == -1) {
					missionItem = new ItemStack(Material.GREEN_CONCRETE_POWDER, 1);
				}

				meta = missionItem.getItemMeta();
				meta.displayName(Component.text("Mission " + (i + 1), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				GUIUtils.splitLoreLine(meta, mission.mDescription, 30, ChatColor.RED, false);
				lore = meta.lore();

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
				mInventory.setItem(WEEKLY_MISSION_LOC + (i + 1), missionItem);
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
				mInventory.setItem(levelLocation, levelItem);

				//Reward item
				SeasonalReward reward = rewards.get(level - 1);
				// If we have a loot table already for the item, just show that
				if (reward.mLootTable != null) {
					mInventory.setItem(rewardLocation, reward.mLootTable);
					continue;
				}
				ItemStack rewardItem = new ItemStack(reward.mDisplayItem, 1);
				meta = rewardItem.getItemMeta();
				meta.displayName(Component.text(reward.mName, reward.mNameColor).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				GUIUtils.splitLoreLine(meta, reward.mDescription, 30, GUIUtils.namedTextColorToChatColor(reward.mDescriptionColor), false);
				rewardItem.setItemMeta(meta);

				mInventory.setItem(rewardLocation, rewardItem);
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
			mInventory.setItem(PREV_PAGE_LOC, pageItem);
		}

		if (mCurrentPage < (SeasonalEventManager.LEVEL_COUNT / LEVELS_PER_PAGE)) {
			// Display next page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(NEXT_PAGE_LOC, pageItem);
		}
		return true;
	}
}
