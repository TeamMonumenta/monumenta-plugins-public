package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class SpellDreadnaughtParticle extends Spell {

	private static final Particle.DustOptions DREADFUL_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	private final World mWorld;
	private final LivingEntity mBoss;

	public SpellDreadnaughtParticle(LivingEntity boss) {
		mWorld = boss.getWorld();
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		double yaw = loc.getYaw();
		mWorld.spawnParticle(Particle.FLAME, loc.clone().add(-0.2 * Math.cos(yaw), 0.8, -0.2 * Math.sin(yaw)), 1, 0, 0, 0, 0);
		mWorld.spawnParticle(Particle.FLAME, loc.clone().add(0.2 * Math.cos(yaw), 0.8, 0.2 * Math.sin(yaw)), 1, 0, 0, 0, 0);
		mWorld.spawnParticle(Particle.REDSTONE, loc.clone().add(0, -0.5, 0), 30, 0.2, 0.5, 0.2, 0, DREADFUL_COLOR);
		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.4, 0.5, 0.4, 0);
	}

	@Override
	public int duration() {
		// This is the period of run()
		return 5;
	}
}
