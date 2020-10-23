package com.playmonumenta.plugins.abilities;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class AbilityCollection {
	private final Map<Class<? extends Ability>, Ability> mAbilities = new LinkedHashMap<>();
	private final Player mPlayer;

	/* TODO: This should persist across relog so players can't skip it easily */
	private int mSilencedUntil = 0;

	public AbilityCollection(Player player, List<Ability> abilities) {
		mPlayer = player;
		for (Ability ability : abilities) {
			mAbilities.put(ability.getClass(), ability);
		}
	}

	public Collection<Ability> getAbilities() {
		if (mPlayer.getTicksLived() < mSilencedUntil) {
			MessagingUtils.sendActionBarMessage(mPlayer, ChatColor.DARK_RED, false, "You are silenced! You cannot use abilites for " + (mSilencedUntil - mPlayer.getTicksLived()) / 20 + "s");
			// A silenced player has no abilities
			return Collections.emptySet();
		} else {
			return mAbilities.values();
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Ability> T getAbility(Class<T> cls) {
		if (mPlayer.getTicksLived() < mSilencedUntil) {
			// A silenced player has no abilities
			return null;
		} else {
			return (T)mAbilities.get(cls);
		}
	}

	public JsonObject getAsJsonObject() {
		JsonObject playerAbilities = new JsonObject();

		for (Ability ability : mAbilities.values()) {
			playerAbilities.add(ability.getClass().getName(), ability.getAsJsonObject());
		}

		return playerAbilities;
	}

	/* Silence a player for this many ticks */
	public void silence(int tickDuration) {
		mSilencedUntil = mPlayer.getTicksLived() + tickDuration;
	}

	/* Silence a player for this many ticks */
	public void unsilence() {
		mSilencedUntil = 0;
	}
}
