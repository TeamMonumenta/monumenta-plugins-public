package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.MessagingUtils;

/* Rampage: Killing an enemy starts a kill streak. If
 * you kill another mob within 8/10 seconds, the streak
 * continues and your kill count increases. Every 3 kills,
 * you are granted 1 armor point and +1 damage. This caps
 * at 15 kills (+5 in stats). At level 2, earn regen 1
 * after 5 kills and regen 2 after 10 kills. When the
 * streak ends, stat modifiers reset.
*/
public class Rampage extends Ability {

	private static final int RAMPAGE_1_KILL_TIMER = 8 * 20; //ticks
	private static final int RAMPAGE_2_KILL_TIMER = 10 * 20; //ticks
	private static final int RAMPAGE_KILL_THRESHOLD = 3;
	private static final int RAMPAGE_KILL_LIMIT = 15;
	private static final int RAMPAGE_2_REGEN_THRESHOLD = 5;

	private int rampageKillStreak = 0;
	private int rampageArmorBuff = 0;
	private int rampageKillStreakTime;

	private int timeToNextDecrement = 0;

	private static final Particle.DustOptions RAMPAGE_COLOR_1 = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);
	private static final Particle.DustOptions RAMPAGE_COLOR_2 = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	public Rampage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Rampage";
		rampageKillStreakTime = getAbilityScore() == 1 ? RAMPAGE_1_KILL_TIMER : RAMPAGE_2_KILL_TIMER;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		timeToNextDecrement = 0;
		if (rampageKillStreak < RAMPAGE_KILL_LIMIT) {
			rampageKillStreak++;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Kill Streak: " + rampageKillStreak);
		} else {
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Kill Streak Maxed Out!");
		}
		float pitch = (float)(((float) rampageKillStreak) / 11);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.5f, pitch);
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (rampageKillStreak > 0) {
			timeToNextDecrement += ticks;
			if (timeToNextDecrement >= rampageKillStreakTime) {
				timeToNextDecrement = 0;
				rampageKillStreak--;
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Kill Streak: " + rampageKillStreak);
			}
		}

		// Apply regen buffs at thresholds
		if (getAbilityScore() > 1) {
			Location loc = mPlayer.getLocation().add(0, 1, 0);
			if (rampageKillStreak >= RAMPAGE_2_REGEN_THRESHOLD * 2) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, false));
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, .4, 0.45, .4, RAMPAGE_COLOR_1);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, .4, 0.45, .4, RAMPAGE_COLOR_2);
			} else if (rampageKillStreak >= RAMPAGE_2_REGEN_THRESHOLD) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
				mWorld.spawnParticle(Particle.REDSTONE, loc, 5, .4, 0.45, .4, RAMPAGE_COLOR_2);
			}
		}

		// Update armor attribute if it has changed
		if (rampageKillStreak / RAMPAGE_KILL_THRESHOLD != rampageArmorBuff) {
			AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
			attarmor.setBaseValue(attarmor.getBaseValue() - rampageArmorBuff + rampageKillStreak / RAMPAGE_KILL_THRESHOLD);
			rampageArmorBuff = rampageKillStreak / RAMPAGE_KILL_THRESHOLD;
		}
	}

	// Increase damage
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() + rampageKillStreak / RAMPAGE_KILL_THRESHOLD);
		return true;
	}

}
