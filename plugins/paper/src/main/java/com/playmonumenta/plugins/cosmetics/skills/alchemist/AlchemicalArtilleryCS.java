package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AlchemicalArtilleryCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ALCHEMICAL_ARTILLERY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_CREAM;
	}

	public void periodicEffects(Player caster, MagmaCube grenade) {
		Location particleLoc = grenade.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.SMOKE_LARGE, particleLoc, 2, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLAME, particleLoc, 3, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
	}

	public void explosionEffect(Player caster, Location loc, double radius) {
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 10,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5,
					(Location location) -> new PartialParticle(Particle.FLAME, location, 1, 0, 0, 0, 0.0025).spawnAsPlayerActive(caster))
			)
		);
		ParticleUtils.drawRing(loc, 45, new Vector(0, 1, 0), radius,
			(loc1, t) -> new PartialParticle(Particle.REDSTONE, loc1, 1, 0, 0, 0, 0.0025, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f)).spawnAsPlayerActive(caster)
		);
		new PartialParticle(Particle.FLAME, loc, 100, radius / 2, 0, radius / 2, 0.2).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(caster);

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.5f, 2f);
	}

}
