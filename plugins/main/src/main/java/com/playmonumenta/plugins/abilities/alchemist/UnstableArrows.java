package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;


public class UnstableArrows extends Ability {
	private static final int UNSTABLE_ARROWS_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_ARROWS_DURATION = 2 * 20;
	private static final int UNSTABLE_ARROWS_PARTICLE_PERIOD = 3;
	private static final float UNSTABLE_ARROWS_KNOCKBACK_SPEED = 0.55f;
	private static final int UNSTABLE_ARROWS_1_DAMAGE = 15;
	private static final int UNSTABLE_ARROWS_2_DAMAGE = 24;
	private static final int UNSTABLE_ARROWS_RADIUS = 4;

	private Arrow mUnstableArrow = null;

	public UnstableArrows(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.UNSTABLE_ARROWS;
		mInfo.scoreboardId = "BombArrow";
		mInfo.cooldown = UNSTABLE_ARROWS_COOLDOWN;
	}

	@Override
	public void ProjectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		if (mUnstableArrow != null && arrow == mUnstableArrow) {
			arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
			putOnCooldown();
			mUnstableArrow = null;

			Location loc = arrow.getLocation();

			// Run cleansing rain here until it finishes
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mWorld.spawnParticle(Particle.FLAME, loc, 1, 0.3, 0.3, 0.3, 0.001);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.5, 0.5, 0.5, 0.001);
					mWorld.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f,
					                 ((UNSTABLE_ARROWS_DURATION / 2.0f) + mTicks) / (2.0f * UNSTABLE_ARROWS_DURATION));

					mTicks += UNSTABLE_ARROWS_PARTICLE_PERIOD;
					if (mTicks > UNSTABLE_ARROWS_DURATION) {
						arrow.remove();
						Location explodeLoc = loc.add(0, 1.2, 0);
						mWorld.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);
						mWorld.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.0f);

						mWorld.spawnParticle(Particle.EXPLOSION_HUGE, explodeLoc, 3, 0.02, 0.02, 0.02, 0.001);
						BasiliskPoison bp = (BasiliskPoison) AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);

						int baseDamage = (getAbilityScore() == 1) ? UNSTABLE_ARROWS_1_DAMAGE : UNSTABLE_ARROWS_2_DAMAGE;

						for (LivingEntity mob : EntityUtils.getNearbyMobs(explodeLoc, UNSTABLE_ARROWS_RADIUS, mPlayer)) {
							EntityUtils.damageEntity(mPlugin, mob, baseDamage, mPlayer);
							MovementUtils.KnockAway(explodeLoc, mob, UNSTABLE_ARROWS_KNOCKBACK_SPEED);
							if (bp != null) {
								bp.apply(mob);
							}
						}
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, UNSTABLE_ARROWS_PARTICLE_PERIOD);
		}
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		// Can't use runCheck for this because player doesn't need to be sneaking when arrow lands
		if (mPlayer.isSneaking()) {
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 5.0f, 0.25f);
			mUnstableArrow = arrow;
		}

		return true;
	}
}
