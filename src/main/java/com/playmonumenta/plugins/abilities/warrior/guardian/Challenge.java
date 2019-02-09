package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.List;
import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;


/*
 * Challenge: Shifting while left-clicking makes all enemies
 * within 12 blocks target you. You gain Absorption I (2 hearts)
 * / III (6 hearts) and one armor toughness per affected mob
 * (max: 8) for 10 s. Cooldown: 30 / 25 s
 */
public class Challenge extends Ability {

	public Challenge(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Challenge";
		mInfo.cooldown = getAbilityScore() == 1 ? 20 * 30 : 20 * 25;
		mInfo.linkedSpell = Spells.CHELLENGE;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		List<Mob> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 12);
		int increase = mobs.size();
		if (increase > 8) {
			increase = 8;
		}
		mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(increase);
		new BukkitRunnable() {

			@Override
			public void run() {
				mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(0);
			}

		}.runTaskLater(mPlugin, 20 * 10);
		int amp = getAbilityScore() == 1 ? 0 : 2;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.ABSORPTION,
		                                                  20 * 10,
		                                                  amp, false, true));
		for (Mob mob : mobs) {
			mob.setTarget(mPlayer);
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 1);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 25, 0.4, 1, 0.4, 0.7f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1.25, 0), 250, 0, 0, 0, 0.425);
		mWorld.spawnParticle(Particle.CRIT, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
