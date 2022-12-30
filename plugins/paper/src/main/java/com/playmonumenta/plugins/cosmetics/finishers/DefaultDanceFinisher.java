package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DefaultDanceFinisher implements EliteFinisher {

	public static final String NAME = "Default Dance";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		loc.add(0, 1.5, 0);
		Location loc2 = loc.clone().add(-1, 0, 0);
		Location loc3 = loc.clone().add(1, 0, 0);
		BukkitRunnable defaultDanceRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, Constants.NotePitches.FS12);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 10:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 12:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, Constants.NotePitches.FS12);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 16:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 18:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 22:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 24:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, Constants.NotePitches.FS12);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 30:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 36:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, Constants.NotePitches.FS12);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 48:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, Constants.NotePitches.FS12);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 49:
						this.cancel();
						break;
					default:
						break;
				}
				if (mTicks >= 49) {
					this.cancel();
				}
				mTicks++;
			}
		};
		defaultDanceRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SCAFFOLDING;
	}
}
