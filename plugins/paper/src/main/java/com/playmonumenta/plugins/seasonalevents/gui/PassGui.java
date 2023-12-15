package com.playmonumenta.plugins.seasonalevents.gui;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Keybind;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.seasonalevents.PlayerProgress;
import com.playmonumenta.plugins.seasonalevents.PlayerProgress.PassProgress;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.seasonalevents.SeasonalPass;
import com.playmonumenta.plugins.seasonalevents.SeasonalReward;
import com.playmonumenta.plugins.seasonalevents.SeasonalRewardType;
import com.playmonumenta.plugins.seasonalevents.WeeklyMission;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.Nullable;

public class PassGui extends Gui {
	protected static final int MAX_X = 8;

	private static final Set<PassGui> mOpenGuis = new HashSet<>();

	private final ItemStack mMetamorphosisToken;
	protected final UUID mDisplayedPlayerId;
	protected final boolean mIsModerator;
	protected long mOpenedEpochWeek;
	protected long mDisplayedEpochWeek;
	protected View mView;
	protected SeasonalPass mPass;
	protected PlayerProgress mModifiedPlayerProgress;

	public PassGui(SeasonalPass pass, Player viewingPlayer, Player displayedPlayer, LocalDateTime openedTime, boolean isModerator) {
		super(viewingPlayer, 54, Component.text(pass.mName, pass.mNameColor));

		mMetamorphosisToken = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(displayedPlayer,
			NamespacedKeyUtils.fromString(SeasonalPass.ITEM_SKIN_KEY)));

		mDisplayedPlayerId = displayedPlayer.getUniqueId();
		mIsModerator = isModerator;
		mPass = pass;
		mOpenedEpochWeek = DateUtils.getWeeklyVersion(openedTime);

		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(displayedPlayer);
		if (!isModerator || playerProgress == null) {
			mModifiedPlayerProgress = new PlayerProgress();
		} else {
			mModifiedPlayerProgress = new PlayerProgress(playerProgress);
		}

		if (pass.isActive(openedTime)) {
			mDisplayedEpochWeek = mOpenedEpochWeek;
			mView = new WeekView(this);
		} else {
			mDisplayedEpochWeek = DateUtils.getWeeklyVersion(mPass.mPassStart) + mPass.mNumberOfWeeks - 1;
			mView = new PassesView(this);
		}

