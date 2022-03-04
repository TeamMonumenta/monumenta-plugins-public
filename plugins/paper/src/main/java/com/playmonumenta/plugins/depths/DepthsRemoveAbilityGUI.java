package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DepthsRemoveAbilityGUI extends CustomInventory {
	private static final int START_OF_PASSIVES = 36;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Material CONFIRM_MAT = Material.GREEN_STAINED_GLASS_PANE;
	private static final Material CANCEL_MAT = Material.ORANGE_STAINED_GLASS_PANE;
	private static final int CONFIRM_ABILITY_LOC = 13;

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

	public static List<TriggerData> TRIGGER_STRINGS = new ArrayList<>();

	private String mAbilityName;

	public DepthsRemoveAbilityGUI(Player targetPlayer) {
		super(targetPlayer, 54, "Remove an Ability");

		TRIGGER_STRINGS.add(new TriggerData(18, DepthsTrigger.COMBO, "No Combo ability!"));
		TRIGGER_STRINGS.add(new TriggerData(19, DepthsTrigger.RIGHT_CLICK, "No Right Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(20, DepthsTrigger.SHIFT_LEFT_CLICK, "No Sneak Left Click ability!"));
		TRIGGER_STRINGS.add(new TriggerData(21, DepthsTrigger.SHIFT_RIGHT_CLICK, "No Sneak Right Click ability!"));
		//empty space to be even
		TRIGGER_STRINGS.add(new TriggerData(23, DepthsTrigger.SPAWNER, "No Spawner Break ability!"));
		TRIGGER_STRINGS.add(new TriggerData(24, DepthsTrigger.SHIFT_BOW, "No Sneak Bow ability!"));
		TRIGGER_STRINGS.add(new TriggerData(25, DepthsTrigger.SWAP, "No Swap ability!"));
		TRIGGER_STRINGS.add(new TriggerData(26, DepthsTrigger.LIFELINE, "No Lifeline ability!"));

		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		setAbilities(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory
			    || clickedItem == null
			    || clickedItem.getType() == FILLER) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		DepthsManager instance = DepthsManager.getInstance();

		List<DepthsAbility> abilities = instance.getPlayerAbilities(player);
		if (abilities == null) {
			return;
		}

		if (clickedItem.getType() == CONFIRM_MAT) {
			for (DepthsAbility ability : abilities) {
				if (ability.mInfo.mDisplayName != null && mAbilityName.contains(ability.mInfo.mDisplayName)) {
					DepthsPlayer depthsplayer = instance.mPlayers.get(player.getUniqueId());
					if (depthsplayer != null && !depthsplayer.mUsedAbilityDeletion) {
						depthsplayer.mUsedAbilityDeletion = true;
						instance.setPlayerLevelInAbility(ability.getDisplayName(), player, 0);
						event.getWhoClicked().closeInventory();
						player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.0f);
						MessagingUtils.sendActionBarMessage(player, "Ability removed!");
						return;
					}
				}
			}
		} else if (clickedItem.getType() == CANCEL_MAT) {
			setAbilities(player);
		} else {
			for (DepthsAbility ability : abilities) {
				if (ability.mInfo.mDisplayName != null
					    && ItemUtils.getPlainName(clickedItem).contains(ability.mInfo.mDisplayName)) {
					setConfirmation(clickedItem);
					return;
				}
			}
		}
	}

	public void setConfirmation(ItemStack item) {
		for (int i = 0; i < mInventory.getSize(); i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}
		mAbilityName = ItemUtils.getPlainName(item);

		mInventory.setItem(CONFIRM_ABILITY_LOC, item);
		ItemStack createItem = createCustomItem(CONFIRM_MAT, "Confirm", "Confirm ability removal");
		mInventory.setItem(29, createItem);
		createItem = createCustomItem(CANCEL_MAT, "Cancel", "Returns to previous page.");
		mInventory.setItem(33, createItem);
	}

	public Boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}

		for (int i = 0; i < mInventory.getSize(); i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		ItemStack createItem = createCustomItem(Material.PURPLE_STAINED_GLASS_PANE,
		                                        "Click the ability to remove",
		                                        "Remove 1 ability of your choosing at no cost.");
		mInventory.setItem(4, createItem);

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

		for (int i = 19; i <= 25; i++) {
			ItemStack checkItem = mInventory.getItem(i);
			if (checkItem != null && checkItem.getType() == FILLER) {
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

		return true;
	}

	public ItemStack createCustomItem(Material type, String name, String lore) {
		ItemStack newItem = new ItemStack(type, 1);
		ItemMeta meta = newItem.getItemMeta();
		if (!name.isEmpty()) {
			meta.displayName(Component.text(name, NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false)
					.decoration(TextDecoration.BOLD, true));
		}
		ChatColor defaultColor = ChatColor.GRAY;
		if (!lore.isEmpty()) {
			GUIUtils.splitLoreLine(meta, lore, 30, defaultColor, true);
		}
		newItem.setItemMeta(meta);
		return newItem;
	}
}
