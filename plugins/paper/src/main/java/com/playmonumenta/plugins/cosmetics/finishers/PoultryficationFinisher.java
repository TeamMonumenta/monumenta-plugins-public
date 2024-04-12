package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.ArrayList;
import java.util.List;
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

public class PoultryficationFinisher implements EliteFinisher {
	public static final String NAME = "Poultryfication";
	public static final int TICKS_PER_HALF_BEAT = 3;
	public static final int HALF_BEATS_PER_BAR = 8;
	public static final int TICKS_PER_BAR = TICKS_PER_HALF_BEAT * HALF_BEATS_PER_BAR;
	public static final int BARS_PER_KILL = 4;
	public static final int KILLS_TO_RESTART = 4;
	public static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 7);

	public enum Instrument {
		TREBLE(Sound.BLOCK_NOTE_BLOCK_BANJO),
		BASE(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO);

		public final Sound mSound;

		Instrument(Sound sound) {
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new PlayingPoultryfication(p, killedMob, loc);
	}

	public static class PlayingPoultryfication extends BukkitRunnable implements PlayingFinisher {
		Player mPlayer;
		int mTicks = 0;
		List<Chicken> mChickens = new ArrayList<>();

		public PlayingPoultryfication(Player p, Entity killedMob, Location loc) {
			mPlayer = p;
			registerKill(killedMob, loc);
			CosmeticsManager.getInstance().registerPlayingFinisher(this);
			this.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		@Override
		public UUID playerUuid() {
			return mPlayer.getUniqueId();
		}

		@Override
		public void registerKill(Entity killedMob, Location loc) {
			new PartialParticle(Particle.REDSTONE, loc, 50, 0.5, 1, 0.5, WHITE).spawnAsPlayerActive(mPlayer);
			Chicken chicken = (Chicken) LibraryOfSoulsIntegration.summon(loc, "Poultry");
			Objects.requireNonNull(chicken).addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			hurtChicken(chicken);
			mChickens.add(chicken);
		}

		@Override
		public void run() {
			if (mChickens.isEmpty()) {
				return;
			}
			if (mTicks % TICKS_PER_HALF_BEAT == 0) {
				int bar = mTicks / TICKS_PER_BAR + 1;
				int halfBeatInBar = (mTicks % TICKS_PER_BAR) / TICKS_PER_HALF_BEAT;

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
			if (chickenTicks >= 0 && chickenTicks % TICKS_PER_HALF_BEAT == 0) {
				int bar = chickenTicks / TICKS_PER_BAR + 1;
				int halfBeatInBar = (chickenTicks % TICKS_PER_BAR) / TICKS_PER_HALF_BEAT;

				switch (bar) {
					case 4, 8, 12, 16 -> {
						switch (halfBeatInBar) {
							case 0, 2, 4, 6 -> hurtChickens();
							default -> {
							}
						}
					}
					default -> {
					}
				}
			}

			mTicks++;

			if (mTicks % (TICKS_PER_BAR * BARS_PER_KILL) == 0) {
				pause();
				if (mTicks == TICKS_PER_BAR * BARS_PER_KILL * KILLS_TO_RESTART) {
					CosmeticsManager.getInstance().cancelPlayingFinisher(mPlayer);
				}
			}
		}

		public void pause() {
			for (Chicken chicken : mChickens) {
				if (chicken != null) {
					new PartialParticle(Particle.REDSTONE, chicken.getLocation(), 50,
						0.5, 1, 0.5, WHITE).spawnAsPlayerActive(mPlayer);
					chicken.remove();
				}
			}
			mChickens.clear();
		}

		@Override
		public synchronized void cancel() throws IllegalStateException {
			super.cancel();
			pause();
		}

		public void hurtChickens() {
			for (Chicken chicken : mChickens) {
				hurtChicken(chicken);
			}
		}

		public void hurtChicken(Chicken chicken) {
			if (chicken != null && chicken.isValid()) {
				chicken.setInvulnerable(false);
				DamageUtils.damage(mPlayer, chicken, DamageEvent.DamageType.TRUE, 0, null, true);
				chicken.setInvulnerable(true);
				chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_CHICKEN_HURT, SoundCategory.PLAYERS, 1F, 1F);
			}
		}

		public void playNote(Instrument instrument, Note note) {
			List<Location> locs = new ArrayList<>();
			if (mChickens.isEmpty()) {
				locs.add(mPlayer.getLocation());
			} else {
				for (int i = 0; i < mChickens.size(); i++) {
					if (i % mChickens.size() == instrument.ordinal() % mChickens.size()) {
						locs.add(mChickens.get(i).getLocation());
					}
				}
			}

			for (Location loc : locs) {
				loc.getWorld().playSound(loc, instrument.mSound, SoundCategory.PLAYERS, 1F, note.mPitch);
				Location particleLoc = loc.clone();
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
					true).minimumCount(1).spawnAsPlayerActive(mPlayer);
			}
		}
	}

	@Override public Material getDisplayItem() {
		return Material.CHICKEN;
	}
}
