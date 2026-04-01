package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.TacticalManeuverCS;
import com.playmonumenta.plugins.effects.ZeroArgumentEffect;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class TacticalManeuver extends MultipleChargeAbility {
	private static final String LEVEL_2_MARK = "TacticalManeuverCDRMark";

	private static final int MAX_CHARGES = 3;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 11;
	private static final int RADIUS = 3;
	private static final int STAGGER_DURATION = 30;
	private static final double COOLDOWN_REDUCTION = 0.5;
	private static final double WEAKNESS = 0.25;
	private static final int WEAKNESS_DURATION = 30;
	private static final int REDUCTION_DURATION = Constants.TICKS_PER_SECOND * 3;

	public static final String CHARM_CHARGES = "Tactical Maneuver Charges";
	public static final String CHARM_COOLDOWN = "Tactical Maneuver Cooldown";
	public static final String CHARM_RADIUS = "Tactical Maneuver Radius";
	public static final String CHARM_DURATION = "Tactical Maneuver Stagger Duration";
	public static final String CHARM_VELOCITY = "Tactical Maneuver Velocity";
	public static final String CHARM_COOLDOWN_REDUCTION = "Tactical Maneuver Cooldown Refund";
	public static final String CHARM_MARK_DURATION = "Tactical Maneuver Mark Duration";
	public static final String CHARM_WEAKNESS = "Tactical Maneuver Weakness Amplifier";
	public static final String CHARM_WEAKNESS_DURATION = "Tactical Maneuver Weakness Duration";

	public static final AbilityInfo<TacticalManeuver> INFO =
		new AbilityInfo<>(TacticalManeuver.class, "Tactical Maneuver", TacticalManeuver::new)
			.linkedSpell(ClassAbility.TACTICAL_MANEUVER)
			.scoreboardId("TacticalManeuver")
			.shorthandName("TM")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Dash forward and stagger nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castForward", "dash forwards", TacticalManeuver::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.STRING);

	private final HashSet<LivingEntity> mStruckMobs = new HashSet<>();

	private final double mRadius;
	private final int mDuration;
	private final double mCooldownReduction;
	private final int mMarkDuration;
	private final double mWeaknessAmplifier;
	private final int mWeaknessDuration;
	private int mLastCastTicks = 0;
	private final TacticalManeuverCS mCosmetic;

	public TacticalManeuver(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getChargesOffCooldown();
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, STAGGER_DURATION);
		mCooldownReduction = COOLDOWN_REDUCTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_COOLDOWN_REDUCTION);
		mMarkDuration = CharmManager.getDuration(mPlayer, CHARM_MARK_DURATION, REDUCTION_DURATION);
		mWeaknessAmplifier = WEAKNESS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
		mWeaknessDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKNESS_DURATION, WEAKNESS_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TacticalManeuverCS());
	}

	public boolean cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES) || mPlugin.mEffectManager.hasEffect(mPlayer, "AllOutScoutMaxHealth")) {
			return false;
		}

		int ticks = Bukkit.getServer().getCurrentTick();

		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 10 || !consumeCharge()) {
			return false;
		}

		mStruckMobs.clear();
		mLastCastTicks = ticks;

		World world = mPlayer.getWorld();
		Vector dir = mPlayer.getLocation().getDirection();
		dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1));
		mCosmetic.maneuverStartEffect(world, mPlayer, dir);
		mPlayer.setVelocity(dir.setY(dir.getY() * 0.5 + 0.4));

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Needs the 5 tick delay since being close to the ground will cancel the runnable
				if ((mTicks > 5 && PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer))
					|| mPlayer.isDead()
					|| !mPlayer.isOnline()
					|| !mPlayer.getLocation().isChunkLoaded()
					|| mTicks > 30 * Constants.TICKS_PER_SECOND) {
					this.cancel();
					return;
				}

				Block block = mPlayer.getLocation().getBlock();
				if (BlockUtils.isWaterlogged(block)
					|| block.getType() == Material.LAVA
					|| BlockUtils.isClimbable(block)) {
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
					for (LivingEntity e : EntityUtils.getNearbyMobs(le.getLocation(), mRadius)) {
						if (isLevelTwo()) {
							mStruckMobs.add(e);
							EntityUtils.applyWeaken(mPlugin, mWeaknessDuration, mWeaknessAmplifier, mPlayer);
							maneuverMark(e);
						}
						EntityUtils.applyStagger(mPlugin, mDuration, e);
						HuntingCompanion.staggerApplied(mPlayer, e);
					}
					mCosmetic.maneuverHitEffect(world, mPlayer, le);

					this.cancel();
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
		return true;
	}

	private void maneuverMark(LivingEntity entity) {
		mPlugin.mEffectManager.addEffect(entity, LEVEL_2_MARK, new ZeroArgumentEffect(REDUCTION_DURATION, LEVEL_2_MARK) {
			@Override
			public String toString() {
				return String.format("%s duration:%d", LEVEL_2_MARK, getDuration());
			}

			@Override
			public void onDeath(EntityDeathEvent event) {
				if (mStruckMobs.contains(event.getEntity())) {
					mStruckMobs.clear();
					mPlugin.mTimers.updateCooldownPercent(mPlayer, ClassAbility.TACTICAL_MANEUVER, mCooldownReduction);
					mCosmetic.maneuverRefresh(mPlayer.getWorld(), mPlayer, mPlayer.getLocation());
				}
			}

			@Override
			public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
				if (entity instanceof LivingEntity le) {
					mCosmetic.maneuverMarkTick(le.getWorld(), mPlayer, le);
				}
			}
		});
	}

	private static Description<TacticalManeuver> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Dash forward and stagger nearby mobs.")
			.addLine()
			.addStat("Effect: Stagger for %t")
			.statValues(stat(a -> a.mDuration, STAGGER_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Charges: %d")
			.statValues(stat(a -> a.mMaxCharges, MAX_CHARGES))
			.addStat("Cooldown: %t (per charge)")
			.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<TacticalManeuver> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Killing mobs staggered by *Tactical Maneuver*").styles(UNDERLINED)
			.addLine("within %t refunds a portion of its cooldown.")
			.statValues(stat(a -> a.mMarkDuration, REDUCTION_DURATION))
			.addLine("(Once per cast)")
			.addLine()
			.addStat("Cooldown Reduction: %p")
			.statValues(stat(a -> a.mCooldownReduction, COOLDOWN_REDUCTION))
			.addLine()
			.addLine("*Tactical Maneuver* applies weakness.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: %p Weakness for %t")
			.statValues(stat(a -> a.mWeaknessAmplifier, WEAKNESS), stat(a -> a.mWeaknessDuration, WEAKNESS_DURATION))
			.addDashedLine();
	}
}
