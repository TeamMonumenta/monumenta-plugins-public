package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfObscurity;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfRuin;
import com.playmonumenta.plugins.depths.abilities.gifts.BottomlessBowl;
import com.playmonumenta.plugins.depths.abilities.gifts.CombOfSelection;
import com.playmonumenta.plugins.depths.abilities.gifts.RainbowGeode;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDepthsRewardGUI extends Gui {

	public static final List<List<Integer>> SLOT_MAP = Arrays.asList(
		List.of(13),
		Arrays.asList(11, 15),
		Arrays.asList(10, 13, 16),
		Arrays.asList(10, 12, 14, 16),
		Arrays.asList(9, 11, 13, 15, 17)
	);

	protected final boolean mReturnToSummary;
	protected final boolean mRoomReward;

	public AbstractDepthsRewardGUI(Player player, boolean fromSummaryGUI, String title, boolean roomReward) {
		super(player, 27, title);
		mReturnToSummary = fromSummaryGUI;
		mRoomReward = roomReward;
	}

	@Override
	public void setup() {
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (depthsPlayer == null) {
			return;
		}

		List<DepthsAbilityItem> items = getOptions();
		if (items == null || items.isEmpty()) {
			return;
		}

		List<Integer> slotsUsed = SLOT_MAP.get(items.size() - 1);

		for (int i = 0; i < slotsUsed.size(); i++) {
			DepthsAbilityItem dai = items.get(i);
			ItemStack item;
			boolean obscure;
			if (dai == null) {
				item = GUIUtils.createBasicItem(Material.BARRIER, "Do not accept the gift", NamedTextColor.RED, true);
				obscure = false;
			} else if (i == slotsUsed.size() - 1 && depthsPlayer.getLevelInAbility(CurseOfObscurity.ABILITY_NAME) > 0) {
				item = GUIUtils.createBasicItem(Material.ROTTEN_FLESH, DepthsTree.CURSE.color("UnknownChoice").decorate(TextDecoration.OBFUSCATED));
				obscure = true;
			} else {
				item = dai.getItem(mPlayer);
				obscure = false;
			}

			int slot = i;
			setItem(slotsUsed.get(i), new GuiItem(item).onLeftClick(() -> {
				depthsPlayer.mCombOfSelectionLevels.clear();
				playerClickedItem(slot, obscure);
				close();

				if (dai != null) {
					// Trigger Curse of Ruin - we could disable this on generosity, but I think it's probably fine
					if (DepthsManager.getInstance().getPlayerLevelInAbility(CurseOfRuin.ABILITY_NAME, mPlayer) > 0) {
						DepthsManager.getInstance().increaseRandomAbilityLevel(mPlayer, depthsPlayer, -1);
					}
				}

				if (!depthsPlayer.mEarnedRewards.isEmpty()) {
					DepthsManager.getInstance().getRoomReward(mPlayer, null, mReturnToSummary);
				} else if (mReturnToSummary) {
					DepthsGUICommands.summary(mPlayer);
				}
			}));
		}

		if (mRoomReward) {
			int rerolls = depthsPlayer.mRerolls;
			if (rerolls > 0) {
				ItemStack item = GUIUtils.createBasicItem(Material.NAUTILUS_SHELL, rerolls, "Reroll", NamedTextColor.DARK_AQUA, true, Component.text("Click to reroll these options.\n", NamedTextColor.GRAY).append(Component.text("You have " + rerolls + " reroll" + (rerolls > 1 ? "s" : "") + " remaining.")), 30, true);
				setItem(2, 4, new GuiItem(item).onLeftClick(() -> {
					close();
					depthsPlayer.mRerolls--;
					depthsPlayer.mAbilityOfferings = null;
					depthsPlayer.mUpgradeOfferings = null;
					// comb of selection trigger increase rarities
					if (depthsPlayer.hasAbility(CombOfSelection.ABILITY_NAME)) {
						depthsPlayer.mCombOfSelectionLevels.replaceAll(integer -> Math.min(integer + 1, 5));
					}
					DepthsManager.getInstance().getRoomReward(mPlayer, null, mReturnToSummary);
				}));
			}

			if ((depthsPlayer.hasAbility(BottomlessBowl.ABILITY_NAME) || depthsPlayer.hasAbility(RainbowGeode.ABILITY_NAME))
				&& !depthsPlayer.mEarnedRewards.isEmpty() && depthsPlayer.peekRewardType() != DepthsRoomType.DepthsRewardType.CURSE) {
				ItemStack item = GUIUtils.createBasicItem(Material.BARRIER, 1, "Skip", NamedTextColor.RED, true, Component.text("Click to skip these options.", NamedTextColor.GRAY), 30, true);
				setItem(2, 8, new GuiItem(item).onLeftClick(() -> {
					close();
					depthsPlayer.mAbilityOfferings = null;
					depthsPlayer.mUpgradeOfferings = null;
					depthsPlayer.mEarnedRewards.poll();
					depthsPlayer.mRewardSkips++;
					if (depthsPlayer.mRewardSkips % 3 == 0 && depthsPlayer.hasAbility(RainbowGeode.ABILITY_NAME)) {
						depthsPlayer.addReward(DepthsRoomType.DepthsRewardType.PRISMATIC);
						mPlayer.playSound(mPlayer, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
					}
					mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 2.0f);

					if (!depthsPlayer.mEarnedRewards.isEmpty()) {
						DepthsManager.getInstance().getRoomReward(mPlayer, null, mReturnToSummary);
					} else if (mReturnToSummary) {
						DepthsGUICommands.summary(mPlayer);
					}
				}));
			}
		}
	}

	protected abstract @Nullable List<@Nullable DepthsAbilityItem> getOptions();

	protected abstract void playerClickedItem(int slot, boolean sendMessage);
}
