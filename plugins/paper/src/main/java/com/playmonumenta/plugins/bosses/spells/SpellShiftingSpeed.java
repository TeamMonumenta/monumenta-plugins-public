package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellShiftingSpeed extends Spell {
	private LivingEntity mBoss;

	public SpellShiftingSpeed(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();

		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1f, 0.6f);
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.5f, 1.5f);
		new PartialParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0).spawnAsEntityActive(mBoss);

		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 1, false, true));
	}

	@Override
	public int cooldownTicks() {
		//Mob has speed for 2.5 seconds then normal speed for 2.5 seconds.
		return 100;
	}
}
