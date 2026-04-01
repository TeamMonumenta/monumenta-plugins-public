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

public class SierhavenSnowglobe extends Ability {
	private static final String SCOREBOARD = "SierhavenSnowglobe";
	private static final int POINT_COST = 1;
	private static final int TIME_BOOST = 20 * 20;

	public static final AbilityInfo<SierhavenSnowglobe> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SierhavenSnowglobe.class, "Sierhaven Snowglobe", SierhavenSnowglobe::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.CLOCK)
			.description(getDescription());

	public SierhavenSnowglobe(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Time boost is handled in mechs, when the Coalrupted game is started
	}

	public static Description<SierhavenSnowglobe> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Your team starts with +%t of time.").styles(SnowPerkGui.COAL_COLOR)
				.statValues(stat(TIME_BOOST))
			.addLine("(If alone, you get +%t instead)")
				.statValues(stat(TIME_BOOST * 6))
			.addLine()
			.addStat("Cost: %d Snow Point").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
