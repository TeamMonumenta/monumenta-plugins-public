package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PoultryficationFinisher implements EliteFinisher {
	public static final String NAME = "Poultryfication";
	public static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 7);

	private static final HashMap<UUID, Integer> mMobsKilled = new HashMap<>();

	private enum Instrument {
		TREBLE(Sound.BLOCK_NOTE_BLOCK_BANJO),
		BASE(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO);

		public final Sound mSound;

		Instrument(Sound sound) {
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		int mobsKilled = mMobsKilled.getOrDefault(p.getUniqueId(), 1);
		int barOffset = 4 * (mobsKilled - 1) + 1;

		new BukkitRunnable() {
			int mTicks = 0;
			@Nullable Chicken mChicken;
			Location mLastNoteLocation = loc;

			@Override public void run() {
				if (mTicks == 0) {
					new PartialParticle(Particle.REDSTONE, loc, 50, 0.5, 1, 0.5, WHITE).spawnAsPlayerActive(p);
					mChicken = (Chicken) LibraryOfSoulsIntegration.summon(loc, "Poultry");
					Objects.requireNonNull(mChicken).addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
					hurtChicken();
				}

				if (mTicks >= 0 && mTicks % 3 == 0) {
					if (mChicken != null && mChicken.isValid()) {
						mLastNoteLocation = mChicken.getEyeLocation();
					}

					int bar = mTicks / 24 + barOffset;
					int halfBeatInBar = (mTicks % 24) / 3;

					switch (bar) {
						case 1, 9 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.G4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.A4);
									playNote(Instrument.BASE, Note.A3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.A4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.C4);
									playNote(Instrument.BASE, Note.B4);
									playNote(Instrument.BASE, Note.B3);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.E4);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						case 2, 10 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.G4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.A4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.C4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.E4);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						case 3, 11 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.G4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.A4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.C5);
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.C5);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.B4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.BASE, Note.D5);
									playNote(Instrument.BASE, Note.D4);
								}
								default -> {
								}
							}
						}
						case 4, 12 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.B4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 2 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 4 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 6 -> {
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.BASE, Note.D5);
									playNote(Instrument.BASE, Note.D4);
								}
								default -> {
								}
							}
						}
						case 5, 6, 13, 14 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.F4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.G4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.TREBLE, Note.B3);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.D4);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.BASE, Note.D5);
									playNote(Instrument.BASE, Note.D4);
								}
								default -> {
								}
							}
						}
						case 7 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.F4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.G4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.B4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.F5);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.B4);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						case 8 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 2 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 4 -> {
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.TREBLE, Note.D4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 6 -> {
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.C4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						case 15 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.G4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.A4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.BASE, Note.A4);
									playNote(Instrument.BASE, Note.A3);
								}
								case 3 -> playNote(Instrument.TREBLE, Note.A4);
								case 4 -> {
									playNote(Instrument.TREBLE, Note.B4);
									playNote(Instrument.TREBLE, Note.F4);
									playNote(Instrument.BASE, Note.B4);
									playNote(Instrument.BASE, Note.B3);
								}
								case 5 -> playNote(Instrument.TREBLE, Note.B4);
								case 6 -> {
									playNote(Instrument.TREBLE, Note.C5);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						case 16 -> {
							switch (halfBeatInBar) {
								case 0 -> {
									playNote(Instrument.TREBLE, Note.B4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E5);
								}
								case 1 -> playNote(Instrument.TREBLE, Note.A4);
								case 2 -> {
									playNote(Instrument.TREBLE, Note.G4);
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.BASE, Note.C5);
									playNote(Instrument.BASE, Note.G4);
								}
								case 4 -> {
									playNote(Instrument.TREBLE, Note.E4);
									playNote(Instrument.TREBLE, Note.C4);
									playNote(Instrument.BASE, Note.G4);
									playNote(Instrument.BASE, Note.G3);
								}
								case 6 -> {
									playNote(Instrument.TREBLE, Note.C4);
									playNote(Instrument.TREBLE, Note.G3);
									playNote(Instrument.BASE, Note.E4);
									playNote(Instrument.BASE, Note.C4);
								}
								default -> {
								}
							}
						}
						default -> {
						}
					}
				}

				int chickenTicks = mTicks + 2;
				if (chickenTicks >= 0 && chickenTicks % 3 == 0) {
					if (mChicken != null && mChicken.isValid()) {
						mLastNoteLocation = mChicken.getEyeLocation();
					}

					int bar = chickenTicks / 24 + 1;
					int halfBeatInBar = (chickenTicks % 24) / 3;

					switch (bar) {
						case 4, 8, 12, 16 -> {
							switch (halfBeatInBar) {
								case 0, 2, 4, 6 -> hurtChicken();
								default -> {
								}
							}
						}
						default -> {
						}
					}
				}

				if (mTicks > 24 * 4 - 3) {
					if (mChicken != null) {
						new PartialParticle(Particle.REDSTONE, mChicken.getLocation(), 50,
							0.5, 1, 0.5, WHITE).spawnAsPlayerActive(p);
						mChicken.remove();
						mChicken = null;
					}
					if (mobsKilled >= 4) {
						mMobsKilled.put(p.getUniqueId(), 1);
					} else {
						mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
					}
					this.cancel();
				}

				mTicks++;
			}

			public void hurtChicken() {
				if (mChicken != null && mChicken.isValid()) {
					mChicken.setInvulnerable(false);
					DamageUtils.damage(p, mChicken, DamageEvent.DamageType.TRUE, 0, null, true);
					mChicken.setInvulnerable(true);
					mChicken.getWorld().playSound(mLastNoteLocation, Sound.ENTITY_CHICKEN_HURT, SoundCategory.PLAYERS, 1F, 1F);
				}
			}

			public void playNote(Instrument instrument, Note note) {
				mLastNoteLocation.getWorld().playSound(mLastNoteLocation, instrument.mSound, SoundCategory.PLAYERS, 1F, note.mPitch);
				Location particleLoc = mLastNoteLocation.clone();
				particleLoc.setPitch(0);
				particleLoc.setYaw(particleLoc.getYaw() + 90.0F);
				particleLoc.add(particleLoc.getDirection().clone().multiply(0.7 * (note.mNoteParticleValue - 0.5)));
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

	@Override public Material getDisplayItem() {
		return Material.CHICKEN;
	}
}
