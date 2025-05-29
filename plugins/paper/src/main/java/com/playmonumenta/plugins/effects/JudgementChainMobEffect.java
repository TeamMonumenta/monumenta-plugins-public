package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.managers.GlowingManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class JudgementChainMobEffect extends Effect {
	public static final String effectID = "JudgementChainMobEffect";

	private final NamedTextColor mGlowColor;
	private @Nullable GlowingManager.ActiveGlowingEffect mGlowingEffect;

	public JudgementChainMobEffect(int duration, NamedTextColor glowColor) {
		super(duration, effectID);
		mGlowColor = glowColor;
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mGlowingEffect = GlowingManager.startGlowing(entity, mGlowColor, getDuration(), GlowingManager.PLAYER_ABILITY_PRIORITY + 2);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mGlowingEffect != null) {
			mGlowingEffect.clear();
		}
	}

	@Override
	public String toString() {
		return String.format("JudgementChainMobEffect duration:%d", this.getDuration());
	}
}
