package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Your Alchemist Potions deal +3/6 damage and have a 10%/20%
 * chance of causing mobs to attack nearby mobs for 4 seconds.
 */

public class NightmarishAlchemy extends Ability {
	private static final int NIGHTMARISH_ALCHEMY_1_DAMAGE = 3;
	private static final int NIGHTMARISH_ALCHEMY_2_DAMAGE = 6;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_DURATION = 20 * 4;
	private static final float NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE = 0.1f;
	private static final float NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE = 0.2f;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_RANGE = 8;

	public NightmarishAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Nightmarish";
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int damage = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_DAMAGE : NIGHTMARISH_ALCHEMY_2_DAMAGE;
				BrutalAlchemy ba = (BrutalAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
				if (ba != null) {
					damage += ba.getDamage();
				}
				float chance = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_CONFUSION_CHANCE : NIGHTMARISH_ALCHEMY_2_CONFUSION_CHANCE;
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity) && entity instanceof Creature) {
						EntityUtils.damageEntity(mPlugin, entity, damage, mPlayer);
						if (!EntityUtils.isBoss(entity) && mRandom.nextFloat() < chance) {
							List<LivingEntity> mobs = EntityUtils.getNearbyMobs(entity.getLocation(), NIGHTMARISH_ALCHEMY_CONFUSION_RANGE);
							for (LivingEntity mob : mobs) {
								if (mob.getUniqueId() != entity.getUniqueId()) {
									((Creature) entity).setTarget(mob);
									new BukkitRunnable() {
										@Override
										public void run() {
											((Creature) entity).setTarget(null);
										}
									}.runTaskLater(mPlugin, NIGHTMARISH_ALCHEMY_CONFUSION_DURATION);
								}
							}
						}
					}
				}
			}
			mWorld.spawnParticle(Particle.SPELL_WITCH, potion.getLocation(), 40, 1, 0, 1, 1);
		}

		return true;
	}
}
