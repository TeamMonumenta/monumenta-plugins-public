package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class VictoryThemeFinisher implements EliteFinisher {

	public static final String NAME = "Victory Theme";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		Location loc1 = p.getEyeLocation();
		loc1.setPitch(0.0F);
		Vector horizontalDir = VectorUtils.crossProd(loc1.getDirection(), new Vector(0.0, 1.0, 0.0));
		Location loc2 = loc1.clone().add(horizontalDir);
		Location loc3 = loc1.clone().subtract(horizontalDir);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 3, 6, 9 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 19 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.FS12);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.B17);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 28 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS14);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS19);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 37 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 43 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS14);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS19);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 46 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS21);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					default -> {
					}
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
