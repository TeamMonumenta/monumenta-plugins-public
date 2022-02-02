package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.abilities.delves.DelveModifier;
import com.playmonumenta.plugins.classes.ClassAbility;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbilityCollection {

	// This map contains all abilities, including delve modifiers
	// LinkedHashMap to preserve ordering
	private final Map<Class<? extends Ability>, Ability> mAbilities = new LinkedHashMap<>();

	/*
	 * This map just contains delve modifiers for when the player is silenced
	 *
	 * Delve modifiers should probably not be piggybacking off the abilities
	 * system, but that's a problem for another day
	 */
	private final Map<Class<? extends Ability>, Ability> mDelveModifiers = new LinkedHashMap<>();

	private boolean mIsSilenced = false;

	public AbilityCollection(List<Ability> abilities) {
		for (Ability ability : abilities) {
			mAbilities.put(ability.getClass(), ability);

			if (ability instanceof DelveModifier) {
				mDelveModifiers.put(ability.getClass(), ability);
			}
		}
	}

	public Collection<Ability> getAbilities() {
		if (mIsSilenced) {
			// A silenced player has no abilities
			return mDelveModifiers.values();
		} else {
			return mAbilities.values();
		}
	}

	public Collection<Ability> getAbilitiesIgnoringSilence() {
		return mAbilities.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends Ability> T getAbility(Class<T> cls) {
		if (mIsSilenced) {
			// A silenced player has no abilities
			return (T) mDelveModifiers.get(cls);
		} else {
			return (T) mAbilities.get(cls);
		}
	}

	public @Nullable Ability getAbility(ClassAbility classAbility) {
		return mIsSilenced ? null : getAbilityIgnoringSilence(classAbility);
	}

	@SuppressWarnings("unchecked")
	public <T extends Ability> T getAbilityIgnoringSilence(Class<T> cls) {
		return (T) mAbilities.get(cls);
	}

	public @Nullable Ability getAbilityIgnoringSilence(ClassAbility classAbility) {
		for (Ability ability : mAbilities.values()) {
			if (ability.getInfo().mLinkedSpell == classAbility) {
				return ability;
			}
		}
		return null;
	}

	public JsonElement getAsJson() {
		JsonArray playerAbilities = new JsonArray();

		for (Ability ability : mAbilities.values()) {
			playerAbilities.add(ability.toString());
		}

		return playerAbilities;
	}

	public void silence() {
		mIsSilenced = true;
	}

	public void unsilence() {
		mIsSilenced = false;
	}

	public boolean isSilenced() {
		return mIsSilenced;
	}
}
