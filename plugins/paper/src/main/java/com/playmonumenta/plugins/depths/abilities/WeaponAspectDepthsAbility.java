package com.playmonumenta.plugins.depths.abilities;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.utils.GUIUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.utils.ItemUtils;

public abstract class WeaponAspectDepthsAbility extends DepthsAbility {

	public WeaponAspectDepthsAbility(Plugin plugin, Player player, String displayName) {
		super(plugin, player, displayName);
		mRarity = DepthsManager.getInstance().getPlayerLevelInAbility(displayName, player);

	}

	/**
	 * Overrides base ability check to tell the system that the player
	 * can use the ability if their rarity level in the manager is above zero
	 */
	@Override
	public boolean canUse(Player player) {
		mRarity = DepthsManager.getInstance().getPlayerLevelInAbility(this.getDisplayName(), player);
		return mRarity > 0;
	}

	//Constructing the inventory item appearance for the ability
	/**
	 * Returns the ability item to display in GUIs given the input rarity
	 * @param rarity the rarity to put on the item
	 * @return the item to display
	 */
	@Override
	public DepthsAbilityItem getAbilityItem(int rarity) {
		DepthsAbilityItem item = new DepthsAbilityItem();
		item.mRarity = rarity;
		item.mAbility = mInfo.mDisplayName;
		ItemStack stack = new ItemStack(mDisplayItem);
		ItemMeta meta = stack.getItemMeta();
		item.mTrigger = DepthsTrigger.WEAPON_ASPECT;
		meta.setDisplayName("" + ChatColor.RESET + ChatColor.BOLD + mInfo.mDisplayName);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		List<String> lore = new ArrayList<>();
		meta.setLore(lore);
		GUIUtils.splitLoreLine(meta, getDescription(rarity), 30, ChatColor.WHITE, true);
		stack.setItemMeta(meta);
		ItemUtils.setPlainName(stack, mInfo.mDisplayName);
		item.mItem = stack;
		return item;
	}

	//Return false since these are manually offered by the system
	@Override
	public boolean canBeOffered(Player player) {
		return false;
	}

	//Description of what the ability does, abilities override this
	@Override
	public String getDescription(int rarity) {
		return "" + mInfo.mDisplayName;
	}


}
