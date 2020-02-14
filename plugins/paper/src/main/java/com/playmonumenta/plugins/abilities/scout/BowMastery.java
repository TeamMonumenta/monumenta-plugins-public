package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

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

	public BowMastery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Bow Mastery");
		mInfo.scoreboardId = "BowMastery";
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
