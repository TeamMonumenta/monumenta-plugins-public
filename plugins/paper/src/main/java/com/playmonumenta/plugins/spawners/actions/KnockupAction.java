package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class KnockupAction extends SpawnerBreakAction {
	// TODO: This is an example action! Remove once there are some actual examples.
	public static final String IDENTIFIER = "spawner_knockup";

	public KnockupAction() {
		super(IDENTIFIER);
		addParameter("velocity", 4.0);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters) {
		double velocity = (double) parameters.get("velocity");
		player.setVelocity(new Vector(0, velocity, 0));
	}
}
