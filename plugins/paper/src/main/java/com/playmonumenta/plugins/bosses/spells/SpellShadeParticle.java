package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class SpellShadeParticle extends Spell {

	private static final Particle.DustOptions SHADE_COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);

	private final World mWorld;
	private final LivingEntity mBoss;

	public SpellShadeParticle(LivingEntity boss) {
		mWorld = boss.getWorld();
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		mWorld.spawnParticle(Particle.REDSTONE, loc, 30, 0.25, 0.6, 0.25, 0, SHADE_COLOR);
		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 8, 0.2, 0.5, 0.2, 0);
		mWorld.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.25, 0.6, 0.25, 0);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
