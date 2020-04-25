package com.playmonumenta.plugins.abilities;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

public class AbilityCollection {
	private Map<Class<? extends Ability>, Ability> mAbilities = new LinkedHashMap<Class<? extends Ability>, Ability>();

	public AbilityCollection(List<Ability> abilities) {
		for (Ability ability : abilities) {
			mAbilities.put(ability.getClass(), ability);
		}
	}

	public Collection<Ability> getAbilities() {
		return mAbilities.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends Ability> T getAbility(Class<T> cls) {
		return (T)mAbilities.get(cls);
	}

	public JsonObject getAsJsonObject() {
		JsonObject playerAbilities = new JsonObject();

		for (Ability ability : mAbilities.values()) {
			playerAbilities.add(ability.getClass().getName(), ability.getAsJsonObject());
		}

		return playerAbilities;
	}
}
