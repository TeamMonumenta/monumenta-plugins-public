package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class ToughCookie extends Ability {
	private static final String SCOREBOARD = "ToughCookie";
	private static final int POINT_COST = 2;
	private static final double HEALTH_BOOST = 0.10;
	private static final double RESIST_BOOST = 0.05;
	private static final int RANGE = 12;

	public static final AbilityInfo<ToughCookie> INFO =
		new SnowPerkGui.SnowPerkInfo<>(ToughCookie.class, "Tough Cookie", ToughCookie::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.COOKIE)
			.description(getDescription());

	public ToughCookie(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mPlugin.mEffectManager.addEffect(mPlayer, "WellWornSweaterHealth", new PercentHealthBoost(6, HEALTH_BOOST, "WellWornSweaterHealth").displaysTime(false));
		mPlugin.mEffectManager.addEffect(mPlayer, "WellWornSweaterResistance-" + mPlayer.getName(), new PercentDamageReceived(6, -RESIST_BOOST).displaysTime(false));

		for (Player player : PlayerUtils.otherPlayersInRange(mPlayer, RANGE, true)) {
			mPlugin.mEffectManager.addEffect(player, "WellWornSweaterResistance-" + mPlayer.getName(), new PercentDamageReceived(6, -RESIST_BOOST).displaysTime(false));
		}
	}

	public static Description<ToughCookie> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("You gain +%p *Max Health*.").styles(WHITE, WHITE)
				.statValues(stat(HEALTH_BOOST), stat(RESIST_BOOST))
			.addLine()
			.addLine("You and other players within %d blocks")
				.statValues(stat(RANGE))
			.addLine("also gain +%p *Resistance*.").styles(WHITE)
				.statValues(stat(RESIST_BOOST))
			.addLine("(Can stack with other Tough Cookies)")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
