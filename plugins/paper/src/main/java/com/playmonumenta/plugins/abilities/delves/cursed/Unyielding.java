package com.playmonumenta.plugins.abilities.delves.cursed;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * UNYIELDING: Mobs are 25% faster, deal x1.5 damage, and have 20 / 40 extra health.
 */

public class Unyielding extends Ability {

	private static final int UNYIELDING_CHALLENGE_SCORE = 12;
	private static final int UNYIELDING_RADIUS = 16;
	private static final int UNYIELDING_1_HEALTH = 20;
	private static final int UNYIELDING_2_HEALTH = 40;
	private static final double UNYIELDING_DAMAGE_TAKEN_MULTIPLIER = 1.5;
	private static final double UNYIELDING_SPEED_MULTIPLIER = 1.25;

	// Tracks which mobs have been speed-ified
	private static Set<LivingEntity> mSpeedyBois = new HashSet<LivingEntity>();
	// HashSet garbage collection
	private static BukkitRunnable mSpeedyBoisCleaner;

	private final int mHealth;

	public Unyielding(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mInfo.mIgnoreTriggerCap = true;
		mHealth = ServerProperties.getClassSpecializationsEnabled() ? UNYIELDING_2_HEALTH : UNYIELDING_1_HEALTH;

		// Only run one of these because multiple players may be contributing to the same set
		if (mSpeedyBoisCleaner == null || mSpeedyBoisCleaner.isCancelled()) {
			mSpeedyBoisCleaner = new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<LivingEntity> iter = mSpeedyBois.iterator();
					while (iter.hasNext()) {
						LivingEntity mob = iter.next();
						if (mob.isDead() || !mob.isValid()) {
							iter.remove();
						}
					}
				}
			};
			mSpeedyBoisCleaner.runTaskTimer(mPlugin, 0, 20 * 10);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, UNYIELDING_CHALLENGE_SCORE);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, UNYIELDING_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, UNYIELDING_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(mPlayer.getAttribute(Attribute.GENERIC_ARMOR).getValue(),
				mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue(), event.getDamage(), UNYIELDING_DAMAGE_TAKEN_MULTIPLIER));
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), UNYIELDING_RADIUS)) {
				if (!mSpeedyBois.contains(mob)) {
					mSpeedyBois.add(mob);
					mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(UNYIELDING_SPEED_MULTIPLIER * mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
					mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mHealth + mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
					mob.setHealth(mob.getHealth() + mHealth);
				}
			}
		}
	}

}
