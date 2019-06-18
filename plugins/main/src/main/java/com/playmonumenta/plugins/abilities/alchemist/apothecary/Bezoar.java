package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Bezoar: When the Apothercary kills an enemy, if 5/3 enemies
 * have died since the last drop, the enemy drops a Bezoar on the ground
 * that lingers for 8/10 seconds. Any player walking over
 * the stone consumes it, healing 4/6 HP and ending
 * non-infinite Poison and Wither. If a mob walks over
 * the stone, it explodes in a 3 block radius for 4/6 damage.
 */

public class Bezoar extends Ability {

	private static final int BEZOAR_FREQUENCY = 4;
	private static final int BEZOAR_1_DAMAGE = 3;
	private static final int BEZOAR_2_DAMAGE = 5;
	private static final int BEZOAR_1_HEAL = 3;
	private static final int BEZOAR_2_HEAL = 5;
	private static final Particle.DustOptions B_COLOUR = new Particle.DustOptions(Color.fromRGB(165, 214, 102), 1.4f);

	private int mKills = 0;
	private int mFrequency = 0;
	private int mBezoarCount = 0;

	public Bezoar(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Bezoar";
		mFrequency = BEZOAR_FREQUENCY;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mBezoarCount < 3) {
			mKills++;
		}
		if (mKills == mFrequency && mBezoarCount < 3) {
			mBezoarCount ++ ;
			if (mBezoarCount < 3) {
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Bezoars: " + mBezoarCount);
			} else {
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Max bezoar count reached!");
			}
			if (canUse(mPlayer)) {
				AbilityUtils.addAlchemistPotions(mPlayer, 1);
			}
			mKills = 0;
		}
	}

	@Override
	public boolean PlayerThrewSplashPotionEvent(SplashPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Alchemist's Potion") && mBezoarCount > 0 && mPlayer.isSneaking()) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.SPELL);
			potion.setMetadata("BezoarInfused", new FixedMetadataValue(mPlugin, 1));
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.TOTEM);
			mBezoarCount--;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Bezoars remaining: " + mBezoarCount);
			Location playerLoc = mPlayer.getLocation();
			mWorld.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7f, 1.5f);
			mWorld.playSound(playerLoc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 0.5f, 1.5f);
		}
		return true;
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion") && potion.hasMetadata("BezoarInfused")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						int damage = (getAbilityScore() == 1) ? BEZOAR_1_DAMAGE : BEZOAR_2_DAMAGE;
						EntityUtils.damageEntity(mPlugin, entity, damage, mPlayer);
					} else if (entity instanceof Player) {
						int heal = (getAbilityScore() == 1) ? BEZOAR_1_HEAL : BEZOAR_2_HEAL;
						PlayerUtils.healPlayer((Player) entity, heal);
						((Player) entity).removePotionEffect(PotionEffectType.WITHER);
						((Player) entity).removePotionEffect(PotionEffectType.POISON);
					}
				}
			}
			Location potionLoc = potion.getLocation();
			mWorld.spawnParticle(Particle.REDSTONE, potionLoc.add(0, 0.25, 0), 25, 1, 0.5, 1, B_COLOUR);
			mWorld.spawnParticle(Particle.SPELL_MOB, potionLoc.add(0, 0.25, 0), 25, 1.25, 0.5, 1.25, 0.001);
			mWorld.spawnParticle(Particle.TOTEM, potionLoc.add(0, 0.2, 0), 30, 0.1, 0.1, 0.1, 0.65);
			mWorld.playSound(potionLoc, Sound.BLOCK_STONE_BREAK, 0.7f, 0.5f);
			mWorld.playSound(potionLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.7f, 1.5f);
			mWorld.playSound(potionLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 2f);
		}
		return true;
	}

	public void incrementKills() {
		if (this.mBezoarCount < 3) {
			mKills++;
		}
	}
}
