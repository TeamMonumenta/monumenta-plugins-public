package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.TacticalManeuverCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class TacticalManeuver extends MultipleChargeAbility {

	private static final int TACTICAL_MANEUVER_1_MAX_CHARGES = 2;
	private static final int TACTICAL_MANEUVER_2_MAX_CHARGES = 3;
	private static final int TACTICAL_MANEUVER_1_COOLDOWN = 20 * 10;
	private static final int TACTICAL_MANEUVER_2_COOLDOWN = 20 * 8;
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

	public static final AbilityInfo<TacticalManeuver> INFO =
		new AbilityInfo<>(TacticalManeuver.class, "Tactical Maneuver", TacticalManeuver::new)
			.linkedSpell(ClassAbility.TACTICAL_MANEUVER)
			.scoreboardId("TacticalManeuver")
			.shorthandName("TM")
			.descriptions(
				String.format("Press the drop key while not sneaking to dash forward, dealing %d damage to the first enemy hit, and stunning it and all enemies in a %d block radius for %d second. " +
					              "Press the drop key while sneaking to leap backwards, dealing %d damage to enemies in a %d block radius and knocking them away. " +
					              "Cooldown: %ds. Charges: %d.",
					TACTICAL_DASH_DAMAGE, TACTICAL_MANEUVER_RADIUS, TACTICAL_DASH_STUN_DURATION / 20, TACTICAL_MANEUVER_RADIUS, TACTICAL_LEAP_DAMAGE, TACTICAL_MANEUVER_1_COOLDOWN / 20, TACTICAL_MANEUVER_1_MAX_CHARGES),
				String.format("Cooldown: %ds. Charges: %d.", TACTICAL_MANEUVER_2_COOLDOWN / 20, TACTICAL_MANEUVER_2_MAX_CHARGES))
			.cooldown(TACTICAL_MANEUVER_1_COOLDOWN, TACTICAL_MANEUVER_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castForward", "dash forwards", tm -> tm.cast(true), new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)))
			.addTrigger(new AbilityTriggerInfo<>("castBackwards", "leap backwards", tm -> tm.cast(false), new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)))
			.displayItem(new ItemStack(Material.STRING, 1));

	private int mLastCastTicks = 0;
	private final TacticalManeuverCS mCosmetic;

	public TacticalManeuver(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (isLevelOne() ? TACTICAL_MANEUVER_1_MAX_CHARGES : TACTICAL_MANEUVER_2_MAX_CHARGES) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TacticalManeuverCS(), TacticalManeuverCS.SKIN_LIST);
	}

	public void cast(boolean forwards) {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		int ticks = Bukkit.getServer().getCurrentTick();

		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 10 || !consumeCharge()) {
			return;
		}

		mLastCastTicks = ticks;

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TACTICAL_MANEUVER_RADIUS);

		World world = mPlayer.getWorld();
		if (forwards) {
			Vector dir = mPlayer.getLocation().getDirection();
			dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1));
			mCosmetic.maneuverStartEffect(world, mPlayer, dir);
			mPlayer.setVelocity(dir.setY(dir.getY() * 0.5 + 0.4));

			cancelOnDeath(new BukkitRunnable() {
				@Override
				public void run() {
					if (PlayerUtils.isOnGround(mPlayer) || mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
						this.cancel();
						return;
					}

					Material block = mPlayer.getLocation().getBlock().getType();
					if (block == Material.WATER || block == Material.LAVA || block == Material.LADDER) {
						this.cancel();
						return;
					}

					mCosmetic.maneuverTickEffect(mPlayer);

					Location loc = mPlayer.getLocation();
					Vector velocity = mPlayer.getVelocity();
					double length = velocity.length();
					if (length > 0.001) {
						loc.add(velocity.normalize());
					}

					LivingEntity le = EntityUtils.getNearestMob(mPlayer.getLocation(), 2);
					if (le != null) {
						DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_DASH_DAMAGE), mInfo.getLinkedSpell(), true);
						int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TACTICAL_DASH_STUN_DURATION);
						for (LivingEntity e : EntityUtils.getNearbyMobs(le.getLocation(), radius)) {
							EntityUtils.applyStun(mPlugin, duration, e);
						}
						mCosmetic.maneuverHitEffect(world, mPlayer);

						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 5, 1));
			// Needs the 5 tick delay since being close to the ground will cancel the runnable
		} else {
			for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius, mPlayer)) {
				DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_LEAP_DAMAGE), mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, le, TACTICAL_LEAP_KNOCKBACK_SPEED);
			}

			mCosmetic.maneuverBackEffect(world, mPlayer);
			Vector vel = mPlayer.getLocation().getDirection().setY(0).normalize().multiply(-1.65).setY(0.65);
			vel.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1));
			mPlayer.setVelocity(vel);
		}
	}
}
