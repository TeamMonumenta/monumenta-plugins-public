package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.guis.AbilityTriggersGui;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class DepthsSummaryGUI extends CustomInventory {
	public static final ArrayList<Integer> HEAD_LOCATIONS = new ArrayList<>(Arrays.asList(47, 48, 50, 51, 46, 52, 45, 53));
	public static final ArrayList<Integer> TREE_LOCATIONS = new ArrayList<>(Arrays.asList(2, 3, 5, 6, 1, 7, 0, 8));
	private static final int START_OF_PASSIVES = 27;
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private static final int REWARD_LOCATION = 49;
	private static final int TRIGGER_GUI_LOCATION = 53;
	private boolean mDebugVersion = false;
	private @Nullable DepthsParty mDepthsParty;
	private final DepthsPlayer mRequestingPlayer;

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
		TRIGGER_MAP.put(14, DepthsTrigger.SPAWNER);
		TRIGGER_MAP.put(15, DepthsTrigger.SHIFT_BOW);
		TRIGGER_MAP.put(16, DepthsTrigger.SWAP);
		TRIGGER_MAP.put(17, DepthsTrigger.LIFELINE);

		GUIUtils.fillWithFiller(mInventory, true);
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

		setAbilities(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getClickedInventory() != mInventory ||
			    event.getCurrentItem() == null ||
			    event.getCurrentItem().getType() == FILLER) {
			return;
		}
		Player clicker = (Player) event.getWhoClicked();
		if (event.getCurrentItem().getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() instanceof Player chosenPlayer && chosenPlayer.isOnline()) {
			setAbilities(chosenPlayer);
			return;
		}
		if (event.getSlot() == REWARD_LOCATION && !mDebugVersion) {
			DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(clicker);
			if (depthsPlayer != null && depthsPlayer.mEarnedRewards.size() > 0) {
				clicker.closeInventory();
				DepthsManager.getInstance().getRoomReward(clicker, null, true);
			}
		}
		if (event.getSlot() == TRIGGER_GUI_LOCATION) {
			new AbilityTriggersGui(clicker, false).open();
		}
	}

	public boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}

		GUIUtils.fillWithFiller(mInventory, true);

		//First- check if the player has any rewards to open
		ItemStack rewardItem;
		int rewards = mRequestingPlayer.mEarnedRewards.size();
		if (rewards > 0) {
			String s = rewards > 1 ? "s" : "";
			rewardItem = GUIUtils.createBasicItem(Material.GOLD_INGOT, rewards, "Claim your Room Reward" + s + "!", NamedTextColor.YELLOW);
		} else {
			rewardItem = GUIUtils.createBasicItem(Material.GOLD_NUGGET, "All Room Rewards Claimed!", NamedTextColor.YELLOW);
		}
		mInventory.setItem(REWARD_LOCATION, rewardItem);

		//Set actives, save passives for future
		List<DepthsAbilityItem> passiveItems = new ArrayList<>();
		for (DepthsAbilityItem item : items) {
			if (item.mTrigger == DepthsTrigger.PASSIVE) {
				passiveItems.add(item);
			} else {
				for (Map.Entry<Integer, DepthsTrigger> slot : TRIGGER_MAP.entrySet()) {
					if (slot.getValue() == item.mTrigger) {
						mInventory.setItem(slot.getKey(), item.mItem);
						break;
					}
				}
			}
		}

		//Lay out all passives
		for (int i = 0; i < passiveItems.size() && i < 18; i++) {
			mInventory.setItem(i + START_OF_PASSIVES, passiveItems.get(i).mItem);
		}

		//all for mystery box
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(targetPlayer);
		ItemStack weaponAspectItem = mInventory.getItem(9);
		if (depthsPlayer != null && depthsPlayer.mHasWeaponAspect &&
				weaponAspectItem != null && weaponAspectItem.getType() == FILLER) {
			//TODO this probably never triggers - the lore text isn't what it should be and no one has ever reported it
			ItemStack mysteryBox = GUIUtils.createBasicItem(Material.BARREL, "Mystery Box", NamedTextColor.WHITE, true, "Obtain a random ability.", NamedTextColor.WHITE);
			mInventory.setItem(9, mysteryBox);
		}

		//Tree info
		if (depthsPlayer != null) {
			int i = 0;
			for (DepthsTree tree : depthsPlayer.mEligibleTrees) {
				mInventory.setItem(TREE_LOCATIONS.get(i++), tree.createItem());
			}
		}

		//Place the "no active" glass panes
		for (int i = 9; i <= 17; i++) {
			ItemStack triggerItem = mInventory.getItem(i);
			if (triggerItem != null && triggerItem.getType() == FILLER) {
				DepthsTrigger trigger = TRIGGER_MAP.get(i);
				if (trigger != null) {
					mInventory.setItem(i, trigger.getNoAbilityItem());
				}
			}
		}
		updatePlayerHeads(targetPlayer);

		ItemStack triggersItem = GUIUtils.createBasicItem(Material.JIGSAW, "Change Ability Triggers", NamedTextColor.WHITE, false,
			"Click here to change which key combinations are used to cast abilities.", NamedTextColor.LIGHT_PURPLE);
		mInventory.setItem(TRIGGER_GUI_LOCATION, triggersItem);
		return true;
	}

	private void updatePlayerHeads(Player targetPlayer) {
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
				ItemStack playerHead = GUIUtils.createBasicItem(Material.PLAYER_HEAD, itemName, NamedTextColor.YELLOW);
				GUIUtils.setSkullOwner(playerHead, player);

				if (player == targetPlayer) {
					ItemStack activePlayerIndicator = GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, itemName, NamedTextColor.YELLOW, false, "Currently Shown", NamedTextColor.GRAY);
					mInventory.setItem(HEAD_LOCATIONS.get(i), activePlayerIndicator);

					mInventory.setItem(4, playerHead);
				} else {
					mInventory.setItem(HEAD_LOCATIONS.get(i), playerHead);
				}
			}
		}
	}
}
