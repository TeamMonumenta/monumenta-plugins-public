package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class CoalInsurance extends Ability {
	private static final String SCOREBOARD = "CoalInsurance";
	private static final int POINT_COST = 2;
	private static final double COAL_SAVED = 0.5;

	public static final AbilityInfo<CoalInsurance> INFO =
		new SnowPerkGui.SnowPerkInfo<>(CoalInsurance.class, "Coal Insurance", CoalInsurance::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.MINECART)
			.description(getDescription());

	public CoalInsurance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Perk effect is handled in the coalrupted SQ death function
	}

	public static Description<CoalInsurance> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Dying with undeposited *Coal* automatically").styles(SnowPerkGui.COAL_COLOR)
			.addLine("deposits %p of it into the pile.").statValues(stat(COAL_SAVED))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
