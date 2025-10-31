package com.playmonumenta.plugins.bosses.parameters;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.bosses.parameters.phases.Trigger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Tokenizer {
	private static final Pattern CONSTANT = Pattern.compile("^[A-Z]+(?:_[A-Z]+)*$");
	private final List<Tokens.Token> mTokens = new ArrayList<>();
	private final List<IntermediateToken> mIntermediateTokens = new ArrayList<>();

	private State mState;
	private int mStartingIndex = 0;
	private int mIndex = -1;
	private final String mRaw;
	StringBuilder mLexeme = new StringBuilder();
	private char mCurrentChar;
	// State -> (Terminal or not?, New State)
	private static final EnumMap<State, Function<Character, TokenizeResult>> TRANSITION_TABLE = new EnumMap<>(State.class);

	private record TokenizeResult(
		State state,
		boolean addToken,
		boolean addCharacter
	) {
		private TokenizeResult(State state, boolean addToken) {
			this(state, addToken, true);
		}
	}

	static {
		TRANSITION_TABLE.put(State.EQUALS, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.OPEN_SQUARE, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.OPEN_ROUND, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.CLOSE_ROUND, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.COLON, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.COMMA, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.SPACE, character -> new TokenizeResult(getFirstState(character), false));
		TRANSITION_TABLE.put(State.CLOSE_SQUARE, character -> new TokenizeResult(getFirstState(character), true));
		TRANSITION_TABLE.put(State.INTEGER, character -> {
			if (Character.isDigit(character) || character == 'f' || character == 'd') {
				return new TokenizeResult(State.INTEGER, false);
			}
			return switch (character) {
				case '.' -> new TokenizeResult(State.FRACTION, false);
				case ',' -> new TokenizeResult(State.COMMA, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				// May be operator
				case '>' -> new TokenizeResult(State.ARROW, false);
				case ':' -> new TokenizeResult(State.COLON, true);
				case ' ' -> new TokenizeResult(State.SPACE, true);
				default -> new TokenizeResult(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.FRACTION, character -> {
			if (Character.isDigit(character) || character == 'f' || character == 'd') {
				return new TokenizeResult(State.FRACTION, false);
			}
			return switch (character) {
				case ',' -> new TokenizeResult(State.COMMA, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				case ':' -> new TokenizeResult(State.COLON, true);
				case ' ' -> new TokenizeResult(State.SPACE, true);
				default -> new TokenizeResult(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.HEX_PREF, character -> {
			if (isHexCharacter(character)) {
				return new TokenizeResult(State.HEX, false);
			}
			return switch (character) {
				case ',' -> new TokenizeResult(State.COMMA, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				case ':' -> new TokenizeResult(State.COLON, true);
				case ' ' -> new TokenizeResult(State.SPACE, true);
				default -> new TokenizeResult(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.HEX, character -> {
			if (isHexCharacter(character)) {
				return new TokenizeResult(State.HEX, false);
			}
			return switch (character) {
				case ',' -> new TokenizeResult(State.COMMA, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				case ':' -> new TokenizeResult(State.COLON, true);
				case ' ' -> new TokenizeResult(State.SPACE, true);
				default -> new TokenizeResult(State.IDENTIFIER, false);
			};
		});
		TRANSITION_TABLE.put(State.IDENTIFIER, character ->
			switch (character) {
				case ',' -> new TokenizeResult(State.COMMA, true);
				case '=' -> new TokenizeResult(State.EQUALS, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case '[' -> new TokenizeResult(State.OPEN_SQUARE, true);
				case '(' -> new TokenizeResult(State.OPEN_ROUND, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				case ' ' -> new TokenizeResult(State.SPACE, true);
				case '-' -> new TokenizeResult(State.INTEGER, true); // Bosstag Phase manager '->' operator
				case ':' -> new TokenizeResult(State.COLON, true);
				default -> new TokenizeResult(State.IDENTIFIER, false);
			}
		);
		TRANSITION_TABLE.put(State.QUOTE_START, character -> {
			if (character == '"') {
				return new TokenizeResult(State.QUOTE_END, true);
			}
			return new TokenizeResult(State.QUOTED_STRING, true);
		});
		TRANSITION_TABLE.put(State.QUOTED_STRING, character -> {
			if (character == '\\') {
				return new TokenizeResult(State.ESCAPE_CHARACTER, false, false);
			} else if (character == '"') {
				return new TokenizeResult(State.QUOTE_END, true);
			}
			return new TokenizeResult(State.QUOTED_STRING, false);
		});
		TRANSITION_TABLE.put(State.ESCAPE_CHARACTER, character -> new TokenizeResult(State.QUOTED_STRING, false));
		TRANSITION_TABLE.put(State.QUOTE_END, character ->
			switch (character) {
				case ',' -> new TokenizeResult(State.COMMA, true);
				case ']' -> new TokenizeResult(State.CLOSE_SQUARE, true);
				case ')' -> new TokenizeResult(State.CLOSE_ROUND, true);
				case ':' -> new TokenizeResult(State.COLON, true);
				default -> new TokenizeResult(State.IDENTIFIER, true);
			}
		);
		TRANSITION_TABLE.put(State.ARROW, character -> new TokenizeResult(getFirstState(character), true));
	}

	public Tokenizer(String raw) {
		mRaw = raw;

		// initialization: character will not be able to add a token because there was no previous character to make a token!
		advance();
		mState = getFirstState(mCurrentChar);
		mLexeme.append(mCurrentChar);

		while (true) {
			advance();
			if (mCurrentChar == '\0') {
				// done parsing
				addToken();
				break;
			}
			TokenizeResult information = Objects.requireNonNull(TRANSITION_TABLE.get(mState)).apply(mCurrentChar);
			if (information.addToken()) {
				addToken();
			}
			mState = information.state();
			if (mState == State.SPACE) {
				// skip spaces
				mStartingIndex++;
				continue;
			}

			if (information.addCharacter()) {
				mLexeme.append(mCurrentChar);
			}
		}

		processIdentifierTypes();
	}

	public Tokens getTokens() {
		return new Tokens(ImmutableList.copyOf(mTokens), mRaw);
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
		IDENTIFIER(string -> Tokens.TokenType.STRING), // (cheating) "Hello." "WORLD"
		ARROW(string -> Tokens.TokenType.TRIGGER_OPERATOR),
		QUOTE_START(string -> Tokens.TokenType.QUOTE),
		QUOTED_STRING(string -> Tokens.TokenType.STRING),
		ESCAPE_CHARACTER(string -> Tokens.TokenType.STRING),
		QUOTE_END(string -> Tokens.TokenType.QUOTE),
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
		String content = mLexeme.toString();
		Tokens.TokenType type = mState.getType(content);

		mIntermediateTokens.add(new IntermediateToken(type, content, mStartingIndex, mIndex));

		// reset lexeme
		mLexeme.setLength(0);
		mStartingIndex = mIndex;
	}

	private void processIdentifierTypes() {
		int len = mIntermediateTokens.size();

		boolean inString = false;

		for (int i = 0; i < len; i++) {
			IntermediateToken intermediateToken = mIntermediateTokens.get(i);
			String value = intermediateToken.mValue;

			if (intermediateToken.mType == Tokens.TokenType.QUOTE) {
				inString = !inString;
			}

			if (!inString) {

				Tokens.TokenType nextType = i == len - 1 ? Tokens.TokenType.TERMINATOR : mIntermediateTokens.get(i + 1).mType;
				if (intermediateToken.mType == Tokens.TokenType.STRING && nextType == Tokens.TokenType.EQUALS) {
					intermediateToken.setType(Tokens.TokenType.PARAMETER_NAME);
				} else if (nextType != Tokens.TokenType.STRING) {
					if (value.equals("true") || value.equals("false")) {
						intermediateToken.setType(Tokens.TokenType.BOOLEAN);
					} else if (CONSTANT.matcher(value).matches()) {
						intermediateToken.setType(Tokens.TokenType.CONSTANT);
					}
				} else if (Trigger.isOperator(value)) {
					intermediateToken.setType(Tokens.TokenType.TRIGGER_OPERATOR);
				}
			}

			mTokens.add(intermediateToken.toToken());
		}
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

	private static class IntermediateToken {
		private Tokens.TokenType mType;
		private final String mValue;
		private final int mStarting;
		private final int mEnding;

		public IntermediateToken(Tokens.TokenType type, String value, int starting, int ending) {
			mType = type;
			mValue = value;
			mStarting = starting;
			mEnding = ending;
		}

		public void setType(Tokens.TokenType type) {
			mType = type;
		}

		public Tokens.Token toToken() {
			return new Tokens.Token(mType, mValue, mStarting, mEnding);
		}

		@Override
		public String toString() {
			return String.format("{<%s>: '%s'}", mType, mValue);
		}
	}
}
