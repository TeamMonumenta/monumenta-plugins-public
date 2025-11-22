package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class UnstableAmalgamCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.UNSTABLE_AMALGAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SLIME_BLOCK;
	}

	public void periodicEffects(Player caster, Location loc, double radius, int ticks, int duration) {
		if (ticks % (duration / 3) == 0) {
			new PartialParticle(Particle.FLAME, loc, 20, 0.02, 0.02, 0.02, 0.1).spawnAsPlayerActive(caster);
			loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 1.7f);
			new PPCircle(Particle.FALLING_DUST, loc.clone().add(0, 0.2, 0), radius)
				.countPerMeter(2)
				.data(Material.AMETHYST_BLOCK.createBlockData())
				.spawnAsPlayerActive(caster);
		}
	}

	public void explodeEffects(Player caster, Location loc, double radius) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.25f);
		new PartialParticle(Particle.FLAME, loc, 115, 0.02, 0.02, 0.02, 0.2).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(caster);
	}

	public void unstableMobEffects(Player caster, LivingEntity mob) {
		new PartialParticle(Particle.REDSTONE, mob.getEyeLocation(), 12, 0.5, 0.5, 0.5, 0.3, new Particle.DustOptions(Color.WHITE, 0.8f)).spawnAsPlayerActive(caster);
	}

	public void unstableMobDeath(Player caster, LivingEntity mob) {
	}

	public void unstablePotionSplash(Player caster, Location loc, double radius) {
		new GruesomeAlchemyCS().effectsOnSplash(caster, loc, false, radius, false);
	}

}
