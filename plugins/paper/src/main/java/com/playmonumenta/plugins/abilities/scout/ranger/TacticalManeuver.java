package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.TacticalManeuverCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.BlockUtils;
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
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;


public class TacticalManeuver extends MultipleChargeAbility {

	private static final int TACTICAL_MANEUVER_1_MAX_CHARGES = 2;
	private static final int TACTICAL_MANEUVER_2_MAX_CHARGES = 3;
	private static final int TACTICAL_MANEUVER_1_COOLDOWN = 20 * 10;
	private static final int TACTICAL_MANEUVER_2_COOLDOWN = 20 * 8;
	private static final int TACTICAL_MANEUVER_RADIUS = 3;
	private static final int TACTICAL_DASH_DAMAGE = 14;
	private static final int TACTICAL_DASH_STUN_DURATION = 20;
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
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Dash forward and stun nearby mobs. While sneaking, dash backwards and knock away nearby mobs.")
			.cooldown(TACTICAL_MANEUVER_1_COOLDOWN, TACTICAL_MANEUVER_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castForward", "dash forwards", tm -> tm.cast(true), new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)))
			.addTrigger(new AbilityTriggerInfo<>("castBackwards", "leap backwards", tm -> tm.cast(false), new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)))
			.displayItem(Material.STRING);

	private final double mLeapDamage;
	private final double mDashDamage;
	private final double mRadius;
	private final int mDuration;
	private int mLastCastTicks = 0;
	private @Nullable SwiftCuts mSwiftCuts;
	private final TacticalManeuverCS mCosmetic;

	public TacticalManeuver(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (isLevelOne() ? TACTICAL_MANEUVER_1_MAX_CHARGES : TACTICAL_MANEUVER_2_MAX_CHARGES) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getChargesOffCooldown();
		mLeapDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_LEAP_DAMAGE);
		mDashDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, TACTICAL_DASH_DAMAGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TACTICAL_MANEUVER_RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TACTICAL_DASH_STUN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TacticalManeuverCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mSwiftCuts = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SwiftCuts.class);
		});
	}

	public boolean cast(boolean forwards) {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		int ticks = Bukkit.getServer().getCurrentTick();

		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 10 || !consumeCharge()) {
			return false;
		}

		mLastCastTicks = ticks;

		int cooldown = getModifiedCooldown();
		if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
			cooldown = (int) (cooldown * (1 - mSwiftCuts.getTacticalManeuverCDR()));
		}
		putOnCooldown(cooldown);

		World world = mPlayer.getWorld();
		if (forwards) {
			Vector dir = mPlayer.getLocation().getDirection();
			dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1));
			mCosmetic.maneuverStartEffect(world, mPlayer, dir);
			mPlayer.setVelocity(dir.setY(dir.getY() * 0.5 + 0.4));

			cancelOnDeath(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					// Needs the 5 tick delay since being close to the ground will cancel the runnable
					if ((mTicks > 5 && PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer)) || mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded() || mTicks > 30 * 20) {
						this.cancel();
						return;
					}

					Block block = mPlayer.getLocation().getBlock();
					if (BlockUtils.isWaterlogged(block) || block.getType() == Material.LAVA || BlockUtils.isClimbable(block)) {
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
						DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, mDashDamage, mInfo.getLinkedSpell(), true);
						for (LivingEntity e : EntityUtils.getNearbyMobs(le.getLocation(), mRadius)) {
							EntityUtils.applyStun(mPlugin, mDuration, e);
						}
						mCosmetic.maneuverHitEffect(world, mPlayer, le);

						this.cancel();
					}

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1));
		} else {
			for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius, mPlayer)) {
				DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, mLeapDamage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, le, TACTICAL_LEAP_KNOCKBACK_SPEED);
			}

			mCosmetic.maneuverBackEffect(world, mPlayer);
			Vector vel = mPlayer.getLocation().getDirection().setY(0).normalize().multiply(-1.65).setY(0.65);
			vel.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1));
			mPlayer.setVelocity(vel);
		}
		return true;
	}

	private static Description<TacticalManeuver> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger(0)
			.tab().addLine("to dash forwards").styles(WHITE)
			.addTrigger(1)
			.tab().addLine("to dash backwards").styles(WHITE)
			.addDashedLine()
			.addLine("Dash forwards and damage the first mob")
			.addLine("you hit, stunning it and nearby mobs.")
			.addLine()
			.addStat("Damage: %d (m)")
				.statValues(stat(a -> a.mDashDamage, TACTICAL_DASH_DAMAGE))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mDuration, TACTICAL_DASH_STUN_DURATION))
			.addStat("Stun Radius: %r")
				.statValues(stat(a -> a.mRadius, TACTICAL_MANEUVER_RADIUS))
			.addLine()
			.addLine("Dash backwards to deal damage to all")
			.addLine("nearby mobs and knock them away.")
			.addLine()
			.addStat("Damage: %d (m)")
				.statValues(stat(a -> a.mLeapDamage, TACTICAL_LEAP_DAMAGE))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, TACTICAL_MANEUVER_RADIUS))
			.addLine()
			.addStat("Charges: %d1")
				.statValues(stat(a -> a.mMaxCharges, TACTICAL_MANEUVER_1_MAX_CHARGES))
			.addStat("Cooldown: %t1 (per charge)")
				.statValues(cooldown(TACTICAL_MANEUVER_1_COOLDOWN))
			.addDashedLine();
	}

	private static Description<TacticalManeuver> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Tactical Maneuver*'s maximum").styles(UNDERLINED)
			.addLine("charges and reduce its cooldown.")
			.addLine()
			.addStatComparison("Charges: %d1 -> %d2")
				.statValues(stat(TACTICAL_MANEUVER_1_MAX_CHARGES), stat(a -> a.mMaxCharges, TACTICAL_MANEUVER_2_MAX_CHARGES))
			.addStatComparison("Cooldown: %t1 -> %t2 (per charge)")
				.statValues(cooldown(TACTICAL_MANEUVER_1_COOLDOWN), cooldown(TACTICAL_MANEUVER_2_COOLDOWN))
			.addDashedLine();
	}
}
