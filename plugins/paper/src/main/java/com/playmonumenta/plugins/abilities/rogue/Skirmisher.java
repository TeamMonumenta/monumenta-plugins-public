package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
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

public class Skirmisher extends Ability {

	private static final double GROUPED_FLAT_DAMAGE = 1;
	private static final double GROUPED_FLAT_DAMAGE_2 = 2;
	private static final double GROUPED_PERCENT_DAMAGE_1 = 0.1;
	private static final double GROUPED_PERCENT_DAMAGE_2 = 0.15;
	private static final double SKIRMISHER_FRIENDLY_RADIUS = 2.5;
	private static final int MOB_COUNT_CUTOFF = 1;
	private static final double ENHANCEMENT_SPLASH_RADIUS = 3;
	private static final double ENHANCEMENT_SPLASH_PERCENT_DAMAGE = 0.3;

	public static final String CHARM_DAMAGE = "Skirmisher Damage Multiplier";
	public static final String CHARM_RADIUS = "Skirmisher Damage Radius";

	public static final AbilityInfo<Skirmisher> INFO =
		new AbilityInfo<>(Skirmisher.class, "Skirmisher", Skirmisher::new)
			.linkedSpell(ClassAbility.SKIRMISHER)
			.scoreboardId("Skirmisher")
			.shorthandName("Sk")
			.descriptions(
				String.format("When dealing melee damage to a mob that has at least one other mob within %s blocks, deal + %s + %s%% final damage.",
					SKIRMISHER_FRIENDLY_RADIUS,
					(int) GROUPED_FLAT_DAMAGE,
					(int) (GROUPED_PERCENT_DAMAGE_1 * 100)),
				String.format("The damage bonus now also applies to mobs not targeting you, and the damage bonus is increased to %s + %s%% final damage done.",
					(int) GROUPED_FLAT_DAMAGE_2,
					(int) (GROUPED_PERCENT_DAMAGE_2 * 100)),
				String.format("When you hit an enemy with a sword, the nearest enemy within %s blocks takes %s%% of the original attack's damage.",
					(int) ENHANCEMENT_SPLASH_RADIUS,
					(int) (ENHANCEMENT_SPLASH_PERCENT_DAMAGE * 100)))
			.simpleDescription("Deal more damage if other mobs are nearby.")
			.displayItem(Material.BONE);

	private final double mIsolatedPercentDamage;
	private final double mIsolatedFlatDamage;
	private final double mFriendlyRadius;
	private final double mSplashRadius;
	private final SkirmisherCS mCosmetic;

	public Skirmisher(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mFriendlyRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, SKIRMISHER_FRIENDLY_RADIUS);
		mSplashRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_SPLASH_RADIUS);
		mIsolatedPercentDamage = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? GROUPED_PERCENT_DAMAGE_1 : GROUPED_PERCENT_DAMAGE_2);
		mIsolatedFlatDamage = isLevelOne() ? GROUPED_FLAT_DAMAGE : GROUPED_FLAT_DAMAGE_2;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SkirmisherCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();

			// If Enhanced and triggers on a melee strike,
			if (isEnhanced() && event.getType() == DamageType.MELEE) {
				List<LivingEntity> nearbyEntities = EntityUtils.getNearbyMobs(loc, mSplashRadius, enemy);
				nearbyEntities.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				LivingEntity selectedEnemy = EntityUtils.getNearestMob(loc, nearbyEntities);

				if (selectedEnemy != null) {
					DamageUtils.damage(mPlayer, selectedEnemy, DamageType.OTHER, event.getDamage() * ENHANCEMENT_SPLASH_PERCENT_DAMAGE, mInfo.getLinkedSpell(), true);
					Location eLoc = selectedEnemy.getLocation();
					mCosmetic.aesthetics(mPlayer, eLoc, world, enemy);
				}
			}

			if (event.getAbility() != mInfo.getLinkedSpell() && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)) {
				if (EntityUtils.getNearbyMobs(loc, mFriendlyRadius, enemy).size() >= MOB_COUNT_CUTOFF
					    || (isLevelTwo() && enemy instanceof Mob mob && !mPlayer.equals(mob.getTarget()))) {
					event.addUnmodifiableDamage(mIsolatedFlatDamage);
					event.updateDamageWithMultiplier(1 + mIsolatedPercentDamage);
					mCosmetic.aesthetics(mPlayer, loc, world, enemy);
				}
			}
		}
		return false; // only changes event damage
	}
}

