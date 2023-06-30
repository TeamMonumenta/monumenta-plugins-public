package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Woolerman implements EliteFinisher {
	public static final String NAME = "Woolerman";

	// https://musescore.com/user/36153916/scores/6376624

	private enum Instrument {
		LEAD(Sound.BLOCK_NOTE_BLOCK_HARP),
		TENOR(Sound.BLOCK_NOTE_BLOCK_HARP),
		TENOR_2(Sound.BLOCK_NOTE_BLOCK_HARP),
		BARITONE(Sound.BLOCK_NOTE_BLOCK_BASS),
		BASS(Sound.BLOCK_NOTE_BLOCK_BASS);

		public final Sound mSound;

		Instrument(Sound sound) {
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		final World world = loc.getWorld();
		Location songLocTemp = killedMob.getLocation().clone()
			.setDirection(p.getLocation().toVector().clone()
				.subtract(loc.toVector()))
			.add(new Vector(0.0, killedMob.getHeight() + 0.1, 0.0));
		songLocTemp.setPitch(0F);
		final Location songLoc = songLocTemp.clone();
		final Vector noteOffsetUnitVector = VectorUtils.crossProd(songLoc.getDirection(),
			new Vector(0.0F, 1.0F, 0.0F));

		new BukkitRunnable() {
			int mTicks = -1;
			final ArmorStand mLyrics = world.spawn(songLoc.clone().subtract(new Vector(0.0F, 0.8F, 0.0F)),
				ArmorStand.class,
				armorStand -> {
					armorStand.setSilent(true);
					armorStand.setVisible(false);
					armorStand.setGravity(false);
					armorStand.setMarker(true);
					armorStand.setSmall(true);
					armorStand.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			});

			@Override public void run() {
				mTicks++;
				if (mTicks == 19) {
					// Disable poof animation, but keep the keeling over part
					killedMob.remove();
				}
				if (mTicks % 3 != 0) {
					return;
				}

				int sixteenthNote = mTicks / 3;
				int bar = sixteenthNote / 16 + 21;
				sixteenthNote %= 16;

				switch (bar) {
					case 21 -> {
						switch (sixteenthNote) {
							case 0 -> {
								setLyric("Soon");
								playNote(Instrument.LEAD, Note.C4);
								playNote(Instrument.TENOR, Note.C5);
								playNote(Instrument.TENOR_2, Note.EB4);
								playNote(Instrument.BARITONE, Note.AB4);
								playNote(Instrument.BASS, Note.AB4);
							}
							case 4 -> {
								setLyric("may");
								playNote(Instrument.LEAD, Note.C4);
								playNote(Instrument.TENOR, Note.C5);
								playNote(Instrument.TENOR_2, Note.EB4);
								playNote(Instrument.BARITONE, Note.AB4);
								playNote(Instrument.BASS, Note.AB4);
							}
							case 7 -> {
								setLyric("the");
								playNote(Instrument.LEAD, Note.AB3);
								playNote(Instrument.TENOR, Note.AB4);
								playNote(Instrument.TENOR_2, Note.C4);
								playNote(Instrument.BARITONE, Note.AB4);
								playNote(Instrument.BASS, Note.AB4);
							}
							case 8 -> {
								setLyric("Wool");
								playNote(Instrument.LEAD, Note.BB3);
								playNote(Instrument.TENOR, Note.BB4);
								playNote(Instrument.TENOR_2, Note.BB3);
								playNote(Instrument.BARITONE, Note.EB5);
								playNote(Instrument.BASS, Note.EB5);
							}
							case 9 -> {
								setLyric("ler");
								playNote(Instrument.LEAD, Note.BB3);
								playNote(Instrument.TENOR, Note.BB4);
								playNote(Instrument.TENOR_2, Note.BB3);
								playNote(Instrument.BARITONE, Note.EB5);
								playNote(Instrument.BASS, Note.EB5);
							}
							case 10 -> {
								setLyric("man");
								playNote(Instrument.LEAD, Note.G3);
								playNote(Instrument.TENOR, Note.G4);
								playNote(Instrument.TENOR_2, Note.BB3);
								playNote(Instrument.BARITONE, Note.EB5);
								playNote(Instrument.BASS, Note.EB5);
							}
							case 12 -> {
								setLyric("come");
								playNote(Instrument.LEAD, Note.G3);
								playNote(Instrument.TENOR, Note.G4);
								playNote(Instrument.TENOR_2, Note.BB3);
								playNote(Instrument.BARITONE, Note.EB5);
								playNote(Instrument.BASS, Note.EB5);
							}
							case 15 -> clearLyric();
							default -> {
							}
						}
					}
					case 22 -> {
						mLyrics.remove();
						cancel();
					}
					default -> {
					}
				}
			}

			public void setLyric(String lyric) {
				mLyrics.customName(Component.text(lyric, TextColor.color(0x3f3f3fff)));
				mLyrics.setCustomNameVisible(true);
			}

			public void clearLyric() {
				mLyrics.setCustomNameVisible(false);
				mLyrics.customName(Component.empty());
			}

			public void playNote(Instrument instrument, Note note) {
				world.playSound(songLoc, instrument.mSound, SoundCategory.PLAYERS, 1F, note.mPitch);

				double noteOffset;
				switch (instrument) {
					case LEAD, TENOR, TENOR_2 -> noteOffset = 0.5 * note.mNoteParticleValue + 0.5;
					case BARITONE, BASS -> noteOffset = 0.5 * note.mNoteParticleValue;
					default -> noteOffset = note.mNoteParticleValue;
				}

				Location particleLoc = songLoc.clone();
				particleLoc.setPitch(0);
				particleLoc.add(noteOffsetUnitVector
					.clone()
					.multiply(0.7 * (noteOffset - 0.5)));
				// TODO spawn wool items and kill them off in a few ticks instead
				new PartialParticle(Particle.NOTE,
					particleLoc,
					1,
					note.mNoteParticleValue,
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
		return Material.LIGHT_BLUE_WOOL;
	}
}
