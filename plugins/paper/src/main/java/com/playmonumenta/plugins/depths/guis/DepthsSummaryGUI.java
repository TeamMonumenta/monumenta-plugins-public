package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsListener;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.prismatic.Convergence;
import com.playmonumenta.plugins.guis.AbilityTriggersGui;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DepthsSummaryGUI extends Gui {
	public static final List<Integer> HEAD_LOCATIONS = new ArrayList<>(Arrays.asList(48, 50, 47, 51, 46, 52, 45, 53));
	public static final List<Integer> TREE_LOCATIONS = new ArrayList<>(Arrays.asList(3, 5, 2, 6, 1, 7, 0, 8));
	private static final int START_OF_PASSIVES = 27;
	private static final int PASSIVES_PER_PAGE = 18;
	private static final int REWARD_LOCATION = 49;
	private static final int TRIGGER_GUI_LOCATION = 53;
	private boolean mDebugVersion = false;
	private @Nullable DepthsParty mDepthsParty;
	private final DepthsPlayer mRequestingPlayer;
	private DepthsPlayer mTargetPlayer;
	private int mPage = 0;

	public static final Map<Integer, DepthsTrigger> TRIGGER_MAP = new HashMap<>();

	public DepthsSummaryGUI(Player player) {
		this(player, player);
	}

	public DepthsSummaryGUI(Player requestingPlayer, Player targetPlayer) {
		super(requestingPlayer, 54, "Current Abilities");
		if (!requestingPlayer.getUniqueId().equals(targetPlayer.getUniqueId())) {
			mDebugVersion = true;
		}

		TRIGGER_MAP.put(9, DepthsTrigger.WEAPON_ASPECT);
		TRIGGER_MAP.put(10, DepthsTrigger.COMBO);
		TRIGGER_MAP.put(11, DepthsTrigger.RIGHT_CLICK);
		TRIGGER_MAP.put(12, DepthsTrigger.SHIFT_LEFT_CLICK);
		TRIGGER_MAP.put(13, DepthsTrigger.SHIFT_RIGHT_CLICK);
		TRIGGER_MAP.put(14, DepthsTrigger.WILDCARD);
		TRIGGER_MAP.put(15, DepthsTrigger.SHIFT_BOW);
		TRIGGER_MAP.put(16, DepthsTrigger.SWAP);
		TRIGGER_MAP.put(17, DepthsTrigger.LIFELINE);

		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(targetPlayer);

		if (depthsPlayer != null) {
			DepthsParty playerParty = DepthsManager.getInstance().getPartyFromId(depthsPlayer);
			if (playerParty != null && playerParty.mPlayersInParty != null) {
				mDepthsParty = playerParty;
			}
		} else {
			throw new IllegalArgumentException("Player " + targetPlayer.getName() + " not in depths system!");
		}
		mRequestingPlayer = depthsPlayer;
		mTargetPlayer = depthsPlayer; // Is changed later, but always starts as the viewer
	}

	@Override
	public void setup() {
		Player targetPlayer = mTargetPlayer.getPlayer();
		if (targetPlayer == null) {
			return;
		}
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer, mTargetPlayer);

		//First- check if the player has any rewards to open
		ItemStack rewardItem;
		int rewards = mRequestingPlayer.mEarnedRewards.size();
		if (rewards > 0) {
			String s = rewards > 1 ? "s" : "";
			rewardItem = GUIUtils.createBasicItem(Material.GOLD_INGOT, rewards, "Claim your Room Reward" + s + "!", NamedTextColor.YELLOW);
		} else {
			rewardItem = GUIUtils.createBasicItem(Material.GOLD_NUGGET, "All Room Rewards Claimed!", NamedTextColor.YELLOW);
		}
		setItem(REWARD_LOCATION, new GuiItem(rewardItem).onClick(event -> {
			if (!mDebugVersion && !mRequestingPlayer.mEarnedRewards.isEmpty()) {
				close();
				DepthsManager.getInstance().getRoomReward(mPlayer, null, true);
			}
		}));

		//Set actives, save passives for future
		List<DepthsAbilityItem> passiveItems = new ArrayList<>();
		boolean hasConvergence = mTargetPlayer.hasAbility(Convergence.ABILITY_NAME);
		for (DepthsAbilityItem item : items) {
			// if ability is wildcard and not convergence, add to passive list for display if player has convergence
			if (item.mTrigger == DepthsTrigger.WILDCARD
				&& hasConvergence
				&& !Convergence.ABILITY_NAME.equals(item.mAbility)) {
				passiveItems.add(item);
				continue;
			}
			if (item.mTrigger == DepthsTrigger.PASSIVE) {
				passiveItems.add(item);
			} else {
				for (Map.Entry<Integer, DepthsTrigger> slot : TRIGGER_MAP.entrySet()) {
					if (slot.getValue() == item.mTrigger) {
						setItem(slot.getKey(), item.getItem(targetPlayer));
						break;
					}
				}
			}
		}

		int startIndex = mPage * PASSIVES_PER_PAGE;
		for (int i = 0; i < passiveItems.size() - startIndex && i < PASSIVES_PER_PAGE; i++) {
			setItem(i + START_OF_PASSIVES, passiveItems.get(i + startIndex).getItem(targetPlayer));
		}

		if (mPage > 0) {
			ItemStack backItem = GUIUtils.createBasicItem(Material.ARROW, "Previous", NamedTextColor.GRAY);
			setItem(START_OF_PASSIVES - 9, new GuiItem(backItem).onClick(event -> {
				mPage--;
				update();
			}));
		}

		if (passiveItems.size() > PASSIVES_PER_PAGE + startIndex) {
			ItemStack forwardItem = GUIUtils.createBasicItem(Material.ARROW, "Next", NamedTextColor.GRAY);
			setItem(START_OF_PASSIVES - 1, new GuiItem(forwardItem).onClick(event -> {
				mPage++;
				update();
			}));
		}

		//Tree info
		int treeIndex = 0;
		for (DepthsTree tree : mTargetPlayer.mEligibleTrees) {
			setItem(TREE_LOCATIONS.get(treeIndex++), tree.createItem());
		}

		//Place the "no active" glass panes
		for (int i = 9; i <= 17; i++) {
			if (getItem(i) == null) {
				DepthsTrigger trigger = TRIGGER_MAP.get(i);
				if (trigger != null) {
					setItem(i, trigger.getNoAbilityItem(mTargetPlayer));
				}
			}
		}
		updatePlayerHeads();

		ItemStack triggersItem = GUIUtils.createBasicItem(Material.JIGSAW, "Change Ability Triggers", NamedTextColor.WHITE, false,
			"Click here to change which key combinations are used to cast abilities.", NamedTextColor.LIGHT_PURPLE);
		setItem(TRIGGER_GUI_LOCATION, new GuiItem(triggersItem).onClick(event -> new AbilityTriggersGui(mPlayer, false).open()));
	}

	private void updatePlayerHeads() {
		if (mDepthsParty == null) {
			return;
		}
		for (int i = 0; i < mDepthsParty.mPlayersInParty.size(); i++) {
			DepthsPlayer dp = mDepthsParty.mPlayersInParty.get(i);
			if (dp == null) {
				return;
			}
			Player player = dp.getPlayer();
			if (player != null && player.isOnline()) {
				String itemName = player.getName() + "'s Abilities";
				ItemStack playerHead = GUIUtils.createBasicItem(Material.PLAYER_HEAD, itemName, NamedTextColor.YELLOW, "Grave Timer: " + DepthsListener.getGraveDuration(mDepthsParty, dp, player) / 20.0 + "s");
				GUIUtils.setSkullOwner(playerHead, player);

				if (dp == mTargetPlayer) {
					ItemStack activePlayerIndicator = GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, itemName, NamedTextColor.YELLOW, false, "Currently Shown\nGrave Timer: " + DepthsListener.getGraveDuration(mDepthsParty, dp, player) / 20.0 + "s", NamedTextColor.GRAY);
					setItem(HEAD_LOCATIONS.get(i), activePlayerIndicator);
					setItem(4, playerHead);
				} else {
					setItem(HEAD_LOCATIONS.get(i), new GuiItem(playerHead).onClick(event -> {
						mTargetPlayer = dp;
						mPage = 0;
						update();
					}));
				}
			}
		}
	}
}
