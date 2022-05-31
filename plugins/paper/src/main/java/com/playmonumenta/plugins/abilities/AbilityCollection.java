package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.classes.ClassAbility;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

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

	private boolean mIsSilenced = false;

	public AbilityCollection(List<Ability> abilities) {
		for (Ability ability : abilities) {
			mAbilities.put(ability.getClass(), ability);

		}
	}

	public Collection<Ability> getAbilities() {
		if (!mIsSilenced) {
			return mAbilities.values();
		}
		return Collections.EMPTY_SET;
	}

	public Collection<Ability> getAbilitiesIgnoringSilence() {
		return mAbilities.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends Ability> T getAbility(Class<T> cls) {
		if (!mIsSilenced) {
			return (T) mAbilities.get(cls);
		}
		return null;

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
