package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class DarkPact extends Ability {

	/*
	 * Dark Pact: Sprint + left-click to greatly amplify your
	 * power for 10 s, making your skills and melee attacks deal
	 * 50% / 75% more damage. At lvl 2, your scythe attacks also
	 * cleave, dealing AoE damage in front of you. When this
	 *  buff expires, you suffer from Weakness II for 10 s.
	 *  Cooldown: 30 s
	 */

	private boolean active = false;
	public DarkPact(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "DarkPact";
		mInfo.cooldown = 20 * 30;
		mInfo.linkedSpell = Spells.DARK_PACT;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		active = true;
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 50, 0.2, 0.1, 0.2, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1.25f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.75f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 1, 0.25, 0.35, 0.25, 0);
				if (t >= 20 * 10 || mPlayer.isDead() || mPlayer == null) {
					this.cancel();
					active = false;
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 0.75f);
					mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 35, 0.25, 0.35, 0.25, 1);
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 1, true, true));
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active) {
			if (event.getCause() == DamageCause.ENTITY_ATTACK) {
				return true;
			}
			int level = getAbilityScore();
			double percent = level == 1 ? 1.5 : 1.75;
			event.setDamage(event.getDamage() * percent);
			if (level > 1) {
				if (InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
					Location loc = mPlayer.getLocation().add(0, 1.35, 0);
					Vector dir = loc.getDirection();
					loc.add(dir);
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.4f);
					for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
						if (EntityUtils.isHostileMob(e)) {
							Vector toMobVector = e.getLocation().toVector().subtract(loc.toVector())
							                     .normalize();
							if (dir.dot(toMobVector) > 0.6) {
								EntityUtils.damageEntityNoEvent(mPlugin, (LivingEntity) e, event.getDamage() / 2, mPlayer);
							}
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (active) {
			double percent = getAbilityScore() == 1 ? 1.5 : 1.75;
			event.setDamage(event.getDamage() * percent);
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSprinting();
	}

}
