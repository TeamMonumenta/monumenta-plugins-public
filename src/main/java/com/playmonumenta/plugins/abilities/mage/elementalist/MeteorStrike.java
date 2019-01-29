package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Sprint left-click makes a meteor fall where the player is looking, dealing
 * 14 / 20 damage in a 5-block radius and setting mobs on fire for 3s.
 * Cooldown 16 / 12 s
 */

public class MeteorStrike extends Ability {
	private static final int METEOR_STRIKE_1_COOLDOWN = 16 * 20;
	private static final int METEOR_STRIKE_2_COOLDOWN = 12 * 20;
	private static final int METEOR_STRIKE_1_DAMAGE = 16;
	private static final int METEOR_STRIKE_2_DAMAGE = 22;
	private static final int METEOR_STRIKE_FIRE_DURATION = 3 * 20;
	private static final double METEOR_STRIKE_RADIUS = 5;

	public MeteorStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.METEOR_STRIKE;
		mInfo.scoreboardId = "MeteorStrike";
		mInfo.cooldown = getAbilityScore() == 1 ? METEOR_STRIKE_1_COOLDOWN : METEOR_STRIKE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSprinting();
	}

	@Override
	public boolean cast() {
		Location loc = mPlayer.getEyeLocation();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
		mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < 25; i++) {
			loc.add(dir);

			mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
			int size = EntityUtils.getNearbyMobs(loc, 2).size();
			if (loc.getBlock().getType().isSolid()) {
				launchMeteor(mPlayer, loc);
				break;
			} else if (i >= 24) {
				launchMeteor(mPlayer, loc);
				break;
			} else if (size > 0) {
				launchMeteor(mPlayer, loc);
				break;
			}
		}

		putOnCooldown();
		return true;
	}

	private void launchMeteor(final Player player, final Location loc) {
		double damage = getAbilityScore() == 1 ? METEOR_STRIKE_1_DAMAGE : METEOR_STRIKE_2_DAMAGE;
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double t = 0;
			public void run() {
				t += 1;
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							mWorld.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, METEOR_STRIKE_RADIUS)) {
								EntityUtils.damageEntity(mPlugin, e, damage, player, MagicType.FIRE);
								e.setFireTicks(METEOR_STRIKE_FIRE_DURATION);

								Vector v = e.getLocation().toVector().subtract(loc.toVector()).normalize();
								v.add(new Vector(0, 0.2, 0));
								e.setVelocity(v);
							}
							break;
						}
					}
				}
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				mWorld.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (t >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
