package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ImplosionFinisher implements EliteFinisher {

	public static final String NAME = "Implosion";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		Location centered = loc.clone().add(0, killedMob.getHeight() / 2, 0);
		World world = loc.getWorld();

		// SFX
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 3F, 0F);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 3F, 2F);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 3F, 1.5F);

		// Particle Magic
		new PartialParticle(Particle.FLASH, centered, 1).spawnAsPlayerActive(p);
		ParticleUtils.drawSphere(centered, 75, 1.5,
				(l, t) -> {
					Vector vel = centered.clone().subtract(l).toVector().normalize();
					new PartialParticle(Particle.FLAME, l, 1).directionalMode(true)
							.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.5).spawnAsPlayerActive(p);
				}
		);
		ParticleUtils.drawSphere(centered, 30, 0.1,
				(l, t) -> {
					Vector vel = centered.clone().subtract(l).toVector().normalize();
					new PartialParticle(Particle.END_ROD, l, 1).directionalMode(true)
							.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.15).spawnAsPlayerActive(p);
				}
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}
}
