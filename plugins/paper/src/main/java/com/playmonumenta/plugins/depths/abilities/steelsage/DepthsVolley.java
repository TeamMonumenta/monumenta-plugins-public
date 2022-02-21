package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class DepthsVolley extends DepthsAbility {

	public static final String ABILITY_NAME = "Volley";
	private static final String VOLLEY_METAKEY = "VolleyArrowMetakey";
	private static final String VOLLEY_HIT_METAKEY = "VolleyMobHitTickMetakey";
	private static final int COOLDOWN = 12 * 20;
	public static final int[] ARROWS = {7, 10, 12, 15, 18, 24};
	private static final double[] DAMAGE_MULTIPLIER = {1.6, 1.7, 1.8, 1.9, 2.0, 2.2};

	public DepthsVolley(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.VOLLEY;
		mInfo.mIgnoreCooldown = true;
		mDisplayMaterial = Material.ARROW;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public double getPriorityAmount() {
		return 900; // cancels damage events of volley arrows, so needs to run before other abilities
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (!mPlayer.isSneaking()
			    || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			/* This ability is actually on cooldown - event proceeds as normal */
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		mInfo.mCooldown = (int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer));
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

				List<AbstractArrow> projectiles = EntityUtils.spawnArrowVolley(mPlayer, ARROWS[mRarity - 1], arrowSpeed, 5, arrow.getClass());
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
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Arrow arrow && arrow.hasMetadata(VOLLEY_METAKEY)) {
			if (MetadataUtils.checkOnceThisTick(mPlugin, enemy, VOLLEY_HIT_METAKEY)) {
				double damageMultiplier = DAMAGE_MULTIPLIER[mRarity - 1];
				event.setDamage(event.getDamage() * damageMultiplier);
			} else {
				// Only let one Volley arrow hit a given mob
				event.setCancelled(true);
			}
		}
		return false; // only changes event damage
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting an arrow or trident while sneaking shoots a volley consisting of " + DepthsUtils.getRarityColor(rarity) + ARROWS[rarity - 1] + ChatColor.WHITE + " arrows instead. Only one arrow is consumed, and each arrow's damage is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE_MULTIPLIER[rarity - 1] + ChatColor.WHITE + ". Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}

}
