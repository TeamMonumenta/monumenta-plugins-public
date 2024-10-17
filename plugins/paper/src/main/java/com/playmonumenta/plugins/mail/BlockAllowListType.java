package com.playmonumenta.plugins.mail;

import org.jetbrains.annotations.Nullable;

public enum BlockAllowListType {
	BLOCK("blocklist"),
	ALLOW("allowlist"),
	;

	private final String mArgString;

	BlockAllowListType(String argString) {
		mArgString = argString;
	}

	public static @Nullable BlockAllowListType byArgument(String argument) {
		for (BlockAllowListType listType : values()) {
			if (listType.argument().equals(argument)) {
				return listType;
			}
		}
		return null;
	}

	public String argument() {
		return mArgString;
	}
}
