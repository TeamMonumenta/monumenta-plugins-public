package com.playmonumenta.plugins.abilities;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

public class AbilityCollection {
	private Map<Class<?>, Ability> mAbilities = new LinkedHashMap<Class<?>, Ability>();

	public AbilityCollection(List<Ability> abilities) {
		for (Ability ability : abilities) {
			mAbilities.put(ability.getClass(), ability);
		}
	}

	public Collection<Ability> getAbilities() {
		return mAbilities.values();
	}

	public Ability getAbility(Class<?> cls) {
		return mAbilities.get(cls);
	}

	public JsonObject getAsJsonObject() {
		JsonObject playerAbilities = new JsonObject();

		for (Ability ability : mAbilities.values()) {
			playerAbilities.add(ability.getClass().getName(), ability.getAsJsonObject());
		}

		return playerAbilities;
	}
}
