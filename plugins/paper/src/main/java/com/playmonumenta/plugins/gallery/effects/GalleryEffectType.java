package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.gallery.GalleryPlayer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum GalleryEffectType {
	//effects with stacks
	HEALTH("Health", GalleryHealthEffect.class, GalleryHealthEffect::new),
	SPEED("Speed", GallerySpeedEffect.class, GallerySpeedEffect::new),
	DAMAGE("Damage", GalleryDamageEffect.class, GalleryDamageEffect::new),
	REVIVE_TIME("Revive Time", GalleryReviveTimeEffect.class, GalleryReviveTimeEffect::new),

	//Consumables and special rules effects
	PHOENIX("Phoenix", GalleryPhoenixEffect.class, GalleryPhoenixEffect::new),
	WIDOW_WEB("Widow's Web", GalleryWidowWebEffect.class, GalleryWidowWebEffect::new),
	ENLIGHTENMENT("Enlightenment", GalleryEnlightenmentEffect.class, GalleryEnlightenmentEffect::new),
	FALLING_WRATH("Falling Wrath", GalleryFallingWrathEffect.class, GalleryFallingWrathEffect::new),
	EXECUTIONER_RAGE("Executioner's Rage", GalleryExecutionerRageEffect.class, GalleryExecutionerRageEffect::new);

	public final @NotNull String mName;
	public final @NotNull Class<? extends GalleryEffect> mBaseEffect;
	private final Supplier<? extends GalleryEffect> mConstructor;

	<T extends GalleryEffect> GalleryEffectType(@NotNull String name, @NotNull Class<T> baseEffect, Supplier<T> constructor) {
		mName = name;
		mBaseEffect = baseEffect;
		mConstructor = constructor;
	}

	public String getRealName() {
		return mName;
	}

	public boolean canBuy(GalleryPlayer player) {
		return newEffect().canBuy(player);
	}

	public GalleryEffect newEffect() {
		return mConstructor.get();
	}

	public static @Nullable GalleryEffectType fromName(@Nullable String name) {
		for (GalleryEffectType type : values()) {
			if (type.name().equals(name)) {
				return type;
			}
		}
		return null;
	}

}
