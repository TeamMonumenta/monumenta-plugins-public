package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

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
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.MessagingUtils;

/* Rampage: Killing an enemy starts a kill streak. If
 * you kill another mob within 2/3 seconds, the streak
 * continues and your kill count increases. Every 4 kills,
 * you are granted 1 armor point and +1 damage. This caps
 * at 24 kills (+6 in stats). At level 2, earn regen 1
 * after 5 kills and regen 2 after 10 kills. When the
 * streak ends, stat modifiers reset.
*/
public class Rampage extends Ability {

	private static final int RAMPAGE_1_KILL_TIMER = 2 * 20; //ticks
	private static final int RAMPAGE_2_KILL_TIMER = 3 * 20; //ticks
	private static final int RAMPAGE_KILL_THRESHOLD = 4;
	private static final int RAMPAGE_KILL_LIMIT = 24;
	private static final int RAMPAGE_2_REGEN_THRESHOLD = 5;

	private int rampageKillStreak = 0;
	private int rampageDuration = 0;
	private int rampageArmorBuff = 0;

	public Rampage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Rampage";
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (rampageKillStreak < RAMPAGE_KILL_LIMIT) {
			Location loc = mPlayer.getLocation();
			rampageKillStreak++;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Kill Streak: " + rampageKillStreak);
			if (rampageKillStreak % RAMPAGE_KILL_THRESHOLD == 0) {
				rampageArmorBuff++;
				AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
				attarmor.setBaseValue(attarmor.getBaseValue() + 1);
				mWorld.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, 2, 0);
			}
			if (rampageKillStreak == RAMPAGE_2_REGEN_THRESHOLD) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 1000000, 0, true, false));
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 25, .25, 1, .25, 1);
			}
			if (rampageKillStreak == (RAMPAGE_2_REGEN_THRESHOLD * 2)) {
				mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.REGENERATION);
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 1000000, 1, true, false));
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 20, .25, 1, .25, 1);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 20, .25, 1, .25, 1);
			}
		} else {
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Kill Streak Maxed Out!");
		}
		boolean run = rampageDuration <= 0;
		int rampage = getAbilityScore();
		rampageDuration = rampage == 1 ? RAMPAGE_1_KILL_TIMER : RAMPAGE_2_KILL_TIMER;
		if (run) {
			new BukkitRunnable() {
				@Override
				public void run() {
					rampageDuration--;
					if (rampageDuration <= 0) {
						this.cancel();
						deactivateRampage();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int rampageDamage = 0;
		if (rampageKillStreak > 0) {
			rampageDamage = (int)(rampageKillStreak / RAMPAGE_KILL_THRESHOLD);
		}
		event.setDamage(event.getDamage() + rampageDamage);
		return true;
	}

	private void deactivateRampage() {
		AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
		if (attarmor.getBaseValue() - rampageArmorBuff > 0) {
			attarmor.setBaseValue(attarmor.getBaseValue() - rampageArmorBuff);
		} else {
			attarmor.setBaseValue(0);
		}
		mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.REGENERATION);
		rampageKillStreak = 0;
	}
}