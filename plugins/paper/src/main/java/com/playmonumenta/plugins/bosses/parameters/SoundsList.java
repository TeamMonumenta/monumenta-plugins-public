package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
	}

	private final List<CSound> mSoundsList;

	private SoundsList(List<CSound> sounds) {
		mSoundsList = sounds;
	}

	public static SoundsList fromString(String string) {
		ParseResult<SoundsList> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as SoundsList");
			Thread.dumpStack();
			return new SoundsList(new ArrayList<>(0));
		}

		return result.getResult();
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
		boolean first = true;
		for (CSound cSound : mSoundsList) {
			msg = msg + (first ? "" : ",") + cSound.toString();
			first = false;
		}
		return msg + "]";
	}

	/*
	 * Parses a SoundsList at the next position in the StringReader.
	 * If this item parses successfully:
	 *   The returned ParseResult will contain a non-null getResult() and a null getTooltip()
	 *   The reader will be advanced to the next character past this SoundsList value.
	 * Else:
	 *   The returned ParseResult will contain a null getResult() and a non-null getTooltip()
	 *   The reader will not be advanced
	 */
	public static ParseResult<SoundsList> fromReader(StringReader reader, String hoverDescription) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
		}

		List<CSound> soundsList = new ArrayList<>(2);

		boolean atLeastOneSoundIter = false;
		while (true) {
			// Start trying to parse the next individual sound entry in the list

			if (reader.advance("]")) {
				// Got closing bracket and parsed rest successfully - complete sound list, break this loop
				break;
			}

			if (atLeastOneSoundIter) {
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", hoverDescription)));
				}
			} else {
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "(", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
			}

			atLeastOneSoundIter = true;

			Sound sound = reader.readSound();
			if (sound == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(Sound.values().length);
				String soFar = reader.readSoFar();
				for (Sound valid : Sound.values()) {
					suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify volume and optionally pitch"),
						Tooltip.of(reader.readSoFar() + ")", "Use 1.0 as default volume and pitch")
					));
				}
				// End of this sound, loop to next
				soundsList.add(new CSound(sound));
				continue;
			}

			Double volume = reader.readDouble();
			if (volume == null || volume <= 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Sound volume > 0")));
			}

			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify pitch"),
						Tooltip.of(reader.readSoFar() + ")", "Use 1.0 as default pitch")
					));
				}
				// End of this sound, loop to next
				soundsList.add(new CSound(sound, volume.floatValue()));
				continue;
			}

			Double pitch = reader.readDouble();
			if (pitch == null || pitch < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Sound pitch >= 0")));
			}

			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
			}

			// End of this sound, loop to next
			soundsList.add(new CSound(sound, volume.floatValue(), pitch.floatValue()));
		}

		return ParseResult.of(new SoundsList(soundsList));
	}
}
