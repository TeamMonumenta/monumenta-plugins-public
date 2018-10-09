package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.World;

public class Volley extends Ability {

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 10;
	private static final double VOLLEY_1_DAMAGE_INCREASE = 0.75;
	private static final double VOLLEY_2_DAMAGE_INCREASE = 1.5;

	public Volley(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 6;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.VOLLEY;
		mInfo.scoreboardId = "Volley";
		mInfo.cooldown = VOLLEY_COOLDOWN;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		List<Projectile> projectiles;
		//  Volley
		if (mPlayer.isSneaking()) {
			int volley = getAbilityScore();
			if (volley > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.VOLLEY)) {
					boolean isCritical = arrow.isCritical();
					int fireTicks = arrow.getFireTicks();
					int knockbackStrength = arrow.getKnockbackStrength();
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
						projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.5, 5, Arrow.class);
					} else {
						projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, 1.5, 5, TippedArrow.class);
					}

					for (Projectile proj : projectiles) {
						Arrow _arrow = (Arrow)proj;

						proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));

						// If the base arrow's potion data is still stored, apply it to the new arrows
						if (tArrowData != null && _arrow instanceof TippedArrow) {
							((TippedArrow)_arrow).setBasePotionData(tArrowData);
						}

						_arrow.setCritical(isCritical);
						_arrow.setFireTicks(fireTicks);
						_arrow.setKnockbackStrength(knockbackStrength);

						mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);
					}

					//  I hate this so much, you don't even know... [Rock]
					Location jankWorkAround = mPlayer.getLocation();
					jankWorkAround.setY(-15);
					arrow.teleport(jankWorkAround);

					mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), Spells.VOLLEY, VOLLEY_COOLDOWN);
				} else {
					projectiles = new ArrayList<Projectile>();
					projectiles.add(arrow);
				}
			} else {
				projectiles = new ArrayList<Projectile>();
				projectiles.add(arrow);
			}
		} else {
			projectiles = new ArrayList<Projectile>();
			projectiles.add(arrow);
		}

		if (AbilityUtils.getBowMasteryDamage(mPlayer) > 0) {
			if (arrow.isCritical()) {
				for (Projectile proj : projectiles) {
					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.CLOUD);
				}
			}
		}
		putOnCooldown();
		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata("Volley")) {
			double damageMultiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_INCREASE : VOLLEY_2_DAMAGE_INCREASE;
			double oldDamage = event.getDamage();

			double newDamage = oldDamage + (oldDamage * damageMultiplier);
			event.setDamage(newDamage);
		}
		return true;
	}
}
