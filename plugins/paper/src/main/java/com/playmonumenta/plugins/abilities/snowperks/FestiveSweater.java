package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class FestiveSweater extends Ability {
	private static final String SCOREBOARD = "FestiveSweater";
	private static final int POINT_COST = 3;
	private static final double DAMAGE_BOOST = 0.08;
	private static final int MAX_FESTIVE = 3;

	public static final AbilityInfo<FestiveSweater> INFO =
		new SnowPerkGui.SnowPerkInfo<>(FestiveSweater.class, "Festive Sweater", FestiveSweater::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.LEATHER_CHESTPLATE)
			.description(getDescription());

	public FestiveSweater(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return false;
		}

		double festiveLevel = Math.min(mPlugin.mItemStatManager.getInfusionLevel(mPlayer, InfusionType.FESTIVE), MAX_FESTIVE);
		if (festiveLevel > 0) {
			event.updateDamageWithMultiplier(1 + DAMAGE_BOOST * festiveLevel);
		}
		return false;
	}

	public static Description<FestiveSweater> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Deal +%p more damage for each *Festive*").styles(DescriptionUtils.WHITE).statValues(stat(DAMAGE_BOOST))
			.addLine("item you're wearing or holding. (max %d)").statValues(stat(MAX_FESTIVE))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
