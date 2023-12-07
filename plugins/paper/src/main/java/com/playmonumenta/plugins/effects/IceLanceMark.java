package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class IceLanceMark extends Effect {
	public static final String effectID = "IceLanceMark";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(200, 225, 255), 1.0f);
	private int mIceTicks;
	private Player mPlayer;

	public IceLanceMark(int iceTicks, int duration, Player player) {
		super(duration, effectID);
		mIceTicks = iceTicks;
		mPlayer = player;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			Location loc = entity.getLocation().add(0, 1.25, 0);
			new PartialParticle(Particle.BLOCK_CRACK, loc, 6, 0.25, 0.5, 0.25, 0.02, Material.ICE.createBlockData()).spawnAsEnemyBuff();
			new PartialParticle(Particle.REDSTONE, loc, 8, 0.2, 0.2, 0.2, 0, COLOR).spawnAsEnemyBuff();
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		Block deathSpot = event.getEntity().getLocation().add(0, -1, 0).getBlock();
		DepthsUtils.iceExposedBlock(deathSpot, mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(-1, 0, -1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(-1, 0, 0), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(-1, 0, 1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(0, 0, -1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(0, 0, 1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(1, 0, -1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(1, 0, 0), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(1, 0, 1), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(-2, 0, 0), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(2, 0, 0), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(0, 0, -2), mIceTicks, mPlayer);
		DepthsUtils.iceExposedBlock(deathSpot.getRelative(0, 0, 2), mIceTicks, mPlayer);
	}

	@Override
	public String toString() {
		return String.format("IceLanceMark duration:%d", this.getDuration());
	}
}
