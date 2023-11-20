package com.playmonumenta.plugins.effects;

import org.bukkit.entity.LivingEntity;

public class SkySeekersTag extends Effect {
	public static final String effectID = "SkySeekersTag";

	private final LivingEntity mPlayer;

	public SkySeekersTag(int duration, LivingEntity player) {
		super(duration, effectID);
		mPlayer = player;
	}

	public LivingEntity getPlayer() {
		return mPlayer;
	}

	@Override
	public String toString() {
		return String.format("SkySeekersTag duration:%d player:%s", this.getDuration(), mPlayer.getName());
	}
}
