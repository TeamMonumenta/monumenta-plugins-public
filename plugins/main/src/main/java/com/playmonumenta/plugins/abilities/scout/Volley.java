package com.playmonumenta.plugins.abilities.scout;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Volley extends Ability {

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 10;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.5;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 2.0;

	public Volley(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.VOLLEY;
		mInfo.scoreboardId = "Volley";
		mInfo.cooldown = VOLLEY_COOLDOWN;

		/*
		 * NOTE! Because Volley has two events - the actual shot event won't trigger by default
		 * when volley is on cooldown. Therefor it needs to bypass the automatic cooldown check
		 * and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (!mPlayer.isSneaking()
		    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			/* This ability is actually on cooldown - event proceeds as normal */
			return true;
		}

		//Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.33f);
		Location eye = mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection());
		mWorld.spawnParticle(Particle.CRIT, eye, 35, 0, 0, 0, 0.7f);
		mWorld.spawnParticle(Particle.CRIT_MAGIC, eye, 35, 0, 0, 0, 0.7f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, eye, 15, 0, 0, 0, 0.15f);
		// This needs to be run 1 tick later so that Bow Mastery, Sharpshooter, and Quickdraw can set their metadata on the original arrow
		new BukkitRunnable() {
			@Override
			public void run() {
				List<Projectile> projectiles;
				int volley = getAbilityScore();
				int numArrows = (volley == 1) ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;

				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;
				if (arrow instanceof TippedArrow) {
					TippedArrow tArrow = (TippedArrow)arrow;

					tArrowData = tArrow.getBasePotionData();
					if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
						// This arrow isn't weakness or slowness - don't store the potion data
						tArrowData = null;
					}
				}

				if (tArrowData == null) {
					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.75, 5, Arrow.class);
				} else {
					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.75, 5, TippedArrow.class);
				}

				for (Projectile proj : projectiles) {
					Arrow _arrow = (Arrow)proj;

					proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));

					// If the base arrow's potion data is still stored, apply it to the new arrows
					if (tArrowData != null && _arrow instanceof TippedArrow) {
						((TippedArrow)_arrow).setBasePotionData(tArrowData);
					}

					_arrow.setCritical(arrow.isCritical());
					_arrow.setFireTicks(arrow.getFireTicks());
					_arrow.setKnockbackStrength(arrow.getKnockbackStrength());
					_arrow.setDamage(arrow.getDamage());
					if (arrow.hasMetadata("ArrowQuickdraw")) {
						// Manually register these tags because volley doesn't for some reason
						_arrow.setMetadata("ArrowQuickdraw", new FixedMetadataValue(mPlugin, null));
						if (AbilityUtils.getArrowBaseDamage(_arrow) == 0) {
							AbilityUtils.setArrowBaseDamage(mPlugin, _arrow, AbilityUtils.getArrowBaseDamage(arrow));
						}
					}
					double multiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;
					AbilityUtils.multiplyArrowFinalDamageMultiplier(mPlugin, _arrow, multiplier);
					// Manually register these tags because volley doesn't for some reason
					if (AbilityUtils.getArrowBonusDamage(_arrow) == 0) {
						AbilityUtils.addArrowBonusDamage(mPlugin, _arrow, AbilityUtils.getArrowBonusDamage(arrow));
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

					//Fire: How stupid of me. I completely forgot we can call Bukkit Events on our OWN
					ProjectileLaunchEvent event = new ProjectileLaunchEvent(_arrow);
					Bukkit.getPluginManager().callEvent(event);
				}

				//  I hate this so much, you don't even know... [Rock]
				Location jankWorkAround = mPlayer.getLocation();
				jankWorkAround.setY(-15);
				arrow.teleport(jankWorkAround);
			}
		}.runTaskLater(mPlugin, 1);

		return true;
	}

}
