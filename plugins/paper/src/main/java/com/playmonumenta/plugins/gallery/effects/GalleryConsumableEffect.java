package com.playmonumenta.plugins.gallery.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public abstract class GalleryConsumableEffect extends GalleryEffect {

	protected static final int GENERIC_ROUND_DURATION_EFFECT = 3;

	protected int mRoundsLeft;

	public GalleryConsumableEffect(@NotNull GalleryEffectType type) {
		super(type);
		mRoundsLeft = getMaxRoundLeft();
	}

	@Override
	public String getDisplay() {
		return ChatColor.GOLD + mType.getRealName() + " " + mRoundsLeft + " remaining";
	}

	@Override public void onRoundStart(GalleryPlayer player, GalleryGame game) {
		super.onRoundStart(player, game);
		mRoundsLeft--;
		if (mRoundsLeft <= 0) {
			clear(player);
		}
	}

	@Override public boolean canBuy(GalleryPlayer player) {
		return player.getEffectOfType(mType) == null;
	}

	@Override public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("RoundsLeft", mRoundsLeft);
		return obj;
	}

	@Override public GalleryConsumableEffect fromJson(JsonObject object) {
		GalleryConsumableEffect effect = (GalleryConsumableEffect) super.fromJson(object);
		effect.mRoundsLeft = object.getAsJsonPrimitive("RoundsLeft").getAsInt();
		return effect;
	}

	public int getMaxRoundLeft() {
		return GENERIC_ROUND_DURATION_EFFECT;
	}
}
