package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.RecipientType;
import java.util.EnumSet;

public class MailGuiSettings {
	public MailDirection mDirection;
	public final EnumSet<RecipientType> mRecipientTypes;

	public MailGuiSettings() {
		mDirection = MailDirection.DEFAULT;
		mRecipientTypes = EnumSet.allOf(RecipientType.class);
		mRecipientTypes.remove(RecipientType.UNKNOWN);
	}
}
