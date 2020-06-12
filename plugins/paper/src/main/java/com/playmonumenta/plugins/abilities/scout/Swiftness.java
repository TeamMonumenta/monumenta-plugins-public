package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class Swiftness extends Ability {

	private static final String SWIFTNESS_SPEED_MODIFIER = "SwiftnessSpeedModifier";
	private static final double SWIFTNESS_SPEED_BONUS = 0.2;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;

	private boolean mWasInNoMobilityZone = false;

	public Swiftness(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Swiftness");
		mInfo.mScoreboardId = "Swiftness";
		mInfo.mShorthandName = "Swf";
		mInfo.mDescriptions.add("You gain +20% Speed while not inside a town.");
		mInfo.mDescriptions.add("In addition, you gain Jump Boost III while you are not inside a town.");

		addModifier(player);
	}

	@Override
	public void setupClassPotionEffects() {
		if (getAbilityScore() > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);

		if (mWasInNoMobilityZone && !isInNoMobilityZone) {
			addModifier(mPlayer);
		} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
			removeModifier(mPlayer);
		}

		mWasInNoMobilityZone = isInNoMobilityZone;
	}

	public static void addModifier(Player player) {
		if (player != null) {
			AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			if (speed != null) {
				AttributeModifier mod = new AttributeModifier(SWIFTNESS_SPEED_MODIFIER, SWIFTNESS_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
				speed.addModifier(mod);
			}
		}
	}

	public static void removeModifier(Player player) {
		AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		if (speed != null) {
			for (AttributeModifier mod : speed.getModifiers()) {
				if (mod != null && mod.getName().equals(SWIFTNESS_SPEED_MODIFIER)) {
					speed.removeModifier(mod);
				}
			}
		}
	}

}
