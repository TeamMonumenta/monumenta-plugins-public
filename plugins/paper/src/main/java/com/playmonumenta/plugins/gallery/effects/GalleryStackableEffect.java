package com.playmonumenta.plugins.gallery.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public abstract class GalleryStackableEffect extends GalleryEffect {

	protected int mStacks = 1;

	public GalleryStackableEffect(@NotNull GalleryEffectType type) {
		super(type);
	}

	@Override public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {
		GalleryManager.mPlugin.mEffectManager.addEffect(player.getPlayer(), "Gallery" + mType.getRealName(), new Effect(20, "Gallery" + mType.getRealName()) {
			@Override public double getMagnitude() {
				return mStacks;
			}

			@Override public String toString() {
				return "Gallery" + mType.getRealName() + " lvl. " + mStacks;
			}

			@Override public @Nullable String getDisplay() {
				return ChatColor.GOLD + mType.getRealName() + " " + StringUtils.toRoman(mStacks);
			}
		});
	}

	@Override public void playerGainEffect(GalleryPlayer player) {
		GalleryStackableEffect effect = (GalleryStackableEffect) player.getEffectOfType(mType);
		if (effect != null) {
			player.removeEffect(effect);
			mStacks = effect.mStacks + 1;
		}

		player.sendMessage("You have obtained " + ChatColor.GOLD + mType.getRealName() + " Lvl. " + mStacks);
	}

	@Override
	public boolean canBuy(@NotNull GalleryPlayer player) {
		GalleryStackableEffect effect = (GalleryStackableEffect) player.getEffectOfType(mType);
		return effect == null || effect.getMaxStacks() > effect.getCurrentStacks();
	}

	@Override
	public JsonObject toJson() {
		JsonObject obj = super.toJson();
		obj.addProperty("EffectStacks", mStacks);
		return obj;
	}

	@Override
	public GalleryEffect fromJson(JsonObject object) {
		GalleryStackableEffect effect = (GalleryStackableEffect) super.fromJson(object);
		effect.mStacks = object.getAsJsonPrimitive("EffectStacks").getAsInt();
		return effect;
	}

	public int getCurrentStacks() {
		return mStacks;
	}

	public abstract int getMaxStacks();
}
