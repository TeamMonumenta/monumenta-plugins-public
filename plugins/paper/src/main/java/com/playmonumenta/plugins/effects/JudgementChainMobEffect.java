package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class JudgementChainMobEffect extends Effect {
	public static final String effectID = "JudgementChainMobEffect";

	private final Player mPlayer;
	private final NamedTextColor mGlowColor;
	private @Nullable GlowingManager.ActiveGlowingEffect mGlowingEffect;

	public JudgementChainMobEffect(int duration, Player player, NamedTextColor glowColor) {
		super(duration, effectID);
		mPlayer = player;
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
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		PotionUtils.applyPotion(mPlayer, (LivingEntity) entity,
			new PotionEffect(PotionEffectType.GLOWING, 6, 0, true, false));
	}

	@Override
	public String toString() {
		return String.format("JudgementChainMobEffect duration:%d", this.getDuration());
	}
}
