package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MessageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_message";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The text to be sent to players.")
		public String TEXT = "";
		@BossParam(help = "The radius in which players need to be to receive the message.")
		public int RANGE = 10;
		@BossParam(help = "Bold or not bold")
		public boolean BOLD = false;
		@BossParam(help = "Italics or not italics")
		public boolean ITALICS = false;
		@BossParam(help = "Obfuscated or not obfuscated")
		public boolean OBFUSCATED = false;
		@BossParam(help = "The color of the text. Accepts Hex and mc colors")
		public String COLOR = "GRAY";
	}

	public MessageBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		TextComponent mText = Component.text(mParams.TEXT, parseColor(mParams.COLOR)).decoration(TextDecoration.BOLD, mParams.BOLD).decoration(TextDecoration.ITALIC, mParams.ITALICS).decoration(TextDecoration.OBFUSCATED, mParams.OBFUSCATED);
		List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), mParams.RANGE, true);
		for (Player player : players) {
			player.sendMessage(mText);

		}
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.RANGE, null);
	}

	//njols textcolor parser.
	private static @Nullable TextColor parseColor(String string) {
		TextColor textColor = TextColor.fromHexString(string);
		if (textColor != null) {
			return textColor;
		}
		return NamedTextColor.NAMES.value(string.toLowerCase(Locale.getDefault()));
	}

}
