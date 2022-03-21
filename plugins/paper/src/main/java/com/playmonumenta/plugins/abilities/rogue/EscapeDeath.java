package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;

public class EscapeDeath extends Ability {

	private static final double TRIGGER_THRESHOLD_HEALTH = 10;
	private static final int RANGE = 5;
	private static final int STUN_DURATION = 20 * 3;
	private static final int BUFF_DURATION = 20 * 8;
	private static final int ABSORPTION_HEALTH = 8;
	private static final double SPEED_PERCENT = 0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EscapeDeathPercentSpeedEffect";
	private static final int JUMP_BOOST_AMPLIFIER = 2;
	private static final int COOLDOWN = 90 * 20;

	public EscapeDeath(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Escape Death");
		mInfo.mLinkedSpell = ClassAbility.ESCAPE_DEATH;
		mInfo.mScoreboardId = "EscapeDeath";
		mInfo.mShorthandName = "ED";
		mInfo.mDescriptions.add("When taking damage from a mob leaves you below 5 hearts, throw a paralyzing grenade that stuns all enemies within 5 blocks for 3 seconds. Cooldown: 90s.");
		mInfo.mDescriptions.add("When this skill is triggered, also gain 4 Absorption hearts for 8 seconds, 30% Speed, and Jump Boost III. If damage taken would kill you but could have been prevented by this skill it will instead do so.");
		mInfo.mCooldown = COOLDOWN;
		mDisplayItem = new ItemStack(Material.DRAGON_BREATH, 1);
	}

	@Override
	public double getPriorityAmount() {
		return 10000;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlocked() && mPlayer != null) {
			double newHealth = mPlayer.getHealth() - event.getFinalDamage(true);
			boolean dealDamageLater = newHealth < 0 && newHealth > -ABSORPTION_HEALTH && isLevelTwo();
			if (newHealth <= TRIGGER_THRESHOLD_HEALTH && (newHealth > 0 || dealDamageLater)) {
				if (dealDamageLater) {
					event.setCancelled(true);
				}
				putOnCooldown();

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE, mPlayer)) {
					EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
				}

				if (isLevelTwo()) {
					AbsorptionUtils.addAbsorption(mPlayer, ABSORPTION_HEALTH, ABSORPTION_HEALTH, BUFF_DURATION);
					mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(BUFF_DURATION, SPEED_PERCENT, PERCENT_SPEED_EFFECT_NAME));
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							new PotionEffect(PotionEffectType.JUMP, BUFF_DURATION, JUMP_BOOST_AMPLIFIER, true, true));
				}

				Location loc = mPlayer.getLocation();
				loc.add(0, 1, 0);

				World world = mPlayer.getWorld();
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3).spawnAsPlayerActive(mPlayer);

				world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.75f, 1.5f);
				world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 0f);

				MessagingUtils.sendActionBarMessage(mPlayer, "Escape Death has been activated");

				if (dealDamageLater) {
					mPlayer.setHealth(1);
					AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) newHealth);
				}
			}
		}
	}

	// this should not happen, but better play it safe
	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}
}
