package com.playmonumenta.plugins.social;

class SocialCachePair {
	private final PlayerSocialCache mSender;
	private final PlayerSocialCache mReceiver;

	SocialCachePair(PlayerSocialCache sender, PlayerSocialCache receiver) {
		mSender = sender;
		mReceiver = receiver;
	}

	PlayerSocialCache getSender() {
		return mSender;
	}

	PlayerSocialCache getReceiver() {
		return mReceiver;
	}
}
