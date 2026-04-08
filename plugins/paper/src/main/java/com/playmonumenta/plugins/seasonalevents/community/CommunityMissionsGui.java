package com.playmonumenta.plugins.seasonalevents.community;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;

public class CommunityMissionsGui extends Gui {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM");

	private boolean mIsLoading = true;
	private @Nullable List<CommunityMissionData> mCachedData = null;

	public CommunityMissionsGui(Player player) {
		super(player, 54, Component.text("Community Missions", NamedTextColor.BLACK));
		loadData();
	}

	private void loadData() {
		CommunityMissionManager manager = CommunityMissionManager.getInstance();

		// see if there's any rewards from last mission to claim
		manager.tryClaimRewards(mPlayer);

		// async data retrieval
		manager.getCurrentMissionData(mPlayer.getUniqueId()).thenAccept(data -> {
			this.mCachedData = data;
			this.mIsLoading = false;

			// update on main thread (TODO: make sure i did this right and no memory leaks)
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (getOpenGui(mPlayer) == this) {
					this.update();
				}
			});
		});
	}

	@Override
	protected void setup() {
		if (mIsLoading) {
			ItemStack loading = new ItemStack(Material.CLOCK);
			ItemMeta meta = loading.getItemMeta();
			meta.displayName(Component.text("Loading Data...", NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false));
			loading.setItemMeta(meta);
			setItem(22, loading);
			return;
		}

		if (mCachedData == null || mCachedData.isEmpty()) {
			ItemStack error = new ItemStack(Material.BARRIER);
			ItemMeta meta = error.getItemMeta();
			meta.displayName(Component.text("No Active Event", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			error.setItemMeta(meta);
			setItem(22, error);
			return;
		}

		// title painting item
		ItemStack titleItem = new ItemStack(Material.PAINTING);
		ItemMeta titleMeta = titleItem.getItemMeta();
		// see if event is currently on
		CommunityEvent displayEvent = CommunityMissionManager.getInstance().getDisplayEvent();
		LocalDateTime now = DateUtils.localDateTime();
		boolean isActive = displayEvent != null && displayEvent.isActive(now);

		if (isActive) {
			titleMeta.displayName(Component.text("Community Missions", NamedTextColor.YELLOW, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			titleMeta.displayName(Component.text("Community Missions (Ended)", NamedTextColor.RED, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
		}
		if (displayEvent != null) {
			List<Component> lore = new ArrayList<>();
			String dateRange = displayEvent.mStart.format(DATE_FORMAT) + " - " + displayEvent.mEnd.format(DATE_FORMAT);
			lore.add(Component.text(dateRange, NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));

			// time remaining
			if (isActive) {
				long days = ChronoUnit.DAYS.between(now, displayEvent.mEnd);
				long hours = ChronoUnit.HOURS.between(now, displayEvent.mEnd) % 24;
				lore.add(Component.text(String.format("Missions end in %dd %dh", days, hours), NamedTextColor.YELLOW)
					.decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text("Event Ended", NamedTextColor.RED)
					.decoration(TextDecoration.ITALIC, false));
			}

			// description if there's a mission unlock
			if (displayEvent.mCompletionDescription != null) {
				lore.add(Component.empty());
				lore.add(Component.text("Complete all 3 missions to ", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
				lore.add(Component.text(displayEvent.mCompletionDescription, NamedTextColor.GOLD, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));
			}
			titleMeta.lore(lore);
		}
		titleItem.setItemMeta(titleMeta);
		setItem(0, 4, titleItem);

		// mission rows
		for (int i = 0; i < 3; i++) {
			if (i >= mCachedData.size()) {
				break;
			}
			CommunityMissionData data = mCachedData.get(i);
			int row = i + 1;
			setupMissionRow(row, data);
		}
		// contribution row
		for (int i = 0; i < 3; i++) {
			if (i >= mCachedData.size()) {
				break;
			}
			CommunityMissionData data = mCachedData.get(i);
			int col = 2 + (i * 2);
			setupContributionItem(5, col, data);
		}
	}

	private void setupMissionRow(int row, CommunityMissionData data) {
		CommunityMissionDefinition def = data.mDef;
		long total = data.mTotalContribution;

		// info book for mission info
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		ItemMeta bookMeta = book.getItemMeta();
		bookMeta.displayName(Component.text("Mission " + row, NamedTextColor.YELLOW, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));
		List<Component> bookLore = new ArrayList<>();
		bookLore.add(Component.text(def.mType.mDescription, NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		bookMeta.lore(bookLore);
		book.setItemMeta(bookMeta);
		setItem(row, 1, book);

		// goal tier 1 and progress bar towards it (always visible)
		setItem(row, 2, createProgressBar(total, 0, def.mGoalTier1));
		setItem(row, 3, createGoalItem(1, total, def.mGoalTier1, def, true));
		// goal tier 2 and progress bar towards it (hidden if g1 not met yet)
		setItem(row, 4, createProgressBar(total, def.mGoalTier1, def.mGoalTier2));
		boolean showTier2 = total >= def.mGoalTier1;
		setItem(row, 5, createGoalItem(2, total, def.mGoalTier2, def, showTier2));
		// goal tier 3 and progress bar towards it (hidden if g2 not met yet)
		setItem(row, 6, createProgressBar(total, def.mGoalTier2, def.mGoalTier3));
		boolean showTier3 = total >= def.mGoalTier2;
		setItem(row, 7, createGoalItem(3, total, def.mGoalTier3, def, showTier3));
	}

	private ItemStack createProgressBar(long current, long start, long end) {
		double percent = 0.0;
		if (current >= end) {
			percent = 1.0;
		} else if (current > start) {
			percent = (double) (current - start) / (double) (end - start);
		}

		int totalBars = 50;
		int greenBars = (int) (percent * totalBars);

		Material mat = Material.YELLOW_STAINED_GLASS_PANE;
		if (percent <= 0) {
			mat = Material.RED_STAINED_GLASS_PANE;
		}
		if (percent >= 1.0) {
			mat = Material.GREEN_STAINED_GLASS_PANE;
		}

		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Progress: " + (int)(percent * 100) + "%", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true));

		List<Component> lore = new ArrayList<>();
		Component barComp = Component.text(":".repeat(Math.max(0, greenBars)), NamedTextColor.GREEN)
			.append(Component.text(":".repeat(Math.max(0, totalBars - greenBars)), NamedTextColor.RED))
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true);
		lore.add(barComp);

		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createGoalItem(int tier, long current, long goal, CommunityMissionDefinition def, boolean isVisible) {
		boolean reached = current >= goal;
		Material mat = reached ? Material.EMERALD : Material.REDSTONE;

		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Goal Tier " + tier, NamedTextColor.RED, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(def.mType.mDescription, NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));

		if (isVisible) {
			lore.add(Component.text(current + "/" + goal, NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false));

			// get reward name for specific tier
			String rewardName = "Unknown";
			int amount = 0;
			if (tier == 1) {
				rewardName = getPrettyLootName(def.mLootT1);
				amount = def.mAmountT1;
			} else if (tier == 2) {
				rewardName = getPrettyLootName(def.mLootT2);
				amount = def.mAmountT2;
			} else if (tier == 3) {
				rewardName = getPrettyLootName(def.mLootT3);
				amount = def.mAmountT3;
			}

			lore.add(Component.text("Reward: " + amount + "x " + rewardName, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("???", NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Reward: ???", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		}

		if (reached) {
			lore.add(Component.text("Goal Reached!", NamedTextColor.GREEN)
				.decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private String getPrettyLootName(@Nullable String lootPath) {
		if (lootPath == null) {
			return "None";
		}

		try {
			LootTable table = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootPath));
			if (table != null) {
				LootContext context = new LootContext.Builder(mPlayer.getLocation()).build();
				Collection<ItemStack> items = table.populateLoot(FastUtils.RANDOM, context);

				if (!items.isEmpty()) {
					ItemStack item = items.iterator().next();
					String displayName = ItemUtils.getPlainName(item);
					if (!displayName.isEmpty()) {
						return displayName;
					}
				}
			}
		} catch (Exception ignored) {
			MMLog.warning("Community Mission Error: Couldn't get reward item");
		}

		int slash = lootPath.lastIndexOf('/');
		if (slash != -1 && slash < lootPath.length() - 1) {
			return lootPath.substring(slash + 1).replace('_', ' ');
		}
		// default
		return "Item";
	}

	private void setupContributionItem(int row, int col, CommunityMissionData data) {
		CommunityMissionDefinition def = data.mDef;
		long contribution = data.mPersonalContribution;
		// cant be top x without having reached tier 2
		boolean qualified = contribution >= def.mContribTier2;

		Material dyeMat = Material.RED_DYE;
		if (qualified && data.mRank == 1) {
			dyeMat = Material.PINK_DYE;
		} else if (qualified && data.mRank > 0 && data.mRank <= 10) {
			dyeMat = Material.PURPLE_DYE; // top 10
		} else if (data.mIsTop10) {
			dyeMat = Material.CYAN_DYE;
		} else if (data.mIsTop25) {
			dyeMat = Material.GREEN_DYE;
		} else if (contribution >= def.mContribTier2) {
			dyeMat = Material.LIME_DYE;
		} else if (contribution >= def.mContribTier1) {
			dyeMat = Material.YELLOW_DYE;
		}

		ItemStack item = new ItemStack(dyeMat);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Mission " + (col / 2) + " Contributions", NamedTextColor.YELLOW, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("Total Contributions: " + contribution, NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));

		if (data.mRank > 0) {
			lore.add(Component.text("Rank: #" + data.mRank + " out of " + data.mTotalParticipants, NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("Rank: Unranked", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
		}

		lore.add(getTierLine("Tier 0 (0x Reward Mult)", true));
		lore.add(getTierLine("Tier 1: " + def.mContribTier1 + " (0.5x Reward Mult)", contribution >= def.mContribTier1));
		lore.add(getTierLine("Tier 2: " + def.mContribTier2 + " (1.0x Reward Mult)", contribution >= def.mContribTier2));
		lore.add(getTierLine("Top 25% (1.5x Reward Mult)", data.mIsTop25));
		lore.add(getTierLine("Top 10% (2.0x Reward Mult)", data.mIsTop10));

		meta.lore(lore);
		item.setItemMeta(meta);
		setItem(row, col, item);
	}

	private Component getTierLine(String text, boolean achieved) {
		NamedTextColor color = achieved ? NamedTextColor.GREEN : NamedTextColor.RED;
		String icon = achieved ? "✔ " : "✘ ";
		return Component.text(icon + text, color)
			.decoration(TextDecoration.ITALIC, false);
	}
}
