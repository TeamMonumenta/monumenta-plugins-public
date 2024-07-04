package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VoodooBondsOtherPlayer extends Effect {
	public static final String effectID = "VoodooBondsOtherPlayer";
	private final Player mReaper;
	private final double mResist;

	int mRotation = 0;
	private boolean mTriggerTickParticle = false;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBondsOtherPlayer(int duration, Player reaper, double resist) {
		super(duration, effectID);
		mReaper = reaper;
		mResist = resist;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.isBlocked() || !event.getType().isDefendable()) {
			return;
		}

		LivingEntity source = event.getSource();
		if (source != null) {
			if (event.getDamager() instanceof LivingEntity le) {
				MovementUtils.knockAway(mReaper.getLocation(), le, 0.3f, 0.15f, true);
			}
			event.setDamage(0);
			event.setCancelled(true);
		} else {
			// Ignore DoT damage
			if (event.getType() == DamageType.FIRE || event.getType() == DamageType.AILMENT) {
				return;
			}
			entity.setLastDamage(event.getDamage());
			entity.setNoDamageTicks(20);
			event.setDamage(0);
			event.setCancelled(true);
		}

		mTriggerTickParticle = true;

		// send damage to reaper
		double percentDamage = Math.min(event.getFinalDamage(true), entity.getHealth()) / EntityUtils.getMaxHealth(entity);
		double receivedDamageModifier = (1 - mResist) * (1 + CharmManager.getLevelPercentDecimal(mReaper, VoodooBonds.CHARM_RECEIVED_DAMAGE));
		double damageToDeal = EntityUtils.getMaxHealth(mReaper) * (percentDamage * receivedDamageModifier);
		double absorbHealth = AbsorptionUtils.getAbsorption(mReaper);
		if (absorbHealth <= 0) {
			mReaper.setHealth(Math.max(Math.min(mReaper.getHealth() - damageToDeal, EntityUtils.getMaxHealth(mReaper)), 1));
		} else {
			if (damageToDeal >= absorbHealth) {
				double leftoverHealth = mReaper.getHealth() + absorbHealth - damageToDeal;
				AbsorptionUtils.subtractAbsorption(mReaper, absorbHealth);
				mReaper.setHealth(Math.max(leftoverHealth, 1));
			} else {
				AbsorptionUtils.subtractAbsorption(mReaper, absorbHealth);
			}
		}

		Location loc = entity.getLocation();
		World world = loc.getWorld();
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1, 2);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2f, 0.75f);
		new PPLine(Particle.SPELL_WITCH, loc, mReaper.getLocation()).countPerMeter(6).spawnAsPlayerActive(mReaper);
		new PPLine(Particle.REDSTONE, loc, mReaper.getLocation()).data(COLOR).countPerMeter(6).spawnAsPlayerActive(mReaper);

		Location reaperLoc = mReaper.getLocation();
		new PartialParticle(Particle.SPELL_WITCH, reaperLoc, 40, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(mReaper);
		new PartialParticle(Particle.REDSTONE, reaperLoc, 40, 0.5, 0.5, 0.5, 0, COLOR).spawnAsPlayerActive(mReaper);
		world.playSound(reaperLoc, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(reaperLoc, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 1f, 0.5f);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le) {
			mRotation += 20;
			Location loc = le.getLocation();
			World world = loc.getWorld();
			//replace with halo effect
			double angle = Math.toRadians(mRotation);
			loc.add(FastUtils.cos(angle) * 0.5, le.getHeight(), FastUtils.sin(angle) * 0.5);
			new PartialParticle(Particle.REDSTONE, loc, 5, 0, 0, 0, COLOR).spawnAsEnemy();
			if (mTriggerTickParticle) {
				setDuration(0);
				mTriggerTickParticle = false;
				//replace with better particles
				new PartialParticle(Particle.REDSTONE, loc, 60, 3, 3, 3, 0.075, COLOR).spawnAsEnemy();
				world.playSound(loc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 2f, 1.0f);
			}
		}
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Voodoo Bonds";
	}

	@Override
	public String toString() {
		return String.format("VoodooBondsOtherPlayer duration:%d", this.getDuration());
	}
}
