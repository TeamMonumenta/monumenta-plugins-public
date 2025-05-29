package com.playmonumenta.plugins.bosses.parameters;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.bosses.parameters.phases.Trigger;
import it.unimi.dsi.fastutil.objects.ObjectBooleanImmutablePair;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Tokenizer {
	private static final Pattern CONSTANT = Pattern.compile("^[A-Z]+(?:_[A-Z]+)*$");
	private State mState;
	private final List<Tokens.Token> mTokens = new ArrayList<>();

	private int mStartingIndex = 0;
	private int mIndex = -1;
	private final String mRaw;
	StringBuilder mLexeme = new StringBuilder();
	private char mCurrentChar;
	// State -> (Terminal or not?, New State)
	private static final EnumMap<State, Function<Character, ObjectBooleanImmutablePair<State>>> TRANSITION_TABLE = new EnumMap<>(State.class);

	static  {
		TRANSITION_TABLE.put(State.EQUALS, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.OPEN_SQUARE, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.OPEN_ROUND, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.CLOSE_ROUND, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.COLON, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.COMMA, character -> ObjectBooleanImmutablePair.of(getFirstState(character), true));
		TRANSITION_TABLE.put(State.SPACE, character -> ObjectBooleanImmutablePair.of(getFirstState(character), false));
		TRANSITION_TABLE.put(State.CLOSE_SQUARE, character ->
			switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			}
		);
		TRANSITION_TABLE.put(State.INTEGER, character -> {
			if (Character.isDigit(character) || character == 'f' || character == 'd') return ObjectBooleanImmutablePair.of(State.INTEGER, false);
			return switch (character) {
				case '.' -> ObjectBooleanImmutablePair.of(State.FRACTION, false);
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				// May be operator
				case '>' -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.FRACTION, character -> {
			if (Character.isDigit(character) || character == 'f' || character == 'd')
				return ObjectBooleanImmutablePair.of(State.FRACTION, false);
			return switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.HEX_PREF, character -> {
			if (isHexCharacter(character))
				return ObjectBooleanImmutablePair.of(State.HEX, false);
			return switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.HEX, character -> {
			if (isHexCharacter(character))
				return ObjectBooleanImmutablePair.of(State.HEX, false);
			return switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.IDENTIFIER, character ->
			switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case '=' -> ObjectBooleanImmutablePair.of(State.EQUALS, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case '[' -> ObjectBooleanImmutablePair.of(State.OPEN_SQUARE, true);
				case '(' -> ObjectBooleanImmutablePair.of(State.OPEN_ROUND, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ' ' -> ObjectBooleanImmutablePair.of(State.SPACE, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			}
		);
		TRANSITION_TABLE.put(State.QUOTE_START, character -> {
			if (character == '"')
				return ObjectBooleanImmutablePair.of(State.QUOTE_END, false);
			return ObjectBooleanImmutablePair.of(State.QUOTED_STRING, false);
		});
		TRANSITION_TABLE.put(State.QUOTED_STRING, character -> {
			if (character == '"')
				return ObjectBooleanImmutablePair.of(State.QUOTE_END, false);
			return ObjectBooleanImmutablePair.of(State.QUOTED_STRING, false);
		});
		TRANSITION_TABLE.put(State.QUOTE_END, character ->
			switch (character) {
				case ',' -> ObjectBooleanImmutablePair.of(State.COMMA, true);
				case ']' -> ObjectBooleanImmutablePair.of(State.CLOSE_SQUARE, true);
				case ')' -> ObjectBooleanImmutablePair.of(State.CLOSE_ROUND, true);
				case ':' -> ObjectBooleanImmutablePair.of(State.COLON, true);
				default -> ObjectBooleanImmutablePair.of(State.IDENTIFIER, false);
			}
		);
	}

	public Tokenizer(String raw) {
		mRaw = raw;

		advance();
		mState = getFirstState(mCurrentChar);

		while (true) {
			advance();
			if (mCurrentChar == '\0') {
				// done parsing
				addToken();
				break;
			}
			ObjectBooleanImmutablePair<State> information = Objects.requireNonNull(TRANSITION_TABLE.get(mState)).apply(mCurrentChar);
			if (information.valueBoolean()) {
				addToken();
			}
			mState = information.first();
			if (information.first() == State.SPACE) {
				// skip spaces
				mStartingIndex++;
				continue;
			}

			mLexeme.append(mCurrentChar);
		}
	}

	public Tokens getTokens() {
		return new Tokens(ImmutableList.copyOf(mTokens));
	}

	private void advance() {
		mIndex++;
		mCurrentChar = mIndex < mRaw.length() ? mRaw.charAt(mIndex) : '\0';
	}


	private enum State {
		EQUALS(string -> Tokens.TokenType.EQUALS),
		OPEN_SQUARE(string -> Tokens.TokenType.OPEN_SQUARE),
		CLOSE_ROUND(string -> Tokens.TokenType.CLOSE_ROUND),
		OPEN_ROUND(string -> Tokens.TokenType.OPEN_ROUND),
		CLOSE_SQUARE(string -> Tokens.TokenType.CLOSE_SQUARE),
		COLON(string -> Tokens.TokenType.COLON),
		COMMA(string -> Tokens.TokenType.COMMA),
		SPACE(string -> Tokens.TokenType.STRING),
		INTEGER(string -> Tokens.TokenType.INTEGER), // 123
		FRACTION(string -> Tokens.TokenType.FLOATING_POINT), // .345
		HEX_PREF(string -> Tokens.TokenType.STRING), // #
		HEX(string -> Tokens.TokenType.HEXADECIMAL), // ab22
		IDENTIFIER(string -> {
			if (Trigger.isOperator(string)) {
				return Tokens.TokenType.TRIGGER_OPERATOR;
			} else if (string.equals("true") || string.equals("false")) {
				return Tokens.TokenType.BOOLEAN;
			} else if (CONSTANT.matcher(string).matches()) {
				return Tokens.TokenType.CONSTANT;
			}
			return Tokens.TokenType.STRING;
		}), // (cheating) "Hello." "WORLD"
		QUOTE_START(string -> Tokens.TokenType.STRING),
		QUOTED_STRING(string -> Tokens.TokenType.STRING),
		QUOTE_END(string -> Tokens.TokenType.STRING),
		;

		private final Function<String, Tokens.TokenType> mTypeTranslator;

		State(Function<String, Tokens.TokenType> typeTranslator) {
			mTypeTranslator = typeTranslator;
		}

		public Tokens.TokenType getType(String value) {
			return mTypeTranslator.apply(value);
		}
	}

	private void addToken() {
		if (mState == State.QUOTE_END) {
			// strip the quotes from the string!
			mLexeme.deleteCharAt(mLexeme.length() - 1);
			mLexeme.deleteCharAt(0);
		}
		String content = mLexeme.toString();
		Tokens.TokenType type = mState.getType(content);
		// parameter name silliness
		if (mCurrentChar == '=') type = Tokens.TokenType.PARAMETER_NAME;

		mTokens.add(new Tokens.Token(type, content, mStartingIndex, mIndex));

		// reset lexeme
		mLexeme.setLength(0);
		mStartingIndex = mIndex;
	}

	private static State getFirstState(char character) {
		return switch (character) {
			case '[' -> State.OPEN_SQUARE;
			case ']' -> State.CLOSE_SQUARE;
			case '(' -> State.OPEN_ROUND;
			case ')' -> State.CLOSE_ROUND;
			case '=' -> State.EQUALS;
			case ':' -> State.COLON;
			case ',' -> State.COMMA;
			case '#' -> State.HEX_PREF;
			case '"' -> State.QUOTE_START;
			case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> State.INTEGER;
			case '.' -> State.FRACTION;
			case ' ' -> State.SPACE;
			default -> State.IDENTIFIER;
		};
	}

	private static boolean isHexCharacter(char character) {
		return (character >= '0' && character <= '9') ||
			(character >= 'A' && character <= 'F') ||
			(character >= 'a' && character <= 'f');
	}

}
