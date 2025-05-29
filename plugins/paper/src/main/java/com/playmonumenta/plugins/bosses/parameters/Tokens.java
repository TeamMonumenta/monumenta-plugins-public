package com.playmonumenta.plugins.bosses.parameters;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Tokens {
	private final ImmutableList<Token> mTokens;
	private int mIndex = -1;

	public Tokens(ImmutableList<Token> tokens) {
		mTokens = tokens;
	}

	public Component syntaxHighlight() {
		Component highlighted = Component.empty();
		int mLastEnd = 0;
		for (Token token : mTokens) {
			int spaceCount = token.getStarting() - mLastEnd;
			if (spaceCount > 0) {
				// space was skipped
				highlighted = highlighted.append(Component.text(" ".repeat(spaceCount)));
			}
			highlighted = highlighted.append(token.getType().convertToComponent(token.getValue()));
			mLastEnd = token.getEnding();
		}
		return highlighted;
	}

	public enum TokenType {
		OPEN_SQUARE(value -> Component.text("[", NamedTextColor.GRAY), false),
		CLOSE_SQUARE(value -> Component.text("]", NamedTextColor.GRAY), false),
		OPEN_ROUND(value -> Component.text("(", NamedTextColor.GRAY), false),
		CLOSE_ROUND(value -> Component.text(")", NamedTextColor.GRAY), false),
		EQUALS(value -> Component.text("=", NamedTextColor.GRAY), false),
		COMMA(value -> Component.text(",", NamedTextColor.GRAY), false),
		COLON(value -> Component.text(":", NamedTextColor.GRAY), false),
		TRIGGER_OPERATOR(value -> Component.text(value, NamedTextColor.GRAY), false),
		HEXADECIMAL(value -> Component.text(value, TextColor.color(0xffcb6b)), true),
		FLOATING_POINT(value -> Component.text(value, TextColor.color(0xffcb6b)), true),
		INTEGER(value -> Component.text(value, TextColor.color(0xffcb6b)), true),
		BOOLEAN(value -> Component.text(value, TextColor.color(0xff6666)), true),
		STRING(value -> Component.text(value, TextColor.color(0xadcc69), TextDecoration.ITALIC), true),
		CONSTANT(value -> Component.text(value, NamedTextColor.WHITE, TextDecoration.BOLD), false),
		PARAMETER_NAME(value -> Component.text(value, NamedTextColor.WHITE), false),
		UNKNOWN(value -> Component.text(value, NamedTextColor.DARK_RED), false),
		TERMINATOR(value -> Component.text(value, TextColor.color(0)), false);

		private final Function<String, Component> mConverter;
		private final boolean mIsValue;

		TokenType(Function<String, Component> converter, boolean isValue) {
			mConverter = converter;
			mIsValue = isValue;
		}

		public Component convertToComponent(String value) {
			return mConverter.apply(value);
		}

		public boolean isValue() {
			return mIsValue;
		}
	}

	public int getIndex() {
		return mIndex;
	}

	private int getIndexInBound(int index) {
		if (index < 0) {
			return 0;
		}
		if (index >= mTokens.size()) {
			return mTokens.size() - 1;
		}
		return index;
	}

	public Token advance() {
		return mTokens.get(getIndexInBound(++mIndex));
	}

	public static class Token {
		private final TokenType mType;
		private final String mValue;
		private final int mStarting;
		private final int mEnding;
		private boolean mIsError = false;

		public Token(TokenType type, String value, int starting, int ending) {
			mType = type;
			mValue = value;
			mStarting = starting;
			mEnding = ending;
		}

		public void setError() {
			mIsError = true;
		}

		public TokenType getType() {
			return mType;
		}

		public String getValue() {
			return mValue;
		}

		public int getStarting() {
			return mStarting;
		}

		public int getEnding() {
			return mEnding;
		}

		public boolean isError() {
			return mIsError;
		}
	}
}
