package com.playmonumenta.plugins.market;

import net.kyori.adventure.text.Component;

public enum MarketListingStatus {
	PURCHASE_SUCCESSFUL(false, ""),
	NOT_FOUND(true, "Error: listing does not exist."),
	NOT_ENOUGH_REMAINING(true, "Error: listing ran out of stock."),
	IS_PURCHASABLE(false, ""),
	UPDATE_FAILED(true, "Error: listing failed to get updated."),
	EXPIRED(true, "This listing has expired. you can only claim back the items by going to 'your listings' GUI"),
	LOCKED(true, "This listing has been locked. you can unlock it by going to 'your listings' GUI");

	private final boolean mIsError;
	private final String mAssociatedMessage;

	MarketListingStatus(boolean isError, String associatedMessage) {
		mIsError = isError;
		mAssociatedMessage = associatedMessage;
	}

	public boolean isError() {
		return mIsError;
	}

	public Component getFormattedAssociatedMessage() {
		return Component.text(mAssociatedMessage);
	}
}
