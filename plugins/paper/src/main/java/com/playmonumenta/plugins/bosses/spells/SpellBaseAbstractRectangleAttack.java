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
import org.bukkit.util.BoundingBox;

public class SpellBaseAbstractRectangleAttack extends SpellBaseAbstractAttack {

	public SpellBaseAbstractRectangleAttack(RectangleInfo rectangleInfo, int particleAmount, int telegraphPulses,
	                                        int castDelay, double particleSpeed, Particle particle, DamageEvent.DamageType damageType, double damage,
	                                        boolean bypassIframes, boolean causeKnockback, String attackName, Particle attackParticle, Plugin plugin, LivingEntity boss,
	                                        AttackAesthetics attackAesthetics) {
		this(rectangleInfo, particleAmount, telegraphPulses, castDelay, 0, particleSpeed, particle, damageType, damage, bypassIframes,
			causeKnockback, attackName, attackParticle, plugin, boss, attackAesthetics);
	}

	public SpellBaseAbstractRectangleAttack(RectangleInfo rectangleInfo, int particleAmount, int telegraphPulses,
	                                        int castDelay, int pulseStartDelay, double particleSpeed, Particle particle, DamageEvent.DamageType damageType, double damage,
	                                        boolean bypassIframes, boolean causeKnockback, String attackName, Particle attackParticle, Plugin plugin, LivingEntity boss,
	                                        AttackAesthetics attackAesthetics) {
		super(
			() -> {
				ParticleUtils.drawRectangleTelegraph(
					rectangleInfo.getStart(),
					rectangleInfo.getDx(),
					rectangleInfo.getDz(),
					particleAmount,
					telegraphPulses,
					castDelay,
					pulseStartDelay,
					particleSpeed,
					particle,
					plugin,
					boss
				);
			},
			() -> {
				// Create a new bounding box
				Hitbox hitbox = new Hitbox.AABBHitbox(boss.getWorld(), BoundingBox.of(rectangleInfo.getCenter(), rectangleInfo.getHalfDx(), 30, rectangleInfo.getHalfDz()));
				// Hit the players in it
				List<Player> hitPlayers = hitbox.getHitPlayers(true);
				for (Player player : hitPlayers) {
					DamageUtils.damage(boss, player, damageType, damage, null, bypassIframes, causeKnockback, attackName);
				}
			},
			particleAmount, telegraphPulses, castDelay, particleSpeed, particle, damageType,
			damage, bypassIframes, causeKnockback, attackName, attackParticle, plugin, boss, attackAesthetics
		);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public record RectangleInfo(Location mStart, double mDx, double mDz, double mDy) {
		public RectangleInfo(Location start, double dx, double dz) {
			this(start, dx, dz, 30);
		}

		public Location getStart() {
			return mStart;
		}

		public double getDx() {
			return mDx;
		}

		public double getHalfDx() {
			return mDx / 2;
		}

		public double getDz() {
			return mDz;
		}

		public double getHalfDz() {
			return mDz / 2;
		}

		public Location getCenter() {
			return new Location(mStart.getWorld(), mStart.getX() + getHalfDx(), mStart.getY(), mStart.getZ() + getHalfDz());
		}
	}
}
