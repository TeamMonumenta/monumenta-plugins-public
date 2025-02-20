package com.playmonumenta.plugins.bosses.parameters;

import dev.jorel.commandapi.Tooltip;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class ParseResult<T> {
	private final @Nullable List<Tooltip<String>> mTooltip;
	private final @Nullable T mResults;

	public @Nullable Set<String> mDeprecatedParameters = null;

	private ParseResult(@Nullable List<Tooltip<String>> tooltip, @Nullable T results) {
		mTooltip = tooltip;
		mResults = results;
	}

	public static <T> ParseResult<T> of(T results) {
		return new ParseResult<T>(null, results);
	}

	public static <T> ParseResult<T> of(Tooltip<String>[] tooltip) {
		return new ParseResult<T>(List.of(tooltip), null);
	}

	public static <T> ParseResult<T> of(List<Tooltip<String>> tooltip) {
		return new ParseResult<T>(tooltip, null);
	}

	public @Nullable List<Tooltip<String>> getTooltip() {
		return mTooltip;
	}

	public @Nullable T getResult() {
		return mResults;
	}
}
