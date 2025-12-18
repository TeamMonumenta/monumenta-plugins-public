package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DaggerThrowCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_RECAST_DELAY = 2;
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 4;
	private static final int DAGGER_THROW_2_DAMAGE = 8;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_SILENCE_DURATION = 2 * 20;
	private static final int DAGGER_THROW_RECAST_DURATION = 5 * 20;
	private static final int DAGGER_THROW_DAGGERS = 3;
	private static final double DAGGER_THROW_1_VULN = 0.2;
	private static final double DAGGER_THROW_2_VULN = 0.4;
	private static final double DAGGER_THROW_RECAST_MULTIPLIER = 0.8;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);

	public static final String CHARM_DAMAGE = "Dagger Throw Damage";
	public static final String CHARM_COOLDOWN = "Dagger Throw Cooldown";
	public static final String CHARM_RANGE = "Dagger Throw Range";
	public static final String CHARM_VULN = "Dagger Throw Vulnerability Amplifier";
	public static final String CHARM_VULN_DURATION = "Dagger Throw Vulnerability Duration";
	public static final String CHARM_SILENCE_DURATION = "Dagger Throw Silence Duration";
	public static final String CHARM_RECAST_DURATION = "Dagger Throw Recast Duration";
	public static final String CHARM_RECAST_MULTIPLIER = "Dagger Throw Recast Damage Multiplier";
	public static final String CHARM_DAGGERS = "Dagger Throw Daggers";

	public static final AbilityInfo<DaggerThrow> INFO =
		new AbilityInfo<>(DaggerThrow.class, "Dagger Throw", DaggerThrow::new)
			.linkedSpell(ClassAbility.DAGGER_THROW)
			.scoreboardId("DaggerThrow")
			.shorthandName("DT")
			.descriptions(getDescription1(), getDescription2(), getDescritpionEnhancement())
			.simpleDescription("Throw daggers that deal damage and apply vulnerability.")
			.cooldown(DAGGER_THROW_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DaggerThrow::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.WOODEN_SWORD);

	private final int mDaggers;
	private final double mRange;
	private final double mDamage;
	private final double mVulnBase;
	private final int mVulnDuration;
	private final int mSilenceDuration;
	private final int mRecastDuration;
	private final double mRecastMultiplier;
	private final DaggerThrowCS mCosmetic;

	private boolean mCanRecast = false;
	private int mCastTime = 0;
	private final ArrayList<Location> mDaggerEndPoints = new ArrayList<>();

	public DaggerThrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDaggers = DAGGER_THROW_DAGGERS + (int) CharmManager.getLevel(mPlayer, CHARM_DAGGERS);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, DAGGER_THROW_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE);
		mVulnBase = (isLevelOne() ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN) + CharmManager.getLevelPercentDecimal(player, CHARM_VULN);
		mVulnDuration = CharmManager.getDuration(mPlayer, CHARM_VULN_DURATION, DAGGER_THROW_DURATION);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, DAGGER_THROW_SILENCE_DURATION);
		mRecastDuration = CharmManager.getDuration(mPlayer, CHARM_RECAST_DURATION, DAGGER_THROW_RECAST_DURATION);
		mRecastMultiplier = DAGGER_THROW_RECAST_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_RECAST_MULTIPLIER);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DaggerThrowCS());
	}

	public boolean cast() {
		World world = mPlayer.getWorld();
		if (isOnCooldown()) {
			if (mCanRecast && Bukkit.getServer().getCurrentTick() > mCastTime + DAGGER_THROW_RECAST_DELAY) {
				recallDaggers(world);
				return true;
			} else {
				return false;
			}
		}
		putOnCooldown();
		mCastTime = Bukkit.getServer().getCurrentTick();

		// DEFAULT BEHAVIOR
		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		mCosmetic.daggerThrowEffect(world, startLoc, mPlayer);

		for (int a = (mDaggers / 2) * -1; a <= (mDaggers / 2); a++) {
			double totalSpread = DAGGER_THROW_SPREAD * DAGGER_THROW_DAGGERS;
			double individualSpread = totalSpread / mDaggers;
			double angle = a * individualSpread;
			Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY(), FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
			newDir.normalize();

			Location endLoc = LocationUtils.rayTraceToBlock(startLoc, newDir, mRange, loc -> mCosmetic.daggerHitBlockEffect(loc, mPlayer));

			traceDaggerPathAndDamage(startLoc, endLoc);

			if (isEnhanced()) {
				mDaggerEndPoints.add(endLoc);
			}
		}

		// CHECK IF ENHANCED TO PREP RECAST
		if (!isEnhanced()) {
			return true;
		}

		mCanRecast = true;
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!mCanRecast) { // Already recalled daggers, cancel runnable
					this.cancel();
					return;
				}
				if (mTicks >= mRecastDuration || !isOnCooldown()) {
					recallDaggers(world);
					this.cancel();
					return;
				}

				for (Location location : mDaggerEndPoints) {
					mCosmetic.daggerParticle(location, location, mPlayer);
				}

				mTicks += 4;
			}
		}.runTaskTimer(mPlugin, 1, 4);

		return true;
	}

	private void recallDaggers(World world) {
		mCanRecast = false;

		Location playerLoc = mPlayer.getEyeLocation(); // eye location for consistency with casting
		mCosmetic.daggerThrowEffect(world, playerLoc, mPlayer);

		for (Location location : mDaggerEndPoints) {
			traceDaggerPathAndDamage(location, playerLoc);
		}

		mDaggerEndPoints.clear();
	}

	private void traceDaggerPathAndDamage(Location startLoc, Location endLoc) {
		mCosmetic.daggerParticle(startLoc, endLoc, mPlayer);
		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
			if (!MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
				continue;
			}
			mCosmetic.daggerHitEffect(startLoc.getWorld(), endLoc, mob, mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage * mRecastMultiplier, mInfo.getLinkedSpell(), true);
			EntityUtils.applyVulnerability(mPlugin, mVulnDuration, mVulnBase, mob);
			if (isEnhanced()) {
				EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
			}
		}
	}

	private static Description<DaggerThrow> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a fan of %d daggers in front of you,")
				.statValues(stat(a -> a.mDaggers, DAGGER_THROW_DAGGERS))
			.addLine("dealing damage and inflicting vulnerability.")
			.addLine()
			.addStat("Damage: %d1 (m)")
				.statValues(stat(a -> a.mDamage, DAGGER_THROW_1_DAMAGE))
			.addStat("Effect: %p1 Vulnerability for %t")
				.statValues(stat(a -> a.mVulnBase, DAGGER_THROW_1_VULN), stat(a -> a.mVulnDuration, DAGGER_THROW_DURATION))
			.addStat("Range: %r (Cone-Shaped)")
				.statValues(stat(a -> a.mRange, DAGGER_THROW_RANGE))
			.addStat("Cooldown: %t")
				.statValues(cooldown(DAGGER_THROW_COOLDOWN))
			.addDashedLine();
	}

	private static Description<DaggerThrow> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Dagger Throw*'s damage").styles(UNDERLINED)
			.addLine("and vulnerability.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (m)")
				.statValues(stat(DAGGER_THROW_1_DAMAGE), stat(a -> a.mDamage, DAGGER_THROW_2_DAMAGE))
			.addStatComparison("Effect: %p1 -> %p2 Vulnerability")
				.statValues(stat(DAGGER_THROW_1_VULN), stat(a -> a.mVulnBase, DAGGER_THROW_2_VULN))
			.addDashedLine();
	}

	private static Description<DaggerThrow> getDescritpionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Dagger Throw*'s daggers stay stationary").styles(UNDERLINED)
			.addLine("at the end of their path for %t.")
				.statValues(stat(a -> a.mRecastDuration, DAGGER_THROW_RECAST_DURATION))
			.addLine()
			.addLine("Recast *Dagger Throw* to return the daggers").styles(UNDERLINED)
			.addLine("to you and deal reduced damage.")
			.addLine("(Automatically returns after %t)")
				.statValues(stat(a -> a.mRecastDuration, DAGGER_THROW_RECAST_DURATION))
			.addLine()
			.addStat("Return Damage: %p (m) of original")
			.statValues(stat(a -> a.mRecastMultiplier, DAGGER_THROW_RECAST_MULTIPLIER))
			.addLine()
			.addLine("*Dagger Throw* now silences mobs it hits.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: Silence for %t")
				.statValues(stat(a -> a.mSilenceDuration, DAGGER_THROW_SILENCE_DURATION))
			.addDashedLine();
	}
}
