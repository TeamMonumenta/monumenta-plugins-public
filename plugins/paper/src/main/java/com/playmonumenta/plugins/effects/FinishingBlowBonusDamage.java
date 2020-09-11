package com.playmonumenta.plugins.effects;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.abilities.scout.FinishingBlow;

public class FinishingBlowBonusDamage extends Effect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(168, 0, 0), 1.0f);
	private static final BlockData BLOCK_CRACK = Bukkit.createBlockData("redstone_wire[power=8]");

	private final double mAmount;
	private final Player mPlayer;

	public FinishingBlowBonusDamage(int duration, double amount, Player player) {
		super(duration);
		mAmount = amount;
		mPlayer = player;
	}

	// This needs to trigger after any percent damage
	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent && mPlayer.equals(((EntityDamageByEntityEvent) event).getDamager())
				&& event.getCause() == DamageCause.ENTITY_ATTACK) {
			Entity entity = event.getEntity();
			if (entity instanceof LivingEntity) {
				LivingEntity mob = (LivingEntity) entity;

				World world = mob.getWorld();
				Location loc = mob.getLocation().add(0, 1, 0);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.25f);
				world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 1f, 1.75f);
				world.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1f, 1.25f);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.65f);
				world.spawnParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.4);
				world.spawnParticle(Particle.SMOKE_NORMAL, loc, 20, 0.25, 0.5, 0.25, 0.1);
				world.spawnParticle(Particle.BLOCK_CRACK, loc, 20, 0.25, 0.5, 0.25, 0.4, BLOCK_CRACK);

				AttributeInstance maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if ((mob.getHealth() - event.getFinalDamage()) / maxHealth.getValue() < FinishingBlow.LOW_HEALTH_THRESHOLD) {
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 1.75f);
					world.spawnParticle(Particle.CRIT, loc, 15, 0.25, 0.5, 0.25, 0.4);
					world.spawnParticle(Particle.REDSTONE, loc, 25, 0.35, 0.5, 0.35, 1.2, COLOR);

					event.setDamage(event.getDamage() + mAmount * FinishingBlow.LOW_HEALTH_DAMAGE_DEALT_MULTIPLIER);
				} else {
					event.setDamage(event.getDamage() + mAmount);
				}
			}
		}

		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			World world = entity.getWorld();
			Location loc = entity.getLocation().add(0, 1, 0);
			world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02);
			world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.5, 0.25, 0);
		}
	}

}
