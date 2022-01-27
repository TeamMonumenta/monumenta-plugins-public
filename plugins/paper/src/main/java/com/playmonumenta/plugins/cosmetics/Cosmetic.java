package com.playmonumenta.plugins.cosmetics;

import org.bukkit.Material;

public class Cosmetic {
	public String mName;
	public CosmeticType mType;
	public Material mDisplayItem;
	public boolean mEquipped;

	public String getName() {
		return mName;
	}

	public CosmeticType getType() {
		return mType;
	}

	public boolean isEquipped() {
		return mEquipped;
	}

	public Cosmetic(CosmeticType type, String name) {
		mName = name;
		mType = type;
		mEquipped = false;
	}

	public Cosmetic(CosmeticType type, String name, boolean isEquipped) {
		mName = name;
		mType = type;
		mEquipped = isEquipped;
	}
}
