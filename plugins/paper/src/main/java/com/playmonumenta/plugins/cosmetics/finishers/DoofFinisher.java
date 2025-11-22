package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

// https://musescore.com/squixter/deinc
// https://upload.wikimedia.org/wikipedia/commons/7/73/Clef_Diagram.png
// https://minecraft.wiki/w/Note_Block#Instruments
public class DoofFinisher implements EliteFinisher {
	public static final String NAME = "Doof";
	public static final int HALF_BEATS_PER_BAR = 8;

	public enum Instrument {
		PIANO_ALTO_1(0, Sound.BLOCK_NOTE_BLOCK_HARP),
		PIANO_ALTO_2(0, Sound.BLOCK_NOTE_BLOCK_HARP),
		// Because Mojang made the scales 2 octaves of F# to F# instead of C to C
		PIANO_ALTO_3(-1, Sound.BLOCK_NOTE_BLOCK_GUITAR),
		PIANO_BASS_1(-1, Sound.BLOCK_NOTE_BLOCK_GUITAR),
		PIANO_BASS_2(-1, Sound.BLOCK_NOTE_BLOCK_GUITAR),

		CONTRABASS(-2, Sound.BLOCK_NOTE_BLOCK_BASS),

		// One semitone away from using the intended Iron Xylophone (Vibraphone) :suffer:
		VIBRAPHONE(1, Sound.BLOCK_NOTE_BLOCK_COW_BELL),

		// A whole octave off from using actual Glockenspiel (bells)
		GLOCKENSPIEL(1, Sound.BLOCK_NOTE_BLOCK_COW_BELL),
		;

		public final int mOctaveOffset;
		public final Sound mSound;

		Instrument(int octaveOffset, Sound sound) {
			mOctaveOffset = octaveOffset;
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new PlayingDoof(p, killedMob, loc);
	}

	public static class PlayingDoof extends BukkitRunnable implements PlayingFinisher {
		Player mPlayer;
		int mTicks = 0;
		List<Location> mLocations = new ArrayList<>();

		public PlayingDoof(Player p, Entity killedMob, Location loc) {
			mPlayer = p;
			registerKill(killedMob, loc);
			CosmeticsManager.getInstance().registerPlayingFinisher(this);
			this.runTaskTimer(Plugin.getInstance(), 0, 4);
		}

		@Override
		public UUID playerUuid() {
			return mPlayer.getUniqueId();
		}

		@Override
		public void registerKill(Entity killedMob, Location loc) {
			Location loc2;
			if (killedMob instanceof LivingEntity livingEntity) {
				loc2 = livingEntity.getEyeLocation();
			} else {
				loc2 = loc.clone().add(0.0, 1.7, 0.0);
			}
			mLocations.add(loc2);
		}

		@Override
		public void run() {
			if (mLocations.isEmpty()) {
				return;
			}

			int bar = mTicks / HALF_BEATS_PER_BAR + 1;
			int halfBeatInBar = mTicks % HALF_BEATS_PER_BAR;

			switch (bar) {
				case 1 -> {
					switch (halfBeatInBar) {
						case 0 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.F4);
							playNote(Instrument.PIANO_ALTO_2, Note.C4);
							playNote(Instrument.PIANO_ALTO_3, Note.A4);
							playNote(Instrument.PIANO_BASS_2, Note.D4);

							playNote(Instrument.CONTRABASS, Note.D5);

							playNote(Instrument.VIBRAPHONE, Note.C4);

							playNote(Instrument.GLOCKENSPIEL, Note.C4);
						}
						case 1 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.D4);
							playNote(Instrument.PIANO_ALTO_2, Note.B3);
							playNote(Instrument.PIANO_ALTO_3, Note.F4);
							playNote(Instrument.PIANO_BASS_2, Note.D4);

							playNote(Instrument.VIBRAPHONE, Note.A3);

							playNote(Instrument.GLOCKENSPIEL, Note.A3);
						}
						case 2 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.F4);
							playNote(Instrument.PIANO_ALTO_2, Note.C4);
							playNote(Instrument.PIANO_ALTO_3, Note.A4);
							playNote(Instrument.PIANO_BASS_2, Note.D4);

							playNote(Instrument.VIBRAPHONE, Note.C4);

