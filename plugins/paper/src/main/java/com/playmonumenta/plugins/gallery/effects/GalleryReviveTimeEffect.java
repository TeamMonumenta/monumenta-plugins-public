package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.gallery.GalleryPlayer;

public class GalleryReviveTimeEffect extends GalleryStackableEffect {
	/**
	 * Notes: this effect does nothing here. it just used to store info on the player and managed when reviving someone grave
	 * GalleryGrave R 70~
	 */

	private static final int REVIVE_TIME_EFFECT_MAX_STACK = 5;
	public static final int REVIVE_TIME_EFFECT_PER_STACK = 20;

	public GalleryReviveTimeEffect() {
		super(GalleryEffectType.REVIVE_TIME);
	}

	@Override public void clear(GalleryPlayer player) {

	}

	@Override public int getMaxStacks() {
		return REVIVE_TIME_EFFECT_MAX_STACK;
	}
}
