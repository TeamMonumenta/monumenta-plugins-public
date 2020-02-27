package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class BruteForce extends Ability {

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final double BRUTE_FORCE_1_DAMAGE = 2;
	private static final double BRUTE_FORCE_2_DAMAGE = 5;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.7f;

	private final double damageBonus;

	public BruteForce(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Brute Force");
		mInfo.scoreboardId = "BruteForce";
		mInfo.mShorthandName = "BF";
		mInfo.mDescriptions.add("When you critically strike, you deal 2 damage to all enemies near your target and knock them back.");
		mInfo.mDescriptions.add("The damage is increased to 5.");
		damageBonus = getAbilityScore() == 1 ? BRUTE_FORCE_1_DAMAGE : BRUTE_FORCE_2_DAMAGE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (PlayerUtils.isCritical(mPlayer) && event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() + damageBonus);

			Location loc = event.getEntity().getLocation().add(0, 0.75, 0);
			mWorld.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, BRUTE_FORCE_RADIUS, mPlayer)) {
				if (mob != event.getEntity()) {
					EntityUtils.damageEntity(mPlugin, mob, damageBonus, mPlayer);
				}
				if (!EntityUtils.isBoss(mob)) {
					MovementUtils.knockAway(mPlayer.getLocation(), mob, BRUTE_FORCE_KNOCKBACK_SPEED, BRUTE_FORCE_KNOCKBACK_SPEED / 2);
				}
			}
		}

		return true;
	}
}
