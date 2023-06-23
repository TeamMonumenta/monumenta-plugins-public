package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ExplodeAction extends SpawnerBreakAction {
	// TODO: This is an example action! Remove once there are some actual examples.
	public static final String IDENTIFIER = "spawner_explode";

	public ExplodeAction() {
		super(IDENTIFIER);
		addParameter("message", "Boooooooom!");
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters) {
		String message = (String) parameters.get("message");
		player.sendMessage(message);
	}
}
