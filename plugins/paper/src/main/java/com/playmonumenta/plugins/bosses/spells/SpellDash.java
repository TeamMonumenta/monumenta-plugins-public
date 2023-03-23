package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellDash extends Spell {

	public final Plugin mPlugin;
	public final LivingEntity mBoss;
	public final int mCooldown;
	public final double mJumpVelocity;
	public final double mVelocity;
	public final SoundsList mSoundsStart;
	public final SoundsList mSoundsLand;
	public final ParticlesList mParticlesStart;
	public final ParticlesList mParticlesAir;
	public final ParticlesList mParticlesLand;

	public SpellDash(Plugin plugin, LivingEntity boss, int cooldown, double jumpVelocity, double velocity,
					 SoundsList soundsStart, SoundsList soundsLand,
					 ParticlesList particlesStart, ParticlesList particlesAir, ParticlesList particlesLand) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mJumpVelocity = jumpVelocity;
		mVelocity = velocity;
		mSoundsStart = soundsStart;
		mSoundsLand = soundsLand;
		mParticlesStart = particlesStart;
		mParticlesAir = particlesAir;
		mParticlesLand = particlesLand;
	}

	@Override
	public void run() {
		Location bossLoc = mBoss.getLocation();
		Player target = EntityUtils.getNearestPlayer(bossLoc, 5);
		if (target == null) {
			return;
		}

		Vector velocity = bossLoc.toVector().subtract(target.getLocation().toVector()).normalize();
		velocity.multiply(new Vector(mVelocity, 1, mVelocity));
		velocity.setY(mJumpVelocity);
		mBoss.setVelocity(velocity);

		// Aesthetics
		playJumpAesthetics();
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isOnGround()) {
					playLandingAesthetics();
					this.cancel();
				}
				playAirAesthetics();
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void playJumpAesthetics() {
		// Jump Sound
		mSoundsStart.play(mBoss.getLocation());
		// Jump Particles
		ParticleUtils.explodingRingEffect(mPlugin, mBoss.getLocation(), 2, 1, 4,
				List.of(
						new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> mParticlesStart.spawn(mBoss, location))
				)
		);
	}

	private void playAirAesthetics() {
		// Air Particles
		mParticlesAir.spawn(mBoss, mBoss.getLocation());
	}

	private void playLandingAesthetics() {
		// Landing Sound
		mSoundsLand.play(mBoss.getLocation());
		// Landing Particles
		ParticleUtils.explodingRingEffect(mPlugin, mBoss.getLocation(), 2, 1, 4,
				List.of(
						new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> mParticlesLand.spawn(mBoss, location))
				)
		);
	}
}
