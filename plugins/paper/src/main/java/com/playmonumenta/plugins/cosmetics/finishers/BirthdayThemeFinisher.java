package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BirthdayThemeFinisher implements EliteFinisher {

	public static final String NAME = "Birthday Theme";

	private static HashMap<UUID, Integer> mMobsKilled = new HashMap<>();

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		loc.add(0, 1.5, 0);

		if (!mMobsKilled.containsKey(p.getUniqueId())) {
			mMobsKilled.put(p.getUniqueId(), 1);
		}

		int mobsKilled = mMobsKilled.get(p.getUniqueId());

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
					case 6:
						if (mobsKilled >= 1 && mobsKilled <= 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.C6);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 4) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						}
						break;
					case 9:
						if (mobsKilled == 1 || mobsKilled == 2) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.D8);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.C18);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 4) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.A15);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						}
						break;
					case 18:
						if (mobsKilled == 1 || mobsKilled == 2) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.C6);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.A15);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 4) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						}
						break;
					case 27:
						if (mobsKilled == 1 || mobsKilled == 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 2 || mobsKilled == 4) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						}
							break;
					case 36:
						if (mobsKilled == 1 || mobsKilled == 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.E10);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						} else if (mobsKilled == 2 || mobsKilled == 4) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						}

						if (mobsKilled == 1) {
							world.spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.5, 0.5, 0.1);
						} else if (mobsKilled == 2) {
							world.spawnParticle(Particle.ITEM_CRACK, loc, 50, 0.5, 0.5, 0.5, 0.1, new ItemStack(Material.CAKE));
						} else if (mobsKilled == 4) {
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1);
							world.spawnParticle(Particle.FLASH, loc, 1, 0.1, 0.2, 0.1, 0.1);
							world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 50, 0.1, 0.2, 0.1, 0.1);
						}


						break;
					case 45:
						if (mobsKilled == 3) {
							world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.D8);
							world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
							world.spawnParticle(Particle.ITEM_CRACK, loc, 50, 0.5, 0.5, 0.5, 0.1, new ItemStack(Material.CAKE));
						}

						if (mobsKilled >= 4) {
							mMobsKilled.put(p.getUniqueId(), 1);
						} else {
							mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
						}
						this.cancel();
						break;
					default:
						break;
				}
				if (mTicks >= 47) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NOTE_BLOCK;
	}

}
