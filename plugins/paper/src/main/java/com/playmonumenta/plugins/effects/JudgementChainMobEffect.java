package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JudgementChainMobEffect extends Effect {

	private final Player mPlayer;
	private final String mModifierName;

	public JudgementChainMobEffect(int duration, Player player, String source) {
		super(duration);
		mPlayer = player;
		mModifierName = source;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.addAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(mModifierName, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.removeAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, mModifierName);
		}
	}

	@Override
	public void onHurt(@NotNull LivingEntity entity, @NotNull DamageEvent event) {
		List<LivingEntity> e = EntityUtils.getNearbyMobs(entity.getLocation(), 8, entity);
		if (!e.isEmpty()) {
			event.setDamage(0);
		}
	}

	@Override
	public void onDamage(@NotNull LivingEntity entity, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (entity instanceof Player) {
			if (entity != mPlayer) {
				event.setDamage(0);
			} else {
				event.setDamage(event.getDamage() / 2);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("JudgementChainMobEffect duration:%d", this.getDuration());
	}
}
