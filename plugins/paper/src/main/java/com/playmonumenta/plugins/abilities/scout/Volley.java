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
		super(plugin, world, random, player, "Volley");
		mInfo.linkedSpell = Spells.VOLLEY;
		mInfo.scoreboardId = "Volley";
		mInfo.mShorthandName = "Vly";
		mInfo.mDescriptions.add("When you shoot an arrow while sneaking, you shoot a volley consisting of 7 arrows instead (Cooldown: 15 s). Only one arrow is consumed, and each arrow deals 50% bonus damage.");
		mInfo.mDescriptions.add("Increases the number of Arrows to 10 and enhances the bonus damage to 100%.");
		mInfo.cooldown = VOLLEY_COOLDOWN;

		/*
		 * NOTE! Because Volley has two events - the actual shot event won't trigger by default
		 * when volley is on cooldown. Therefor it needs to bypass the automatic cooldown check
		 * and manage cooldown itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
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
					Arrow projArrow = (Arrow)proj;

					proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));

					// If the base arrow's potion data is still stored, apply it to the new arrows
					if (tArrowData != null && projArrow instanceof TippedArrow) {
						((TippedArrow)projArrow).setBasePotionData(tArrowData);
					}

					projArrow.setCritical(arrow.isCritical());
					projArrow.setFireTicks(arrow.getFireTicks());
					projArrow.setKnockbackStrength(arrow.getKnockbackStrength());
					projArrow.setDamage(arrow.getDamage());
					if (arrow.hasMetadata("ArrowQuickdraw")) {
						// Manually register these tags because volley doesn't for some reason
						projArrow.setMetadata("ArrowQuickdraw", new FixedMetadataValue(mPlugin, null));
						if (AbilityUtils.getArrowBaseDamage(projArrow) == 0) {
							AbilityUtils.setArrowBaseDamage(mPlugin, projArrow, AbilityUtils.getArrowBaseDamage(arrow));
						}
					}
					double multiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;
					AbilityUtils.multiplyArrowFinalDamageMultiplier(mPlugin, projArrow, multiplier);
					// Manually register these tags because volley doesn't for some reason
					if (AbilityUtils.getArrowBonusDamage(projArrow) == 0) {
						AbilityUtils.addArrowBonusDamage(mPlugin, projArrow, AbilityUtils.getArrowBonusDamage(arrow));
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

					//Fire: How stupid of me. I completely forgot we can call Bukkit Events on our OWN
					ProjectileLaunchEvent event = new ProjectileLaunchEvent(projArrow);
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
