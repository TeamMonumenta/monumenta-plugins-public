package com.playmonumenta.plugins.bosses.parameters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

public class Tokens {
	private final ImmutableList<Token> mTokens;
	private int mIndex = -1;
	private final String mRaw;

	public Tokens(ImmutableList<Token> tokens, String raw) {
		mTokens = tokens;
		mRaw = raw;
	}

	public ImmutableList<Token> getTokens() {
		return mTokens;
	}

	public Component syntaxHighlight() {
		Component highlighted = Component.empty();
		for (int i = 0; i < mTokens.size(); i++) {
			Token token = mTokens.get(i);

			highlighted = highlighted.append(token.toComponent());
			if (i < mTokens.size() - 1) {
				Token peek = mTokens.get(i + 1);

				String notTokenizedCharacters = getRaw().substring(token.mEnding, peek.mStarting == -1 ? getRaw().length() : peek.mStarting);

				if (!notTokenizedCharacters.isEmpty()) {
					highlighted = highlighted.append(Component.text(notTokenizedCharacters, NamedTextColor.GRAY));
				}
			}
		}
		return highlighted;
	}

	public String getRaw() {
		return mRaw;
	}

	public @Nullable Token get(int index) {
		return (index < 0 || index >= mTokens.size()) ? null : mTokens.get(index);
	}

	public enum TokenType {
		OPEN_SQUARE(value -> Component.text("[", NamedTextColor.GRAY), false, "["),
		CLOSE_SQUARE(value -> Component.text("]", NamedTextColor.GRAY), false, "]"),
		OPEN_ROUND(value -> Component.text("(", NamedTextColor.GRAY), false, "("),
		CLOSE_ROUND(value -> Component.text(")", NamedTextColor.GRAY), false, ")"),
		EQUALS(value -> Component.text("=", NamedTextColor.GRAY), false, "="),
		COMMA(value -> Component.text(",", NamedTextColor.GRAY), false, ","),
		COLON(value -> Component.text(":", NamedTextColor.GRAY), false, ":"),
		QUOTE(value -> Component.text("\"", TextColor.color(0xadcc69), TextDecoration.ITALIC), false, "\""),
		TRIGGER_OPERATOR(value -> Component.text(value, NamedTextColor.GRAY), true, null),
		HEXADECIMAL(value -> Component.text(value, TextColor.color(0xffcb6b)), true, null),
		FLOATING_POINT(value -> Component.text(value, TextColor.color(0xffcb6b)), true, null),
		INTEGER(value -> Component.text(value, TextColor.color(0xffcb6b)), true, null),
		BOOLEAN(value -> Component.text(value, TextColor.color(0xff6666)), true, null),
		STRING(value -> Component.text(value, TextColor.color(0xadcc69), TextDecoration.ITALIC), true, null),
		CONSTANT(value -> Component.text(value, NamedTextColor.WHITE, TextDecoration.BOLD), true, null),
		PARAMETER_NAME(value -> Component.text(value, NamedTextColor.WHITE), false, null),
		UNKNOWN(value -> Component.text(value, NamedTextColor.DARK_RED), false, null),
		TERMINATOR(value -> Component.text(value, TextColor.color(0)), false, null);

		private final Function<String, Component> mConverter;
		private final boolean mIsValue;
		@Nullable
		private final String mDefaultValue;

		TokenType(Function<String, Component> converter, boolean isValue, @Nullable String defaultValue) {
			mConverter = converter;
			mIsValue = isValue;
			mDefaultValue = defaultValue;
		}

		public Component convertToComponent(String value) {
			return mConverter.apply(value);
		}

		public boolean isValue() {
			return mIsValue;
		}

		@Nullable
		public String getDefaultValue() {
			return mDefaultValue;
		}
	}

	public int getIndex() {
		return mIndex;
	}

	public boolean hasRemaining() {
		return mIndex < mTokens.size() - 1;
	}

	public Token peek() {
		return hasRemaining() ? mTokens.get(mIndex + 1) : Token.TERMINATOR;
	}

	public boolean matchThenConsume(TokenType nextType) {
		if (matchNext(nextType)) {
			advance();
			return true;
		}
		return false;
	}

	private static String getDefaultSuggestion(TokenType nextType) {
		return Optional.ofNullable(nextType.getDefaultValue()).orElse("");
	}

	public Token consume(TokenType nextType) throws Parser.ParseError {
		return consume(nextType, Parser.Suggestion.of(getIndex(), getDefaultSuggestion(nextType), ""));
	}


	public Token consume(TokenType nextType, Parser.Suggestion suggestions) throws Parser.ParseError {
		assertNext(nextType, suggestions);
		return advance();
	}

	public Token consume(TokenType nextType, Supplier<Collection<Parser.Suggestion>> suggestions) throws Parser.ParseError {
		assertNext(nextType, suggestions);
		return advance();
	}

	public Token consume(EnumSet<TokenType> nextTypes, Supplier<Collection<Parser.Suggestion>> suggestions) throws Parser.ParseError {
		assertNext(nextTypes, suggestions);
		return advance();
	}

	public boolean matchNext(TokenType nextType) {
		return hasRemaining() && peek().getType() == nextType;
	}

	public boolean matchNext(EnumSet<TokenType> nextType) {
		return hasRemaining() && nextType.contains(peek().getType());
	}

	public void assertNext(TokenType nextType) throws Parser.ParseError {
		assertNext(nextType, Parser.Suggestion.of(getIndex(), Optional.ofNullable(nextType.getDefaultValue()).orElse(""), ""));
	}

	public void assertNext(TokenType nextType, Parser.Suggestion suggestion) throws Parser.ParseError {
		assertNext(nextType, () -> List.of(suggestion));
	}

	public void assertNext(TokenType nextType, Supplier<Collection<Parser.Suggestion>> suggestions) throws Parser.ParseError {
		if (!matchNext(nextType)) {
			throw Parser.ParseError.of(String.format("Un-expected token, expected token of type %s", nextType), getIndex() + 1, this)
				.suggests(suggestions.get());
		}
	}

	public void assertNext(EnumSet<TokenType> nextTypes, Supplier<Collection<Parser.Suggestion>> suggestions) throws Parser.ParseError {
		if (!matchNext(nextTypes)) {
			throw Parser.ParseError.of(String.format("Un-expected token, expected token of types %s", nextTypes), getIndex() + 1, this)
				.suggests(suggestions.get());
		}
	}

	public Token advance() {
		return hasRemaining() ? mTokens.get(++mIndex) : Token.TERMINATOR;
	}

	public void retreat() {
		--mIndex;
	}

	public static class Token {
		public static final Token TERMINATOR = new Token(TokenType.TERMINATOR, "", 1, 0);
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

		public Component toComponent() {
			Component component = getType().convertToComponent(getValue());
			if (isError()) {
				return component.color(NamedTextColor.DARK_RED);
			}
			return component;
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

		@Override
		public String toString() {
			return String.format("Token{<%s>: '%s'}", mType, mValue);
		}
	}

	@Override
	public String toString() {
		return mTokens.toString();
	}
}
