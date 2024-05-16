package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PermafrostMark extends Effect {
	public static final String effectID = "PermafrostMark";
	private final int mIceTicks;
	private final Player mPlayer;


	public PermafrostMark(int iceTicks, int duration, Player player) {
		super(duration, effectID);
		mIceTicks = iceTicks;
		mPlayer = player;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			Location loc = entity.getLocation().add(0, 0.25, 0);
			new PartialParticle(Particle.SNOWFLAKE, loc, 4, 0.25, 0.5, 0.25, 0).spawnAsEnemyBuff();
			new PartialParticle(Particle.SNOWBALL, loc, 4, 0.2, 0.2, 0.2, 0).spawnAsEnemyBuff();

			Block b = entity.getLocation().add(0, -1, 0).getBlock();
			if (!(b.getType() == Material.ICE || b.getType() == Material.PACKED_ICE)) {
				DepthsUtils.iceExposedBlock(b, mIceTicks, mPlayer);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("PermafrostMark duration:%d", this.getDuration());
	}
}
