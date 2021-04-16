package com.playmonumenta.plugins.effects;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.utils.EntityUtils;

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
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		List<LivingEntity> e = EntityUtils.getNearbyMobs(event.getEntity().getLocation(), 8);
		e.remove(event.getEntity());
		if (!e.isEmpty()) {
			event.setDamage(0);
		}
		return true;
	}

	@Override
	public boolean entityDealDamageEvent(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Player p = (Player) event.getEntity();
			if (p.getName() != mPlayer.getName()) {
				event.setDamage(0);
			} else {
				event.setDamage(event.getDamage() / 2.0);
			}
		}

		return true;
	}


	@Override
	public String toString() {
		return String.format("JudgementChainMobEffect duration:%d", this.getDuration());
	}
}
