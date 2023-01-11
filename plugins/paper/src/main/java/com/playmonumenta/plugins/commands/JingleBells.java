package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Constants.NotePitches;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class JingleBells extends GenericCommand {
	public static final String OBJECTIVE = "JingleBells";
	public static final String METADATA_TAG = "LastJinglingTime";
	public static final int RESET_TIME = 20 * 5;
	public static final List<Float> NOTES = Arrays.asList(
			NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.AS16, NotePitches.CS19, NotePitches.FS12, NotePitches.GS14, NotePitches.AS16,
			NotePitches.B17, NotePitches.B17, NotePitches.B17, NotePitches.B17,
			NotePitches.B17, NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.AS16, NotePitches.GS14, NotePitches.GS14, NotePitches.AS16, NotePitches.GS14, NotePitches.CS19,
			NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.AS16, NotePitches.CS19, NotePitches.FS12, NotePitches.GS14, NotePitches.AS16,
			NotePitches.B17, NotePitches.B17, NotePitches.B17, NotePitches.B17,
			NotePitches.B17, NotePitches.AS16, NotePitches.AS16, NotePitches.AS16,
			NotePitches.CS19, NotePitches.CS19, NotePitches.B17, NotePitches.GS14, NotePitches.FS12);

	public static void register() {
		registerPlayerCommand("jinglebells", "monumenta.command.jinglebells", JingleBells::run);
	}

	public static void run(CommandSender commandSender, Player player) throws WrapperCommandSyntaxException {
		int note = ScoreboardUtils.getScoreboardValue(player, OBJECTIVE).orElse(0);
		if (player.hasMetadata(METADATA_TAG)) {
			int timeSinceJingling = player.getTicksLived() - player.getMetadata(METADATA_TAG).get(0).asInt();
			if (timeSinceJingling < 0 || timeSinceJingling >= RESET_TIME) {
				note = 0;
			}
		}
		player.setMetadata(METADATA_TAG, new FixedMetadataValue(Plugin.getInstance(), player.getTicksLived()));

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1, NOTES.get(note));
		ScoreboardUtils.setScoreboardValue(player, OBJECTIVE, (note + 1) % NOTES.size());
	}
}
