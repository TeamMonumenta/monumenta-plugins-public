package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SoundsList {
	public static class CSound {
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

		public void play(Location loc) {
			play(loc, 1);
		}

		public void play(Location loc, float volume) {
			play(loc, volume, 1);
		}

		public void play(Location loc, float volume, float pitch) {
			float fVolume = mVolume != 0 ? mVolume : volume;
			float fPitch = mPitch != 0 ? mPitch : pitch;
			World world = loc.getWorld();
			world.playSound(loc, mSound, fVolume, fPitch);
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

		public static CSound fromString(String value) throws RuntimeException {
			if (value.startsWith("(")) {
				value = value.substring(1);
			}
			if (value.endsWith(")")) {
				value = value.substring(0, value.length() - 1);
			}
			String[] split = value.split(",");
			Sound sound = Sound.valueOf(split[0].toUpperCase());
			if (sound == null) {
				throw new SoundNotFoundException(split[0]);
			}
			if (split.length == 3) {
				return new CSound(sound, Float.valueOf(split[1]), Float.valueOf(split[2]));
			} else if (split.length == 2) {
				return new CSound(sound, Float.valueOf(split[1]));
			} else if (split.length == 1) {
				return new CSound(sound);
			} else {
				throw new IllegalFormatException("Fail loading custom sound. Object of size " + split.length);
			}
		}
	}

	private List<CSound> mSoundsList;

	public SoundsList(String values) throws RuntimeException {
		mSoundsList = new ArrayList<>();

		List<String> split = BossUtils.splitByCommasUsingBrackets(values);
		if (split.isEmpty()) {
			// Allow deliberately empty sound lists
			return;
		}

		for (String cSoundString : split) {
			try {
				mSoundsList.add(CSound.fromString(cSoundString));
			} catch (Exception ex) {
				Plugin.getInstance().getLogger().warning("Failed to parse '" + cSoundString + "': " + ex.getMessage());
			}
		}

		if (mSoundsList.isEmpty()) {
			throw new ListEmptyException("Fail parsing string to list. List empty");
		}
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

	@Override
	public String toString() {
		String msg = "[";
		for (CSound cSound : mSoundsList) {
			msg = msg + cSound.toString() + ",";
		}
		//remove last comma
		if (msg.endsWith(",")) {
			msg = msg.substring(0, msg.length() - 1);
		}
		return msg + "]";
	}

	public static SoundsList fromString(String string) throws RuntimeException {
		return new SoundsList(string.replace(" ", "").toLowerCase());
	}

	private static class SoundNotFoundException extends RuntimeException {
		public SoundNotFoundException(String value) {
			super("Sound don't found for argument: " + value);
		}
	}

	private class ListEmptyException extends RuntimeException {
		public ListEmptyException(String value) {
			super(value);
		}
	}

	private static class IllegalFormatException extends RuntimeException {
		public IllegalFormatException(String value) {
			super(value);
		}
	}
}
