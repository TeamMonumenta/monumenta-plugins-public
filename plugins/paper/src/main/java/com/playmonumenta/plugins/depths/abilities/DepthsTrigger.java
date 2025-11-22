package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.guis.DepthsSummaryGUI;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

//These are unique ability triggers, once you have an ability with one
//You can't be offered other abilities with that same trigger
public enum DepthsTrigger {
	WEAPON_ASPECT("Weapon Aspect"),
	COMBO("Combo"),
	RIGHT_CLICK("Right Click", new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)),
	SHIFT_LEFT_CLICK("Sneak Left Click", new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE, AbilityTrigger.KeyOptions.NO_SHOVEL)),
	SHIFT_RIGHT_CLICK("Sneak Right Click", new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)),
	WILDCARD("Wildcard"),
	SHIFT_BOW("Sneak Bow"),
	SWAP("Swap", new AbilityTrigger(AbilityTrigger.Key.SWAP)),
	LIFELINE("Lifeline"),
	PASSIVE("Passive"),
	;

	public static final AbilityTriggerInfo.TriggerRestriction DEPTHS_TRIGGER_RESTRICTION = new AbilityTriggerInfo.TriggerRestriction("holding a melee or projectile weapon or tool", player -> {
		ItemStack item = player.getInventory().getItemInMainHand();
		return DepthsUtils.isWeaponItem(item) || item.getType() == Material.TRIDENT || ItemUtils.isProjectileWeapon(item) || ItemUtils.isPickaxe(item) || ItemUtils.isShovel(item);
	}, false);

	public final AbilityTrigger mTrigger;
	public final String mName;

	DepthsTrigger(String name) {
		// Dummy trigger, never use
		this(name, new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false));
	}

	DepthsTrigger(String name, AbilityTrigger trigger) {
		mTrigger = trigger;
		mName = name;
	}

	public ItemStack getNoAbilityItem(DepthsPlayer dp) {
		List<Component> lore = new ArrayList<>();
		if (this != WEAPON_ASPECT) {
			lore.add(Component.text("Possible abilities:", NamedTextColor.GRAY));
			List<DepthsTree> cappedTrees = DepthsManager.getInstance().getCappedTrees(dp);
			List<DepthsTree> sortedTrees = new ArrayList<>(dp.mEligibleTrees);
			sortedTrees.sort(Comparator.comparingInt(t -> DepthsSummaryGUI.TREE_LOCATIONS.get(dp.mEligibleTrees.indexOf(t))));
			for (DepthsTree tree : sortedTrees) {
				boolean capped = cappedTrees.contains(tree);
				for (DepthsAbilityInfo<?> info : DepthsManager.getAbilitiesOfTree(tree).stream().filter(i -> i.getDepthsTrigger() == this).toList()) {
					lore.add(info.getColoredName().decoration(TextDecoration.STRIKETHROUGH, capped));
				}
			}
			if (lore.size() == 1) {
				lore.add(Component.text("None", NamedTextColor.GRAY));
			}
		}
		return GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE, 1, Component.text("No " + mName + (this == WEAPON_ASPECT ? "!" : " ability!"), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false), lore, true);
	}

	public boolean isActive() {
		return this != PASSIVE && this != WEAPON_ASPECT;
	}
}
