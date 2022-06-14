package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class TacticalManeuver extends MultipleChargeAbility {

	private static final int TACTICAL_MANEUVER_1_MAX_CHARGES = 2;
	private static final int TACTICAL_MANEUVER_2_MAX_CHARGES = 3;
	private static final int TACTICAL_MANEUVER_1_COOLDOWN = 20 * 12;
	private static final int TACTICAL_MANEUVER_2_COOLDOWN = 20 * 10;
	private static final int TACTICAL_MANEUVER_RADIUS = 3;
	private static final int TACTICAL_DASH_DAMAGE = 14;
	private static final int TACTICAL_DASH_STUN_DURATION = 20 * 1;
	private static final int TACTICAL_LEAP_DAMAGE = 8;
	private static final float TACTICAL_LEAP_KNOCKBACK_SPEED = 0.5f;

	public static final String CHARM_CHARGES = "Tactical Maneuver Charge";
	public static final String CHARM_COOLDOWN = "Tactical Maneuver Cooldown";
	public static final String CHARM_RADIUS = "Tactical Maneuver Radius";
	public static final String CHARM_DURATION = "Tactical Maneuver Stun Duration";
	public static final String CHARM_DAMAGE = "Tactical Maneuver Damage";
	public static final String CHARM_VELOCITY = "Tactical Maneuver Velocity";

	private int mLastCastTicks = 0;

	public TacticalManeuver(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Tactical Maneuver");
		mInfo.mLinkedSpell = ClassAbility.TACTICAL_MANEUVER;
		mInfo.mScoreboardId = "TacticalManeuver";
		mInfo.mShorthandName = "TM";
		mInfo.mDescriptions.add("Sprint right click to dash forward, dealing the first enemy hit 14 damage, and stunning it and all enemies in a 3 block radius for 1 second. Shift right click to leap backwards, dealing enemies in a 3 block radius 8 damage and knocking them away. Only triggers with non-trident melee weapons. Cooldown: 12s. Charges: 2.");
		mInfo.mDescriptions.add("Cooldown: 10s. Charges: 3.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? TACTICAL_MANEUVER_1_COOLDOWN : TACTICAL_MANEUVER_2_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.STRING, 1);
		mMaxCharges = (isLevelOne() ? TACTICAL_MANEUVER_1_MAX_CHARGES : TACTICAL_MANEUVER_2_MAX_CHARGES) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
	}

	@Override
	public void cast(Action action) {
		if ((!mPlayer.isSprinting() && !mPlayer.isSneaking()) || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isBowOrTrident(inMainHand) || ItemUtils.isSomePotion(inMainHand) || inMainHand.getType().isBlock()
				|| inMainHand.getType().isEdible() || inMainHand.getType() == Material.COMPASS || inMainHand.getType() == Material.SHIELD || inMainHand.getType() == Material.SNOWBALL) {
			return;
		}

		int ticks = mPlayer.getTicksLived();

		// Prevent double casting on accident. Also, strange bug, this seems to trigger twice when right clicking, but not the
		// case for stuff like Bodkin Blitz. This check also fixes that bug.
		if (ticks - mLastCastTicks <= 10 || !consumeCharge()) {
			return;
		}

		mLastCastTicks = ticks;

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TACTICAL_MANEUVER_RADIUS);

		World world = mPlayer.getWorld();
		if (mPlayer.isSprinting()) {
			new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.7f);
			Vector dir = mPlayer.getLocation().getDirection();
			mPlayer.setVelocity(dir.setY(dir.getY() * 0.5 + 0.4));

			new BukkitRunnable() {
				@Override
				public void run() {
					if (mPlayer.isOnGround() || mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
						this.cancel();
						return;
					}

					Material block = mPlayer.getLocation().getBlock().getType();
					if (block == Material.WATER || block == Material.LAVA || block == Material.LADDER) {
						this.cancel();
						return;
					}

					new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 5, 0.25, 0.1, 0.25, 0.1).spawnAsPlayerActive(mPlayer);

					Location loc = mPlayer.getLocation();
					Vector velocity = mPlayer.getVelocity();
					double length = velocity.length();
					if (length > 0.001) {
						loc.add(velocity.normalize().multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, length)));
					}

					LivingEntity le = EntityUtils.getNearestMob(mPlayer.getLocation(), 2);
					if (le != null) {
						DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_DASH_DAMAGE), mInfo.mLinkedSpell, true);
						int duration = TACTICAL_DASH_STUN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
						for (LivingEntity e : EntityUtils.getNearbyMobs(le.getLocation(), radius)) {
							EntityUtils.applyStun(mPlugin, duration, e);
						}

						new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
						world.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);

						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 5, 1);
			// Needs the 5 tick delay since being close to the ground will cancel the runnable
		} else {
			for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius, mPlayer)) {
				DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_LEAP_DAMAGE), mInfo.mLinkedSpell, true);
				MovementUtils.knockAway(mPlayer, le, TACTICAL_LEAP_KNOCKBACK_SPEED);
			}

			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.2f);
			new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.1f, 0, 0.1f, 0.125f).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 10, 0.1f, 0, 0.1f, 0.15f).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 25, 0.1f, 0, 0.1f, 0.15f).spawnAsPlayerActive(mPlayer);
			mPlayer.setVelocity(mPlayer.getLocation().getDirection().setY(0).normalize().multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, -1.65)).setY(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 0.65)));
		}
	}
}
