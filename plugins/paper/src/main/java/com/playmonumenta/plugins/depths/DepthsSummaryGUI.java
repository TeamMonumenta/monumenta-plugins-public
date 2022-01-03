package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DepthsSummaryGUI extends CustomInventory {
	public static final ArrayList<Integer> HEAD_LOCATIONS = new ArrayList<>(Arrays.asList(45, 48, 50, 53));
	public static final ArrayList<Integer> TREE_LOCATIONS = new ArrayList<>(Arrays.asList(2, 3, 5, 6));
	private static final int START_OF_PASSIVES = 27;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int REWARD_LOCATION = 49;
	private Boolean mDebugVersion = false;

	static class TriggerData {
		int mInvLocation;
		DepthsTrigger mTrigger;
		String mString;

		TriggerData(int location, DepthsTrigger trigger, String desc) {
			mInvLocation = location;
			mTrigger = trigger;
			mString = desc;
		}
	}

	public static final List<TriggerData> TRIGGER_STRINGS = new ArrayList<>();

	public DepthsSummaryGUI(Player player) {
		this(player, player);
	}

	public DepthsSummaryGUI(Player requestingPlayer, Player targetPlayer) {
		super(requestingPlayer, 54, "Current Abilities");
		if (requestingPlayer.getUniqueId() != targetPlayer.getUniqueId()) {
			mDebugVersion = true;
		}

		TRIGGER_STRINGS.add(new TriggerData(9, DepthsTrigger.WEAPON_ASPECT, "No Weapon Aspect!"));
		TRIGGER_STRINGS.add(new TriggerData(10, DepthsTrigger.COMBO, "No Combo ability!"));
		TRIGGER_STRINGS.add(new TriggerData(11, DepthsTrigger.RIGHT_CLICK, "No Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(12, DepthsTrigger.SHIFT_LEFT_CLICK, "No Sneak Left Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(13, DepthsTrigger.SHIFT_RIGHT_CLICK, "No Sneak Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(14, DepthsTrigger.SPAWNER, "No Spawner Break ability!"));
		TRIGGER_STRINGS.add(new TriggerData(15, DepthsTrigger.SHIFT_BOW, "No Sneak Bow ability!"));
		TRIGGER_STRINGS.add(new TriggerData(16, DepthsTrigger.SWAP, "No Swap ability!"));
		TRIGGER_STRINGS.add(new TriggerData(17, DepthsTrigger.LIFELINE, "No Lifeline ability!"));

		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(targetPlayer.getUniqueId());

		if (playerInstance != null) {
			DepthsParty playerParty = DepthsManager.getInstance().getPartyFromId(playerInstance);
			if (playerParty != null && playerParty.mPlayersInParty != null) {
				for (int i = 0; i < playerParty.mPlayersInParty.size(); i++) {
					DepthsPlayer player = playerParty.mPlayersInParty.get(i);
					if (player == null) {
						return;
					}
					Player actualPlayer = Bukkit.getPlayer(player.mPlayerId);
					if (actualPlayer != null && actualPlayer.isOnline()) {
						ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
						meta.setOwningPlayer(actualPlayer);
						meta.displayName(Component.text(actualPlayer.getName() + "'s Abilities", NamedTextColor.YELLOW)
							.decoration(TextDecoration.ITALIC, false));
						playerHead.setItemMeta(meta);
						mInventory.setItem(HEAD_LOCATIONS.get(i), playerHead);
					}
				}
			}
		}
		DepthsPlayer playerWhoAsked = DepthsManager.getInstance().mPlayers.get(targetPlayer.getUniqueId());


		//First- check if the player has any rewards to open
		ItemStack rewardItem;
		if (playerWhoAsked.mEarnedRewards.size() > 0) {
			rewardItem = new ItemStack(Material.GOLD_INGOT, playerWhoAsked.mEarnedRewards.size());
			ItemMeta rewardMeta = rewardItem.getItemMeta();
			rewardMeta.displayName(Component.text("Claim your Room Reward!", NamedTextColor.YELLOW)
										.decoration(TextDecoration.ITALIC, false));
			rewardMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			ItemUtils.setPlainName(rewardItem, "Claim your Room Reward!");
			if (playerWhoAsked.mEarnedRewards.size() > 1) {
				rewardMeta.displayName(Component.text("Claim your Room Rewards!", NamedTextColor.YELLOW)
						.decoration(TextDecoration.ITALIC, false));
				rewardMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				ItemUtils.setPlainName(rewardItem, "Claim your Room Rewards!");
			}
			rewardItem.setItemMeta(rewardMeta);
		} else {
			rewardItem = new ItemStack(Material.GOLD_NUGGET, 1);
			ItemMeta rewardMeta = rewardItem.getItemMeta();
			rewardMeta.displayName(Component.text("All Room Rewards Claimed!", NamedTextColor.YELLOW)
										.decoration(TextDecoration.ITALIC, false));
			rewardMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			ItemUtils.setPlainName(rewardItem, "All Room Rewards Claimed!");
			rewardItem.setItemMeta(rewardMeta);
		}
		mInventory.setItem(REWARD_LOCATION, rewardItem);
		setAbilities(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory ||
			    event.getCurrentItem() == null ||
			    event.getCurrentItem().getType() == FILLER) {
			return;
		}
		Player clicker = (Player) event.getWhoClicked();
		if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
			SkullMeta chosenMeta = (SkullMeta) event.getCurrentItem().getItemMeta();
			OfflinePlayer chosenPlayer = chosenMeta.getOwningPlayer();
			if (chosenPlayer != null && chosenPlayer.isOnline()) {
				setAbilities((Player) chosenPlayer);
				return;
			}
		}
		if (event.getSlot() == REWARD_LOCATION && !mDebugVersion) {
			DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(clicker.getUniqueId());
			if (playerInstance != null && playerInstance.mEarnedRewards.size() > 0) {
				event.getWhoClicked().closeInventory();
				DepthsManager.getInstance().getRoomReward((Player) event.getWhoClicked(), null);
			}
		}
	}

	public Boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}

		for (int i = 0; i < 45; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		List<DepthsAbilityItem> passiveItems = new ArrayList<>();
		for (DepthsAbilityItem item : items) {
			if (item.mTrigger == DepthsTrigger.PASSIVE) {
				passiveItems.add(item);
			} else {
				for (TriggerData data : TRIGGER_STRINGS) {
					if (data.mTrigger == item.mTrigger) {
						mInventory.setItem(data.mInvLocation, item.mItem);
						break;
					}
				}
			}
		}

		for (int i = 0; i < passiveItems.size() && i < 18; i++) {
			mInventory.setItem(i + START_OF_PASSIVES, passiveItems.get(i).mItem);
		}

		//all for mystery box
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(targetPlayer.getUniqueId());
		ItemStack weaponAspectItem = mInventory.getItem(9);
		if (playerInstance != null && playerInstance.mHasWeaponAspect &&
				weaponAspectItem != null && weaponAspectItem.getType() == FILLER) {
			ItemStack mysteryBox = new ItemStack(Material.BARREL, 1);
			ItemMeta boxMeta = mysteryBox.getItemMeta();
			boxMeta.displayName(Component.text("Mystery Box", NamedTextColor.WHITE)
								.decoration(TextDecoration.ITALIC, false)
								.decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Obtain a random ability.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false));
			boxMeta.lore(lore);
			mysteryBox.setItemMeta(boxMeta);
			mInventory.setItem(9, mysteryBox);
		}

		//Tree info
		if (playerInstance != null) {
			DepthsTree playerTree;
			for (int i = 0; i < 4; i++) {
				playerTree = playerInstance.mEligibleTrees.get(i);
				mInventory.setItem(TREE_LOCATIONS.get(i), DepthsUtils.getTreeItem(playerTree));
			}
		}

		for (int i = 9; i <= 17; i++) {
			ItemStack triggerItem = mInventory.getItem(i);
			if (triggerItem != null && triggerItem.getType() == FILLER) {
				ItemStack noAbility = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
				ItemMeta noAbilityMeta = noAbility.getItemMeta();
				for (TriggerData data : TRIGGER_STRINGS) {
					if (data.mInvLocation == i) {
						noAbilityMeta.displayName(Component.text(data.mString, NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false));
						noAbility.setItemMeta(noAbilityMeta);
						mInventory.setItem(i, noAbility);
					}
				}
			}
		}

		for (int location : HEAD_LOCATIONS) {
			ItemStack headItem = mInventory.getItem(location);
			if (headItem != null && headItem.getType() == Material.PLAYER_HEAD) {
				SkullMeta chosenMeta = (SkullMeta) headItem.getItemMeta();
				OfflinePlayer chosenPlayer = chosenMeta.getOwningPlayer();
				ItemStack indicatorItem;
				if (chosenPlayer != null && chosenPlayer.equals(targetPlayer)) {
					indicatorItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
				} else {
					indicatorItem = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
				}
				switch (location) {
				case 45, 50 -> mInventory.setItem(location + 1, indicatorItem);
				case 48, 53 -> mInventory.setItem(location - 1, indicatorItem);
				default -> mInventory.setItem(location, indicatorItem);
				}
			}
		}
		return true;
	}
}
