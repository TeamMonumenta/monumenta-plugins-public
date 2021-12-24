package com.playmonumenta.plugins.depths.abilities;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.utils.GUIUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class DepthsAbility extends Ability {

	public int mRarity;
	public Material mDisplayItem = Material.STICK;
	public @Nullable DepthsTree mTree = null;


	public DepthsAbility(Plugin plugin, Player player, String displayName) {
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
	 *
	 * @param rarity the rarity to put on the item
	 * @return the item to display
	 */
	public @Nullable DepthsAbilityItem getAbilityItem(int rarity) {
		DepthsAbilityItem item = null;

		//Don't crash our abilities because of a null item
		try {
			item = new DepthsAbilityItem();
			item.mRarity = rarity;
			item.mAbility = mInfo.mDisplayName;
			item.mTrigger = getTrigger();
			ItemStack stack = new ItemStack(mDisplayItem);
			ItemMeta meta = stack.getItemMeta();
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			meta.displayName(Component.text("" + ChatColor.BOLD + mInfo.mDisplayName, DepthsUtils.getTreeColor(mTree)).decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();

			if (rarity > 0) {
				lore.add(DepthsUtils.getLoreForItem(mTree, rarity));
			}


			meta.lore(lore);
			GUIUtils.splitLoreLine(meta, getDescription(rarity), 30, ChatColor.WHITE, false);
			stack.setItemMeta(meta);
			ItemUtils.setPlainName(stack, mInfo.mDisplayName);
			item.mItem = stack;
		} catch (Exception e) {
			mPlugin.getLogger().info("Invalid depths ability item: " + getDisplayName());
		}
		return item;
	}

	//Whether the player is eligible to have this ability offered
	public boolean canBeOffered(Player player) {

		//Make sure the player doesn't have this ability already

		if (DepthsManager.getInstance().getPlayerLevelInAbility(mInfo.mDisplayName, player) > 0) {
			return false;
		}

		//Make sure player has all prereqs for this ability
		if (getAbilityPreReqs() != null && getAbilityPreReqs().size() > 0) {
			for (String ability : getAbilityPreReqs()) {
				if (DepthsManager.getInstance().getPlayerLevelInAbility(ability, player) <= 0) {
					return false;
				}
			}
		}
		//Make sure player has no incompatibles for this ability
		if (getIncompatibles() != null && getIncompatibles().size() > 0) {
			for (String ability : getIncompatibles()) {
				if (DepthsManager.getInstance().getPlayerLevelInAbility(ability, player) > 0) {
					return false;
				}
			}
		}

		//Make sure player doesn't already have an ability with the same trigger
		DepthsTrigger trigger = getTrigger();
		if (trigger != DepthsTrigger.PASSIVE && DepthsManager.getInstance().isInSystem(player)) {
			for (DepthsAbility ability : DepthsManager.getAbilities()) {
				//Iterate over abilities and return false if the player has an ability with the same trigger already
				if (DepthsManager.getInstance().getPlayerLevelInAbility(ability.getDisplayName(), player) > 0 && ability.getTrigger() == trigger) {
					return false;
				}
			}
		}

		return true;
	}

	//Description of what the ability does, abilities override this
	public String getDescription(int rarity) {
		return "" + mInfo.mDisplayName;
	}

	//Trigger the player uses to activate it, override this if it uses a trigger
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}

	//Which tree the ability is a part of. Like description, abilities must override this
	public DepthsTree getDepthsTree() {
		//Default
		return DepthsTree.FROSTBORN;
	}

	public List<String> getAbilityPreReqs() {
		return new ArrayList<String>();
	}

	public List<String> getIncompatibles() {
		return new ArrayList<String>();
	}

}
