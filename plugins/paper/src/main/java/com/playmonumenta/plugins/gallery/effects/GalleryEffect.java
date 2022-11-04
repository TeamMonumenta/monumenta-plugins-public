package com.playmonumenta.plugins.gallery.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public abstract class GalleryEffect implements DisplayableEffect {

	protected final @NotNull GalleryEffectType mType;

	public GalleryEffect(@NotNull GalleryEffectType type) {
		mType = type;
	}

	//event called after the player obtain this effect but before this object is insert inside the list of player effects
	//can be used to store info on the player (Scoreboard Tags etc..) or clean up others old effects
	public void playerGainEffect(GalleryPlayer player) {
		player.sendMessage("You have obtained " + ChatColor.GOLD + mType.getRealName());
		GalleryEffect effect = player.getEffectOfType(mType);
		if (effect != null) {
			player.removeEffect(effect);
		}

	}

	//event called after the player lose this effect but before this object is removed from the list of player effects
	public void playerLoseEffect(GalleryPlayer player) {

	}

	public void onPlayerDamage(GalleryPlayer player, DamageEvent event, LivingEntity entity) {

	}

	public void onPlayerHurt(GalleryPlayer player, DamageEvent event, LivingEntity enemy) {

	}

	public void onPlayerFatalHurt(GalleryPlayer player, DamageEvent event, LivingEntity enemy) {

	}

	public void onPlayerDeathEvent(GalleryPlayer player, EntityDeathEvent event, int ticks) {

	}

	public void onOtherPlayerDeathEvent(GalleryPlayer player, EntityDeathEvent event, LivingEntity otherPlayer, int ticks) {

	}

	public void onRoundStart(GalleryPlayer player, GalleryGame game) {

	}

	public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {

	}

	public void refresh(GalleryPlayer player) {

	}

	public void clear(GalleryPlayer player) {
		player.removeEffect(this);
	}

	public GalleryEffectType getType() {
		return mType;
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("EffectTypeName", mType.name());
		return object;
	}

	public <A extends GalleryEffect> GalleryEffect fromJson(JsonObject object) {
		GalleryEffectType type = GalleryEffectType.valueOf(object.get("EffectTypeName").getAsString());
		return type.newEffect();
	}

	// This is not a real duration - gallery effects are infinite
	// This only controls the order of display in the tab list
	// These effects should always be first, and be in a consistent order based on the order in GalleryEffectType
	@Override
	public int getDuration() {
		return 1000000000 + mType.ordinal();
	}

	public abstract boolean canBuy(GalleryPlayer player);




	public static GalleryEffect fromJsonObject(JsonObject object) {
		GalleryEffectType type = GalleryEffectType.fromName(object.get("EffectTypeName").getAsString());
		if (type != null) {
			GalleryEffect effect = type.newEffect();
			if (effect != null) {
				return effect.fromJson(object);
			}
		}
		return null;
	}

}
