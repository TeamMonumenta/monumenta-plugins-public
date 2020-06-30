package com.playmonumenta.plugins.abilities.scout;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class Volley extends Ability {

	private static final String VOLLEY_METAKEY = "VolleyArrowMetakey";
	private static final String VOLLEY_HIT_METAKEY = "VolleyMobHitTickMetakey";
	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 11;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.3;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 1.5;

	private final double mDamageMultiplier;
	private final int mArrowCount;

	public Volley(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Volley");
		mInfo.mLinkedSpell = Spells.VOLLEY;
		mInfo.mScoreboardId = "Volley";
		mInfo.mShorthandName = "Vly";
		mInfo.mDescriptions.add("When you shoot an arrow while sneaking, you shoot a volley consisting of 7 arrows instead (Cooldown: 15 s). Only one arrow is consumed, and each arrow deals 30% bonus damage.");
		mInfo.mDescriptions.add("Increases the number of Arrows to 11 and enhances the bonus damage to 50%.");
		mInfo.mCooldown = VOLLEY_COOLDOWN;
		mInfo.mIgnoreCooldown = true;

		mDamageMultiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;
		mArrowCount = getAbilityScore() == 1 ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (!mPlayer.isSneaking()
		    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			/* This ability is actually on cooldown - event proceeds as normal */
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.33f);

		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				List<Projectile> projectiles;

				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;

				if (arrow instanceof Arrow) {
					Arrow regularArrow = (Arrow) arrow;
					if (regularArrow.hasCustomEffects()) {
						tArrowData = regularArrow.getBasePotionData();
						if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
							// This arrow isn't weakness or slowness - don't store the potion data
							tArrowData = null;
						}
					}

					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, mArrowCount, 1.75, 5, Arrow.class);
				} else {
					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, mArrowCount, 1.75, 5, SpectralArrow.class);
				}



				for (Projectile proj : projectiles) {
					AbstractArrow projArrow = (AbstractArrow) proj;
					projArrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

					projArrow.setMetadata(VOLLEY_METAKEY, new FixedMetadataValue(mPlugin, null));
					projArrow.setCritical(arrow.isCritical());
					projArrow.setPierceLevel(arrow.getPierceLevel());

					// If the base arrow's potion data is still stored, apply it to the new arrows
					if (tArrowData != null) {
						((Arrow) projArrow).setBasePotionData(tArrowData);
					}

					mPlugin.mProjectileEffectTimers.addEntity(projArrow, Particle.SMOKE_NORMAL);

					ProjectileLaunchEvent event = new ProjectileLaunchEvent(projArrow);
					Bukkit.getPluginManager().callEvent(event);
				}

				// We can't just use arrow.remove() because that cancels the event and refunds the arrow
				Location jankWorkAround = mPlayer.getLocation();
				jankWorkAround.setY(-15);
				arrow.teleport(jankWorkAround);
			}
		}.runTaskLater(mPlugin, 0);

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow && ((Arrow) proj).hasMetadata(VOLLEY_METAKEY)) {
			if (MetadataUtils.checkOnceThisTick(mPlugin, le, VOLLEY_HIT_METAKEY)) {
				event.setDamage(event.getDamage() * mDamageMultiplier);
			} else {
				// Only let one Volley arrow hit a given mob
				return false;
			}
		}

		return true;
	}

}
