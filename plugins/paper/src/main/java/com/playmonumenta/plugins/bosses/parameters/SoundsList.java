package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SoundsList {
	public static class CSound implements Cloneable {
		Sound mSound;
		float mVolume;
		float mPitch;

		public CSound(Sound sound) {
			this(sound, 0);
		}

		public CSound(Sound sound, float volume) {
			this(sound, volume, 0);
		}

		public CSound(Sound sound, float volume, float pitch) {
			mSound = sound;
			mVolume = volume;
			mPitch = pitch;
		}

		public void setPitch(float pitch) {
			this.mPitch = pitch;
		}

		public float getPitch() {
			return mPitch;
		}

		public void play(Location loc) {
			play(loc, 1);
		}

		public void play(Location loc, SoundCategory category) {
			play(loc, 1, 1, category);
		}

		public void play(Location loc, float volume) {
			play(loc, volume, 1);
		}

		public void play(Location loc, float volume, float pitch) {
			play(loc, volume, pitch, SoundCategory.HOSTILE);
		}

		public void play(Location loc, float volume, float pitch, SoundCategory category) {
			float fVolume = mVolume != 0 ? mVolume : volume;
			float fPitch = mPitch != 0 ? mPitch : pitch;
			World world = loc.getWorld();
			world.playSound(loc, mSound, category, fVolume, fPitch);
		}

		public void play(Player player) {
			play(player, 1);
		}

		public void play(Player player, float volume) {
			play(player, volume, 1);
		}

		public void play(Player player, float volume, float pitch) {
			play(player.getLocation(), volume, pitch);
		}

		@Override
		public String toString() {
			return "(" + mSound + ", " + mVolume + ", " + mPitch + ")";
		}

		@Override
		public CSound clone() {
			return new CSound(mSound, mVolume, mPitch);
		}

	}

	private final List<CSound> mSoundsList;

	public SoundsList(List<CSound> sounds) {
		mSoundsList = sounds;
	}

	public static final SoundsList EMPTY = new SoundsList(List.of());

	public static SoundsList fromString(String string) {
		return Parser.parseOrDefault(Parser.getParserMethod(SoundsList.class), string, EMPTY);
	}

	public void play(Location loc) {
		play(loc, 1);
	}

	public void play(Location loc, float volume) {
		play(loc, volume, 1);
	}

	public void play(Location loc, float volume, float pitch) {
		//if the list is empty the for will be skipped
		for (CSound cSound : mSoundsList) {
			cSound.play(loc, volume, pitch);
		}
	}

	public void play(Player player) {
		play(player, 1);
	}

	public void play(Player player, float volume) {
		play(player, volume, 1);
	}

	public void play(Player player, float volume, float pitch) {
		//if the list is empty the for will be skipped
		for (CSound cSound : mSoundsList) {
			cSound.play(player, volume, pitch);
		}
	}

	public void playSoundsModified(Consumer<CSound> action, Location loc) {
		List<CSound> sounds = mSoundsList.stream().map(CSound::clone).toList();
		sounds.forEach(action);
		new SoundsList(sounds).play(loc);
	}

	public boolean isEmpty() {
		return mSoundsList.isEmpty();
	}

	@Override
	public String toString() {
		String msg = "[";
		boolean first = true;
		for (CSound cSound : mSoundsList) {
			msg = msg + (first ? "" : ",") + cSound.toString();
			first = false;
		}
		return msg + "]";
	}


	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Builder() {

		}

		List<CSound> mSounds = new ArrayList<>();

		public Builder add(CSound sound) {
			mSounds.add(sound);
			return this;
		}

		public SoundsList build() {
			return new SoundsList(mSounds);
		}
	}
}