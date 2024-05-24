package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.AstralOmenCS;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AstralOmenBonusDamage extends Effect {
	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(136, 147, 228), 1f);
	public static final String effectID = "AstralOmenBonusDamage";
	private final double mAmount;
	private final Player mPlayer;
	private final AstralOmenCS mCosmetic;

	public AstralOmenBonusDamage(int duration, double amount, Player player, AstralOmenCS cosmetic) {
		super(duration, effectID);
		mAmount = amount;
		mPlayer = player;
		mCosmetic = cosmetic;

	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void onHurtByEntityWithSource(LivingEntity entity, DamageEvent event, Entity damager, LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (source == mPlayer) {
			mCosmetic.bonusDamage(mPlayer, entity, COLOR);
			event.setDamage(event.getDamage() * (1 + mAmount));
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			mCosmetic.bonusDamageTick(mPlayer, entity, COLOR);
		}
	}

	@Override
	public String toString() {
		return String.format("AstralOmenBonusDamage duration:%d player:%s amount:%f", this.getDuration(), mPlayer.getName(), mAmount);
	}
}
