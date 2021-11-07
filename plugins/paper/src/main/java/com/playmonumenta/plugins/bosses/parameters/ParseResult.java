package com.playmonumenta.plugins.bosses.parameters;

import dev.jorel.commandapi.Tooltip;

public class ParseResult<T> {
	private final Tooltip<String>[] mTooltip;
	private final T mResults;

	private ParseResult(Tooltip<String>[] tooltip, T results) {
		mTooltip = tooltip;
		mResults = results;
	}

	public static <T> ParseResult<T> of(T results) {
		return new ParseResult<T>(null, results);
	}

	public static <T> ParseResult<T> of(Tooltip<String>[] tooltip) {
		return new ParseResult<T>(tooltip, null);
	}

	/* TODO nullable */
	public Tooltip<String>[] getTooltip() {
		return mTooltip;
	}

	/* TODO nullable */
	public T getResult() {
		return mResults;
	}
}
