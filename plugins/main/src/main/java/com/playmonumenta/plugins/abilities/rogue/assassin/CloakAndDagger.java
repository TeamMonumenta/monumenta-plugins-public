package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

/*
 * Cloak & Dagger: Every time you kill a normal mob by any means,
 * you gain a stack of "Cloak". Elite kills give you 3 stacks.
 * Your current stack of Cloaks is X.
 * Level 1 - Cloak stacks are capped at 8. When you shift right
 * click while looking down with dual wielded swords, stacks set
 * to 0 and you gain X seconds of invisibility and 1.5X extra
 * damage on your next attack while invisible. This requires a
 * minimum of 5 stacks. Attacking or switching the main hand
 * weapon to anything but a sword cancels invisibility. If you
 * let invisibility expire without attacking, you get Mining
 * Fatigue 2 for 5 seconds.
 * Level 2 - Cloak stacks are capped at 12 and bonus damage is 2.5X.

 */

public class CloakAndDagger extends Ability {

	private static final String CLOAK_METADATA = "CloakAndDaggerPlayerIsInvisible";
	private static final double CLOAK_1_DAMAGE_MULTIPLIER = 1.5;
	private static final double CLOAK_2_DAMAGE_MULTIPLIER = 2.5;
	private static final int CLOAK_1_MAX_STACKS = 8;
	private static final int CLOAK_2_MAX_STACKS = 12;
	private static final int CLOAK_MIN_STACKS = 5;
	private static final int CLOAK_PENALTY_DURATION = 20 * 5;

	public CloakAndDagger(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CloakAndDagger";
		mInfo.linkedSpell = Spells.CLOAK_AND_DAGGER;
		mInfo.cooldown = 0;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	private boolean active = false;
	private int mTickAttacked = 0;
	private int cloak = 0;
	private int cloakOnActivation = 0;

	@Override
	public void cast() {
		if (!active && cloak >= CLOAK_MIN_STACKS && mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50) {
			cloakOnActivation = cloak;
			cloak = 0;
			mTickAttacked = mPlayer.getTicksLived();
			active = true;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.INVISIBILITY, 20 * cloakOnActivation, 0, false, true));
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
			mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32)) {
				if (mob instanceof Mob) {
					Mob m = (Mob) mob;
					if (m.getTarget() != null && m.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
						m.setTarget(null);
					}
				}
			}
			mPlayer.setMetadata(CLOAK_METADATA, new FixedMetadataValue(mPlugin, null));
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
					if (t >= 20 * cloakOnActivation || !active || !InventoryUtils.isSwordItem(mHand)) {
						if (active) {
							mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							                                 new PotionEffect(PotionEffectType.SLOW_DIGGING, CLOAK_PENALTY_DURATION, 1, false, true));
						}
						mPlayer.removeMetadata(CLOAK_METADATA, mPlugin);
						this.cancel();
						active = false;
						mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
						mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
						mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
					}
					t++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active && mTickAttacked != mPlayer.getTicksLived()) {
			active = false;
			mPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
			double multiplier = getAbilityScore() == 1 ? CLOAK_1_DAMAGE_MULTIPLIER : CLOAK_2_DAMAGE_MULTIPLIER;
			event.setDamage(event.getDamage() + cloakOnActivation * multiplier);
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int maxStacks = getAbilityScore() == 1 ? CLOAK_1_MAX_STACKS : CLOAK_2_MAX_STACKS;
		if (cloak < maxStacks) {
			if (EntityUtils.isElite(event.getEntity())) {
				if (cloak <= maxStacks - 3) {
					cloak += 3;
				} else {
					cloak = maxStacks;
				}
			} else {
				cloak++;
			}
		}
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Cloak stacks: " + cloak);
	}

	@Override
	public void EntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (active) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}
}
