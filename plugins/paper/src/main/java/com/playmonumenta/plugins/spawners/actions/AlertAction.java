package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AlertAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "alert";

	private static final Particle.DustOptions ALERT_DUST_OPTIONS = new Particle.DustOptions(Color.YELLOW, 3f);
	private static final Particle.DustOptions PERIODIC_DUST_OPTIONS = new Particle.DustOptions(Color.ORANGE, 1.5f);

	public AlertAction() {
		super(IDENTIFIER);
		addParameter("radius", 10.0);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		if (!AdvancementUtils.checkAdvancement(player, "monumenta:handbook/spawners_/alert_spawner")) {
			AdvancementUtils.grantAdvancement(player, "monumenta:handbook/spawners_/alert_spawner");
		}
		double radius = (double) getParameter(parameters, "radius");
		Location spawnerLoc = BlockUtils.getCenterBlockLocation(spawner);
		new Hitbox.SphereHitbox(spawnerLoc, radius)
			.getHitMobs().forEach(mob -> {
				if (mob instanceof Mob mobAI) {
					mobAI.setTarget(player);
				}
			});

		aesthetics(spawnerLoc);
	}

	private void aesthetics(Location spawnerLoc) {
		spawnerLoc.getWorld().playSound(spawnerLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 1f, 2f);
		spawnerLoc.getWorld().playSound(spawnerLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 1f, 2f);

		new PartialParticle(Particle.REDSTONE, spawnerLoc, 5).data(ALERT_DUST_OPTIONS).spawnAsEnemy();
		new PPLine(Particle.REDSTONE, spawnerLoc.clone().add(0, 1, 0), spawnerLoc.clone().add(0, 2, 0))
			.countPerMeter(4).data(ALERT_DUST_OPTIONS).spawnAsEnemy();
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PartialParticle(Particle.REDSTONE, blockLoc, 15).delta(0.25).data(PERIODIC_DUST_OPTIONS)
			.spawnAsEnemy();
	}
}
