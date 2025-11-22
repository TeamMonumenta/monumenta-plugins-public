package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.Parser;
import com.playmonumenta.plugins.bosses.parameters.Tokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public abstract class BossParameters implements Cloneable {

	public static <T extends BossParameters> T getParameters(Entity boss, String identityTag, T parameters) {
		String modTag = identityTag + "[";

		List<String> scoreboardTags = new ArrayList<>(boss.getScoreboardTags());
		for (String tag : scoreboardTags) {
			if (tag.startsWith(modTag)) {
				Location mobLoc = boss.getLocation();
				Component bossDescription = Objects.requireNonNullElseGet(boss.customName(), () -> Component.text("Unnamed " + boss.getType(), NamedTextColor.WHITE))
					.append(Component.text(" (at x: " + mobLoc.getBlockX() + " y: " + mobLoc.getBlockY() + " z: " + mobLoc.getBlockZ() + ")", NamedTextColor.GRAY));
				List<Player> receivingPlayers = Plugin.IS_PLAY_SERVER ? Collections.emptyList() : boss.getWorld().getPlayers();
				parseParametersWithWarnings(bossDescription, identityTag, parameters, tag, receivingPlayers);
			}
		}
		return parameters;
	}

	public static <T extends BossParameters> void parseParametersWithWarnings(Component bossDescription, String identityTag, T parameters, String tag, List<Player> receivingPlayers) {
		try {
			Parser.parseParameters(parameters, new Tokenizer(tag.substring(identityTag.length())).getTokens());
		} catch (Parser.ParseError e) {
			receivingPlayers.forEach(player -> {
				player.sendMessage(Component.text("Problems during parsing tag for ", NamedTextColor.GOLD).append(bossDescription));
				player.sendMessage(e.getMessage());
				player.sendMessage(Component.text(identityTag).append(e.getErrorHighlighting()));
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends BossParameters> T shallowClone(T parameters) {
		try {
			return (T) parameters.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public BossParameters clone() throws CloneNotSupportedException {
		return (BossParameters) super.clone();
	}
}
