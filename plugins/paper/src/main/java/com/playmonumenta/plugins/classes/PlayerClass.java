package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

// fields are initialised in subclasses
@SuppressWarnings("NullAway.Init")
public abstract class PlayerClass {

	public List<AbilityInfo<?>> mAbilities = new ArrayList<>();
	public int mClass;
	public String mClassName;
	public TextColor mClassColor;
	public Material mClassGlassFiller;
	public Material mDisplayItem;
	public @Nullable String mQuestReq = null;
	public int mQuestReqMin;
	public String mClassDescription;
	public AbilityInfo<?> mPassive;

	public PlayerSpec mSpecOne = new PlayerSpec();
	public PlayerSpec mSpecTwo = new PlayerSpec();

	public ImmutableList<AbilityInfo<?>> mTriggerOrder;

	public JsonObject toJson() {
		JsonArray abilities = new JsonArray();
		for (AbilityInfo<?> ability : mAbilities) {
			if (ability != null) {
				abilities.add(ability.toJson());
			}
		}

		JsonArray specs = new JsonArray();
		specs.add(mSpecOne.toJson());
		specs.add(mSpecTwo.toJson());

		JsonObject info = new JsonObject();
		info.addProperty("classId", mClass);
		info.addProperty("className", mClassName);
		info.add("classPassive", mPassive.toJson());
		info.add("skills", abilities);
		info.add("specs", specs);
		return info;
	}

	public @Nullable PlayerSpec getSpecById(int specId) {
		return mSpecOne.mSpecialization == specId ? mSpecOne
			: mSpecTwo.mSpecialization == specId ? mSpecTwo
			: null;
	}

}
