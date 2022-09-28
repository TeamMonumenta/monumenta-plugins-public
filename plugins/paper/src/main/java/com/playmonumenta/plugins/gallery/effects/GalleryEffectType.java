package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.gallery.GalleryPlayer;
import org.jetbrains.annotations.NotNull;

public enum GalleryEffectType {
	//effects with stacks
	HEALTH("Health", GalleryHealthEffect.class),
	SPEED("Speed", GallerySpeedEffect.class),
	DAMAGE("Damage", GalleryDamageEffect.class),
	REVIVE_TIME("Revive Time", GalleryReviveTimeEffect.class),

	//Consumables and special rules effects
	PHOENIX("Phoenix", GalleryPhoenixEffect.class),
	WIDOW_WEB("Widow's Web", GalleryWidowWebEffect.class),
	ENLIGHTENMENT("Enlightenment", GalleryEnlightenmentEffect.class),
	FALLING_WRATH("Falling Wrath", GalleryFallingWrathEffect.class),
	EXECUTIONER_RAGE("Executioner's Rage", GalleryExecutionerRageEffect.class);

	public final @NotNull String mName;
	public final @NotNull Class<? extends GalleryEffect> mBaseEffect;

	GalleryEffectType(@NotNull String name, @NotNull Class<? extends GalleryEffect> baseEffect) {
		mName = name;
		mBaseEffect = baseEffect;
	}

	public String getRealName() {
		return mName;
	}

	public boolean canBuy(GalleryPlayer player) {
		return newEffect().canBuy(player);
	}

	public GalleryEffect newEffect() {
		try {
			return mBaseEffect.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static GalleryEffectType fromName(String name) {
		for (GalleryEffectType type : values()) {
			if (type.name().equals(name)) {
				return type;
			}
		}
		return null;
	}

}
