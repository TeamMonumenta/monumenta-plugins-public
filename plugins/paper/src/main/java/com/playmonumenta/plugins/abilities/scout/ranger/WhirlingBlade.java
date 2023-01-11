package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class WhirlingBlade extends MultipleChargeAbility {

	private static final int BLADE_1_DAMAGE = 7;
	private static final int BLADE_2_DAMAGE = 14;
	private static final float BLADE_KNOCKBACK = 0.4f;
	private static final double BLADE_WEAKEN = 0.3;
	private static final int BLADE_WEAKEN_DURATION = 3 * 20;
	private static final int BLADE_2_SILENCE_DURATION = 1 * 20;
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
	public static final String CHARM_WEAKEN_DURATION = "Whirling Blade Weakness Duration";
	public static final String CHARM_SILENCE_DURATION = "Whirling Blade Silence Duration";

	public static final AbilityInfo<WhirlingBlade> INFO =
		new AbilityInfo<>(WhirlingBlade.class, "Whirling Blade", WhirlingBlade::new)
			.linkedSpell(ClassAbility.WHIRLING_BLADE)
			.scoreboardId("WhirlingBlade")
			.shorthandName("WB")
			.descriptions(
				"Use the swap key while holding a weapon to throw a whirling blade that circles around you, " +
					"knocking back and dealing " + BLADE_1_DAMAGE + " melee damage to enemies it hits and inflicts " + (int) (100 * BLADE_WEAKEN) + "% weakness for " + BLADE_WEAKEN_DURATION / 3 + "s. " +
					"Cooldown: " + BLADE_COOLDOWN / 20 + "s. Charges: " + BLADE_MAX_CHARGES + ".",
				"The damage is increased to " + BLADE_2_DAMAGE + " and also silences for " + BLADE_2_SILENCE_DURATION / 20 + "s.")
			.cooldown(BLADE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlingBlade::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP)
				                                                                          .keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(new ItemStack(Material.IRON_SWORD, 1));

	private final double mDamage;
	private final float mKnockback;

	private int mLastCastTicks = 0;

	public WhirlingBlade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? BLADE_1_DAMAGE : BLADE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, BLADE_KNOCKBACK);
		mMaxCharges = BLADE_MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
	}

	public void cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident. Also, strange bug, this seems to trigger twice when right clicking, but not the
		// case for stuff like Bodkin Blitz. This check also fixes that bug.
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		double throwRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, THROW_RADIUS);
		double bladeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BLADE_RADIUS);

		double weakenAmount = BLADE_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		int weakenDuration = CharmManager.getDuration(mPlayer, CHARM_WEAKEN_DURATION, BLADE_WEAKEN_DURATION);
		int silenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, BLADE_2_SILENCE_DURATION);
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
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
						MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
						EntityUtils.applyWeaken(mPlugin, weakenDuration, weakenAmount, mob);
						if (isLevelTwo()) {
							EntityUtils.applySilence(mPlugin, silenceDuration, mob);
						}
						mobIter.remove();
					}
				}

				new PartialParticle(Particle.SWEEP_ATTACK, bladeLoc1, 3, 0.35, 0, 0.35, 1).spawnAsPlayerActive(mPlayer);

				mIncrementDegrees += 30;
				if (mIncrementDegrees > 360) {
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.75f);
					this.cancel();
				}
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.5f);
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}
}
