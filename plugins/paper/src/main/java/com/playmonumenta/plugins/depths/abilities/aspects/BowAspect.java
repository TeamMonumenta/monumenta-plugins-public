package com.playmonumenta.plugins.depths.abilities.aspects;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class BowAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Bow";
	public static final double COOLDOWN_REDUCTION = 0.25;
	public static final double PASSIVE_ARROW_SAVE = 0.5;

	public BowAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.BOW;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer != null && FastUtils.RANDOM.nextDouble() < PASSIVE_ARROW_SAVE) {
			boolean refunded = AbilityUtils.refundArrow(mPlayer, arrow);
			if (refunded) {
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 1.0f);
			}
		}
		return true;
	}

	public static double getCooldownReduction(Player player) {
		if (player != null && AbilityManager.getManager().getPlayerAbility(player, BowAspect.class) != null) {
			return 1 - COOLDOWN_REDUCTION;
		}
		return 1;
	}

	@Override
	public String getDescription(int rarity) {
		return "Your sneak fire with bow ability has " + (int) DepthsUtils.roundPercent(COOLDOWN_REDUCTION) + "% reduced cooldown, and you have a " + (int) DepthsUtils.roundPercent(PASSIVE_ARROW_SAVE) + "% chance for arrows to not be consumed when using a bow or crossbow.";
	}
}

