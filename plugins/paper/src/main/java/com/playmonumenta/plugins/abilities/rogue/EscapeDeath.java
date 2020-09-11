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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
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
	private static final int SPEED_AMPLIFIER = 1;
	private static final int JUMP_BOOST_AMPLIFIER = 2;
	private static final int COOLDOWN = 90 * 20;

	public EscapeDeath(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Escape Death");
		mInfo.mLinkedSpell = Spells.ESCAPE_DEATH;
		mInfo.mScoreboardId = "EscapeDeath";
		mInfo.mShorthandName = "ED";
		mInfo.mDescriptions.add("When taking damage from a mob leaves you below 5 hearts, throw a paralyzing grenade that stuns all enemies within 5 blocks for 3 seconds. Cooldown: 90 seconds.");
		mInfo.mDescriptions.add("When this skill is triggered, also gain 8 seconds of Absorption II, Speed II, and Jump Boost III.");
		mInfo.mCooldown = COOLDOWN;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		execute(event);
		return true;
	}

	private void execute(EntityDamageByEntityEvent event) {
		double newHealth = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);
		if (newHealth <= TRIGGER_THRESHOLD_HEALTH && newHealth > 0) {
			putOnCooldown();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE, mPlayer)) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
			}

			if (getAbilityScore() > 1) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.ABSORPTION, BUFF_DURATION, ABSORPTION_AMPLIFIER, true, true));
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, SPEED_AMPLIFIER, true, true));
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.JUMP, BUFF_DURATION, JUMP_BOOST_AMPLIFIER, true, true));
			}

			Location loc = mPlayer.getLocation();
			loc.add(0, 1, 0);

			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3);

			mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, 0.75f, 1.5f);
			mWorld.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 0f);

			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Escape Death has been activated");
		}
	}

}
