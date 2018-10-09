package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public class CounterStrike extends Ability {

	private static final float COUNTER_STRIKE_RADIUS = 5.0f;

	public CounterStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
		mInfo.scoreboardId = "CounterStrike";
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		//  If we're not going to succeed in our Random we probably don't want to attempt to grab the scoreboard value anyways.
		if (mRandom.nextFloat() < 0.15f) {
			int counterStrike = getAbilityScore();
			if (counterStrike > 0) {
				Location loc = mPlayer.getLocation();
				mPlayer.spawnParticle(Particle.SWEEP_ATTACK, loc.getX(), loc.getY() + 1.5D, loc.getZ(), 20, 1.5D, 1.5D, 1.5D);
				mPlayer.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);

				double csDamage = counterStrike == 1 ? 12D : 24D;

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_RADIUS)) {
					EntityUtils.damageEntity(mPlugin, mob, csDamage, mPlayer);
				}
			}
		}
		return true;
	}
}
