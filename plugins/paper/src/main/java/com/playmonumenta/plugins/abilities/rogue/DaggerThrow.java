package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 4;
	private static final int DAGGER_THROW_2_DAMAGE = 8;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_SILENCE_DURATION = 2 * 20;
	private static final int DAGGER_THROW_DAGGERS = 3;
	private static final double DAGGER_THROW_1_VULN = 0.2;
	private static final double DAGGER_THROW_2_VULN = 0.4;
	private static final double DAGGER_THROW_VULN_ENHANCEMENT = 0.2;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);

	public static final String CHARM_DAMAGE = "Dagger Throw Damage";
	public static final String CHARM_COOLDOWN = "Dagger Throw Cooldown";
	public static final String CHARM_RANGE = "Dagger Throw Range";
	public static final String CHARM_VULN = "Dagger Throw Vulnerability Amplifier";
	public static final String CHARM_VULN_DURATION = "Dagger Throw Vulnerability Duration";
	public static final String CHARM_SILENCE_DURATION = "Dagger Throw Silence Duration";
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
	private final double mVulnAmplifier;
	private final int mVulnDuration;
	private final int mSilenceDuration;
	private final DaggerThrowCS mCosmetic;

	public DaggerThrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDaggers = DAGGER_THROW_DAGGERS + (int) CharmManager.getLevel(mPlayer, CHARM_DAGGERS);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, DAGGER_THROW_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE);
		mVulnBase = (isLevelOne() ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN) + CharmManager.getLevelPercentDecimal(player, CHARM_VULN);
		mVulnAmplifier = mVulnBase + (isEnhanced() ? DAGGER_THROW_VULN_ENHANCEMENT : 0);
		mVulnDuration = CharmManager.getDuration(mPlayer, CHARM_VULN_DURATION, DAGGER_THROW_DURATION);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, DAGGER_THROW_SILENCE_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DaggerThrowCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		World world = mPlayer.getWorld();
		mCosmetic.daggerThrowEffect(world, startLoc, mPlayer);

		for (int a = (mDaggers / 2) * -1; a <= (mDaggers / 2); a++) {
			double totalSpread = DAGGER_THROW_SPREAD * DAGGER_THROW_DAGGERS;
			double individualSpread = totalSpread / mDaggers;
			double angle = a * individualSpread;
			Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY(), FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
			newDir.normalize();

			Location endLoc = LocationUtils.rayTraceToBlock(startLoc, newDir, mRange, loc -> mCosmetic.daggerHitBlockEffect(loc, mPlayer));
			for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
				if (!MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
					continue;
				}
				mCosmetic.daggerHitEffect(world, startLoc, mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
				EntityUtils.applyVulnerability(mPlugin, mVulnDuration, mVulnAmplifier, mob);
				if (isEnhanced()) {
					EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
				}
			}
			mCosmetic.daggerParticle(startLoc, endLoc, mPlayer);
		}

		return true;
	}

	private static Description<DaggerThrow> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to throw ")
			.add(a -> a.mDaggers, DAGGER_THROW_DAGGERS)
			.add(" daggers which deal ")
			.add(a -> a.mDamage, DAGGER_THROW_1_DAMAGE, false, Ability::isLevelOne)
			.add(" melee damage and apply ")
			.addPercent(a -> a.mVulnBase, DAGGER_THROW_1_VULN, false, Ability::isLevelOne)
			.add(" vulnerability for ")
			.addDuration(a -> a.mVulnDuration, DAGGER_THROW_DURATION)
			.add(" seconds. The daggers travel up to ")
			.add(a -> a.mRange, DAGGER_THROW_RANGE)
			.add(" blocks.")
			.addCooldown(DAGGER_THROW_COOLDOWN);
	}

	private static Description<DaggerThrow> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamage, DAGGER_THROW_2_DAMAGE, false, Ability::isLevelTwo)
			.add(" and the vulnerability is increased to ")
			.addPercent(a -> a.mVulnBase, DAGGER_THROW_2_VULN, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<DaggerThrow> getDescritpionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Additionally silences hit mobs for ")
			.addDuration(a -> a.mSilenceDuration, DAGGER_THROW_SILENCE_DURATION)
			.add(" seconds. Vulnerability is increased by ")
			.addPercent(DAGGER_THROW_VULN_ENHANCEMENT)
			.add(".");
	}
}
