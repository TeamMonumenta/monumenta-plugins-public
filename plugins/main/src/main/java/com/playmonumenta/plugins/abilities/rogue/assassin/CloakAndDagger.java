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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Cloak & Dagger: When you kill an enemy while sneaking, you cloak
 *  yourself in Invisibility for 5 s, automatically
 *  making enemies un-target you, and preventing them
 *  from targeting you while invisible (dealing any
 *  damage cancels this effect). Your next sword attack
 *  after coming out of stealth deals 8 / 16 extra
 *  damage. At Level 2, you gain Speed II during the
 *  effect. Cooldown: 20 s
 */

public class CloakAndDagger extends Ability {

	private static final int CLOAK_COOLDOWN = 20 * 20;
	private static final int CLOAK_DURATION = 20 * 5;
	private static final int CLOAK_1_DAMAGE = 8;
	private static final int CLOAK_2_DAMAGE = 16;

	public CloakAndDagger(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CloakAndDagger";
		mInfo.cooldown = CLOAK_COOLDOWN;
		mInfo.linkedSpell = Spells.CLOAK_AND_DAGGER;
		mInfo.ignoreCooldown = true;
	}

	private boolean active = false;
	private int mTickAttacked = 0;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active && mTickAttacked != mPlayer.getTicksLived()) {
			active = false;
			mPlayer.removePotionEffect(PotionEffectType.SPEED);
			mPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
			int damage = getAbilityScore() == 1 ? CLOAK_1_DAMAGE : CLOAK_2_DAMAGE;
			event.setDamage(event.getDamage() + damage);
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.CLOAK_AND_DAGGER) ||
		    !mPlayer.isSneaking() || !InventoryUtils.isSwordItem(mHand) || !InventoryUtils.isSwordItem(oHand)) {
			return;
		}

		mTickAttacked = mPlayer.getTicksLived();
		active = true;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 5, 0, false, true));
		if (getAbilityScore() > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 20 * 5, 0, false, true));
		}
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
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				if (t >= CLOAK_DURATION || !active) {
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
		putOnCooldown();
	}

	@Override
	public void EntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (active) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}
}