		mOpenGuis.add(this);
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		super.onClose(event);
		mOpenGuis.remove(this);
	}

	@Override
	protected void setup() {
		Player displayedPlayer = getDisplayedPlayer();
		if (displayedPlayer == null) {
			mPlayer.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
			close();
			return;
		}

		Component displayName = Component.empty().decoration(TextDecoration.ITALIC, false);
		if (mIsModerator) {
			displayName = displayName.append(Component.text("Moderator: ", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		}
		displayName = displayName.append(Component.text(mPass.mName, mPass.mNameColor));
		setTitle(displayName);

		if (!mIsModerator && mPlayer.getUniqueId().equals(mDisplayedPlayerId)) {
			// Give rewards
			for (SeasonalPass oldPass : SeasonalEventManager.mAllPasses.values()) {
				oldPass.claimMP(mPlayer);
			}
		}

		PassProgress passProgress = getPassProgress();
		int mp = getMp(passProgress);
		int level = getLevel(passProgress);
		int mpToNextLevel = getMpToNextLevel(passProgress);

		//Set up week view/summary icon
		ItemStack weekViewItem = new ItemStack(mPass.mDisplayItem, Math.min(64, Math.max(1, level)));
		ItemMeta meta = weekViewItem.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.displayName(Component.text(mPass.mName, mPass.mNameColor, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(String.format("Your Level: %d", level), NamedTextColor.GOLD)
			.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("Earned MP: %d", mp), NamedTextColor.GREEN)
			.decoration(TextDecoration.ITALIC, false));
		if (level < mPass.mRewards.size()) {
			lore.add(Component.text(String.format("MP to Next Level: %d", mpToNextLevel), NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false));
		}
		lore.add(Component.text(
			String.format("Missions end in %dd %dh",
				mPass.getDaysUntilMissionEnd(),
				mPass.getHoursUntilMissionEnd()),
			NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(
				String.format("Pass ends in %dd %dh",
					mPass.getDaysUntilPassEnd(),
					mPass.getHoursUntilPassEnd()),
				NamedTextColor.BLUE)
			.decoration(TextDecoration.ITALIC, false));
		if (mIsModerator) {
			lore.add(Component.text("", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.keybind(Keybind.SWAP_OFFHAND)
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": Apply moderator changes")));
		}

		meta.lore(lore);
		weekViewItem.setItemMeta(meta);
		setItem(0, 0, weekViewItem)
			.onClick((InventoryClickEvent event) -> {
				if (mIsModerator
					&& event.getClick().equals(ClickType.SWAP_OFFHAND)) {
					if (!displayedPlayer.isOnline() || displayedPlayer.isDead()) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text("That player is no longer online.",
							NamedTextColor.RED));
						close();
						return;
					}
					PlayerProgress oldPlayerProgress = SeasonalEventManager.getPlayerProgress(displayedPlayer);
					if (oldPlayerProgress == null) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(
							Component.text("That player's progress went missing, cancelling changes",
							NamedTextColor.RED));
						close();
						return;
					}
					PlayerProgress updatedPlayerProgress = new PlayerProgress(mModifiedPlayerProgress);
					updatedPlayerProgress.copyClaimedPointsFrom(oldPlayerProgress);
					List<Component> passDiff = oldPlayerProgress.diff(updatedPlayerProgress);
					if (passDiff.isEmpty()) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text("No changes to save", NamedTextColor.RED));
					} else {
						mPlayer.sendMessage(Component.text("Changes summary (this will be logged):",
							NamedTextColor.YELLOW,
							TextDecoration.BOLD));
						StringBuilder auditBuilder = new StringBuilder("[Season Pass] ")
							.append(MessagingUtils.plainText(mPlayer.displayName()))
							.append(" modified pass progress for ")
							.append(MessagingUtils.plainText(displayedPlayer.displayName()))
							.append(":");
						for (Component line : passDiff) {
							mPlayer.sendMessage(line);
							auditBuilder
								.append('\n')
								.append(MessagingUtils.plainText(line));
						}
						AuditListener.log(auditBuilder.toString());
						SeasonalEventManager.overwritePlayerProgress(displayedPlayer, updatedPlayerProgress);
						updateWithClickSound();
					}
					return;
				}
				mView = new WeekView(this);
				updateWithPageSound();
		});

		ItemStack rewardsViewItem = new ItemStack(Material.EMERALD);
		meta = rewardsViewItem.getItemMeta();
		meta.displayName(Component.text("Pass Rewards", NamedTextColor.GREEN, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		lore = new ArrayList<>();
		lore.add(Component.text("Click to view the selected", NamedTextColor.GRAY));
		lore.add(Component.text("pass's rewards", NamedTextColor.GRAY));

		meta.lore(lore);
		rewardsViewItem.setItemMeta(meta);
		setItem(1, 0, rewardsViewItem)
			.onClick((InventoryClickEvent event) -> {
				mView = new RewardsView(this);
				updateWithPageSound();
			});

		ItemStack missionsViewItem = new ItemStack(Material.CLOCK);
		meta = missionsViewItem.getItemMeta();
		meta.displayName(Component.text("Pass Missions", NamedTextColor.GOLD, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		lore = new ArrayList<>();
		lore.add(Component.text("Click to view the selected", NamedTextColor.GRAY));
		lore.add(Component.text("pass's mission progress", NamedTextColor.GRAY));

		meta.lore(lore);
		missionsViewItem.setItemMeta(meta);
		setItem(2, 0, missionsViewItem)
			.onClick((InventoryClickEvent event) -> {
				mView = new MissionsView(this);
				updateWithPageSound();
			});

		ItemStack passesViewItem = new ItemStack(Material.CARTOGRAPHY_TABLE);
		meta = passesViewItem.getItemMeta();
		meta.displayName(Component.text("Season Passes", NamedTextColor.WHITE, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		lore = new ArrayList<>();
		lore.add(Component.text("Click to view old passes and", NamedTextColor.GRAY));
		lore.add(Component.text("purchase rewards from them.", NamedTextColor.GRAY));

		meta.lore(lore);
		passesViewItem.setItemMeta(meta);
		setItem(3, 0, passesViewItem)
			.onClick((InventoryClickEvent event) -> {
				mView = new PassesView(this);
				updateWithPageSound();
			});

		mView.setup(displayedPlayer);
	}

	protected @Nullable Player getDisplayedPlayer() {
		return Bukkit.getPlayer(mDisplayedPlayerId);
	}

	protected @Nullable PassProgress getPassProgress() {
		Player displayPlayer = getDisplayedPlayer();
		if (displayPlayer == null) {
			return null;
		}

		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(displayPlayer);
		if (playerProgress == null) {
			return null;
		} else {
			return playerProgress.getPassProgress(mPass);
		}
	}

	protected int getMp(@Nullable PassProgress passProgress) {
		if (passProgress == null) {
			return 0;
		}
		return passProgress.getMissionPoints();
	}

	protected int getLevel(@Nullable PassProgress passProgress) {
		return mPass.getLevelFromMP(getMp(passProgress));
	}

	protected int getMpToNextLevel(@Nullable PassProgress passProgress) {
		return SeasonalPass.MP_PER_LEVEL - (getMp(passProgress) % SeasonalPass.MP_PER_LEVEL);
	}

	// For hiding future information
	protected boolean isFutureEpochWeek(long epochWeek) {
		return epochWeek > mOpenedEpochWeek;
	}

	protected boolean isFuture(LocalDateTime time) {
		return isFutureEpochWeek(DateUtils.getWeeklyVersion(time));
	}

	public void updateWithPageSound() {
		mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
		update();
	}

	public void updateWithClickSound() {
		mPlayer.playSound(mPlayer, Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 1f, 1f);
		update();
	}

	public void addRewardIndicatorIcon(int y,
	                          int x,
	                          Player displayedPlayer,
	                          int rewardIndex) {
		if (rewardIndex < 0 || rewardIndex >= mPass.mRewards.size()) {
			return;
		}

		SeasonalReward reward = mPass.mRewards.get(rewardIndex);
		if (reward == null) {
			return;
		}

		PassProgress passProgress = getPassProgress();
		int mp = getMp(passProgress);
		int level = getLevel(passProgress);

		boolean isOwned = false;
		CosmeticType cosmeticType = switch (reward.mType) {
			case TITLE -> CosmeticType.TITLE;
			case ELITE_FINISHER -> CosmeticType.ELITE_FINISHER;
			case PLOT_BORDER -> CosmeticType.PLOT_BORDER;
			default -> null;
		};
		if (cosmeticType != null) {
			isOwned = CosmeticsManager.getInstance().playerHasCosmetic(displayedPlayer, cosmeticType, reward.mData);
		}

		Material mat;
		String mpStr = String.valueOf(mp);
		int mpRequirement = (rewardIndex + 1) * SeasonalPass.MP_PER_LEVEL;
		TextColor progressColor;
		Component statusLore;
		if (level > rewardIndex) {
			mat = Material.GREEN_CONCRETE_POWDER;
			mpStr = String.valueOf(mpRequirement);
			progressColor = NamedTextColor.DARK_GREEN;
			statusLore = Component.text("Obtained", NamedTextColor.GREEN);
		} else if (isOwned) {
			mat = Material.LIME_CONCRETE_POWDER;
			if (DateUtils.getWeeklyVersion(mPass.mPassStart) < SeasonalEventManager.PLAYER_PROGRESS_REWORK_WEEK) {
				mpStr = "?";
			}
			progressColor = NamedTextColor.GREEN;
			statusLore = Component.text("Purchased", NamedTextColor.GREEN);
		} else if (DateUtils.getWeeklyVersion(mPass.mPassStart) < SeasonalEventManager.PLAYER_PROGRESS_REWORK_WEEK) {
			mat = Material.LIGHT_GRAY_CONCRETE_POWDER;
			mpStr = "?";
			progressColor = NamedTextColor.DARK_GRAY;
			statusLore = Component.text("Progress Unknown", NamedTextColor.GRAY);
		} else if (level == rewardIndex) {
			mat = Material.YELLOW_CONCRETE_POWDER;
			progressColor = NamedTextColor.GOLD;
			statusLore = Component.text("In Progress", NamedTextColor.YELLOW);
		} else {
			mat = Material.RED_CONCRETE_POWDER;
			progressColor = NamedTextColor.RED;
			statusLore = Component.text("Locked", NamedTextColor.DARK_RED);
		}

		ItemStack item = new ItemStack(mat, rewardIndex + 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Level " + (rewardIndex + 1), NamedTextColor.GREEN, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(mpStr + "/" + mpRequirement + " MP", progressColor)
			.decoration(TextDecoration.ITALIC, false));
		lore.add(statusLore);

		meta.lore(lore);
		item.setItemMeta(meta);
		setItem(y, x, item);
	}

	public void addRewardItem(int y,
	                          int x,
	                          Player displayedPlayer,
	                          int rewardIndex) {
		if (rewardIndex < 0 || rewardIndex >= mPass.mRewards.size()) {
			return;
		}

		SeasonalReward reward = mPass.mRewards.get(rewardIndex);
		if (reward == null) {
			return;
		}

		boolean isFuturePass = false;
		boolean isCurrentPass = false;
		boolean isPreviousPass = false;
		int index = -1;
		for (SeasonalPass otherPass : SeasonalEventManager.mAllPasses.descendingMap().values()) {
			if (index == -1) {
				if (!otherPass.isActive()) {
					if (mPass == otherPass) {
						isFuturePass = true;
						break;
					}
					continue;
				}
				index = 0;
			}

			if (mPass == otherPass) {
				if (index == 0) {
					isCurrentPass = true;
				} else if (index == 1) {
					isPreviousPass = true;
				}
				break;
			}
			index++;
			if (index >= 2) {
				break;
			}
		}

		boolean isOwned;
		CosmeticType cosmeticType = switch (reward.mType) {
			case TITLE -> CosmeticType.TITLE;
			case ELITE_FINISHER -> CosmeticType.ELITE_FINISHER;
			case PLOT_BORDER -> CosmeticType.PLOT_BORDER;
			default -> null;
		};
		if (cosmeticType != null) {
			isOwned = CosmeticsManager.getInstance().playerHasCosmetic(displayedPlayer, cosmeticType, reward.mData);
		} else {
			isOwned = false;
		}

		ItemStack item = null;
		Runnable buy;

		String rewardData = reward.mData;
		ItemStack lootTableItem = reward.mLootTable;
		if (reward.mType == SeasonalRewardType.LOOT_TABLE
			&& lootTableItem != null
			&& lootTableItem.getItemMeta() instanceof SpawnEggMeta) {
			item = ItemUtils.clone(lootTableItem);

			buy = () -> {
				InventoryUtils.giveItem(mPlayer, ItemUtils.clone(lootTableItem));
				mPlayer.sendMessage(Component.text("Bought a [", NamedTextColor.GOLD)
					.append(ItemUtils.getDisplayName(lootTableItem)
						.hoverEvent(lootTableItem))
					.append(Component.text("].")));
			};
		} else if (rewardData != null
			&& cosmeticType != null) {
			Material displayItem = reward.mDisplayItem;
			if (displayItem == null) {
				displayItem = Material.STONE;
			}
			String rewardName = reward.mName;
			if (rewardName == null) {
				rewardName = "Name not set";
			}
			final String finalRewardName = rewardName;
			item = ItemUtils.modifyMeta(new ItemStack(displayItem),
				meta -> {
					meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
					meta.displayName(Component.text(finalRewardName, reward.mNameColor, TextDecoration.BOLD)
						.decoration(TextDecoration.ITALIC, false));
				});
			buy = () -> {
				CosmeticsManager.getInstance().addCosmetic(mPlayer, cosmeticType, rewardData);
				mPlayer.sendMessage(Component.text("Unlocked " + cosmeticType.getDisplayName()
					+ " '" + rewardData + "'.", NamedTextColor.GOLD));
			};
		} else {
			buy = null;
		}

		boolean canBuy = false;
		if (item != null) {
			String description = reward.mDescription;
			if (description != null) {
				TextColor namedTextColor
					= reward.mDescriptionColor == null ? NamedTextColor.WHITE : reward.mDescriptionColor;
				ItemUtils.modifyMeta(item, meta ->
					GUIUtils.splitLoreLine(meta, description, namedTextColor, 30, false));
			}

			// Current pass gets no additional lore about purchasing the item
			if (!isCurrentPass) {
				if (isFuturePass) {
					ItemUtils.modifyMeta(item, meta -> {
						List<Component> lore = meta.lore();
						if (lore == null) {
							lore = new ArrayList<>();
						}
						meta.lore(Stream.concat(lore.stream(), Stream.of(
							Component.text("Cannot buy items from this pass", NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false),
							Component.text("until two passes after its release.", NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false)
						)).toList());
					});
				} else if (isOwned) {
					ItemUtils.modifyMeta(item, meta -> {
						Component displayName = meta.displayName();
						if (displayName == null) {
							displayName = Component.empty();
						}
						meta.displayName(displayName.append(Component.text(" (owned)", NamedTextColor.GOLD)
							.decoration(TextDecoration.BOLD, false)));

						List<Component> lore = meta.lore();
						if (lore == null) {
							lore = new ArrayList<>();
						}
						meta.lore(Stream.concat(lore.stream(), Stream.of(
							Component.text("You already own this.", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false)
						)).toList());
					});
				} else if (isPreviousPass) {
					ItemUtils.modifyMeta(item, meta -> {
						List<Component> lore = meta.lore();
						if (lore == null) {
							lore = new ArrayList<>();
						}
						meta.lore(Stream.concat(lore.stream(), Stream.of(
							Component.text("Cannot buy items from this pass", NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false),
							Component.text("until the current pass ends.", NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false)
						)).toList());
					});
				} else if (reward.mCost >= 0) {
					canBuy = true;
					ItemUtils.modifyMeta(item, meta -> {
						List<Component> lore = meta.lore();
						if (lore == null) {
							lore = new ArrayList<>();
						}
						meta.lore(Stream.concat(lore.stream(), Stream.of(
							Component.text("Click to buy.", NamedTextColor.WHITE)
								.decoration(TextDecoration.ITALIC, false),
							Component.text("Cost: ", NamedTextColor.WHITE)
								.decoration(TextDecoration.ITALIC, false)
								.append(Component.text(reward.mCost, NamedTextColor.GOLD, TextDecoration.BOLD))
								.append(Component.space())
								.append(Objects.requireNonNull(mMetamorphosisToken.getItemMeta().displayName()))
								.append(Component.text(reward.mCost == 1 ? "" : "s", NamedTextColor.GOLD)))
						).toList());
					});
				} // Items with a negative cost cannot be purchased
			}
		} else if (lootTableItem != null) {
			// Item is not a type that can be repurchased
			item = lootTableItem;
		} else {
			// Item is not a type that can be repurchased
			Material displayItem = reward.mDisplayItem;
			if (displayItem == null) {
				displayItem = Material.STONE;
			}
			item = new ItemStack(displayItem, 1);
			ItemMeta meta = item.getItemMeta();
			String rewardName = reward.mName;
			if (rewardName == null) {
				rewardName = "Reward name not set";
			}
			meta.displayName(Component.text(rewardName, reward.mNameColor, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			String description = reward.mDescription;
			if (description == null) {
				description = "Description not set";
			}
			TextColor namedTextColor = reward.mDescriptionColor;
			if (namedTextColor == null) {
				namedTextColor = NamedTextColor.WHITE;
			}
			GUIUtils.splitLoreLine(meta, description, namedTextColor, 30, false);
			item.setItemMeta(meta);
		}

		boolean finalCanBuy = canBuy;
		setItem(y, x, item)
			.onLeftClick(() -> {
				if (isOwned) {
					return;
				}
				if (buy == null) {
					mPlayer.playSound(mPlayer, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1, 1);
					return;
				}
				if (!mPlayer.getUniqueId().equals(mDisplayedPlayerId)) {
					mPlayer.sendMessage(Component.text("You cannot buy from someone else's pass",
						NamedTextColor.RED));
					mPlayer.playSound(mPlayer, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1, 1);
					return;
				}
				if (!finalCanBuy) {
					mPlayer.playSound(mPlayer, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1, 1);
					return;
				}
				if (mPlayer.getInventory().containsAtLeast(mMetamorphosisToken, reward.mCost)) {
					ItemStack toRemove = ItemUtils.clone(mMetamorphosisToken);
					toRemove.setAmount(reward.mCost);
					mPlayer.getInventory().removeItem(toRemove);
					buy.run();
					mPlayer.playSound(mPlayer, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 2);
					update();
				} else {
					mPlayer.sendMessage(Component.text("You don't have enough [", NamedTextColor.RED)
						.append(Component.empty()
							.hoverEvent(mMetamorphosisToken)
							.append(Objects.requireNonNull(mMetamorphosisToken.getItemMeta().displayName()))
							.append(Component.text("s")))
						.append(Component.text("]!")));
					mPlayer.playSound(mPlayer, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1, 1);
				}
			});
	}

	public void addMissionIcon(int y,
	                           int x,
	                           List<WeeklyMission> weekMissions,
	                           @Nullable PlayerProgress playerProgress,
	                           int week,
	                           int missionIndex) {
		if (weekMissions.size() <= missionIndex) {
			ItemStack item = new ItemStack(Material.BARRIER);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Mission?", NamedTextColor.RED, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			GUIUtils.splitLoreLine(meta, "Failed to load mission", NamedTextColor.RED, 30, false);
			List<Component> lore = meta.lore();
			if (lore == null) {
				lore = new ArrayList<>();
			}
			lore.add(0, Component.text("Week " + (week + 1), NamedTextColor.GOLD, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			item.setItemMeta(meta);
			setItem(y, x, item);
			return;
		}
		WeeklyMission mission = weekMissions.get(missionIndex);
		String missionDescription = mission.mDescription;
		if (missionDescription == null) {
			missionDescription = "Mission description not set";
		}
		String missionAmount = String.valueOf(mission.mAmount);

		LocalDateTime weekStart = mPass.mPassStart.plus(week, ChronoUnit.WEEKS);
		long weeklyVersion = DateUtils.getWeeklyVersion(weekStart);
		if (weeklyVersion < SeasonalEventManager.PLAYER_PROGRESS_REWORK_WEEK) {
			// Legacy pass missions, no progress saved
			ItemStack item = new ItemStack(Material.LIGHT_GRAY_CONCRETE_POWDER);
			ItemMeta meta = item.getItemMeta();
			GUIUtils.splitLoreLine(meta, missionDescription, NamedTextColor.RED, 30, false);

			List<Component> lore = meta.lore();
			if (lore == null) {
				lore = new ArrayList<>();
			}
			lore.add(0, Component.text("Week " + (week + 1), NamedTextColor.GOLD, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Progress: ?/" + missionAmount, NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
			if (mission.mIsBonus) {
				lore.add(Component.text("Reward: " + mission.mMP + " Bonus MP", NamedTextColor.GOLD)
					.decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text("Reward: " + mission.mMP + " MP", NamedTextColor.GOLD)
					.decoration(TextDecoration.ITALIC, false));
			}
			lore.add(Component.text("Progress Unknown", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));

			meta.lore(lore);
			meta.displayName(Component.text("Mission " + (missionIndex + 1), NamedTextColor.GREEN, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(y, x, item);
			return;
		}

		int openedWeek = mPass.getWeekOfPass(DateUtils.localDateTime(7 * mOpenedEpochWeek)) - 1;

		int progress;
		if (playerProgress == null) {
			progress = 0;
		} else {
			progress = playerProgress.getPassMissionProgress(weekStart, missionIndex).orElse(0);
		}
		String progressStr = String.valueOf(progress);

		Component statusMessage;
		Material mat;
		if (isFuture(weekStart)) {
			statusMessage = Component.text("Future Mission", NamedTextColor.LIGHT_PURPLE)
				.decoration(TextDecoration.ITALIC, false);
			mat = Material.MAGENTA_CONCRETE_POWDER;
			missionDescription = "???";
			progressStr = "?";
			missionAmount = "?";
		} else if (progress < 0) {
			statusMessage = Component.text("Completed", NamedTextColor.GREEN)
				.decoration(TextDecoration.ITALIC, false);
			mat = Material.GREEN_CONCRETE_POWDER;
			progressStr = missionAmount;
		} else if (week < openedWeek) {
			// Previous week, cannot be completed anymore (except by mods)
			statusMessage = Component.text("Incomplete", NamedTextColor.DARK_RED)
				.decoration(TextDecoration.ITALIC, false);
			mat = Material.RED_CONCRETE_POWDER;
		} else {
			statusMessage = Component.text("In progress", NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false);
			mat = Material.YELLOW_CONCRETE_POWDER;
		}

		ItemStack item = new ItemStack(mat, missionIndex + 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Mission " + (missionIndex + 1), NamedTextColor.GREEN, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));
		GUIUtils.splitLoreLine(meta, missionDescription, NamedTextColor.RED, 30, false);

		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.add(0, Component.text("Week " + (week + 1), NamedTextColor.GOLD, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Progress: " + progressStr + "/" + missionAmount, NamedTextColor.DARK_GREEN)
			.decoration(TextDecoration.ITALIC, false));
		if (mission.mIsBonus) {
			lore.add(Component.text("Reward: " + mission.mMP + " Bonus MP", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("Reward: " + mission.mMP + " MP", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		}
		lore.add(statusMessage);
		if (mIsModerator) {
			int modifiedProgress = mModifiedPlayerProgress.getPassMissionProgress(weekStart, missionIndex).orElse(0);
			if (modifiedProgress < 0) {
				modifiedProgress = mission.mAmount;
			}

			lore.add(Component.text("Modified progress: " + modifiedProgress, NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text("Left click")
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": +1 Progress")));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text("Right click")
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": -1 Progress")));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text("Shift left click")
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": +10 Progress")));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text("Shift right click")
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": -10 Progress")));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.keybind(Keybind.SWAP_OFFHAND)
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": Toggle completion")));

			lore.add(Component.empty()
				.color(NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.keybind(Keybind.DROP)
					.decoration(TextDecoration.BOLD, true))
				.append(Component.text(": Restore unmodified progress")));

			lore.add(Component.text("", NamedTextColor.RED, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false)
				.append(Component.text("Confirm changes with top/left icon")));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		GuiItem guiItem = setItem(y, x, item);
		if (mIsModerator) {
			guiItem.onClick((InventoryClickEvent event) -> {
				switch (event.getClick()) {
					case LEFT -> {
						mModifiedPlayerProgress.addPassMissionProgress(weekStart, missionIndex, 1);
						updateWithClickSound();
					}
					case RIGHT -> {
						mModifiedPlayerProgress.addPassMissionProgress(weekStart, missionIndex, -1);
						updateWithClickSound();
					}
					case SHIFT_LEFT -> {
						mModifiedPlayerProgress.addPassMissionProgress(weekStart, missionIndex, 10);
						updateWithClickSound();
					}
					case SHIFT_RIGHT -> {
						mModifiedPlayerProgress.addPassMissionProgress(weekStart, missionIndex, -10);
						updateWithClickSound();
					}
					case SWAP_OFFHAND -> {
						int modifiedProgress = mModifiedPlayerProgress.getPassMissionProgress(weekStart, missionIndex)
							.orElse(0);
						if (modifiedProgress < 0) {
							mModifiedPlayerProgress.setPassMissionProgress(weekStart, missionIndex, 0);
						} else {
							mModifiedPlayerProgress.setPassMissionProgress(weekStart, missionIndex, -1);
						}
						updateWithClickSound();
					}
					case DROP -> {
						mModifiedPlayerProgress.setPassMissionProgress(weekStart, missionIndex, progress);
						updateWithClickSound();
					}
					default -> {
					}
				}
			});
		}
	}

	public static void refreshOpenGuis() {
		// Only refresh GUIs that aren't opened at a different point in time
		long thisWeek = DateUtils.getWeeklyVersion();
		for (PassGui gui : mOpenGuis) {
			SeasonalPass pass = SeasonalEventManager.mAllPasses.get(gui.mPass.mPassStart);
			if (pass == null) {
				gui.mPlayer.sendMessage(Component.text("That pass is no longer available.", NamedTextColor.RED));
				gui.close();
				return;
			}
			gui.mPass = pass;
			if (gui.mOpenedEpochWeek == thisWeek - 1) {
				gui.mOpenedEpochWeek = thisWeek;
			}
			gui.mView = new WeekView(gui);
			gui.mPlayer.playSound(gui.mPlayer,
				Sound.BLOCK_ENCHANTMENT_TABLE_USE,
				SoundCategory.PLAYERS,
				1.0f,
				Constants.Note.C4.mPitch);
			gui.update();
		}
	}
}
