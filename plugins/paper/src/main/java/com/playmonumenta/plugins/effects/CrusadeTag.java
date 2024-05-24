package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CrusadeCS;
import org.bukkit.entity.Entity;

public class CrusadeTag extends ZeroArgumentEffect {
	public static final String effectID = "CrusadeSlayerTag";

	private final CrusadeCS mCosmetic;

	public CrusadeTag(int duration, CrusadeCS cosmetic) {
		super(duration, effectID);
		mCosmetic = cosmetic;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.crusadeTag(entity);
	}

	public CrusadeTag deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		return new CrusadeTag(duration, mCosmetic);
	}


	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("CrusadeSlayerTag duration:%d", this.getDuration());
	}
}
