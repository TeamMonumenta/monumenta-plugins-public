package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class LocustSwarmFinisher implements EliteFinisher {

	public static final String NAME = "Locust Swarm";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		Location centered = loc.clone().add(0, killedMob.getHeight() / 2, 0);
		World world = loc.getWorld();

		// Bleed
		world.playSound(loc, Sound.ENTITY_VILLAGER_DEATH, SoundCategory.PLAYERS, 1.0F, 0.5F);
		world.playSound(loc, Sound.ENTITY_FOX_BITE, SoundCategory.PLAYERS, 1.1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.9f, 0.4f);
		world.playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 1.1F, 0.5F);
		world.playSound(loc, Sound.ENTITY_SPIDER_STEP, SoundCategory.PLAYERS, 1.1F, 0.5F);
		new PartialParticle(Particle.BLOCK_CRACK, centered, 75, 0.1, 0.1, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK)).spawnAsPlayerActive(p);
		new PartialParticle(Particle.BLOCK_CRACK, centered, 75, 0.1, 0.1, 0.1, Bukkit.createBlockData(Material.REDSTONE_BLOCK)).spawnAsPlayerActive(p);

		ArrayList<Vector> helix = new ArrayList<>();
		for (double d = 0; d < 3 * Math.PI; d += 2 * Math.PI / 40) {
			helix.add(new Vector((0 + d / 5) * FastUtils.cos(d), 0, (0 + d / 5) * FastUtils.sin(d)));
			helix.add(new Vector((0 - d / 5) * FastUtils.cos(d), 0, (0 - d / 5) * FastUtils.sin(d)));
		}

		new BukkitRunnable() {
			int mTicks = 0;
			double mParticleIncrement = 0.2;

			@Override
			public void run() {
				if (mTicks < 10) {
					world.playSound(centered, Sound.ENTITY_VEX_DEATH, 1F, 2F);
					world.playSound(centered, Sound.BLOCK_SAND_PLACE, 1F, 0.8F);
					for (int i = 0; i < 12; i++) {
						new PartialParticle(Particle.BLOCK_CRACK, centered.clone().add(helix.get(i + mTicks * 12)), 10, 0.2, mParticleIncrement, 0.2, Bukkit.createBlockData(Material.BIRCH_LEAVES)).spawnAsPlayerActive(p);
						new PartialParticle(Particle.FALLING_DUST, centered.clone().add(helix.get(i + mTicks * 12)), 1, 0.2, mParticleIncrement, 0.2, 0, Bukkit.createBlockData(Material.SAND)).spawnAsPlayerActive(p);
					}
				} else {
					this.cancel();
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT < 20) {
								world.playSound(centered, Sound.ENTITY_VEX_DEATH, 1F, 2F);
								new PartialParticle(Particle.BLOCK_CRACK, centered, 80, 0.8, 0.75, 0.8, Bukkit.createBlockData(Material.BIRCH_LEAVES)).spawnAsPlayerActive(p);
							} else {
								this.cancel();
								world.playSound(centered, Sound.ENTITY_VEX_DEATH, 1.25F, 1.5F);
							}
							mT++;
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 2);
				}
				mTicks++;
				mParticleIncrement += 0.05;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}


	@Override
	public Material getDisplayItem() {
		return Material.MELON_SEEDS;
	}
}
