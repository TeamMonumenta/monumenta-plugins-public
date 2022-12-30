package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import java.util.ArrayList;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullAway.Init") // fields are initialised in subclasses
public abstract class PlayerClass {

	public ArrayList<AbilityInfo<?>> mAbilities = new ArrayList<>();
	public int mClass;
	public String mClassName;
	public TextColor mClassColor;
	public ItemStack mDisplayItem;
	public @Nullable String mQuestReq = null;
	public int mQuestReqMin;
	public String mClassDescription;
	public String mClassPassiveDescription;
	public String mClassPassiveName;

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
		info.add("skills", abilities);
		info.add("specs", specs);
		return info;
	}
}
