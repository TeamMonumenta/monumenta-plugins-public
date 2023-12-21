package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
		BiPredicate<Player, ItemStack> predicate = (player, mainhand) -> DepthsUtils.isWeaponItem(mainhand);
		if (proj) {
			// or any projectile weapon or riptide trident
			predicate = predicate.or((player, mainhand) -> ItemUtils.isProjectileWeapon(mainhand) || mainhand.getType() == Material.TRIDENT);
		} else {
			// or a riptide trident, if we can't riptide
			predicate = predicate.or((player, mainhand) -> ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.RIPTIDE) && !PlayerUtils.canRiptide(player, mainhand));
		}
		if (tool) {
			predicate = predicate.or((player, mainhand) -> ItemUtils.isPickaxe(mainhand) || ItemUtils.isShovel(mainhand));
		} else {
			predicate = predicate.and((player, mainhand) -> !(ItemUtils.isPickaxe(mainhand) || ItemUtils.isShovel(mainhand)));
		}
		BiPredicate<Player, ItemStack> finalPredicate = predicate;
		Predicate<Player> playerPredicate = player -> finalPredicate.test(player, player.getInventory().getItemInMainHand());
		return new AbilityTriggerInfo.TriggerRestriction(display, playerPredicate);
	}
}
