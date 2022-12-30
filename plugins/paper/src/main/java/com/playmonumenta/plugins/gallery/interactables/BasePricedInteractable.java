package com.playmonumenta.plugins.gallery.interactables;

import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import java.util.HashSet;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasePricedInteractable extends BaseInteractable {
	public static final String TAG_STRING = "GalleryPricedInteractable";

	protected final @Nullable Integer mPrice;

	public BasePricedInteractable(@NotNull String name, @NotNull Entity entity, @Nullable Integer price, @Nullable String showingText, @Nullable String command) {
		super(name, entity, showingText, command);
		mPrice = price;
	}

	public boolean canPay(GalleryGame game) {
		return mPrice == null || GalleryUtils.canBuy(game, mPrice);
	}

	public void pay(GalleryGame game) {
		if (mPrice != null) {
			GalleryUtils.pay(game, mPrice);
		}
	}

	public boolean interactWithObjectAfterPay(GalleryPlayer player) {
		if (mCommand != null) {
			GalleryUtils.runCommandAsEntity(mArmorStand, mCommand);
		}
		return true;
	}

	@Override
	public final boolean interactWithObject(GalleryGame game, GalleryPlayer player) {
		if (canPay(game)) {
			pay(game);
			return interactWithObjectAfterPay(player);
		}
		return false;
	}

	@Override
	public void save() {
		new HashSet<>(mArmorStand.getScoreboardTags()).forEach(s -> {
			if (s.startsWith(TAG_STRING + "-")) {
				mArmorStand.removeScoreboardTag(s);
			}
		});
		//DOOR-name-0000-ShowingText-func
		mArmorStand.addScoreboardTag(BaseInteractable.TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING + "-" + mName + "-" + (mPrice != null ? mPrice : "null") + "-" + (mShowingText != null ? mShowingText : "null") + "-" + (mIsInteractable ? "True" : "False") + "-" + (mCommand != null ? mCommand : "null"));
	}

	public static @Nullable BasePricedInteractable fromEntity(Entity entity) {
		BasePricedInteractable interactable = null;

		try {
			for (String tag : new HashSet<>(entity.getScoreboardTags())) {
				if (tag.startsWith(TAG_STRING + "-")) {
					String[] split = tag.split("-");
					String name = split[1];
					Integer price = readNullableInteger(split[2]);
					String text = readNullableString(split[3]);
					boolean isInteractable = readBoolean(split[4]);
					String func = readNullableString(split[5]);
					interactable = new BasePricedInteractable(name, entity, price, text, func);
					interactable.setInteractable(isInteractable);
				}
			}
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Catch an exception while converting BasePricedInteractable. Reason: " + e.getMessage());
			e.printStackTrace();
		}
		return interactable;
	}
}
