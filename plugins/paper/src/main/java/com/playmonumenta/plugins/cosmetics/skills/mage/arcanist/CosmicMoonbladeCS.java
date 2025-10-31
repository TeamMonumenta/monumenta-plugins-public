package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CosmicMoonbladeCS implements CosmeticSkill {

	private static final Color COLOR1 = Color.fromRGB(100, 190, 255);
	private static final Color COLOR2 = Color.fromRGB(160, 220, 230);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.COSMIC_MOONBLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND_SWORD;
	}

	public void moonbladeSwingEffect(World world, Player player, Location origin, double range, int swings, int maxSwing) {
		float pitch = swings >= maxSwing ? 1.45f : 1.2f;
		world.playSound(origin, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.1f, 2.0f);
		world.playSound(origin, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(origin, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.7f);
		world.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.1f, 2.0f);
		world.playSound(origin, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.75f, pitch);
		world.playSound(origin, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, 2.0f);
		world.playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.8f, 2.0f);

		new BukkitRunnable() {
			final int mI = swings;
			double mRoll;
			double mD = 45;
			boolean mInit = false;
			final PPPeriodic mParticle1 = new PPPeriodic(Particle.DUST_COLOR_TRANSITION, player.getLocation()).count(1).delta(0.1, 0.1, 0.1);
			final PPPeriodic mParticle2 = new PPPeriodic(Particle.DUST_COLOR_TRANSITION, player.getLocation()).count(1).delta(0.1, 0.1, 0.1);

			@Override
			public void run() {
				if (!mInit) {
					if (mI % 2 == 0) {
						mRoll = -8;
						mD = 45;
					} else {
						mRoll = 8;
						mD = 135;
					}
					mInit = true;
				}
				if (mI % 2 == 0) {
					for (double r = 1; r < range; r += 0.5) {
						double transistion = Math.pow(FastUtils.sin(Math.pow(r * 2 * Math.PI / range, 5)), 2);
						for (double degree = mD; degree < mD + 30; degree += 5) {
							mParticle1.data(new Particle.DustTransition(ParticleUtils.getTransition(COLOR1, Color.WHITE, transistion), COLOR1, 1.0f));
							mParticle2.data(new Particle.DustTransition(ParticleUtils.getTransition(COLOR2, Color.WHITE, transistion), COLOR2, 1.0f));
							Location l = origin.clone().add(0, 1.25, 0).add(moonbladeOffset(r, degree, mRoll, origin));
							mParticle1.location(l).spawnAsPlayerActive(player);
							mParticle2.location(l).spawnAsPlayerActive(player);
						}
					}

					mD += 30;
				} else {
					for (double r = 1; r < range; r += 0.5) {
						double transistion = Math.pow(FastUtils.sin(Math.pow(r * 2 * Math.PI / range, 5)), 2);
						for (double degree = mD; degree > mD - 30; degree -= 5) {
							mParticle1.data(new Particle.DustTransition(ParticleUtils.getTransition(COLOR1, Color.WHITE, transistion), COLOR1, 1.0f));
							mParticle2.data(new Particle.DustTransition(ParticleUtils.getTransition(COLOR2, Color.WHITE, transistion), COLOR2, 1.0f));
							Location l = origin.clone().add(0, 1.25, 0).add(moonbladeOffset(r, degree, mRoll, origin));
							l.setPitch(-l.getPitch());
							mParticle1.location(l).spawnAsPlayerActive(player);
							mParticle2.location(l).spawnAsPlayerActive(player);
						}
					}
					mD -= 30;
				}
				if ((mD >= 135 && mI % 2 == 0) || (mD <= 45 && mI % 2 > 0)) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	protected Vector moonbladeOffset(double radius, double degree, double roll, Location origin) {
		double radian = Math.toRadians(degree);
		Vector vec;
		vec = new Vector(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
		vec = VectorUtils.rotateZAxis(vec, roll);
		vec = VectorUtils.rotateXAxis(vec, origin.getPitch());
		vec = VectorUtils.rotateYAxis(vec, origin.getYaw());
		return vec;
	}
}
