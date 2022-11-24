package com.playmonumenta.plugins.abilities;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class AbilityCollection {

	private static final MonumentaClasses MONUMENTA_CLASSES = new MonumentaClasses();

	// This map contains all abilities, including delve modifiers
	// LinkedHashMap to preserve ordering
	private final Map<Class<? extends Ability>, Ability> mAbilities = new LinkedHashMap<>();

	// Abilities that are still active when silenced
	private final Map<Class<? extends Ability>, Ability> mSilenceAbilities = new LinkedHashMap<>();

	private final ImmutableList<Ability> mAbilitiesInTriggerOrder;

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
			if (ability.getInfo().doesIgnoreSilence()) {
				mSilenceAbilities.put(ability.getClass(), ability);
				if (!ability.getInfo().getTriggers().isEmpty()) {
					MMLog.warning("Ability with a trigger is set to ignore silence - it won't actually run! " + ability.getInfo().getAbilityClass());
				}
			}
		}
		mAbilitiesInTriggerOrder =
			abilities.stream()
				.filter(a -> !a.getInfo().getTriggers().isEmpty())
				.sorted(Comparator.comparingDouble(ability -> MONUMENTA_CLASSES.mTriggerOrder.indexOf(ability.getInfo())))
				.collect(ImmutableList.toImmutableList());
	}

	public Collection<Ability> getAbilities() {
		if (!mIsSilenced) {
			return mAbilities.values();
		}
		return mSilenceAbilities.values();
	}

	public ImmutableList<Ability> getAbilitiesInTriggerOrder() {
		if (!mIsSilenced) {
			return mAbilitiesInTriggerOrder;
		}
		return ImmutableList.of();
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
	public <T extends Ability> @Nullable T getAbilityIgnoringSilence(Class<T> cls) {
		return (T) mAbilities.get(cls);
	}

	public @Nullable Ability getAbilityIgnoringSilence(ClassAbility classAbility) {
		for (Ability ability : mAbilities.values()) {
			if (ability.getInfo().getLinkedSpell() == classAbility) {
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
