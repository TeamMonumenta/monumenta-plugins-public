package com.playmonumenta.plugins.infinitytower.mobs;

import com.playmonumenta.plugins.infinitytower.TowerConstants;

public enum TowerMobClass {
	SPECIAL("Special", ""),
	PROTECTOR("Protector", TowerConstants.MOB_TAG_DEFENDER),
	FIGHTER("Fighter", TowerConstants.MOB_TAG_FIGHTER),
	CASTER("Caster", TowerConstants.MOB_TAG_CASTER);

	private final String mName;
	private final String mTag;

	TowerMobClass(String name, String tag) {
		mName = name;
		mTag = tag;
	}

	public String getName() {
		return mName;
	}

	public String getTag() {
		return mTag;
	}

	public static TowerMobClass getFromName(String name) {
		for (TowerMobClass mobClass : values()) {
			if (mobClass.getName().equals(name)) {
				return mobClass;
			}
		}
		return PROTECTOR;
	}
}
