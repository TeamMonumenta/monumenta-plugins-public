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

public class VictoryThemeFinisher implements EliteFinisher {

	public static final String NAME = "Victory Theme";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		loc.add(0, 1.5, 0);
		Location loc2 = loc.clone().add(-1, 0, 0);
		Location loc3 = loc.clone().add(1, 0, 0);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
					case 3:
					case 6:
					case 9:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 19:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS9);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.FS12);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.B17);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 28:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.CS19);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 37:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 43:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.GS14);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.CS19);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 46:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.AS16);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).spawnAsPlayerActive(p);
						break;
					case 47:
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
