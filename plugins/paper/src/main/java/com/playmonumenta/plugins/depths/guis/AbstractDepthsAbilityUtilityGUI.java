package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDepthsAbilityUtilityGUI extends CustomInventory {
	protected static final int START_OF_PASSIVES = 36;
	protected static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	protected static final Material CONFIRM_MAT = Material.GREEN_STAINED_GLASS_PANE;
	protected static final Material CANCEL_MAT = Material.ORANGE_STAINED_GLASS_PANE;
	private static final int CONFIRM_ABILITY_LOC = 13;

	public static Map<Integer, DepthsTrigger> TRIGGER_MAP = new HashMap<>();

	public final boolean mShowPassives;

	protected @Nullable String mAbilityName;

	public AbstractDepthsAbilityUtilityGUI(Player targetPlayer, String name, boolean showPassives) {
		super(targetPlayer, 54, name);
		mShowPassives = showPassives;

		TRIGGER_MAP.put(18, DepthsTrigger.COMBO);
		TRIGGER_MAP.put(19, DepthsTrigger.RIGHT_CLICK);
		TRIGGER_MAP.put(20, DepthsTrigger.SHIFT_LEFT_CLICK);
		TRIGGER_MAP.put(21, DepthsTrigger.SHIFT_RIGHT_CLICK);
		//empty space to be even
		TRIGGER_MAP.put(23, DepthsTrigger.SPAWNER);
		TRIGGER_MAP.put(24, DepthsTrigger.SHIFT_BOW);
		TRIGGER_MAP.put(25, DepthsTrigger.SWAP);
		TRIGGER_MAP.put(26, DepthsTrigger.LIFELINE);

		GUIUtils.fillWithFiller(mInventory, true);

		setAbilities(targetPlayer);
	}

	public boolean setAbilities(Player targetPlayer) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(targetPlayer);

		if (items == null || items.size() == 0) {
			return false;
		}
		items.removeIf(item -> item.mTree == DepthsTree.CURSE);

		GUIUtils.fillWithFiller(mInventory, true);

		mInventory.setItem(4, getDescriptionItem());

		List<DepthsAbilityItem> passiveItems = new ArrayList<>();
		for (DepthsAbilityItem item : items) {
			if (item.mTrigger == DepthsTrigger.PASSIVE) {
				if (mShowPassives) {
					passiveItems.add(item);
				}
			} else {
				for (Map.Entry<Integer, DepthsTrigger> slot : TRIGGER_MAP.entrySet()) {
					if (slot.getValue() == item.mTrigger) {
						mInventory.setItem(slot.getKey(), item.mItem);
						break;
					}
				}
			}
		}

		if (mShowPassives) {
			for (int i = 0; i < passiveItems.size() && i < 18; i++) {
				mInventory.setItem(i + START_OF_PASSIVES, passiveItems.get(i).mItem);
			}
		}

		for (int i = 18; i <= 26; i++) {
			ItemStack checkItem = mInventory.getItem(i);
			if (checkItem != null && checkItem.getType() == FILLER) {
				DepthsTrigger trigger = TRIGGER_MAP.get(i);
				if (trigger != null) {
					mInventory.setItem(i, trigger.getNoAbilityItem());
				}
			}
		}

		return true;
	}

	public void setConfirmation(ItemStack item) {
		GUIUtils.fillWithFiller(mInventory, true);
		mAbilityName = ItemUtils.getPlainName(item);

		mInventory.setItem(CONFIRM_ABILITY_LOC, item);
		ItemStack createItem = createCustomItem(CONFIRM_MAT, "Confirm", "Confirm ability removal");
		mInventory.setItem(29, createItem);
		createItem = createCustomItem(CANCEL_MAT, "Cancel", "Returns to previous page.");
		mInventory.setItem(33, createItem);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory
			    || clickedItem == null
			    || clickedItem.getType() == FILLER) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		DepthsManager instance = DepthsManager.getInstance();

		List<DepthsAbilityInfo<?>> abilities = instance.getPlayerAbilities(player);

		if (clickedItem.getType() == CONFIRM_MAT) {
			for (DepthsAbilityInfo<?> ability : abilities) {
				if (ability.getDisplayName() != null && mAbilityName != null && mAbilityName.contains(ability.getDisplayName())) {
					DepthsPlayer depthsPlayer = instance.mPlayers.get(player.getUniqueId());
					if (depthsPlayer != null) {
						onConfirm(player, depthsPlayer, ability);
					}
				}
			}
		} else if (clickedItem.getType() == CANCEL_MAT) {
			setAbilities(player);
		} else {
			for (DepthsAbilityInfo<?> ability : abilities) {
				if (ability.getDisplayName() != null
					    && ItemUtils.getPlainName(clickedItem).contains(ability.getDisplayName())) {
					setConfirmation(clickedItem);
					return;
				}
			}
		}
	}

	protected abstract void onConfirm(Player player, DepthsPlayer depthsPlayer, DepthsAbilityInfo<?> ability);

	protected abstract ItemStack getDescriptionItem();

	public ItemStack createCustomItem(Material type, String name, String lore) {
		return GUIUtils.createBasicItem(type, name, NamedTextColor.WHITE, true, lore, NamedTextColor.GRAY);
	}
}
