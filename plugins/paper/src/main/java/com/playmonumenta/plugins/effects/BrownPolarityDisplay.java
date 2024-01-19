package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BrownPolarityDisplay {
	//This is not an effect, it is simply a class used to display the polarity in brown

	public static final String POSITIVE_TAG = "brown_positive";
	public static final String NEGATIVE_TAG = "brown_negative";

	public enum Polarity implements DisplayableEffect {
		POSITIVE(Component.text("Magnetic Polarity: Positive", NamedTextColor.RED), POSITIVE_TAG),
		NEGATIVE(Component.text("Magnetic Polarity: Negative", NamedTextColor.BLUE), NEGATIVE_TAG);

		public final Component mDisplay;
		public final String mTag;

		Polarity(Component display, String tag) {
			mDisplay = display;
			mTag = tag;
		}

		@Override
		public int getDisplayPriority() {
			return 100000000;
		}

		@Override
		public @Nullable Component getDisplay() {
			return mDisplay;
		}

		@Override
		public @Nullable Component getDisplayWithoutTime() {
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
