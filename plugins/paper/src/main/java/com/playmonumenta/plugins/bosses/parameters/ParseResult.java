package com.playmonumenta.plugins.bosses.parameters;

import dev.jorel.commandapi.Tooltip;
import org.jetbrains.annotations.Nullable;

public class ParseResult<T> {
	private final @Nullable Tooltip<String>[] mTooltip;
	private final @Nullable T mResults;

	public Boolean mContainsDeprecated = false;

	private ParseResult(@Nullable Tooltip<String>[] tooltip, @Nullable T results) {
		mTooltip = tooltip;
		mResults = results;
	}

	public static <T> ParseResult<T> of(T results) {
		return new ParseResult<T>(null, results);
	}

	public static <T> ParseResult<T> of(Tooltip<String>[] tooltip) {
		return new ParseResult<T>(tooltip, null);
	}

	public @Nullable Tooltip<String>[] getTooltip() {
		return mTooltip;
	}

	public @Nullable T getResult() {
		return mResults;
	}
}
