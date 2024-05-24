package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.AstralOmenCS;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


public class AstralOmenArcaneStacks extends Effect {
	public static final String effectID = "AstralOmenArcaneStacks";
	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1f);
	private final int mLevel;
	private final AstralOmenCS mCosmetic;
	private final Player mPlayer;

	public AstralOmenArcaneStacks(int duration, int level, Player player, AstralOmenCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mCosmetic = cosmetic;
		mLevel = level;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			mCosmetic.arcaneStack(mPlayer, entity, COLOR);
		}
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s magnitude:%s",
			this.getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}
}
