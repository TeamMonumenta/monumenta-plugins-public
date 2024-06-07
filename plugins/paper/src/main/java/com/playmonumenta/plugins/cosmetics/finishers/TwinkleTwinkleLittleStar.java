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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

// https://musescore.com/user/32009255/scores/6469135
// https://en.wikipedia.org/wiki/Key_signature#Scales_with_sharp_key_signatures
// https://minecraft.wiki/w/Note_Block#Mob_heads
public class TwinkleTwinkleLittleStar implements EliteFinisher {
	public static final String NAME = "Twinkle Twinkle Little Star";
	public static final int QUARTER_BEATS_PER_BAR = 16;
	public static final int BARS_PER_KILL = 2;
	public static final int KILLS_TO_RESTART = 6;

	public enum Instrument {
		SOLO_CHIMES(Sound.BLOCK_NOTE_BLOCK_CHIME);

		public final Sound mSound;

		Instrument(Sound sound) {
			mSound = sound;
		}
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new PlayingTwinkleTwinkleLittleStar(p, killedMob, loc);
	}

	public static class PlayingTwinkleTwinkleLittleStar extends BukkitRunnable implements PlayingFinisher {
		Player mPlayer;
		int mTicks = 0;
		List<Location> mLocations = new ArrayList<>();

		public PlayingTwinkleTwinkleLittleStar(Player p, Entity killedMob, Location loc) {
			mPlayer = p;
			registerKill(killedMob, loc);
			CosmeticsManager.getInstance().registerPlayingFinisher(this);
			this.runTaskTimer(Plugin.getInstance(), 0, 3);
		}

		@Override
		public UUID playerUuid() {
			return mPlayer.getUniqueId();
		}

		@Override
		public void registerKill(Entity killedMob, Location loc) {
			mLocations.add(loc);
		}

		@Override
		public void run() {
			if (mLocations.isEmpty()) {
				return;
			}

			int bar = mTicks / QUARTER_BEATS_PER_BAR + 1;
			int quarterBeatInBar = mTicks % QUARTER_BEATS_PER_BAR;

			switch (bar) {
				// Kill 1
				case 1 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						default -> {
						}
					}
				}
				case 2 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.FS4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.FS4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						default -> {
						}
					}
				}
				// Kill 2
				case 3 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						default -> {
						}
					}
				}
				case 4 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						default -> {
						}
					}
				}
				// Kill 3
				case 5 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						default -> {
						}
					}
				}
				case 6 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						default -> {
						}
					}
				}
				// Kill 4
				case 7 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						default -> {
						}
					}
				}
				case 8 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						default -> {
						}
					}
				}
				// Kill 5
				case 9 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						default -> {
						}
					}
				}
				case 10 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.FS4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.FS4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.E4);
						}
						default -> {
						}
					}
				}
				// Kill 6
				case 11 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.D4);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						case 12 -> {
							playNote(Instrument.SOLO_CHIMES, Note.CS4);
						}
						default -> {
						}
					}
				}
				case 12 -> {
					switch (quarterBeatInBar) {
						case 0 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						case 4 -> {
							playNote(Instrument.SOLO_CHIMES, Note.B3);
						}
						case 8 -> {
							playNote(Instrument.SOLO_CHIMES, Note.A3);
						}
						default -> {
						}
					}
				}
				default -> {
				}
			}

			mTicks++;
			if (mTicks % (QUARTER_BEATS_PER_BAR * BARS_PER_KILL) == 0) {
				pause();
				if (mTicks == QUARTER_BEATS_PER_BAR * BARS_PER_KILL * KILLS_TO_RESTART) {
					CosmeticsManager.getInstance().cancelPlayingFinisher(mPlayer);
				}
			}
		}

		public void pause() {
			mLocations.clear();
		}

		@Override
		public synchronized void cancel() throws IllegalStateException {
			super.cancel();
		}

		public void playNote(Instrument instrument, Note note) {
			List<Location> locs = new ArrayList<>();
			if (mLocations.isEmpty()) {
				locs.add(mPlayer.getLocation());
			} else {
				for (int i = 0; i < mLocations.size(); i++) {
					if (i % mLocations.size() % Instrument.values().length == instrument.ordinal() % Instrument.values().length) {
						locs.add(mLocations.get(i));
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

	@Override
	public Material getDisplayItem() {
		return Material.NOTE_BLOCK;
	}
}
