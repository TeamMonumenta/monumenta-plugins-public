package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class HexfallRespawnBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_respawn";

	public static class Parameters extends BossParameters {
		public int DETECTION = 40;
		@BossParam(help = "the mob to respawn. should always be its own LoS name.")
		public String MOB = "";
		@BossParam(help = "where it should respawn: if set, respawns at a random block of this type.")
		public Material RESPAWN_LOCATION_MATERIAL = Material.STRUCTURE_VOID;
		@BossParam(help = "what the respawned block will be replaced with after respawn")
		public Material RESPAWN_BLOCK_REPLACE = Material.AIR;
		@BossParam(help = "see respawnlocationmaterial; searches for blocks within this radius")
		public double RESPAWN_LOCATION_RADIUS = 8;
		@BossParam(help = "amount of time between death and respawn, in ticks")
		public int RESPAWN_DELAY = 20;
		@BossParam(help = "sound played when this ability triggers")
		public SoundsList SOUND_TRIGGERED = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SKELETON_HURT, 1.0f, 0.75f))
			.build();
		@BossParam(help = "particles played when this ability triggers")
		public ParticlesList PARTICLE_TRIGGERED = ParticlesList.EMPTY;
		@BossParam(help = "particle line to the respawn location")
		public Particle PARTICLE_LINE = Particle.TOTEM;
		@BossParam(help = "particles played at the location the mob will respawn at")
		public ParticlesList PARTICLE_LOCATION = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.TOTEM, 1, 0.0, 0.0, 0.0, 0.4))
			.build();
		@BossParam(help = "sound played at the mob when it respawns")
		public SoundsList SOUND_RESPAWNED = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TOTEM_USE, 0.8f, 1.5f))
			.build();
		@BossParam(help = "particles played at the mob when it respawns")
		public ParticlesList PARTICLE_RESPAWNED = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.TOTEM, 30, 0.0, 0.0, 0.0, 0.8))
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 15, 0.2, 0.2, 0.2, 0.0, Material.BLUE_ORCHID.createBlockData()))
			.build();
	}

	private final Parameters mParams;

	public HexfallRespawnBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new HexfallRespawnBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Location loc = mBoss.getLocation();
		List<Block> blocks = BlockUtils.getBlocksInSphere(loc, mParams.RESPAWN_LOCATION_RADIUS);
		blocks.removeIf(block -> !block.getType().equals(mParams.RESPAWN_LOCATION_MATERIAL));
		Collections.shuffle(blocks);
		if (!blocks.isEmpty()) {
			Location respawnLoc = BlockUtils.getCenterBlockLocation(blocks.get(0));

			mParams.PARTICLE_TRIGGERED.spawn(mBoss, LocationUtils.getEntityCenter(mBoss));
			new PPLine(mParams.PARTICLE_LINE, loc, respawnLoc).countPerMeter(3).spawnAsEnemy();
			mParams.SOUND_TRIGGERED.play(loc);

			new BukkitRunnable() {
				int mTicks = 0;
				final Location mLoc = respawnLoc.clone();

				@Override
				public void run() {
					mParams.PARTICLE_LOCATION.spawn(mBoss, mLoc);

					mTicks++;
					if (mTicks >= mParams.RESPAWN_DELAY) {
						LibraryOfSoulsIntegration.summon(mLoc, mParams.MOB);

						mParams.PARTICLE_RESPAWNED.spawn(mBoss, mLoc);
						mParams.SOUND_RESPAWNED.play(mLoc);

						if (mLoc.getBlock().getType() == mParams.RESPAWN_LOCATION_MATERIAL) {
							mLoc.getBlock().setType(mParams.RESPAWN_BLOCK_REPLACE);
						}

						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}
