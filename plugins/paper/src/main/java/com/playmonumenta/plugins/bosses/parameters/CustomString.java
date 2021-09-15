package com.playmonumenta.plugins.bosses.parameters;

public class CustomString {

	private String mRealString = "";

	public CustomString(String value) {
		mRealString = value;
	}

	public String getString() {
		return mRealString;
	}

	public boolean isEmpty() {
		return mRealString.isEmpty();
	}

	@Override
	public String toString() {
		return "\"" + mRealString + "\"";
	}

	public static CustomString fromString(String value) {
		value = value.replace("\"", "");

		return new CustomString(value);
	}

	public static final CustomString EMPTY = CustomString.fromString("");
}
