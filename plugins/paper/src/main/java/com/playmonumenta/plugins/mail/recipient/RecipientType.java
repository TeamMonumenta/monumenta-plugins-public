package com.playmonumenta.plugins.mail.recipient;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public enum RecipientType {
	PLAYER("player", "/mail mailbox player"),
	GUILD("guild", "/mail mailbox guild"),
	UNKNOWN("unknown", null);

	private final String mId;
	private final @Nullable String mMailboxCommand;

	RecipientType(String id, @Nullable String mailboxCommand) {
		mId = id;
		mMailboxCommand = mailboxCommand;
	}

	public static List<RecipientType> validOptions() {
		List<RecipientType> result = new ArrayList<>();
		for (RecipientType recipientType : values()) {
			if (UNKNOWN.equals(recipientType)) {
				continue;
			}
			result.add(recipientType);
		}
		return result;
	}

	public static RecipientType byId(String id) {
		for (RecipientType recipientType : values()) {
			if (recipientType.mId.equals(id)) {
				return recipientType;
			}
		}
		return UNKNOWN;
	}

	public String id() {
		return mId;
	}

	public @Nullable String mailboxCommand() {
		return mMailboxCommand;
	}
}
