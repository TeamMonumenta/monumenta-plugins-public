package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class MonumentaClasses {
	public ArrayList<PlayerClass> mClasses = new ArrayList<>();
	public final ImmutableList<AbilityInfo<?>> mTriggerOrder;

	public MonumentaClasses() {
		mClasses.add(new Alchemist());
		mClasses.add(new Cleric());
		mClasses.add(new Mage());
		mClasses.add(new Rogue());
		mClasses.add(new Scout());
		mClasses.add(new Warlock());
		mClasses.add(new Warrior());

		mTriggerOrder = mClasses.stream()
			                .flatMap(c -> c.mTriggerOrder.stream())
			                .collect(ImmutableList.toImmutableList());
		List<AbilityInfo<?>> missingInTriggerOrder =
			mClasses.stream()
				.flatMap(c -> Stream.concat(Stream.concat(c.mAbilities.stream(), c.mSpecOne.mAbilities.stream()), c.mSpecTwo.mAbilities.stream()))
				.filter(a -> !a.getTriggers().isEmpty() && !mTriggerOrder.contains(a))
				.toList();
		if (!missingInTriggerOrder.isEmpty()) {
			throw new IllegalStateException("Abilities with triggers are missing in trigger order:  " + missingInTriggerOrder);
		}
	}

	public JsonObject toJson() {
		JsonArray classes = new JsonArray();
		for (PlayerClass playerClass : mClasses) {
			classes.add(playerClass.toJson());
		}

		JsonObject obj = new JsonObject();
		obj.add("classes", classes);
		return obj;
	}

	public List<PlayerClass> getClasses() {
		return new ArrayList<>(mClasses);
	}

	public @Nullable PlayerClass getClassById(int classId) {
		for (PlayerClass clazz : mClasses) {
			if (clazz.mClass == classId) {
				return clazz;
			}
		}
		return null;
	}
}
