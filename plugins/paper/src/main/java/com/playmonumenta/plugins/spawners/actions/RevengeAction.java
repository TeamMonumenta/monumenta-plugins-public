package com.playmonumenta.plugins.spawners.actions;

import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.SpawnerNBTWrapper;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RevengeAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "revenge";

	private static final int TELEGRAPH_DELAY = 20;

	private static final Particle.DustOptions REVENGE_DUST_OPTIONS_1 = new Particle.DustOptions(Color.RED, 1.5f);
	private static final Particle.DustOptions REVENGE_DUST_OPTIONS_2 = new Particle.DustOptions(Color.BLACK, 1.5f);

	public RevengeAction() {
		super(IDENTIFIER);
		addParameter("spawn_count", 1);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		if (!AdvancementUtils.checkAdvancement(player, "monumenta:handbook/spawners_/revenge_spawner")) {
			AdvancementUtils.grantAdvancement(player, "monumenta:handbook/spawners_/revenge_spawner");
		}
		int spawnCount = (int) getParameter(parameters, "spawn_count");
		Location spawnerLoc = BlockUtils.getCenteredBlockBaseLocation(spawner);

		if (losPool != null) {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				for (int i = 0; i < spawnCount; i++) {
					EntityNBT.fromEntityData(LibraryOfSoulsIntegration.getPool(losPool).keySet().stream().toList().get(0).getNBT()).spawn(spawnerLoc);
				}
			}, TELEGRAPH_DELAY);
		} else {
			SpawnerNBTWrapper wrapper = new SpawnerNBTWrapper(spawner);
			List<SpawnerNBTWrapper.SpawnerEntity> entities = wrapper.getEntities();
			EntityNBT entityNBT = FastUtils.getRandomElement(entities).entityNBT;
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				for (int i = 0; i < spawnCount; i++) {
					entityNBT.spawn(spawnerLoc);
				}
			}, TELEGRAPH_DELAY);
		}

		telegraphSpawn(spawnerLoc);
	}

	private void telegraphSpawn(Location loc) {
		loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1, 2f);
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PartialParticle(Particle.REDSTONE, blockLoc, 10).delta(0.25).data(REVENGE_DUST_OPTIONS_1)
			.spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, blockLoc, 10).delta(0.25).data(REVENGE_DUST_OPTIONS_2)
			.spawnAsEnemy();
	}
}
