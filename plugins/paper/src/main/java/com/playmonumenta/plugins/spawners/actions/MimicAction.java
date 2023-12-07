package com.playmonumenta.plugins.spawners.actions;

import com.goncalomb.bukkit.nbteditor.nbt.SpawnerNBTWrapper;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SpawnerMimicBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.BlockUtils;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MimicAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "mimic";
	private static final String SPAWNER_MIMIC_MOB_SOUL = "SpawnerMimic";
	private static final int TELEGRAPH_DELAY = 20;

	public MimicAction() {
		super(IDENTIFIER);
		addParameter("cooldown", 100);
		addParameter("spawn_count", 1);
		addParameter("spawn_radius", 3.0);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		SpawnerNBTWrapper wrapper = new SpawnerNBTWrapper(spawner);
		List<SpawnerNBTWrapper.SpawnerEntity> entities = wrapper.getEntities();
		Location spawnerLoc = BlockUtils.getCenterBlockLocation(spawner).subtract(0, 0.5, 0);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Entity entity = LibraryOfSoulsIntegration.summon(spawnerLoc, SPAWNER_MIMIC_MOB_SOUL);
			if (entity instanceof LivingEntity spawnerMimic) {
				SpawnerMimicBoss mimicBoss = Plugin.getInstance().mBossManager.getBoss(spawnerMimic, SpawnerMimicBoss.class);

				if (mimicBoss == null) {
					return;
				}

				// Inject parameters into the spell
				mimicBoss.mSpell.setCooldown((int) getParameter(parameters, "cooldown"));
				mimicBoss.mSpell.setSpawnCount((int) getParameter(parameters, "spawn_count"));
				mimicBoss.mSpell.setSpawnRadius((double) getParameter(parameters, "spawn_radius"));

				if (losPool != null) {
					mimicBoss.mSpell.setSpawnerLosPool(losPool);
				} else {
					mimicBoss.mSpell.setSpawnerEntities(entities.stream().map(e -> e.entityNBT).toList());
				}
			}
		}, TELEGRAPH_DELAY);
		telegraphSpawn(spawnerLoc);
	}

	private void telegraphSpawn(Location loc) {
		new PPSpiral(Particle.FLAME, loc, 3).directionalMode(true).delta(0, 0.1, 0)
			.extra(1).spawnAsEnemy();
		new PartialParticle(Particle.FLASH, loc, 1).spawnAsEnemy();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			loc.getWorld().playSound(loc, Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 1, 1.5f);
		}, TELEGRAPH_DELAY);
	}
}
