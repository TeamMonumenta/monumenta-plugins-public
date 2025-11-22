package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.CoupDeGraceCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/*
 * Coup De Grâce: If you melee attack a normal enemy and that attack
 * brings it under 10% health, they die instantly. If you melee attack
 * an elite enemy and that attack brings it under 20% health, they die
 * instantly. At level 2, the threshold increases to 15% health for
 * normal enemies and 30% health for elite enemies.
 */
public class CoupDeGrace extends Ability {

	private static final double COUP_1_NORMAL_THRESHOLD = 0.15;
	private static final double COUP_2_NORMAL_THRESHOLD = 0.2;
	private static final double COUP_1_ELITE_THRESHOLD = 0.2;
	private static final double COUP_2_ELITE_THRESHOLD = 0.3;

	public static final String CHARM_THRESHOLD = "Coup de Grace Threshold";
	public static final String CHARM_NORMAL = "Coup de Grace Normal Enemy Threshold";
	public static final String CHARM_ELITE = "Coup de Grace Elite Threshold";

	public static final AbilityInfo<CoupDeGrace> INFO =
		new AbilityInfo<>(CoupDeGrace.class, "Coup de Grace", CoupDeGrace::new)
			.linkedSpell(ClassAbility.COUP_DE_GRACE)
			.scoreboardId("CoupDeGrace")
			.shorthandName("CdG")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Instantly kill mobs brought below a health threshold.")
			.displayItem(Material.WITHER_SKELETON_SKULL)
			.priorityAmount(5000); // after all damage modifiers to get the proper final damage

	private final double mNormalThreshold;
	private final double mEliteThreshold;
	private final CoupDeGraceCS mCosmetic;

	public CoupDeGrace(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		double sharedThreshold = CharmManager.getLevelPercentDecimal(player, CHARM_THRESHOLD);
		mNormalThreshold = (isLevelOne() ? COUP_1_NORMAL_THRESHOLD : COUP_2_NORMAL_THRESHOLD) + CharmManager.getLevelPercentDecimal(player, CHARM_NORMAL) + sharedThreshold;
		mEliteThreshold = (isLevelOne() ? COUP_1_ELITE_THRESHOLD : COUP_2_ELITE_THRESHOLD) + CharmManager.getLevelPercentDecimal(player, CHARM_ELITE) + sharedThreshold;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CoupDeGraceCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)
			&& !EntityUtils.isBoss(enemy)
			&& !DamageUtils.isImmuneToDamage(enemy, DamageType.MELEE)
			&& (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH
			|| event.getAbility() == ClassAbility.QUAKE || event.getAbility() == ClassAbility.SKIRMISHER)) {
			// Cannot currently get the real final damage, as some effects like vulnerability will modify it later.
			// Thus, just check the mob's health a tick later.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (enemy.isValid() && enemy.getHealth() < EntityUtils.getMaxHealth(enemy) * (EntityUtils.isElite(enemy) ? mEliteThreshold : mNormalThreshold)) {
					execute(enemy);
				}
			});
		}
		return false;
	}

	private void execute(LivingEntity le) {
		DamageUtils.damage(mPlayer, le, DamageType.TRUE, 9001, ClassAbility.COUP_DE_GRACE, true, false);
		mCosmetic.execution(mPlayer, le);
		if (isLevelTwo()) {
			mCosmetic.executionLv2(mPlayer, le);
		}
	}

	private static Description<CoupDeGrace> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("If melee damage you deal brings a normal mob below ")
			.addPercent(a -> a.mNormalThreshold, COUP_1_NORMAL_THRESHOLD, false, Ability::isLevelOne)
			.add(" health or an Elite mob below ")
			.addPercent(a -> a.mEliteThreshold, COUP_1_ELITE_THRESHOLD, false, Ability::isLevelOne)
			.add(" health, they die instantly.");
	}

	private static Description<CoupDeGrace> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The health threshold is increased to ")
			.addPercent(a -> a.mNormalThreshold, COUP_2_NORMAL_THRESHOLD, false, Ability::isLevelTwo)
			.add(" for normal enemies and ")
			.addPercent(a -> a.mEliteThreshold, COUP_2_ELITE_THRESHOLD, false, Ability::isLevelTwo)
			.add(" for Elites.");
	}
}
