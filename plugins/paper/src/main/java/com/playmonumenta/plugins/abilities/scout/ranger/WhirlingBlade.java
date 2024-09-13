package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class WhirlingBlade extends MultipleChargeAbility {

	private static final int BLADE_1_DAMAGE = 9;
	private static final int BLADE_2_DAMAGE = 18;
	private static final float BLADE_KNOCKBACK = 0.4f;
	private static final double BLADE_WEAKEN = 0.3;
	private static final double BLADE_SLOWNESS = 0.2;
	private static final int BLADE_1_DURATION = 3 * 20;
	private static final int BLADE_2_DURATION = 15;
	private static final double THROW_RADIUS = 3;
	private static final double BLADE_RADIUS = 1;
	private static final int BLADE_MAX_CHARGES = 2;
	private static final int BLADE_COOLDOWN = 20 * 6;

	public static final String CHARM_DAMAGE = "Whirling Blade Damage";
	public static final String CHARM_KNOCKBACK = "Whirling Blade Knockback";
	public static final String CHARM_RADIUS = "Whirling Blade Radius";
	public static final String CHARM_CHARGES = "Whirling Blade Charges";
	public static final String CHARM_COOLDOWN = "Whirling Blade Cooldown";
	public static final String CHARM_WEAKEN = "Whirling Blade Weakness Amplifier";
	public static final String CHARM_SLOWNESS = "Whirling Blade Slowness Amplifier";
	public static final String CHARM_WEAKEN_DURATION = "Whirling Blade Weakness Duration";
	public static final String CHARM_SLOWNESS_DURATION = "Whirling Blade Slowness Duration";
	public static final String CHARM_STUN_DURATION = "Whirling Blade Stun Duration";

	public static final AbilityInfo<WhirlingBlade> INFO =
		new AbilityInfo<>(WhirlingBlade.class, "Whirling Blade", WhirlingBlade::new)
			.linkedSpell(ClassAbility.WHIRLING_BLADE)
			.scoreboardId("WhirlingBlade")
			.shorthandName("WB")
			.hotbarName("Whrl")
			.descriptions(
				"Swap hands while holding a melee weapon to throw a whirling blade that circles around you, " +
					"knocking back and dealing " + BLADE_1_DAMAGE + " melee damage to enemies it hits within a radius of " + (THROW_RADIUS + BLADE_RADIUS) + " and inflicts " + (int) (100 * BLADE_WEAKEN) + "% weakness and " + (int) (100 * BLADE_SLOWNESS) + "% slowness for " + BLADE_1_DURATION / 20 + "s. " +
					"Cooldown: " + BLADE_COOLDOWN / 20 + "s. Charges: " + BLADE_MAX_CHARGES + ".",
				"The damage is increased to " + BLADE_2_DAMAGE + " and also stun for " + BLADE_2_DURATION / 20.0 + "s.")
			.simpleDescription("Damage, weaken, and knock nearby mobs back.")
			.cooldown(BLADE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlingBlade::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP)
				                                                                          .keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.IRON_SWORD);

	private final double mDamage;
	private final float mKnockback;
	private @Nullable SwiftCuts mSwiftCuts;

	private int mLastCastTicks = 0;

	private final WhirlingBladeCS mCosmetic;

	public WhirlingBlade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? BLADE_1_DAMAGE : BLADE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BLADE_KNOCKBACK);
		mMaxCharges = BLADE_MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();

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

		double throwRad = CharmManager.getRadius(mPlayer, CHARM_RADIUS, THROW_RADIUS);
		double bladeRad = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BLADE_RADIUS);
		double dmg = mDamage;

		if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
			double charmBuff = (1 + CharmManager.getLevelPercentDecimal(mPlayer, SwiftCuts.CHARM_ENHANCE));
			throwRad *= (1 + SwiftCuts.WHIRLING_BLADE_BUFF * charmBuff);
			bladeRad *= (1 + SwiftCuts.WHIRLING_BLADE_BUFF * charmBuff);
			dmg *= (1 + SwiftCuts.WHIRLING_BLADE_BUFF * charmBuff);
		}

		double throwRadius = throwRad;
		double bladeRadius = bladeRad;
		double damage = dmg;

		double weakenAmount = BLADE_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		double slowAmount = BLADE_SLOWNESS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		int weakenDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKEN_DURATION, BLADE_1_DURATION);
		int slownessDuration = CharmManager.getDuration(mPlayer, CHARM_SLOWNESS_DURATION, BLADE_1_DURATION);
		int stunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, BLADE_2_DURATION);
		cancelOnDeath(new BukkitRunnable() {
			final World mWorld = mPlayer.getWorld();
			final Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
			final Vector mEyeDir = mLoc.getDirection();

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			final List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, 4 * throwRadius, mPlayer);

			double mStartAngle = Math.atan(mEyeDir.getZ() / mEyeDir.getX());
			int mIncrementDegrees = 0;

			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
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
						EntityUtils.applyWeaken(mPlugin, weakenDuration, weakenAmount, mob);
						EntityUtils.applySlow(mPlugin, slownessDuration, slowAmount, mob);
						if (isLevelTwo()) {
							EntityUtils.applyStun(mPlugin, stunDuration, mob);
						}
						mobIter.remove();
					}
				}

				Location loc = mPlayer.getLocation();
				mCosmetic.tick(mPlayer, bladeLoc1, mWorld, loc);

				mIncrementDegrees += 30;
				if (mIncrementDegrees > 360) {
					mCosmetic.end(mWorld, loc);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}
}
