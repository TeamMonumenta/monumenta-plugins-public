package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.SanctuaryCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Sanctuary extends Ability {
	public static final double SLOWNESS_PERCENT = 0.15;
	public static final double WEAKNESS_PERCENT = 0.2;
	public static final double VULNERABILITY_PERCENT = 0.1;

	public static final String CHARM_SLOWNESS_PERCENT = "Sanctuary Slowness Amplifier";
	public static final String CHARM_WEAKNESS_PERCENT = "Sanctuary Weakness Amplifier";
	public static final String CHARM_VULNERABILITY_PERCENT = "Sanctuary Vulnerability Amplifier";

	public static final AbilityInfo<Sanctuary> INFO =
		new AbilityInfo<>(Sanctuary.class, "Sanctuary", Sanctuary::new)
			.linkedSpell(ClassAbility.SANCTUARY)
			.scoreboardId("Sanctuary")
			.shorthandName("SCT")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Totems now provide additional debuffs when mobs are within range of them.")
			.displayItem(Material.AMETHYST_SHARD);

	public final double mSlownessPercent;
	public final double mWeaknessPercent;
	public final double mVulnerabilityPercent;
	private final SanctuaryCS mCosmetic;

	public Sanctuary(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlownessPercent = SLOWNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS_PERCENT);
		mWeaknessPercent = WEAKNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS_PERCENT);
		mVulnerabilityPercent = VULNERABILITY_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY_PERCENT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SanctuaryCS());
	}

	public void dealSanctuaryDebuffs(List<LivingEntity> targets, int ticks) {
		for (LivingEntity target : targets) {
			EntityUtils.applySlow(mPlugin, ticks, mSlownessPercent, target);
			EntityUtils.applyWeaken(mPlugin, ticks, mWeaknessPercent, target);
			if (isLevelTwo()) {
				EntityUtils.applyVulnerability(mPlugin, ticks, mVulnerabilityPercent, target);
				mCosmetic.sanctuaryLevelTwo(target);
			} else {
				mCosmetic.sanctuaryLevelOne(target);
			}
		}
	}

	private static Description<Sanctuary> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When mobs are within the range of your totems, they receive ")
			.addPercent(a -> a.mSlownessPercent, SLOWNESS_PERCENT)
			.add(" slowness and ")
			.addPercent(a -> a.mWeaknessPercent, WEAKNESS_PERCENT)
			.add(" weakness.");
	}

	private static Description<Sanctuary> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Additionally inflicts ")
			.addPercent(a -> a.mVulnerabilityPercent, VULNERABILITY_PERCENT)
			.add(" vulnerability to mobs within range.");
	}
}
