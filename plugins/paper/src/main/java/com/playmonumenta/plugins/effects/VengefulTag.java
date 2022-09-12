package com.playmonumenta.plugins.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VengefulTag extends Effect {
	public static final String effectID = "VengefulTag";

	private final Player mPlayer;
	private final LivingEntity mEnemy;

	public VengefulTag(int duration, Player player, LivingEntity enemy) {
		super(duration, effectID);
		mPlayer = player;
		mEnemy = enemy;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public LivingEntity getEnemy() {
		return mEnemy;
	}

	@Override
	public String toString() {
		return String.format("VengefulTag duration:%d player:%s enemy:%s", this.getDuration(), mPlayer.getName(), mEnemy.getName());
	}
}
