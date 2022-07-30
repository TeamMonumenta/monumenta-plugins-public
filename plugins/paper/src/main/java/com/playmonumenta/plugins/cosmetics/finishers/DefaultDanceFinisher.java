package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
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
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 10:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 12:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, Constants.NotePitches.FS12);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 16:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 18:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 22:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 24:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, Constants.NotePitches.FS12);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 30:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 36:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, Constants.NotePitches.FS12);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 48:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, Constants.NotePitches.FS12);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
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
