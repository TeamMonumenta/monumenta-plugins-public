package com.playmonumenta.plugins.mail;

import org.jetbrains.annotations.Nullable;

public class LockException extends Exception {
	public LockException(String message) {
		super(message);
	}

	public LockException(String message, Throwable cause) {
		super(message, cause);
	}

	public static LockException withOptCause(String message, @Nullable Throwable cause) {
		if (cause == null) {
			return new LockException(message);
		} else {
			return new LockException(message, cause);
		}
	}
}
