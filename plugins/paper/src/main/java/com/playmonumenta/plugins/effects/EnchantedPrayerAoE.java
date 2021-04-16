package com.playmonumenta.plugins.effects;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.classes.Spells;

public class EnchantedPrayerAoE extends Effect {

	private final Plugin mPlugin;
	private final int mDamageAmount;
	private final double mHealAmount;
	private final Player mPlayer;
	private final EnumSet<EntityDamageEvent.DamageCause> mAffectedDamageCauses;

	public EnchantedPrayerAoE(Plugin plugin, int duration, int damageAmount, double healAmount, Player player, EnumSet<EntityDamageEvent.DamageCause> affectedDamageCauses) {
		super(duration);
		mPlugin = plugin;
		mDamageAmount = damageAmount;
		mHealAmount = healAmount;
		mPlayer = player;
		mAffectedDamageCauses = affectedDamageCauses;
	}

	// This needs to trigger after any percent damage
	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mDamageAmount);
	}

	@Override
	public boolean entityDealDamageEvent(EntityDamageByEntityEvent event) {
		if (mAffectedDamageCauses == null || mAffectedDamageCauses.contains(event.getCause())) {
			Entity damagee = event.getEntity();
			World world = mPlayer.getWorld();
			world.playSound(damagee.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.9f);
			world.playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1, 1.75f);
			world.spawnParticle(Particle.SPELL_INSTANT, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 100, 0.25f, 0.3f, 0.25f, 1);
			world.spawnParticle(Particle.FIREWORKS_SPARK, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 75, 0, 0, 0, 0.3);
			for (LivingEntity le : EntityUtils.getNearbyMobs(damagee.getLocation(), 3.5)) {
				EntityUtils.damageEntity(mPlugin, le, mDamageAmount, mPlayer, MagicType.HOLY, true, Spells.ENCHANTED_PRAYER);
			}
			AttributeInstance maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			PlayerUtils.healPlayer(mPlayer, maxHealth.getValue() * mHealAmount);
			setDuration(0);
		}

		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Player p = (Player) entity;
		if (fourHertz) {
			World world = p.getWorld();
			Location rightHand = PlayerUtils.getRightSide(p.getEyeLocation(), 0.45).subtract(0, .8, 0);
			Location leftHand = PlayerUtils.getRightSide(p.getEyeLocation(), -0.45).subtract(0, .8, 0);
			world.spawnParticle(Particle.SPELL_INSTANT, leftHand, 2, 0.05f, 0.05f, 0.05f, 0);
			world.spawnParticle(Particle.SPELL_INSTANT, rightHand, 2, 0.05f, 0.05f, 0.05f, 0);
		}
	}

	@Override
	public String toString() {
		return String.format("EnchantedPrayerAoE duration:%d player:%s amount:%f", this.getDuration(), mPlayer.getName(), mDamageAmount);
	}
}
