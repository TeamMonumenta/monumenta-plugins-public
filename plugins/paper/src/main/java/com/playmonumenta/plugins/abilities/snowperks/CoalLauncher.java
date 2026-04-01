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

public class CoalLauncher extends Ability {
	private static final String SCOREBOARD = "CoalLauncher";
	private static final int POINT_COST = 3;

	public static final AbilityInfo<CreeperMistletoe> INFO =
		new SnowPerkGui.SnowPerkInfo<>(CreeperMistletoe.class, "Coal Launcher", CreeperMistletoe::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.COAL_BLOCK)
			.description(getDescription());

	public CoalLauncher(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Item is given to the player in mechs, when the Coalrupted game is started
	}

	public static Description<CreeperMistletoe> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Start the game with a *Coal Launcher* that").styles(SnowPerkGui.COAL_COLOR)
			.addLine("can launch *Coal* long distances and").styles(SnowPerkGui.COAL_COLOR)
			.addLine("allow you to deposit it from far away.")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
