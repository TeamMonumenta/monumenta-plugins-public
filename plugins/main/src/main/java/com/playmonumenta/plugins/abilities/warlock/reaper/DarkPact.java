package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class DarkPact extends Ability {

	private static final int DARK_PACT_COOLDOWN = 20 * 10;
	private static final int DARK_PACT_DURATION = 20 * 10;
	private static final double DARK_PACT_1_DAMAGE_MULTIPLIER = 1 + 0.6;
	private static final double DARK_PACT_2_DAMAGE_MULTIPLIER = 1 + 1.0;

	/*
	 * Dark Pact: Sprint + left-click with a scythe to greatly amplify your
	 * power for 10 s, making your skills and melee attacks deal
	 * 60% / 100% more damage. At lvl 2, your scythe attacks also
	 * cleave, dealing AoE damage in front of you. While this
	 * ability is active, you cannot heal health.
	 *  Cooldown: 10 s (Blasphemous Aura treats this skill as if it is always on cooldown)
	 */

	private float mSaturation = 0;
	private double mHealth = 0;
	private boolean active = false;

	public DarkPact(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "DarkPact";
		mInfo.cooldown = DARK_PACT_COOLDOWN;
		mInfo.linkedSpell = Spells.DARK_PACT;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;

		/*
		 * NOTE! Because this skill has two events it needs to bypass the automatic cooldown check
		 * and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public void cast() {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell) || !mPlayer.isSprinting() || !InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}

		active = true;
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 50, 0.2, 0.1, 0.2, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.25f);
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.5f);
		mHealth = mPlayer.getHealth();
		mSaturation = mPlayer.getSaturation();
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 1, 0.25, 0.35, 0.25, 0);

				// Prevent player from losing saturation to healing during Dark Pact
				if (mSaturation < mPlayer.getSaturation()) {
					mSaturation = mPlayer.getSaturation();
				}
				mPlayer.setSaturation(mSaturation);

				if (mHealth < mPlayer.getHealth()) {
					mPlayer.setHealth(mHealth);
				} else {
					mHealth = mPlayer.getHealth();
				}
				if (t >= DARK_PACT_DURATION || mPlayer.isDead()) {
					this.cancel();
					active = false;
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.75f);
					mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 35, 0.25, 0.35, 0.25, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			// Melee attacks only
			if (event.getCause() != DamageCause.ENTITY_ATTACK) {
				return true;
			}

			int level = getAbilityScore();
			double percent = level == 1 ? DARK_PACT_1_DAMAGE_MULTIPLIER : DARK_PACT_2_DAMAGE_MULTIPLIER;
			if (event.getEntity() instanceof Player) {
				percent = level == 1 ? 1.25 : 1.5;
			}

			event.setDamage(event.getDamage() * percent);
			if (level > 1 && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
				Location loc = mPlayer.getLocation().add(0, 1.35, 0);
				Vector dir = loc.getDirection();
				loc.add(dir);
				mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0);
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.4f);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 1.5)) {
					Vector toMobVector = mob.getLocation().toVector().subtract(loc.toVector()).normalize();
					if (mob != event.getEntity() && dir.dot(toMobVector) > 0.6) {
						EntityUtils.damageEntity(mPlugin, mob, event.getDamage() / 2, mPlayer, null, false);
					}
				}
			}
		}
		return true;
	}

}
