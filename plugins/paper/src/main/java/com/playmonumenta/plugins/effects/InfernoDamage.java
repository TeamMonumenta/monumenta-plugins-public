package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class InfernoDamage extends Effect {

	private final int mLevel;
	private final Player mPlayer;

	public InfernoDamage(int duration, int level, Player player) {
		super(duration);
		mLevel = level;
		mPlayer = player;
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof LivingEntity le) {
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, Inferno.CHARM_DAMAGE, mLevel);
			if (le.getFireTicks() <= 0 || EntityUtils.isFireResistant(le)) {
				damage *= 0.5;
			}
			if (Plugin.getInstance().mEffectManager.hasEffect(entity, MagmaShield.ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME)) {
				damage *= 1 + MagmaShield.ENHANCEMENT_FIRE_DAMAGE_BONUS;
			}
			DamageUtils.damage(mPlayer, le, DamageType.AILMENT, damage, null, true, false);
			new PartialParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05).spawnAsEnemyBuff();
		}
	}

	@Override
	public String toString() {
		return String.format("Inferno duration:%d modifier:%s level:%d", this.getDuration(), "CustomDamageOverTime", mLevel);
	}

}
