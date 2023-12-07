package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpellBaseAbstractCircleAttack extends SpellBaseAbstractAttack {

	public SpellBaseAbstractCircleAttack(CircleInfo circleInfo, int particleAmount, int telegraphPulses,
			 int castDelay, double particleSpeed, Particle particle, DamageEvent.DamageType damageType, double damage,
			 boolean bypassIframes, boolean causeKnockback, String attackName, Particle attackParticle, Plugin plugin, LivingEntity boss,
			 AttackAesthetics attackAesthetics) {
		super(
			() -> {
				ParticleUtils.drawCircleTelegraph(
					circleInfo.getCenter(),
					circleInfo.getRadius(),
					particleAmount,
					telegraphPulses,
					castDelay,
					particleSpeed,
					false,
					particle,
					plugin,
					boss
				);
			},
			() -> {
				// Create a new bounding box
				Hitbox hitbox = new Hitbox.UprightCylinderHitbox(circleInfo.getCenter(), circleInfo.getHeight(), circleInfo.getRadius());
				// Hit the players in it
				List<Player> hitPlayers = hitbox.getHitPlayers(true);
				for (Player player : hitPlayers) {
					DamageUtils.damage(boss, player, damageType, damage, null, bypassIframes, causeKnockback, attackName);
				}
			},
			particleAmount, telegraphPulses, castDelay, particleSpeed, particle, damageType, damage,
			bypassIframes, causeKnockback, attackName, attackParticle, plugin, boss, attackAesthetics
		);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public static class CircleInfo {
		public final Location mCenter;
		public final double mRadius;
		public final double mHeight;

		public CircleInfo(Location center, double radius) {
			mCenter = center;
			mRadius = radius;
			mHeight = 30;
		}

		public CircleInfo(Location center, double radius, double height) {
			mCenter = center;
			mRadius = radius;
			mHeight = height;
		}

		public Location getCenter() {
			return mCenter;
		}

		public double getRadius() {
			return mRadius;
		}

		public double getHeight() {
			return mHeight;
		}
	}
}
