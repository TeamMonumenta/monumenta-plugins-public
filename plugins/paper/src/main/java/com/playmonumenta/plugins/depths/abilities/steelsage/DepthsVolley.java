package com.playmonumenta.plugins.depths.abilities.steelsage;

import java.util.List;

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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsVolley extends DepthsAbility {

	public static final String ABILITY_NAME = "Volley";
	private static final String VOLLEY_METAKEY = "VolleyArrowMetakey";
	private static final String VOLLEY_HIT_METAKEY = "VolleyMobHitTickMetakey";
	private static final int COOLDOWN = 15 * 20;
	public static final int[] ARROWS = {7, 10, 12, 15, 18};
	private static final double[] DAMAGE_MULTIPLIER = {1.6, 1.7, 1.8, 1.9, 2.0};

	public DepthsVolley(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.VOLLEY;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = Material.ARROW;
		mTree = DepthsTree.METALLIC;
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
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.33f);

		float arrowSpeed = (float) (arrow.getVelocity().length());
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				int numArrows = ARROWS[mRarity - 1];

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

					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, arrowSpeed, 5, Arrow.class);
				} else {
					projectiles = EntityUtils.spawnArrowVolley(mPlugin, mPlayer, numArrows, arrowSpeed, 5, SpectralArrow.class);
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
				double damageMultiplier = DAMAGE_MULTIPLIER[mRarity - 1];
				event.setDamage(event.getDamage() * damageMultiplier);
			} else {
				// Only let one Volley arrow hit a given mob
				return false;
			}
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting an arrow while sneaking shoots a volley consisting of " + DepthsUtils.getRarityColor(rarity) + ARROWS[rarity - 1] + ChatColor.WHITE + " arrows instead. Only one arrow is consumed, and each arrow's damage is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE_MULTIPLIER[rarity - 1] + ChatColor.WHITE + ". Cooldown: " + COOLDOWN / 20 + "s.";
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
