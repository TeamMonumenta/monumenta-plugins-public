package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.SkirmisherCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Skirmisher extends Ability {

	private static final double GROUPED_FLAT_DAMAGE_1 = 1;
	private static final double GROUPED_FLAT_DAMAGE_2 = 2;
	private static final double GROUPED_PERCENT_DAMAGE_1 = 0.1;
	private static final double GROUPED_PERCENT_DAMAGE_2 = 0.15;
	private static final double SKIRMISHER_FRIENDLY_RADIUS = 2.5;
	private static final int MOB_COUNT_CUTOFF = 1;
	private static final int ENHANCEMENT_SPLASH_TARGETS = 1;
	private static final double ENHANCEMENT_SPLASH_RADIUS = 3;
	private static final double ENHANCEMENT_SPLASH_PERCENT_DAMAGE = 0.3;

	public static final String CHARM_DAMAGE = "Skirmisher Damage Multiplier";
	public static final String CHARM_RADIUS = "Skirmisher Damage Radius";
	public static final String CHARM_ENHANCEMENT_DAMAGE = "Skirmisher Enhancement Damage Multiplier";
	public static final String CHARM_TARGETS = "Skirmisher Enhancement Targets";

	public static final AbilityInfo<Skirmisher> INFO =
		new AbilityInfo<>(Skirmisher.class, "Skirmisher", Skirmisher::new)
			.linkedSpell(ClassAbility.SKIRMISHER)
			.scoreboardId("Skirmisher")
			.shorthandName("Sk")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal more damage if other mobs are nearby.")
			.displayItem(Material.BONE);

	private final double mGroupedPercentDamage;
	private final double mGroupedFlatDamage;
	private final double mFriendlyRadius;
	private final double mSplashRadius;
	private final double mSplashDamage;
	private final int mSplashTargets;
	private final SkirmisherCS mCosmetic;

	public Skirmisher(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mFriendlyRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, SKIRMISHER_FRIENDLY_RADIUS);
		mSplashRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_SPLASH_RADIUS);
		mSplashDamage = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCEMENT_DAMAGE) + ENHANCEMENT_SPLASH_PERCENT_DAMAGE;
		mSplashTargets = (int) CharmManager.getLevel(mPlayer, CHARM_TARGETS) + ENHANCEMENT_SPLASH_TARGETS;
		mGroupedPercentDamage = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? GROUPED_PERCENT_DAMAGE_1 : GROUPED_PERCENT_DAMAGE_2);
		mGroupedFlatDamage = isLevelOne() ? GROUPED_FLAT_DAMAGE_1 : GROUPED_FLAT_DAMAGE_2;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SkirmisherCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();

			// If Enhanced and triggers on a melee strike,
			if (isEnhanced() && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH)) {
				List<LivingEntity> nearbyEntities = EntityUtils.getNearbyMobs(loc, mSplashRadius, enemy);
				for (int i = 0; i < mSplashTargets; i++) {
					nearbyEntities.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
					LivingEntity selectedEnemy = EntityUtils.getNearestMob(loc, nearbyEntities);

					if (selectedEnemy != null) {
						DamageUtils.damage(mPlayer, selectedEnemy, DamageType.OTHER, event.getDamage() * mSplashDamage, mInfo.getLinkedSpell(), true);
						Location eLoc = selectedEnemy.getLocation();
						mCosmetic.aesthetics(mPlayer, eLoc, world, enemy);
					}

					nearbyEntities.remove(selectedEnemy);
				}
			}

			if (event.getAbility() != mInfo.getLinkedSpell() && DamageType.getAllMeleeTypes().contains(event.getType())) {
				if (EntityUtils.getNearbyMobs(loc, mFriendlyRadius, enemy).size() >= MOB_COUNT_CUTOFF
					|| (isLevelTwo() && enemy instanceof Mob mob && !mPlayer.equals(mob.getTarget()))) {
					event.addUnmodifiableDamage(mGroupedFlatDamage);
					event.updateDamageWithMultiplier(1 + mGroupedPercentDamage);
					mCosmetic.aesthetics(mPlayer, loc, world, enemy);
				}
			}
		}
		return false; // only changes event damage
	}

	private static Description<Skirmisher> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Deal increased melee damage to mobs that")
			.addLine("have another mob near them.")
			.addLine()
			.addStat("Damage Boost: +%d1 + %p1 (m)")
				.statValues(stat(a -> a.mGroupedFlatDamage, GROUPED_FLAT_DAMAGE_1), stat(a -> a.mGroupedPercentDamage, GROUPED_PERCENT_DAMAGE_1))
			.addStat("Mob Range: %r")
				.statValues(stat(a -> a.mFriendlyRadius, SKIRMISHER_FRIENDLY_RADIUS))
			.addDashedLine();
	}

	private static Description<Skirmisher> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Skirmisher*'s damage boost.").styles(UNDERLINED)
			.addLine()
			.addLine("*Skirmisher* can now apply to mobs that").styles(UNDERLINED)
			.addLine("are alone and aren't targeting you.")
			.addLine()
			.addStatComparison("Damage Boost: +%d1 + %p1 -> +%d2 + %p2 (m)")
				.statValues(stat(GROUPED_FLAT_DAMAGE_1), stat(GROUPED_PERCENT_DAMAGE_1), stat(a -> a.mGroupedFlatDamage, GROUPED_FLAT_DAMAGE_2), stat(a -> a.mGroupedPercentDamage, GROUPED_PERCENT_DAMAGE_2))
			.addDashedLine();

	}

	private static Description<Skirmisher> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("When you attack a mob, the nearest")
			.addLine("mob within %d blocks of the target")
				.statValues(stat(ENHANCEMENT_SPLASH_RADIUS))
			.addLine("takes %p of the attack's damage.")
				.statValues(stat(ENHANCEMENT_SPLASH_PERCENT_DAMAGE))
			.addDashedLine();
	}
}

