package com.playmonumenta.plugins.abilities.scout;

import java.util.EnumSet;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class Volley extends Ability {
	public static class VolleyMultiplierEnchantment extends BaseAbilityEnchantment {
		public VolleyMultiplierEnchantment() {
			super("Volley Damage Multiplier", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}

		private static float getMultiplier(Player player, float base) {
			int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, VolleyMultiplierEnchantment.class);
			return base * (float) ((level / 100.0) + 1);
		}
	}

	public static class VolleyDamageEnchantment extends BaseAbilityEnchantment {
		public VolleyDamageEnchantment() {
			super("Volley Damage", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class VolleyArrowsEnchantment extends BaseAbilityEnchantment {
		public VolleyArrowsEnchantment() {
			super("Volley Arrows", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}

		private static float getExtraArrows(Player player) {
			int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, VolleyArrowsEnchantment.class);
			return level;
		}
	}

	public static class VolleyCooldownEnchantment extends BaseAbilityEnchantment {
		public VolleyCooldownEnchantment() {
			super("Volley Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final String VOLLEY_METAKEY = "VolleyArrowMetakey";
	private static final String VOLLEY_HIT_METAKEY = "VolleyMobHitTickMetakey";
	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 11;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.3;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 1.5;

	public Volley(Plugin plugin, Player player) {
		super(plugin, player, "Volley");
		mInfo.mLinkedSpell = ClassAbility.VOLLEY;
		mInfo.mScoreboardId = "Volley";
		mInfo.mShorthandName = "Vly";
		mInfo.mDescriptions.add("When you shoot an arrow while sneaking, you shoot a volley consisting of 7 arrows instead. Only one arrow is consumed, and each arrow deals 30% bonus damage. Cooldown: 15s.");
		mInfo.mDescriptions.add("Increases the number of Arrows to 11 and enhances the bonus damage to 50%.");
		mInfo.mCooldown = VOLLEY_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.ARROW, 1);
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
				// Ability Enchantments
				int numArrows = getAbilityScore() == 1 ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;
				numArrows += VolleyArrowsEnchantment.getExtraArrows(mPlayer);

				List<Projectile> projectiles;
				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;
				int fireticks = 0;

				if (arrow instanceof Arrow) {
					Arrow regularArrow = (Arrow) arrow;
					fireticks = regularArrow.getFireTicks();
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
					if (fireticks > 0) {
						projArrow.setFireTicks(fireticks);
					}

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
		if (proj instanceof AbstractArrow && ((AbstractArrow) proj).hasMetadata(VOLLEY_METAKEY)) {
			if (MetadataUtils.checkOnceThisTick(mPlugin, le, VOLLEY_HIT_METAKEY)) {
				double damageMultiplier = getAbilityScore() == 1 ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;
				damageMultiplier = VolleyMultiplierEnchantment.getMultiplier(mPlayer, (float) damageMultiplier);
				event.setDamage(event.getDamage() * damageMultiplier + VolleyDamageEnchantment.getExtraDamage(mPlayer, VolleyDamageEnchantment.class));
			} else {
				// Only let one Volley arrow hit a given mob
				return false;
			}
		}

		return true;
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return VolleyCooldownEnchantment.class;
	}
}
