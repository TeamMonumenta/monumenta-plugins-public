package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;


/*
 * Challenge: Shifting while left-clicking makes all enemies
 * within 12 blocks target you. You gain Absorption I (2 hearts)
 * / II (4 hearts) and one armor toughness per affected mob
 * (max: 8) for 10 s. Cooldown: 20 s
 */
public class Challenge extends Ability {

	private static final int CHALLENGE_RANGE = 12;
	private static final int CHALLENGE_1_ABS_LVL = 0;
	private static final int CHALLENGE_2_ABS_LVL = 1;
	private static final int CHALLENGE_1_ARMOR_MAX = 4;
	private static final int CHALLENGE_2_ARMOR_MAX = 8;
	private static final int CHALLENGE_DURATION = 10 * 20;
	private static final int CHALLENGE_COOLDOWN = 20 * 20;

	private int armorIncrease;
	private int armorMax;

	public Challenge(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Challenge";
		mInfo.cooldown = CHALLENGE_COOLDOWN;
		mInfo.linkedSpell = Spells.CHALLENGE;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		armorMax = getAbilityScore() == 1 ? CHALLENGE_1_ARMOR_MAX : CHALLENGE_2_ARMOR_MAX;
		armorIncrease = getAbilityScore() == 1 ? 1 : 2;
	}

	@Override
	public void cast(Action action) {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHALLENGE_RANGE, mPlayer);
		int increase = Math.min(armorMax, mobs.size() * armorIncrease);
		AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
		armor.setBaseValue(armor.getBaseValue() + increase);
		new BukkitRunnable() {

			@Override
			public void run() {
				armor.setBaseValue(armor.getBaseValue() - increase);
			}

		}.runTaskLater(mPlugin, CHALLENGE_DURATION);
		int amp = getAbilityScore() == 1 ? CHALLENGE_1_ABS_LVL : CHALLENGE_2_ABS_LVL;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.ABSORPTION,
		                                                  CHALLENGE_DURATION,
		                                                  amp, false, true));
		for (LivingEntity mob : mobs) {
			if (mob instanceof Mob) {
				((Mob)mob).setTarget(mPlayer);
			} else if (mob instanceof Player && AbilityManager.getManager().isPvPEnabled((Player)mob)) {
				Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation(), mob.getLocation());
				Location loc = mob.getLocation();
				loc.setDirection(dir);
				mob.teleport(loc);
			}
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 1);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 25, 0.4, 1, 0.4, 0.7f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1.25, 0), 250, 0, 0, 0, 0.425);
		mWorld.spawnParticle(Particle.CRIT, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getLocation().add(0, 1.25, 0), 300, 0, 0, 0, 1);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
