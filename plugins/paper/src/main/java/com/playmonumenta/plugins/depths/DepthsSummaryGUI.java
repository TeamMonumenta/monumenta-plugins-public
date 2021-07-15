package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class DepthsSummaryGUI extends CustomInventory {
	public static final ArrayList<Integer> HEAD_LOCATIONS = new ArrayList<Integer>(Arrays.asList(46, 48, 50, 52));
	private static final int START_OF_PASSIVES = 18;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	class TriggerData {
		int mInvLocation;
		DepthsTrigger mTrigger;
		String mString;

		TriggerData(int location, DepthsTrigger trigger, String desc) {
			mInvLocation = location;
			mTrigger = trigger;
			mString = desc;
		}
	}

	public static List<TriggerData> TRIGGER_STRINGS = new ArrayList<TriggerData>();

	public DepthsSummaryGUI(Player player) {
		this(player, player);
	}

	public DepthsSummaryGUI(Player requestingPlayer, Player targetPlayer) {
		super(requestingPlayer, 54, "Current Abilities");

		TRIGGER_STRINGS.add(new TriggerData(0, DepthsTrigger.WEAPON_ASPECT, "No Weapon Aspect!"));
		TRIGGER_STRINGS.add(new TriggerData(2, DepthsTrigger.COMBO, "No Combo ability!"));
		TRIGGER_STRINGS.add(new TriggerData(3, DepthsTrigger.RIGHT_CLICK, "No Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(4, DepthsTrigger.SHIFT_LEFT_CLICK, "No Sneak Left Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(5, DepthsTrigger.SHIFT_RIGHT_CLICK, "No Sneak Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(6, DepthsTrigger.SPAWNER, "No Spawner Break ability!"));
		TRIGGER_STRINGS.add(new TriggerData(7, DepthsTrigger.SHIFT_BOW, "No Sneak Bow ability!"));
		TRIGGER_STRINGS.add(new TriggerData(8, DepthsTrigger.SWAP, "No Swap ability!"));

		for (int i = 0; i < 54; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(targetPlayer.getUniqueId());

		if (playerInstance != null) {
			DepthsParty playerParty = DepthsManager.getInstance().getPartyFromId(playerInstance);
			for (int i = 0; i < playerParty.mPlayersInParty.size(); i++) {
				if (playerParty.mPlayersInParty.get(i) != null &&
						Bukkit.getPlayer(playerParty.mPlayersInParty.get(i).mPlayerId) != null &&
						Bukkit.getPlayer(playerParty.mPlayersInParty.get(i).mPlayerId).isOnline()) {
					ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
					SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
					OfflinePlayer targetedPlayer = Bukkit.getPlayer(playerParty.mPlayersInParty.get(i).mPlayerId);
					meta.setOwningPlayer(targetedPlayer);
					playerHead.setItemMeta(meta);
					_inventory.setItem(HEAD_LOCATIONS.get(i), playerHead);
				}
			}
		}
		setAbilities(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != _inventory) {
			event.getWhoClicked().closeInventory();
			return;
		}
		if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
			SkullMeta chosenMeta = (SkullMeta) event.getCurrentItem().getItemMeta();
			OfflinePlayer chosenPlayer = chosenMeta.getOwningPlayer();
			if (chosenPlayer.isOnline()) {
				setAbilities((Player) chosenPlayer);
				return;
			}
		} else {
			event.getWhoClicked().closeInventory();
		}
	}

	public Boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}

		for (int i = 0; i < 45; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}

		List<DepthsAbilityItem> passiveItems = new ArrayList<DepthsAbilityItem>();
		for (DepthsAbilityItem item : items) {
			if (item.mTrigger == DepthsTrigger.PASSIVE) {
				passiveItems.add(item);
			} else {
				for (TriggerData data : TRIGGER_STRINGS) {
					if (data.mTrigger == item.mTrigger) {
						_inventory.setItem(data.mInvLocation, item.mItem);
						break;
					}
				}
			}
		}

		for (int i = 0; i < passiveItems.size() && i < 18; i++) {
			_inventory.setItem(i + START_OF_PASSIVES, passiveItems.get(i).mItem);
		}

		//all for mystery box
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(targetPlayer.getUniqueId());
		if (playerInstance != null && playerInstance.mHasWeaponAspect &&
				_inventory.getItem(0) != null && _inventory.getItem(0).getType() == FILLER) {
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
			_inventory.setItem(0, mysteryBox);
		}

		for (int i = 0; i <= 8; i++) {
			if (_inventory.getItem(i) != null && _inventory.getItem(i).getType() == FILLER) {
				ItemStack noAbility = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
				ItemMeta noAbilityMeta = noAbility.getItemMeta();
				for (TriggerData data : TRIGGER_STRINGS) {
					if (data.mInvLocation == i) {
						noAbilityMeta.displayName(Component.text(data.mString, NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false));
						noAbility.setItemMeta(noAbilityMeta);
						_inventory.setItem(i, noAbility);
					}
				}
			}
		}

		for (int location : HEAD_LOCATIONS) {
			ItemStack headItem = _inventory.getItem(location);
			if (headItem != null && headItem.getType() == Material.PLAYER_HEAD) {
				SkullMeta chosenMeta = (SkullMeta) headItem.getItemMeta();
				OfflinePlayer chosenPlayer = chosenMeta.getOwningPlayer();
				if (chosenPlayer.equals(targetPlayer)) {
					_inventory.setItem(location - 9, new ItemStack(Material.GREEN_STAINED_GLASS_PANE));
				} else {
					_inventory.setItem(location - 9, new ItemStack(Material.ORANGE_STAINED_GLASS_PANE));
				}
			}
		}
		return true;
	}
}
