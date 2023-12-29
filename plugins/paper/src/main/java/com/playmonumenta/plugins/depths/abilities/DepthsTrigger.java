package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

//These are unique ability triggers, once you have an ability with one
//You can't be offered other abilities with that same trigger
public enum DepthsTrigger {
	WEAPON_ASPECT,
	COMBO,
	RIGHT_CLICK(new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)),
	SHIFT_LEFT_CLICK(new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE, AbilityTrigger.KeyOptions.NO_SHOVEL)),
	SHIFT_RIGHT_CLICK(new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)),
	SPAWNER,
	SHIFT_BOW,
	SWAP(new AbilityTrigger(AbilityTrigger.Key.SWAP)),
	LIFELINE,
	PASSIVE,
	;

	public static final AbilityTriggerInfo.TriggerRestriction DEPTHS_TRIGGER_RESTRICTION = new AbilityTriggerInfo.TriggerRestriction("holding a melee or projectile weapon or tool", player -> {
		ItemStack item = player.getInventory().getItemInMainHand();
		return DepthsUtils.isWeaponItem(item) || item.getType() == Material.TRIDENT || ItemUtils.isProjectileWeapon(item) || ItemUtils.isPickaxe(item) || ItemUtils.isShovel(item);
	});

	public final AbilityTrigger mTrigger;

	DepthsTrigger() {
		// Dummy trigger, never use
		this(new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false));
	}

	DepthsTrigger(AbilityTrigger trigger) {
		mTrigger = trigger;
	}
}
