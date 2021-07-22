package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class DepthsRemoveAbilityGUI extends CustomInventory {
	public static final ArrayList<Integer> HEAD_LOCATIONS = new ArrayList<Integer>(Arrays.asList(46, 48, 50, 52));
	private static final int START_OF_PASSIVES = 36;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material CONFIRM_MAT = Material.GREEN_STAINED_GLASS_PANE;
	private static final Material CANCEL_MAT = Material.ORANGE_STAINED_GLASS_PANE;
	private static final int CONFIRM_ABILITY_LOC = 13;

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

	public DepthsRemoveAbilityGUI(Player targetPlayer) {
		super(targetPlayer, 54, "Remove an Ability");

		TRIGGER_STRINGS.add(new TriggerData(19, DepthsTrigger.COMBO, "No Combo ability!"));
		TRIGGER_STRINGS.add(new TriggerData(20, DepthsTrigger.RIGHT_CLICK, "No Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(21, DepthsTrigger.SHIFT_LEFT_CLICK, "No Sneak Left Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(22, DepthsTrigger.SHIFT_RIGHT_CLICK, "No Sneak Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(23, DepthsTrigger.SPAWNER, "No Spawner Break ability!"));
		TRIGGER_STRINGS.add(new TriggerData(24, DepthsTrigger.SHIFT_BOW, "No Sneak Bow ability!"));
		TRIGGER_STRINGS.add(new TriggerData(25, DepthsTrigger.SWAP, "No Swap ability!"));

		for (int i = 0; i < 54; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}

		setAbilities(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != _inventory ||
				event.getCurrentItem().getType() == FILLER) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		DepthsManager instance = DepthsManager.getInstance();

		List<DepthsAbility> abilities = instance.getPlayerAbilities(player);

		if (event.getCurrentItem().getType() == CONFIRM_MAT) {
			for (DepthsAbility ability : abilities) {
				if (_inventory.getItem(CONFIRM_ABILITY_LOC).getItemMeta().getDisplayName().contains(ability.mInfo.mDisplayName)) {
					DepthsPlayer depthsplayer = instance.mPlayers.get(player.getUniqueId());
					if (depthsplayer != null && !depthsplayer.mUsedAbilityDeletion) {
						depthsplayer.mUsedAbilityDeletion = true;
						instance.setPlayerLevelInAbility(ability.getDisplayName(), player, 0);
						event.getWhoClicked().closeInventory();
						MessagingUtils.sendActionBarMessage(player, "Ability removed!");
						return;
					}
				}
			}
		} else if (event.getCurrentItem().getType() == CANCEL_MAT) {
			setAbilities(player);
		} else {
			for (DepthsAbility ability : abilities) {
				if (_inventory.getItem(event.getSlot()).getItemMeta().getDisplayName().contains(ability.mInfo.mDisplayName)) {
					setConfirmation(player, _inventory.getItem(event.getSlot()));
					return;
				}
			}
		}
	}

	public void setConfirmation(Player player, ItemStack item) {
		for (int i = 0; i < _inventory.getSize(); i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}

		_inventory.setItem(CONFIRM_ABILITY_LOC, item);
		ItemStack createItem = createCustomItem(CONFIRM_MAT, "Confirm", "Confirm ability removal");
		_inventory.setItem(29, createItem);
		createItem = createCustomItem(CANCEL_MAT, "Cancel", "Returns to previous page.");
		_inventory.setItem(33, createItem);
	}

	public Boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}

		for (int i = 0; i < _inventory.getSize(); i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}

		ItemStack createItem = createCustomItem(Material.PURPLE_STAINED_GLASS_PANE,
				"Click the ability to remove",
				"Remove 1 ability of your choosing at no cost.");
		_inventory.setItem(4, createItem);

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

		for (int i = 19; i <= 25; i++) {
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

		return true;
	}

	public ItemStack createCustomItem(Material type, String name, String lore) {
		ItemStack newItem = new ItemStack(type, 1);
		ItemMeta meta = newItem.getItemMeta();
		if (name != "") {
			meta.displayName(Component.text(name, NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true));
		}
		ChatColor defaultColor = ChatColor.GRAY;
		if (lore != "") {
			splitLoreLine(meta, lore, 30, defaultColor);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}

	public void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (currentString != defaultColor + "") {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}
}
