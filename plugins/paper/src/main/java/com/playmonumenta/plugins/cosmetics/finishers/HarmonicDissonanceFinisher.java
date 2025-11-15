package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HarmonicDissonanceFinisher implements EliteFinisher {
	public static final String NAME = "Harmonic Dissonance";
	private static final HashMap<UUID, List<String>> mMobsKilled = new HashMap<>();
	private static final int MAX_NOTES = 4;
	private static final int MAX_TRACKED_KILLS = MAX_NOTES + 1;
	private static final int EXPIRY_SECONDS = 30;
	private static final NamespacedKey HARMONIC_INSTRUMENT = NamespacedKey.fromString("minecraft:block.note_block.chime");
	private static final NamespacedKey DISSONANT_INSTRUMENT = NamespacedKey.fromString("minecraft:block.note_block.didgeridoo");

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new PlayingHarmonicDissonance(p, killedMob, loc);
	}

	public static class PlayingHarmonicDissonance extends BukkitRunnable implements PlayingFinisher {
		Player mPlayer;
		int mTicks = 0;
		LocalDateTime mExpiryTime = DateUtils.trueUtcDateTime().plusSeconds(EXPIRY_SECONDS);

		public PlayingHarmonicDissonance(Player p, Entity killedMob, Location loc) {
			mPlayer = p;
			registerKill(killedMob, loc);
			CosmeticsManager.getInstance().registerPlayingFinisher(this);
			runTaskTimer(Plugin.getInstance(), 0, 4);
		}

		@Override
		public UUID playerUuid() {
			return mPlayer.getUniqueId();
		}

		@Override
		public void registerKill(Entity killedMob, Location loc) {
			mExpiryTime = DateUtils.trueUtcDateTime().plusSeconds(EXPIRY_SECONDS);
			List<String> recentlyKilled = mMobsKilled.computeIfAbsent(playerUuid(), k -> new ArrayList<>());
			String entityId = killedMob.getType().key().toString();
			StringBuilder killId = new StringBuilder(entityId);
			Component customNameComponent = killedMob.customName();
			if (customNameComponent != null) {
				killId
					.append(":")
					.append(MessagingUtils.plainText(customNameComponent));
			}
			recentlyKilled.add(killId.toString());
			while (recentlyKilled.size() > MAX_TRACKED_KILLS) {
				recentlyKilled.remove(0);
			}
			if (mTicks >= MAX_TRACKED_KILLS) {
				mTicks = 0;
			}
		}

		@Override
		public synchronized void cancel() throws IllegalStateException {
			super.cancel();
			mMobsKilled.remove(mPlayer.getUniqueId());
		}

		@Override
		public void run() {
			if (DateUtils.trueUtcDateTime().isAfter(mExpiryTime)) {
				// If you don't kill more mobs for a while, the kill streak resets
				CosmeticsManager.getInstance().cancelPlayingFinisher(mPlayer);
				return;
			}

			List<String> recentlyKilled = mMobsKilled.computeIfAbsent(playerUuid(), k -> new ArrayList<>());
			int len = recentlyKilled.size();
			// Note that if you get another kill before this iteration, the song continues for more notes if possible!
			if (mTicks >= len) {
				// Otherwise, restart from the start of the notes
				mTicks = MAX_TRACKED_KILLS;
				return;
			}

			int currIdx = mTicks + 1 >= len ? mTicks : mTicks + 1;
			String prev = recentlyKilled.get(mTicks);
			String curr = recentlyKilled.get(currIdx);
			if (prev.equals(curr)) {
				// Harmonic
				switch (mTicks) {
					case 0 -> playNotes(false, List.of(Note.C4, Note.F4, Note.G4), len);
					case 1 -> playNotes(false, List.of(Note.D4, Note.G4, Note.A4), len);
					case 2 -> playNotes(false, List.of(Note.E4, Note.A4, Note.B4), len);
					case 3 -> playNotes(false, List.of(Note.A4, Note.B4, Note.CS5), len);
					default -> {
					}
				}
			} else {
				// Dissonant
				switch (mTicks) {
					case 0 -> playNotes(true, List.of(Note.C5, Note.BB4, Note.AB4), len);
					case 1 -> playNotes(true, List.of(Note.B4, Note.A4, Note.G4), len);
					case 2 -> playNotes(true, List.of(Note.A4, Note.G4, Note.F4), len);
					case 3 -> playNotes(true, List.of(Note.G4, Note.F4, Note.EB4), len);
					default -> {
					}
				}
			}

			mTicks++;
		}

		public void playNotes(boolean isDissonant, List<Note> notes, int maxNotes) {
			NamespacedKey instrument = isDissonant ? DISSONANT_INSTRUMENT : HARMONIC_INSTRUMENT;
			float volume = isDissonant ? 1.0f : 0.4f;
			World world = mPlayer.getWorld();
			Particle particle = isDissonant ? Particle.VILLAGER_ANGRY : Particle.VILLAGER_HAPPY;
			Location particleLoc = mPlayer.getEyeLocation().add(0.0, isDissonant ? 0.4 : 0.6, 0.0);

			float prevPitchBend = 1.0f;
			int noteNum = 0;
			for (Note note : notes) {
				Sound sound;
				float notePitch = note.mPitch;
				if (isDissonant) {
					// The pitch bend gets further from perfect with each additional note
					float currPitchBend = prevPitchBend * FastUtils.randomFloatInRange(1.0375f, 1.075f);
					// Don't bend too far from 1.0; 0.5 to 2.0 are hard limits in Minecraft
					if (notePitch >= 1.0f) {
						notePitch /= currPitchBend;
					} else {
						notePitch *= currPitchBend;
					}
					prevPitchBend = currPitchBend;
				}
				sound = Sound.sound(Objects.requireNonNull(instrument), Sound.Source.PLAYER, volume, notePitch);
				// Attempt to play louder than 100%
				for (int playTimes = 0; playTimes < 3; playTimes++) {
					world.playSound(sound, mPlayer);
				}
				world.spawnParticle(particle, particleLoc, 1);
				noteNum++;
				if (noteNum >= maxNotes) {
					break;
				}
			}
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.CRIMSON_ROOTS;
	}
}
