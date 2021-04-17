package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class EscapeDeath extends Ability {

	private static final double TRIGGER_THRESHOLD_HEALTH = 10;
	private static final int RANGE = 5;
	private static final int STUN_DURATION = 20 * 3;
	private static final int BUFF_DURATION = 20 * 8;
	private static final int ABSORPTION_AMPLIFIER = 1;
	private static final double SPEED_PERCENT = 0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EscapeDeathPercentSpeedEffect";
	private static final int JUMP_BOOST_AMPLIFIER = 2;
	private static final int COOLDOWN = 90 * 20;

	public EscapeDeath(Plugin plugin, Player player) {
		super(plugin, player, "Escape Death");
		mInfo.mLinkedSpell = Spells.ESCAPE_DEATH;
		mInfo.mScoreboardId = "EscapeDeath";
		mInfo.mShorthandName = "ED";
		mInfo.mDescriptions.add("When taking damage from a mob leaves you below 5 hearts, throw a paralyzing grenade that stuns all enemies within 5 blocks for 3 seconds. Cooldown: 90s.");
		mInfo.mDescriptions.add("When this skill is triggered, also gain 8 seconds of Absorption II, 30% Speed, and Jump Boost III. If damage taken would kill you but could have been prevented by this skill it will instead do so");
		mInfo.mCooldown = COOLDOWN;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		if (event.getCause() == DamageCause.ENTITY_EXPLOSION || event.getCause() == DamageCause.CUSTOM) {
			execute((EntityDamageByEntityEvent) event);
			return true;
		}
		return true;
	}

	// *TO DO* - Turn this into function called by both this skill and Prismatic Shield
	private void execute(EntityDamageByEntityEvent event) {
		double newHealth = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);
		boolean dealDamageLater = newHealth < 0 && newHealth > -8 && getAbilityScore() > 1;
		if (newHealth <= TRIGGER_THRESHOLD_HEALTH && (newHealth > 0 || dealDamageLater)) {
			if (dealDamageLater) {
				event.setCancelled(true);
			}
			putOnCooldown();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE, mPlayer)) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
			}

			if (getAbilityScore() > 1) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.ABSORPTION, BUFF_DURATION, ABSORPTION_AMPLIFIER, true, true));
				mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(BUFF_DURATION, SPEED_PERCENT, PERCENT_SPEED_EFFECT_NAME));
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.JUMP, BUFF_DURATION, JUMP_BOOST_AMPLIFIER, true, true));
			}

			Location loc = mPlayer.getLocation();
			loc.add(0, 1, 0);

			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3);

			world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.75f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 0f);

			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Escape Death has been activated");

			if (dealDamageLater) {
				mPlayer.setHealth(1);
				AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) newHealth);
			}
		}
	}

}
