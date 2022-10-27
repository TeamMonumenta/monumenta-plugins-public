package com.playmonumenta.plugins.cosmetics.skills;

import net.kyori.adventure.text.format.TextColor;

public interface GalleryCS extends LockableCS {

	String GALLERY_COMPLETE_SCB = "GallerySanguineHallsEasterEgg";

	enum GalleryMap {
		SANGUINE("Sanguine Halls", TextColor.fromHexString("#AB0000"))
		;

		public final String mName;
		public final TextColor mColor;

		GalleryMap(String name, TextColor color) {
			mName = name;
			mColor = color;
		}
	}

	GalleryMap getMap();

}
