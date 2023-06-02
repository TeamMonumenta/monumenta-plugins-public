package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashMap;
import java.util.UUID;
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

public class Promenade implements EliteFinisher {

	public static final String NAME = "Promenade";

	private static final HashMap<UUID, Integer> mMobsKilled = new HashMap<>();

	private enum Instrument {
		TREBLE(Sound.BLOCK_NOTE_BLOCK_HARP),
		BASS(Sound.BLOCK_NOTE_BLOCK_BASS);

		public final Sound mSound;

		Instrument(Sound sound) {
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		int mobsKilled = mMobsKilled.getOrDefault(p.getUniqueId(), 1);
		final int startTick;
		if (mobsKilled == 2) {
			startTick = 66;
		} else {
			startTick = 0;
		}

		World world = p.getWorld();
		Location loc1 = killedMob.getLocation().add(0, 1.7, 0);
		loc1.setPitch(0.0F);
		final Location eyeLoc = loc1;
		final Vector horizontalDir = VectorUtils.crossProd(eyeLoc.getDirection(), new Vector(0.0, 1.0, 0.0));
		final Location bassLoc = eyeLoc.clone().subtract(horizontalDir);

		new BukkitRunnable() {
			int mTicks = startTick;

			@Override
			public void run() {
				switch (mTicks) {
					case 0 -> playNote(Instrument.TREBLE, Note.G4);
					case 6 -> playNote(Instrument.TREBLE, Note.F4);
					case 12 -> playNote(Instrument.TREBLE, Note.AS4);
					case 18 -> playNote(Instrument.TREBLE, Note.C5);
					case 21 -> playNote(Instrument.TREBLE, Note.F5);
					case 24 -> playNote(Instrument.TREBLE, Note.D5);
					case 30 -> playNote(Instrument.TREBLE, Note.C5);
					case 33 -> playNote(Instrument.TREBLE, Note.F5);
					case 36 -> playNote(Instrument.TREBLE, Note.D5);
					case 42 -> playNote(Instrument.TREBLE, Note.AS4);
					case 48 -> playNote(Instrument.TREBLE, Note.C5);
					case 54 -> playNote(Instrument.TREBLE, Note.G4);
					case 60 -> {
						playNote(Instrument.TREBLE, Note.F4);

						if (mobsKilled >= 2) {
							mMobsKilled.put(p.getUniqueId(), 1);
						} else {
							mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
						}
						this.cancel();
						return;
					}
					case 66 -> {
						playNote(Instrument.TREBLE, Note.G4);
						playNote(Instrument.TREBLE, Note.D4);
						playNote(Instrument.TREBLE, Note.AS3);
						playNote(Instrument.BASS, Note.G4);
						playNote(Instrument.BASS, Note.G3);
					}
					case 72 -> {
						playNote(Instrument.TREBLE, Note.F4);
						playNote(Instrument.TREBLE, Note.C4);
						playNote(Instrument.TREBLE, Note.A3);
						playNote(Instrument.BASS, Note.F4);
						playNote(Instrument.BASS, Note.A3);
					}
					case 78 -> {
						playNote(Instrument.TREBLE, Note.AS4);
						playNote(Instrument.TREBLE, Note.D4);
						playNote(Instrument.TREBLE, Note.AS3);
						playNote(Instrument.BASS, Note.G4);
						playNote(Instrument.BASS, Note.G3);
					}
					case 84 -> {
						playNote(Instrument.TREBLE, Note.C5);
						playNote(Instrument.TREBLE, Note.A4);
						playNote(Instrument.TREBLE, Note.C4);
						playNote(Instrument.BASS, Note.F4);
					}
					case 87 -> playNote(Instrument.TREBLE, Note.F5);
					case 90 -> {
						playNote(Instrument.TREBLE, Note.D5);
						playNote(Instrument.TREBLE, Note.A4);
						playNote(Instrument.TREBLE, Note.F4);
						playNote(Instrument.BASS, Note.D4);
					}
					case 96 -> {
						playNote(Instrument.TREBLE, Note.C5);
						playNote(Instrument.TREBLE, Note.A4);
						playNote(Instrument.TREBLE, Note.C4);
						playNote(Instrument.BASS, Note.F4);
					}
					case 99 -> playNote(Instrument.TREBLE, Note.F5);
					case 102 -> {
						playNote(Instrument.TREBLE, Note.D5);
						playNote(Instrument.TREBLE, Note.AS4);
						playNote(Instrument.TREBLE, Note.F4);
						playNote(Instrument.BASS, Note.AS4);
						playNote(Instrument.BASS, Note.AS3);
					}
					case 108 -> {
						playNote(Instrument.TREBLE, Note.AS4);
						playNote(Instrument.TREBLE, Note.G4);
						playNote(Instrument.TREBLE, Note.D4);
						playNote(Instrument.BASS, Note.G4);
						playNote(Instrument.BASS, Note.G3);
					}
					case 114 -> {
						playNote(Instrument.TREBLE, Note.C5);
						playNote(Instrument.TREBLE, Note.G4);
						playNote(Instrument.TREBLE, Note.E4);
						playNote(Instrument.BASS, Note.C4);
					}
					case 120 -> {
						playNote(Instrument.TREBLE, Note.G4);
						playNote(Instrument.TREBLE, Note.C4);
						playNote(Instrument.TREBLE, Note.G3);
						playNote(Instrument.BASS, Note.E4);
					}
					case 126 -> {
						playNote(Instrument.TREBLE, Note.F4);
						playNote(Instrument.TREBLE, Note.C4);
						playNote(Instrument.TREBLE, Note.A3);
						playNote(Instrument.BASS, Note.F4);

						if (mobsKilled >= 2) {
							mMobsKilled.put(p.getUniqueId(), 1);
						} else {
							mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
						}
						this.cancel();
						return;
					}
					default -> {
					}
				}
				if (mTicks >= 126) {
					this.cancel();
				}
				mTicks++;
			}

			public void playNote(Instrument instrument, Note note) {
				world.playSound(eyeLoc, instrument.mSound, SoundCategory.PLAYERS, 1f, note.mPitch);
				Location particleLoc;
				double noteParticleValueOffset;
				if (instrument == Instrument.BASS) {
					particleLoc = bassLoc.clone();
					noteParticleValueOffset = 0.0;
				} else {
					particleLoc = eyeLoc.clone();
					noteParticleValueOffset = 0.5;
				}
				particleLoc.add(horizontalDir.clone().multiply(note.mNoteParticleValue));
				new PartialParticle(Particle.NOTE,
					particleLoc,
					1,
					noteParticleValueOffset + 0.5 * note.mNoteParticleValue,
					0.0,
					0.0,
					1.0,
					null,
					true).minimumCount(1).spawnAsPlayerActive(p);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.LEATHER_BOOTS;
	}
}
