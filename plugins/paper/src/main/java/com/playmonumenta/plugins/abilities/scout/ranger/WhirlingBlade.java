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
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.WhirlingBladeCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class WhirlingBlade extends MultipleChargeAbility {

	private static final int BLADE_1_DAMAGE = 9;
	private static final int BLADE_2_DAMAGE = 18;
	private static final float BLADE_KNOCKBACK = 0.4f;
	private static final double BLADE_WEAKEN = 0.3;
	private static final double BLADE_SLOWNESS = 0.2;
	private static final int DEBUFF_DURATION = 3 * 20;
	private static final int STUN_DURATION = 15;
	private static final double THROW_RADIUS = 3;
	private static final double BLADE_RADIUS = 1;
	private static final int BLADE_MAX_CHARGES = 2;
	private static final int BLADE_COOLDOWN = 20 * 6;
	private static final int ANGLE_INCREMENT = 30;

	public static final String CHARM_DAMAGE = "Whirling Blade Damage";
	public static final String CHARM_KNOCKBACK = "Whirling Blade Knockback";
	public static final String CHARM_RADIUS = "Whirling Blade Radius";
	public static final String CHARM_CHARGES = "Whirling Blade Charges";
	public static final String CHARM_COOLDOWN = "Whirling Blade Cooldown";
	public static final String CHARM_WEAKEN = "Whirling Blade Weakness Amplifier";
	public static final String CHARM_SLOWNESS = "Whirling Blade Slowness Amplifier";
	public static final String CHARM_DURATION = "Whirling Blade Debuff Duration";
	public static final String CHARM_STUN_DURATION = "Whirling Blade Stun Duration";
	public static final String CHARM_CYCLES = "Whirling Blade Cycles";

	public static final AbilityInfo<WhirlingBlade> INFO =
		new AbilityInfo<>(WhirlingBlade.class, "Whirling Blade", WhirlingBlade::new)
			.linkedSpell(ClassAbility.WHIRLING_BLADE)
			.scoreboardId("WhirlingBlade")
			.shorthandName("WB")
			.hotbarName("Whrl")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Damage, weaken, and knock nearby mobs back.")
			.cooldown(BLADE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlingBlade::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.IRON_SWORD);

	private final double mDamage;
	private final float mKnockback;
	private final double mThrowRadius;
	private final double mBladeRadius;
	private final double mWeaken;
	private final double mSlow;
	private final int mDuration;
	private final int mStunDuration;
	private final int mCycles;
	private @Nullable SwiftCuts mSwiftCuts;

	private int mLastCastTicks = 0;

	private final WhirlingBladeCS mCosmetic;

	public WhirlingBlade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? BLADE_1_DAMAGE : BLADE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BLADE_KNOCKBACK);
		mThrowRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, THROW_RADIUS);
		mBladeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BLADE_RADIUS);
		mWeaken = BLADE_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mSlow = BLADE_SLOWNESS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DEBUFF_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mMaxCharges = BLADE_MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCycles = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CYCLES);
		mCharges = getChargesOffCooldown();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WhirlingBladeCS());
		Bukkit.getScheduler().runTask(plugin, () -> {
			mSwiftCuts = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SwiftCuts.class);
		});
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident. Also, strange bug, this seems to trigger twice when right-clicking, but not the
		// case for stuff like Bodkin Blitz. This check also fixes that bug.
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		double throwRad = mThrowRadius;
		double bladeRad = mBladeRadius;
		double dmg = mDamage;

		if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
			throwRad *= mSwiftCuts.getWhirlingBladeBuff();
			bladeRad *= mSwiftCuts.getWhirlingBladeBuff();
			dmg *= mSwiftCuts.getWhirlingBladeBuff();
		}

		double throwRadius = throwRad;
		double bladeRadius = bladeRad;
		double damage = dmg;

		final World mWorld = mPlayer.getWorld();
		final Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
		final Vector mEyeDir = mLoc.getDirection();
		mCosmetic.onCast(mPlayer, mLoc, mWorld);

		castBlade(mLoc, throwRadius, mEyeDir, bladeRadius, damage, mWorld, mCycles);

		return true;
	}

	private void castBlade(Location location, double throwRadius, Vector eyeDir, double bladeRadius, double damage, World world, int cyclesLeft) {
		cancelOnDeath(new BukkitRunnable() {
			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			final List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(location, 4 * throwRadius, mPlayer);

			double mStartAngle = Math.atan(eyeDir.getZ() / eyeDir.getX());
			int mIncrementDegrees = 0;
			// Get the starting tick of the *first* cycle cast
			final int mStartingTick = Bukkit.getCurrentTick() -
				((mCycles == cyclesLeft) ? 0 : (mCycles - cyclesLeft) * (360 / ANGLE_INCREMENT) + 1);


			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (eyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI * mIncrementDegrees / 180), 0, Math.sin(mStartAngle - Math.PI * mIncrementDegrees / 180));
				Location bladeLoc1 = mLoc.clone().add(direction.clone().multiply(throwRadius));
				Location bladeLoc2 = mLoc.clone().add(direction.clone().multiply(throwRadius / 2));
				Location bladeLoc3 = mLoc.clone().add(direction.clone().multiply(throwRadius / 4));
				BoundingBox mBox1 = BoundingBox.of(bladeLoc1, bladeRadius, bladeRadius, bladeRadius);
				BoundingBox mBox2 = BoundingBox.of(bladeLoc2, bladeRadius / 2, bladeRadius / 2, bladeRadius / 2);
				BoundingBox mBox3 = BoundingBox.of(bladeLoc3, bladeRadius / 4, bladeRadius / 4, bladeRadius / 4);
				Iterator<LivingEntity> mobIter = mMobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (mBox1.overlaps(mob.getBoundingBox()) || mBox2.overlaps(mob.getBoundingBox()) || mBox3.overlaps(mob.getBoundingBox())) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.getLinkedSpell(), true);
						MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
						EntityUtils.applyWeaken(mPlugin, mDuration, mWeaken, mob);
						EntityUtils.applySlow(mPlugin, mDuration, mSlow, mob);
						mCosmetic.hitMob(mPlayer, mob.getLocation(), world);
						if (isLevelTwo()) {
							EntityUtils.applyStun(mPlugin, mStunDuration, mob);
						}
						mobIter.remove();
					}
				}

				Location loc = mPlayer.getLocation();
				mCosmetic.tick(mPlayer, bladeLoc1, world, loc, throwRadius, bladeRadius, mIncrementDegrees, mStartingTick, mCycles == cyclesLeft);

				if (mIncrementDegrees >= 360) {
					this.cancel();
					if (cyclesLeft <= 1) {
						mCosmetic.end(world, loc, mPlayer, mStartingTick);
					} else {
						castBlade(location, throwRadius, eyeDir, bladeRadius, damage, world, cyclesLeft - 1);
					}
				}
				mIncrementDegrees += ANGLE_INCREMENT;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void playerDeathEvent(PlayerDeathEvent event) {
		if (!event.isCancelled()) {
			mCosmetic.onDeath();
		}
	}

	private static Description<WhirlingBlade> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a blade that circles around you,")
			.addLine("damaging and afflicting nearby mobs")
			.addLine("with slowness and weakness.")
			.addLine()
			.addIf((a, p) -> a != null && a.mCycles != 1, desc -> desc
				.addStat("Cycles: %d")
				.statValues(stat(a -> a.mCycles, 1)))
			.addStat("Damage: %d1 (m)")
				.statValues(stat(a -> a.mDamage, BLADE_1_DAMAGE))
			.addStat("Effect: %p Slowness for %t")
				.statValues(stat(a -> a.mSlow, BLADE_SLOWNESS), stat(a -> a.mDuration, DEBUFF_DURATION))
			.addStat("Effect: %p Weakness for %t")
				.statValues(stat(a -> a.mWeaken, BLADE_WEAKEN), stat(a -> a.mDuration, DEBUFF_DURATION))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mThrowRadius + a.mBladeRadius, THROW_RADIUS + BLADE_RADIUS))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, BLADE_MAX_CHARGES))
			.addStat("Cooldown: %t (per charge)")
				.statValues(cooldown(BLADE_COOLDOWN))
			.addDashedLine();
	}

	private static Description<WhirlingBlade> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Whirling Blade*'s damage.").styles(UNDERLINED)
			.addLine()
			.addLine("*Whirling Blade* now stuns mobs it hits.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (m)")
				.statValues(stat(BLADE_1_DAMAGE), stat(a -> a.mDamage, BLADE_2_DAMAGE))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunDuration, STUN_DURATION))
			.addDashedLine();
	}
}
