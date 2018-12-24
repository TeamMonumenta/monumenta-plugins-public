package com.playmonumenta.plugins.abilities.scout;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

public class BowMastery extends Ability {

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	public BowMastery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 6;
		mInfo.specId = -1;
		mInfo.scoreboardId = "BowMastery";
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int bowMastery = getAbilityScore();
		if (arrow.isCritical()) {
			int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
			EntityUtils.damageEntity(mPlugin, damagee, bonusDamage, mPlayer);
		}
		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.CLOUD);
		return true;
	}
}
