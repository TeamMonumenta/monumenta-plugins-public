package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.OBFUSCATED;

public class StringLightHook extends Ability {
	private static final String SCOREBOARD = "StringLightHook";
	private static final String ADVANCEMENT_REQ = "monumenta:challenges/r1/coalrupted/bigcoal";
	private static final int POINT_COST = 6;

	public static final AbilityInfo<StringLightHook> INFO =
		new SnowPerkGui.SnowPerkInfo<>(StringLightHook.class, "String Light Hook", StringLightHook::new)
			.advancementReq(ADVANCEMENT_REQ)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.CROSSBOW)
			.description(getDescription());

	public StringLightHook(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Item is given to the player in mechs, when the Coalrupted game is started
	}

	public static Description<StringLightHook> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.ACHIEVEMENT_ARROW_COLOR)
			.addLine("*Unlock:* *Reach 10000 lifetime Coal collected.*").styles(DescriptionUtils.REQUIREMENT_LABEL, DescriptionUtils.REQUIREMENT_TEXT)
			.addDashedLine()
			.addIfElse((a, p) -> !AdvancementUtils.checkAdvancement(p, ADVANCEMENT_REQ),
				desc -> desc.addLine("*Start the game with a String Light Hook,*").styles(OBFUSCATED)
					.addLine("*allowing you to grapple around the map.*").styles(OBFUSCATED),
				desc -> desc.addLine("Start the game with a *String Light Hook*,").styles(Style.style(TextColor.color(0x146614)))
					.addLine("allowing you to grapple around the map."))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
