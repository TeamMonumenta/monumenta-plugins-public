package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class SpectralSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "Spectral";
	private static final LoSPool SPECTRAL_LAND_POOL = new LoSPool.LibraryPool("~DelveSpectralLand");
	private static final LoSPool SPECTRAL_WATER_POOL = new LoSPool.LibraryPool("~DelveSpectralWater");

	public static class Parameters extends BossParameters {
		public double SPAWN_CHANCE = 0;
	}

	final Parameters mParam;

	public SpectralSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null && !EntityUtils.isElite(mBoss) && !EntityUtils.isBoss(mBoss) && !DelvesUtils.isDelveMob(mBoss) && EntityUtils.isHostileMob(mBoss)) {
			int numSpawns = FastUtils.roundRandomly(mParam.SPAWN_CHANCE);
			for (int i = 0; i < numSpawns; i++) {
				Location loc = mBoss.getLocation();
				boolean isWaterLoc = BlockUtils.containsWater(loc.getBlock());
				Entity entity;
				loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_AMBIENT, SoundCategory.HOSTILE, 1f, 0.2f);
				loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_DEATH, SoundCategory.HOSTILE, 0.2f, 0.1f);
				if (isWaterLoc) {
					entity = SPECTRAL_WATER_POOL.spawn(loc);
				} else {
					entity = SPECTRAL_LAND_POOL.spawn(loc);
				}

				// Safety net in case the Specter doesn't get summoned for some reason
				if (entity != null && mBoss.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
					/* Include the original mob's metadata for spawner counting on the Specter. This should prevent
					   drop farming from Specters when a death event is detected in the mob listener. */
					entity.setMetadata(Constants.SPAWNER_COUNT_METAKEY, mBoss.getMetadata(Constants.SPAWNER_COUNT_METAKEY).get(0));
				}

				loc.add(0, 1, 0);
				new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5).spawnAsEnemy();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0).spawnAsEnemy();
			}
		}
	}
}
