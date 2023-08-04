package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Sanctuary extends Ability {
	public static final double SLOWNESS_PERCENT = 0.15;
	public static final double WEAKNESS_PERCENT = 0.2;
	public static final double VULNERABILITY_PERCENT = 0.125;

	public static final String CHARM_SLOWNESS_PERCENT = "Sanctuary Slowness";
	public static final String CHARM_WEAKNESS_PERCENT = "Sanctuary Weakness";
	public static final String CHARM_VULNERABILITY_PERCENT = "Sanctuary Vulnerability";


	public static final AbilityInfo<Sanctuary> INFO =
		new AbilityInfo<>(Sanctuary.class, "Sanctuary", Sanctuary::new)
			.linkedSpell(ClassAbility.SANCTUARY)
			.scoreboardId("Sanctuary")
			.shorthandName("SCT")
			.descriptions(
				String.format(
					"Mobs within the range of one of your totems receive %s%% slowness and %s%% weakness debuffs while within range.",
					StringUtils.multiplierToPercentage(SLOWNESS_PERCENT),
					StringUtils.multiplierToPercentage(WEAKNESS_PERCENT)
				),
				String.format(
					"Additionally inflicts %s%% vulnerability while within range.",
					StringUtils.multiplierToPercentage(VULNERABILITY_PERCENT)
				))
			.simpleDescription("Totems now provide additional debuffs when mobs are within range of them.")
			.displayItem(Material.AMETHYST_CLUSTER);

	public final double mSlownessPercent;
	public final double mWeaknessPercent;
	public final double mVulnerabilityPercent;


	public Sanctuary(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mSlownessPercent = SLOWNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS_PERCENT);
		mWeaknessPercent = WEAKNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS_PERCENT);
		mVulnerabilityPercent = VULNERABILITY_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULNERABILITY_PERCENT);
	}

	public void dealSanctuaryDebuffs(List<LivingEntity> targets, int ticks) {
		for (LivingEntity target : targets) {
			EntityUtils.applySlow(mPlugin, ticks, mSlownessPercent, target);
			EntityUtils.applyWeaken(mPlugin, ticks, mWeaknessPercent, target);
			if (isLevelTwo()) {
				EntityUtils.applyVulnerability(mPlugin, ticks, mVulnerabilityPercent, target);
			}
		}
	}
}
