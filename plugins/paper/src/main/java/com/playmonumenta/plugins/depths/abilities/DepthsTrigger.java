package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.function.Predicate;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

//These are unique ability triggers, once you have an ability with one
//You can't be offered other abilities with that same trigger
public enum DepthsTrigger {
	WEAPON_ASPECT,
	COMBO,
	RIGHT_CLICK(AbilityTrigger.Key.RIGHT_CLICK, false),
	SHIFT_LEFT_CLICK(AbilityTrigger.Key.LEFT_CLICK, true),
	SHIFT_RIGHT_CLICK(AbilityTrigger.Key.RIGHT_CLICK, true),
	SPAWNER,
	SHIFT_BOW,
	SWAP(AbilityTrigger.Key.SWAP),
	LIFELINE,
	PASSIVE,
	;

	public final AbilityTrigger mTrigger;
	public final @Nullable AbilityTriggerInfo.TriggerRestriction mRestriction;

	DepthsTrigger() {
		// Dummy trigger, never use
		this(new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false), null);
	}

	DepthsTrigger(AbilityTrigger.Key key) {
		this(new AbilityTrigger(key), getRestriction(key));
	}

	DepthsTrigger(AbilityTrigger.Key key, boolean sneak) {
		this(new AbilityTrigger(key).sneaking(sneak), getRestriction(key));
	}

	DepthsTrigger(AbilityTrigger trigger, @Nullable AbilityTriggerInfo.TriggerRestriction restriction) {
		mTrigger = trigger;
		mRestriction = restriction;
	}

	private static AbilityTriggerInfo.TriggerRestriction getRestriction(AbilityTrigger.Key key) {
		return getRestriction(key != AbilityTrigger.Key.RIGHT_CLICK, key != AbilityTrigger.Key.LEFT_CLICK);
	}

	private static AbilityTriggerInfo.TriggerRestriction getRestriction(boolean proj, boolean tool) {
		String display = "holding a melee weapon" + (proj ? (tool ? ", projectile weapon, or tool" : " or projectile weapon") : (tool ? " or tool" : ""));
		Predicate<ItemStack> predicate = DepthsUtils::isWeaponItem;
		if (proj) {
			predicate = predicate.or(ItemUtils::isProjectileWeapon);
		} else {
			predicate = predicate.and(((Predicate<ItemStack>) ItemUtils::isProjectileWeapon).negate());
		}
		if (tool) {
			predicate = predicate.or(ItemUtils::isPickaxe).or(ItemUtils::isShovel);
		} else {
			predicate = predicate.and(((Predicate<ItemStack>) ItemUtils::isPickaxe).or(ItemUtils::isShovel).negate());
		}
		Predicate<ItemStack> finalPredicate = predicate;
		return new AbilityTriggerInfo.TriggerRestriction(display, player -> finalPredicate.test(player.getInventory().getItemInMainHand()));
	}
}
