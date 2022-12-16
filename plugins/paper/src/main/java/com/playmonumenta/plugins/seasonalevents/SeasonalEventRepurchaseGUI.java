package com.playmonumenta.plugins.seasonalevents;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.Nullable;

public class SeasonalEventRepurchaseGUI extends Gui {

	// cost of rewards, in Metamorphosis Tokens
	private static final int[] DUMMY_COSTS = {2, 3};
	private static final ImmutableMap<CosmeticType, int[]> COSMETIC_COSTS = ImmutableMap.of(
		CosmeticType.TITLE, new int[] {1, 1, 2, 2, 3, 3},
		CosmeticType.ELITE_FINISHER, new int[] {2, 3, 4},
		CosmeticType.PLOT_BORDER, new int[] {5}
	);

	private final DateTimeFormatter mDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy");

	private @Nullable SeasonalPass mSelectedPass;

	private final ItemStack mMetamorphosisToken;

	public SeasonalEventRepurchaseGUI(Player player) {
		super(player, 6 * 9, Component.text("Season Passes", NamedTextColor.GOLD));
		mMetamorphosisToken = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(SeasonalPass.ITEM_SKIN_KEY));
		setFiller(Material.GRAY_STAINED_GLASS_PANE);
	}

	@Override
	protected void setup() {
		if (mSelectedPass == null) {
			setTitle(Component.text("Season Passes", NamedTextColor.GOLD));
			ItemStack back = ItemUtils.modifyMeta(new ItemStack(Material.ARROW),
				meta -> meta.displayName(Component.text("Back to current Season Pass").decoration(TextDecoration.ITALIC, false)));
			setItem(0, 0, back)
				.onLeftClick(() -> {
					mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
					new SeasonalEventGUI(SeasonalEventManager.mActivePass, mPlayer).open();
				});

			int i = 0;
			for (SeasonalPass pass : SeasonalEventManager.mAllPasses.values()) {
				if (pass.getWeekOfPass() <= pass.mNumberOfWeeks) {
					// current or future pass, skip
					continue;
				}
				ItemStack passItem = ItemUtils.modifyMeta(new ItemStack(pass.mDisplayItem),
					meta -> {
						meta.displayName(Component.text(pass.mName, pass.mNameColor).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
						meta.lore(List.of(Component.text("Ran from " + pass.mPassStart.format(mDateFormat)
							                                 + " to " + pass.mPassStart.plus(pass.mNumberOfWeeks, ChronoUnit.WEEKS).minus(1, ChronoUnit.DAYS).format(mDateFormat), NamedTextColor.WHITE)
							                  .decoration(TextDecoration.ITALIC, false)));
					});
				setItem(2 + (i / 8), 1 + (i % 8), passItem)
					.onLeftClick(() -> {
						mSelectedPass = pass;
						mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
						update();
					});
				i++;
			}
		} else {
			setTitle(Component.text(mSelectedPass.mName, mSelectedPass.mNameColor));
			ItemStack back = ItemUtils.modifyMeta(new ItemStack(Material.ARROW),
				meta -> meta.displayName(Component.text("Back to Overview").decoration(TextDecoration.ITALIC, false)));
			setItem(0, 0, back)
				.onLeftClick(() -> {
					mSelectedPass = null;
					mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
					update();
				});
			// can only but from passes that aren't the previous one
			boolean canBuy = SeasonalEventManager.mActivePass != null
				                 && mSelectedPass.mPassStart.plus(mSelectedPass.mNumberOfWeeks + 1, ChronoUnit.WEEKS).isBefore(SeasonalEventManager.mActivePass.mPassStart);

			int i = 0;
			int dummiesSoFar = 0;
			Map<CosmeticType, Integer> rewardsSoFar = new HashMap<>();
			for (SeasonalReward reward : mSelectedPass.mRewards) {
				boolean owned;
				int cost;
				ItemStack icon;
				Runnable buy;
				if (reward.mType == SeasonalRewardType.LOOT_TABLE
					    && reward.mLootTable.getItemMeta() instanceof SpawnEggMeta) {
					owned = false; // can buy as many as you want
					cost = DUMMY_COSTS[Math.min(dummiesSoFar, DUMMY_COSTS.length - 1)];
					dummiesSoFar++;
					icon = ItemUtils.clone(reward.mLootTable);
					buy = () -> {
						InventoryUtils.giveItem(mPlayer, ItemUtils.clone(reward.mLootTable));
						mPlayer.sendMessage(Component.text("Bought a " + ItemUtils.getPlainName(reward.mLootTable) + ".", NamedTextColor.GOLD));
					};
				} else {
					CosmeticType cosmeticType = switch (reward.mType) {
						case TITLE -> CosmeticType.TITLE;
						case ELITE_FINISHER -> CosmeticType.ELITE_FINISHER;
						case PLOT_BORDER -> CosmeticType.PLOT_BORDER;
						default -> null;
					};
					if (cosmeticType == null) {
						continue;
					}
					int soFar = rewardsSoFar.merge(cosmeticType, 1, Integer::sum);
					int[] costs = COSMETIC_COSTS.get(cosmeticType);
					cost = costs[Math.min(soFar - 1, costs.length - 1)];
					owned = CosmeticsManager.getInstance().playerHasCosmetic(mPlayer, cosmeticType, reward.mData);
					icon = ItemUtils.modifyMeta(new ItemStack(reward.mDisplayItem),
						meta -> meta.displayName(Component.text(reward.mName, reward.mNameColor).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)));
					buy = () -> {
						CosmeticsManager.getInstance().addCosmetic(mPlayer, cosmeticType, reward.mData);
						mPlayer.sendMessage(Component.text("Unlocked " + cosmeticType.getDisplayName() + " '" + reward.mData + "'.", NamedTextColor.GOLD));
					};
				}
				if (reward.mDescription != null) {
					ItemUtils.modifyMeta(icon, meta -> GUIUtils.splitLoreLine(meta, reward.mDescription, 30, GUIUtils.namedTextColorToChatColor(reward.mDescriptionColor), false));
				}
				if (owned) {
					ItemUtils.modifyMeta(icon, meta -> meta.displayName(meta.displayName().append(Component.text(" (owned)", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false))));
					ItemUtils.modifyMeta(icon, meta -> meta.lore(Stream.concat(meta.lore().stream(), Stream.of(
						Component.text("You already own this.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					)).toList()));
				} else if (!canBuy) {
					ItemUtils.modifyMeta(icon, meta -> meta.lore(Stream.concat(meta.lore().stream(), Stream.of(
						Component.text("Cannot buy items from this pass", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
						Component.text("until the current pass ends.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
					)).toList()));
				} else {
					ItemUtils.modifyMeta(icon, meta -> meta.lore(Stream.concat(meta.lore().stream(), Stream.of(
						Component.text("Click to buy.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
						Component.text("Cost: ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
							.append(Component.text(cost, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
							.append(Component.text(" " + ItemUtils.getPlainName(mMetamorphosisToken) + (cost == 1 ? "" : "s"), NamedTextColor.GOLD)))
					).toList()));
				}
				setItem(2 + (i / 7), 1 + (i % 7), icon)
					.onLeftClick(() -> {
						if (owned) {
							return;
						}
						if (!canBuy) {
							mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
							return;
						}
						if (mPlayer.getInventory().containsAtLeast(mMetamorphosisToken, cost)) {
							ItemStack toRemove = ItemUtils.clone(mMetamorphosisToken);
							toRemove.setAmount(cost);
							mPlayer.getInventory().removeItem(toRemove);
							buy.run();
							mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 2);
							update();
						} else {
							mPlayer.sendMessage(Component.text("You don't have enough " + ItemUtils.getPlainName(mMetamorphosisToken) + "s!", NamedTextColor.RED));
							mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
						}
					});
				i++;
			}
		}
	}

}
