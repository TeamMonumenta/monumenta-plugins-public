package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class BowMastery extends Ability {

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	public BowMastery(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Bow Mastery");
		mInfo.mScoreboardId = "BowMastery";
		mInfo.mShorthandName = "BM";
		mInfo.mDescriptions.add("Your arrows deal up to 3 extra damage scaling with the charge level of your bow.");
		mInfo.mDescriptions.add("The extra damage is increased to up to 6.");
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.CLOUD);
		double bonusDamage = getAbilityScore() == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "BowMasteryBonusDamageRegistrationTick")) {
			AbilityUtils.addArrowBonusDamage(mPlugin, arrow, bonusDamage);
		}
		return true;
	}

	public int getBonusDamage() {
		return getAbilityScore() == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
	}

}
