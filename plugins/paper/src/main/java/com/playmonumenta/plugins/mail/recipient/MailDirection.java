package com.playmonumenta.plugins.mail.recipient;

import org.jetbrains.annotations.Nullable;

public enum MailDirection {
	TO("rx", "inbox", "To ", "Inbox For "),
	FROM("tx", "outbox", "From ", "Outbox For "),
	DEFAULT(null, "mailbox", "", "Mailbox For ");

	private final @Nullable String mRedisSubKey;
	private final String mArgument;
	private final String mTitle;
	private final String mMailboxPrefix;

	MailDirection(
		@Nullable String redisSubKey,
		String argument,
		String title,
		String mailboxPrefix
	) {
		mRedisSubKey = redisSubKey;
		mArgument = argument;
		mTitle = title;
		mMailboxPrefix = mailboxPrefix;
	}

	public @Nullable String redisSubKey() {
		return mRedisSubKey;
	}

	public String argument() {
		return mArgument;
	}

	public String title() {
		return mTitle;
	}

	public String mailboxPrefix() {
		return mMailboxPrefix;
	}
}