							playNote(Instrument.GLOCKENSPIEL, Note.C4);
						}
						case 4 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.A4);
							playNote(Instrument.PIANO_ALTO_2, Note.E4);
							playNote(Instrument.PIANO_ALTO_3, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.E4);

							playNote(Instrument.CONTRABASS, Note.E5);

							playNote(Instrument.VIBRAPHONE, Note.E4);

							playNote(Instrument.GLOCKENSPIEL, Note.E4);
						}
						case 5 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.F4);
							playNote(Instrument.PIANO_ALTO_2, Note.C4);
							playNote(Instrument.PIANO_ALTO_3, Note.A4);
							playNote(Instrument.PIANO_BASS_2, Note.E4);

							playNote(Instrument.VIBRAPHONE, Note.C4);

							playNote(Instrument.GLOCKENSPIEL, Note.C4);
						}
						case 6 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.A4);
							playNote(Instrument.PIANO_ALTO_2, Note.E5);
							playNote(Instrument.PIANO_ALTO_3, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.E5);

							playNote(Instrument.VIBRAPHONE, Note.E4);

							playNote(Instrument.GLOCKENSPIEL, Note.E4);
						}
						default -> {
						}
					}
				}
				case 2 -> {
					switch (halfBeatInBar) {
						case 0 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.B4);
							playNote(Instrument.PIANO_ALTO_2, Note.G4);
							playNote(Instrument.PIANO_ALTO_3, Note.D5);
							playNote(Instrument.PIANO_BASS_1, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.A3);

							playNote(Instrument.CONTRABASS, Note.A4);

							playNote(Instrument.VIBRAPHONE, Note.G4);

							playNote(Instrument.GLOCKENSPIEL, Note.G4);
						}
						case 2 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.B4);
							playNote(Instrument.PIANO_ALTO_2, Note.G4);
							playNote(Instrument.PIANO_ALTO_3, Note.D5);
							playNote(Instrument.PIANO_BASS_1, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.A3);

							playNote(Instrument.VIBRAPHONE, Note.G4);

							playNote(Instrument.GLOCKENSPIEL, Note.G4);
						}
						case 3 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.B4);
							playNote(Instrument.PIANO_ALTO_2, Note.G4);
							playNote(Instrument.PIANO_ALTO_3, Note.D5);
							playNote(Instrument.PIANO_BASS_1, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.A3);

							playNote(Instrument.CONTRABASS, Note.A4);

							playNote(Instrument.VIBRAPHONE, Note.G4);

							playNote(Instrument.GLOCKENSPIEL, Note.G4);
						}
						case 5 -> {
							playNote(Instrument.PIANO_ALTO_1, Note.B4);
							playNote(Instrument.PIANO_ALTO_2, Note.G4);
							playNote(Instrument.PIANO_ALTO_3, Note.D5);
							playNote(Instrument.PIANO_BASS_1, Note.C5);
							playNote(Instrument.PIANO_BASS_2, Note.A3);

							playNote(Instrument.CONTRABASS, Note.A4);

							playNote(Instrument.VIBRAPHONE, Note.G4);

							playNote(Instrument.GLOCKENSPIEL, Note.G4);
						}
						default -> {
						}
					}
				}
				case 3 -> {
					switch (halfBeatInBar) {
						case 2 -> CosmeticsManager.getInstance().cancelPlayingFinisher(mPlayer);
						default -> {
						}
					}
				}
				default -> CosmeticsManager.getInstance().cancelPlayingFinisher(mPlayer);
			}

			mTicks++;
		}

		@Override
		public synchronized void cancel() throws IllegalStateException {
			super.cancel();
		}

		@SuppressWarnings("EnumOrdinal")
		public void playNote(Instrument instrument, Note note) {
			int minOctave = Integer.MAX_VALUE;
			int maxOctave = Integer.MIN_VALUE;
			for (Instrument testInstrument : Instrument.values()) {
				minOctave = Math.min(minOctave, testInstrument.mOctaveOffset);
				maxOctave = Math.max(maxOctave, testInstrument.mOctaveOffset);
			}
			minOctave--;
			maxOctave++;

			int minClicksAdjusted = minOctave * 12;
			int maxClicksAdjusted = maxOctave * 12;

			int noteClicksAdjusted = note.mClicks + 12 * instrument.mOctaveOffset;

			double relativeNotePosition = 1.0 * (noteClicksAdjusted - minClicksAdjusted) / (maxClicksAdjusted - minClicksAdjusted);

			List<Location> locs = new ArrayList<>();
			if (mLocations.isEmpty()) {
				locs.add(mPlayer.getLocation());
			} else {
				for (int i = 0; i < mLocations.size(); i++) {
					if (i % Instrument.values().length % mLocations.size() == instrument.ordinal() % mLocations.size()) {
						locs.add(mLocations.get(i));
					}
				}
			}

			for (Location loc : locs) {
				loc.getWorld().playSound(loc, instrument.mSound, SoundCategory.PLAYERS, 1F, note.mPitch);
				Location particleLoc = loc.clone();
				particleLoc.setPitch(0);
				particleLoc.setYaw(particleLoc.getYaw() + 90.0F);
				particleLoc.add(particleLoc.getDirection().clone().multiply(relativeNotePosition - 0.5));
				new PartialParticle(Particle.NOTE,
					particleLoc,
					1,
					relativeNotePosition,
					0.0,
					0.0,
					1.0,
					null,
					true).minimumCount(1).spawnAsPlayerActive(mPlayer);
			}
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOAT_HORN;
	}
}
