package com.playmonumenta.plugins.gallery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.effects.GalleryEffect;
import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GalleryPlayer {

	private final UUID mPlayerUUID;

	private final UUID mMapUUID;

	private boolean mIsDead = false;

	private boolean mShouldTeleportToSpawn = false;

	private final Map<GalleryEffectType, GalleryEffect> mEffects = new HashMap<>();

	public GalleryPlayer(UUID player, UUID mapUUID) {
		mPlayerUUID = player;
		mMapUUID = mapUUID;
	}

	public @NotNull UUID getPlayerUUID() {
		return mPlayerUUID;
	}

	public @Nullable Player getPlayer() {
		return Bukkit.getPlayer(mPlayerUUID);
	}

	public @Nullable GalleryGame getGame() {
		return GalleryManager.GAMES.get(mMapUUID);
	}

	public boolean isOnline() {
		Player player = getPlayer();
		return player != null && player.isOnline();
	}

	public boolean isDead() {
		return mIsDead;
	}

	public void setAlive(boolean alive) {
		mIsDead = !alive;
	}

	public void setShouldTeleportWhenJoining(boolean bool) {
		mShouldTeleportToSpawn = bool;
	}

	public boolean getShouldTeleportWhenJoining() {
		return mShouldTeleportToSpawn;
	}

	public void giveEffect(@Nullable GalleryEffect effect) {
		if (effect != null) {
			effect.playerGainEffect(this);
			mEffects.put(effect.getType(), effect);
		}
	}

	public void removeEffect(@Nullable GalleryEffect effect) {
		if (effect != null && mEffects.containsKey(effect.getType())) {
			effect.playerLoseEffect(this);
			mEffects.remove(effect.getType());
		}
	}

	public @Nullable GalleryEffect getEffectOfType(GalleryEffectType type) {
		return mEffects.get(type);
	}

	public Collection<GalleryEffect> getAllEffects() {
		return mEffects.values();
	}

	public void clearEffects() {
		for (GalleryEffectType type : GalleryEffectType.values()) {
			removeEffect(type.newEffect());
		}
	}

	public void refreshEffects() {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.refresh(this);
		}
	}

	protected void tick(boolean oneSecond, boolean twoHertz, int ticks) {
		if (!isOnline()) {
			return;
		}

		for (GalleryEffect effect : mEffects.values()) {
			effect.tick(this, oneSecond, twoHertz, ticks);
		}

	}

	protected void printPlayerInfo() {
		Player player = getPlayer();
		if (player != null) {
			GalleryGame game = getGame();
			player.sendMessage(Component.text("Mobs this round: " + game.mMobsToSpawnThisRound, NamedTextColor.GRAY));
			player.sendMessage(Component.text("Mobs spawned this round: " + game.mMobsSpawnedThisRound, NamedTextColor.GRAY));
			player.sendMessage(Component.text("Mobs killed this round: " + game.mMobsKilledThisRound, NamedTextColor.GRAY));
			player.sendMessage(Component.text("Round: " + game.getCurrentRound(), NamedTextColor.GRAY));
			player.sendMessage(Component.text("Team coins: " + game.getPlayersCoins(), NamedTextColor.GRAY));
		}
	}

	public void sendMessage(String s) {
		Player player = getPlayer();
		if (player != null) {
			player.sendMessage(Component.text(s, NamedTextColor.GRAY));
		}
	}

	// TODO Add sound category argument
	public void playSound(Sound sound, float pitch, float volume) {
		Player player = getPlayer();
		if (player != null) {
			player.getWorld().playSound(player.getEyeLocation(), sound, SoundCategory.HOSTILE, volume, pitch);
		}
	}

	public void onPlayerJoinEvent() {
		if (isOnline()) {
			if (mShouldTeleportToSpawn) {
				Bukkit.getScheduler().runTaskLater(GalleryManager.mPlugin, () -> {
					Location loc = getGame().getSpawnLocation();
					Player player = getPlayer();
					if (loc != null) {
						if (player != null && player.isOnline() && player.isValid()) {
							player.teleport(loc);
						} else {
							GalleryUtils.printDebugMessage("Somehow teleporting a player== null? or an invalid player!");
						}
					} else {
						GalleryUtils.printDebugMessage("LOCATION NULL when teleporting player back to game! how this happen?");
					}
				}, 3L);
			}
			refreshEffects();
		}
	}

	public void otherPlayerDeathEvent(EntityDeathEvent event, LivingEntity otherPlayer, int ticks) {
		if (isOnline()) {
			for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
				effect.onOtherPlayerDeathEvent(this, event, otherPlayer, ticks);
			}
		}
	}

	public void playerDeathEvent(EntityDeathEvent event, LivingEntity self, int ticks) {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.onPlayerDeathEvent(this, event, ticks);
		}
	}

	public void onPlayerDamageEvent(DamageEvent event, LivingEntity damagee) {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.onPlayerDamage(this, event, damagee);
		}
	}


	public void onPlayerHurtEvent(DamageEvent event, @Nullable LivingEntity source) {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.onPlayerHurt(this, event, source);
		}
	}

	public void onPlayerFatalHurtEvent(DamageEvent event, @Nullable LivingEntity source) {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.onPlayerFatalHurt(this, event, source);
		}
	}

	public void onRoundStart(GalleryGame game) {
		for (GalleryEffect effect : new ArrayList<>(mEffects.values())) {
			effect.onRoundStart(this, game);
		}
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("PlayerUUID", mPlayerUUID.toString());
		obj.addProperty("Death", mIsDead);
		obj.addProperty("ShouldTeleport", mShouldTeleportToSpawn);

		JsonArray effects = new JsonArray();

		for (GalleryEffect effect : mEffects.values()) {
			effects.add(effect.toJson());
		}
		obj.add("Effects", effects);

		return obj;
	}

	public static GalleryPlayer fromJson(JsonObject object, UUID gameUUID) throws Exception {
		UUID playerUUID = UUID.fromString(object.getAsJsonPrimitive("PlayerUUID").getAsString());
		GalleryPlayer player = new GalleryPlayer(playerUUID, gameUUID);
		player.mIsDead = object.getAsJsonPrimitive("Death").getAsBoolean();
		player.mShouldTeleportToSpawn = object.getAsJsonPrimitive("ShouldTeleport").getAsBoolean();

		JsonArray effects = object.getAsJsonArray("Effects");
		for (JsonElement element : effects) {
			GalleryEffect effect = GalleryEffect.fromJsonObject(element.getAsJsonObject());
			if (effect != null) {
				player.mEffects.put(effect.getType(), effect);
			}
		}

		return player;
	}
}
