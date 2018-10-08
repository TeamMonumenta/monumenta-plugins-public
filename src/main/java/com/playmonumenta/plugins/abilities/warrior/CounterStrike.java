package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class CounterStrike extends Ability {

	private static final float COUNTER_STRIKE_RADIUS = 5.0f;

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		//  If we're not going to succeed in our Random we probably don't want to attempt to grab the scoreboard value anyways.
		if (mRandom.nextFloat() < 0.15f) {
			int counterStrike = getAbilityScore(player);
			if (counterStrike > 0) {
				Location loc = player.getLocation();
				player.spawnParticle(Particle.SWEEP_ATTACK, loc.getX(), loc.getY() + 1.5D, loc.getZ(), 20, 1.5D, 1.5D, 1.5D);
				player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);

				double csDamage = counterStrike == 1 ? 12D : 24D;

				for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), COUNTER_STRIKE_RADIUS)) {
					EntityUtils.damageEntity(mPlugin, mob, csDamage, player);
				}
			}
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.scoreboardId = "CounterStrike";
		return info;
	}

}
