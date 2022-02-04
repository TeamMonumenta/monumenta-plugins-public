package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Volley extends Ability {

	private static final String VOLLEY_METAKEY = "VolleyArrowMetakey";
	private static final String VOLLEY_HIT_METAKEY = "VolleyMobHitTickMetakey";
	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 11;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.3;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 1.5;

	public Volley(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Volley");
		mInfo.mLinkedSpell = ClassAbility.VOLLEY;
		mInfo.mScoreboardId = "Volley";
		mInfo.mShorthandName = "Vly";
		mInfo.mDescriptions.add("When you shoot an arrow or trident while sneaking, you shoot a volley consisting of 7 projectiles instead. Only one arrow is consumed, and each projectile deals 30% bonus damage. Cooldown: 15s.");
		mInfo.mDescriptions.add("Increases the number of projectiles to 11 and enhances the bonus damage to 50%.");
		mInfo.mCooldown = VOLLEY_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.ARROW, 1);
	}

	@Override
	public double getPriorityAmount() {
		return 900; // cancels damage events of volley arrows, so needs to run before other abilities
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer == null
			    || !mPlayer.isSneaking()
			    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			/* This ability is actually on cooldown - event proceeds as normal */
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.33f);

		float arrowSpeed = (float) arrow.getVelocity().length();
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				// Ability Enchantments
				int numArrows = getAbilityScore() == 1 ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;

				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;
				int fireticks = 0;

				if (arrow instanceof Arrow regularArrow) {
					fireticks = regularArrow.getFireTicks();
					if (regularArrow.hasCustomEffects()) {
						tArrowData = regularArrow.getBasePotionData();
						if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
							// This arrow isn't weakness or slowness - don't store the potion data
							tArrowData = null;
						}
					}
				}

				List<AbstractArrow> projectiles = EntityUtils.spawnArrowVolley(mPlayer, numArrows, arrowSpeed, 5, arrow.getClass());

				for (AbstractArrow proj : projectiles) {
					proj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
					if (fireticks > 0) {
						proj.setFireTicks(fireticks);
					}

					proj.setMetadata(VOLLEY_METAKEY, new FixedMetadataValue(mPlugin, null));
					proj.setCritical(arrow.isCritical());
					proj.setPierceLevel(arrow.getPierceLevel());

					// If the base arrow's potion data is still stored, apply it to the new arrows
					if (tArrowData != null) {
						((Arrow) proj).setBasePotionData(tArrowData);
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

					ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
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
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && proj instanceof AbstractArrow && proj.hasMetadata(VOLLEY_METAKEY)) {
			if (MetadataUtils.checkOnceThisTick(mPlugin, enemy, VOLLEY_HIT_METAKEY)) {
				double damageMultiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;
				event.setDamage(event.getDamage() * damageMultiplier);
			} else {
				// Only let one Volley arrow hit a given mob
				event.setCancelled(true);
			}
		}
		return false; // only changes event damage
	}

}
