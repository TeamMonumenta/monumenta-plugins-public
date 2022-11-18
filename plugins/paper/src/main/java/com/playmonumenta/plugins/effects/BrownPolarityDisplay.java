package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BrownPolarityDisplay {
	//This is not an effect, it is simply a class used to display the polarity in brown

	public static final String POSITIVE_TAG = "brown_positive";
	public static final String NEGATIVE_TAG = "brown_negative";

	public enum Polarity implements DisplayableEffect {
		POSITIVE(ChatColor.RED + "Magnetic Polarity: Positive", POSITIVE_TAG),
		NEGATIVE(ChatColor.BLUE + "Magnetic Polarity: Negative", NEGATIVE_TAG);

		public final String mDisplay;
		public final String mTag;

		Polarity(String display, String tag) {
			mDisplay = display;
			mTag = tag;
		}

		@Override
		public int getDisplayPriority() {
			return 100000000;
		}

		@Override
		public String getDisplay() {
			return mDisplay;
		}
	}

	private static boolean IS_INITIALIZED = false;
	private static boolean IS_ACTIVE = false;

	public static @Nullable DisplayableEffect getPolarityDisplay(Player player) {
		if (!IS_INITIALIZED) {
			IS_ACTIVE = ServerProperties.getShardName().contains("brown");
			IS_INITIALIZED = true;
		}

		if (IS_ACTIVE) {
			Set<String> tags = player.getScoreboardTags();
			for (Polarity polarity : Polarity.values()) {
				if (tags.contains(polarity.mTag)) {
					return polarity;
				}
			}
		}

		return null;
	}
}
