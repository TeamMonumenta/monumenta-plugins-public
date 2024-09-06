package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

public class SpellHarrakfarRecover extends Spell {

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;

	public SpellHarrakfarRecover(Location spawnLoc, LivingEntity boss) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		if (mBoss.getLocation().getY() < mSpawnLoc.getY() - 0.85) {
			effects(mBoss.getLocation());
			mBoss.teleport(mSpawnLoc);
			effects(mSpawnLoc);
		}
	}

	private void effects(Location loc) {
		mBoss.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 5, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
