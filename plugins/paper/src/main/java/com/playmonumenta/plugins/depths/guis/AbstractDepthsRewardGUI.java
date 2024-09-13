package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfObscurity;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfRuin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDepthsRewardGUI extends Gui {

	public static final List<List<Integer>> SLOT_MAP = Arrays.asList(
		Arrays.asList(13),
		Arrays.asList(11, 15),
		Arrays.asList(10, 13, 16),
		Arrays.asList(10, 12, 14, 16),
		Arrays.asList(9, 11, 13, 15, 17)
	);

	protected final boolean mReturnToSummary;
	protected final boolean mReroll;

	public AbstractDepthsRewardGUI(Player player, boolean fromSummaryGUI, String title, boolean reroll) {
		super(player, 27, title);
		mReturnToSummary = fromSummaryGUI;
		mReroll = reroll;
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
				item = dai.mItem;
				obscure = false;
			}

			int slot = i;
			setItem(slotsUsed.get(i), new GuiItem(item).onLeftClick(() -> {
				playerClickedItem(slot, obscure);
				close();

				if (dai != null) {
					// Trigger Curse of Ruin - we could disable this on generosity, but I think it's probably fine
					CurseOfRuin ruin = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, CurseOfRuin.class);
					if (ruin != null) {
						ruin.downgrade(depthsPlayer);
					}
				}

				if (!depthsPlayer.mEarnedRewards.isEmpty()) {
					DepthsManager.getInstance().getRoomReward(mPlayer, null, mReturnToSummary);
				} else if (mReturnToSummary) {
					DepthsGUICommands.summary(mPlayer);
				}
			}));
		}

		if (mReroll) {
			int count = depthsPlayer.mRerolls;
			if (count > 0) {
				ItemStack item = GUIUtils.createBasicItem(Material.NAUTILUS_SHELL, count, "Reroll", NamedTextColor.DARK_AQUA, true, Component.text("Click to reroll these options.\n", NamedTextColor.GRAY).append(Component.text("You have " + count + " reroll" + (count > 1 ? "s" : "") + " remaining.")), 30, true);
				setItem(2, 4, new GuiItem(item).onLeftClick(() -> {
					close();
					depthsPlayer.mRerolls--;
					UUID uuid = depthsPlayer.mPlayerId;
					DepthsManager.getInstance().mAbilityOfferings.remove(uuid);
					DepthsManager.getInstance().mUpgradeOfferings.remove(uuid);
					DepthsManager.getInstance().getRoomReward(mPlayer, null, mReturnToSummary);
				}));
			}
		}
	}

	protected abstract @Nullable List<@Nullable DepthsAbilityItem> getOptions();

	protected abstract void playerClickedItem(int slot, boolean sendMessage);
}
