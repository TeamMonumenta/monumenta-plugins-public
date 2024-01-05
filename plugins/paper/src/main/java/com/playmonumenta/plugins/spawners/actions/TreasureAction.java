package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TreasureAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "treasure";

	private static final Particle.DustOptions TREASURE_DUST = new Particle.DustOptions(Color.fromRGB(231, 162, 32), 0.75f);

	public TreasureAction() {
		super(IDENTIFIER);
		addParameter("score", 1);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		int treasureCount = (int) getParameter(parameters, "score");
		if (ServerProperties.getDepthsEnabled()) {
			DepthsManager.getInstance().incrementTreasure(spawner.getLocation(), player, treasureCount);
		} else {
			MMLog.fine("Tried to give treasure score from invalid spawner! Location: " + spawner.getLocation().toString());
		}
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PPCircle(Particle.REDSTONE, blockLoc.clone().add(0, -0.25, 0), 1).data(TREASURE_DUST)
			.countPerMeter(4).distanceFalloff(20).ringMode(true).spawnFull();
		new PPCircle(Particle.REDSTONE, blockLoc.clone().add(0, 0.25, 0), 1).data(TREASURE_DUST)
			.countPerMeter(4).distanceFalloff(20).ringMode(true).spawnFull();
	}
}
