package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FlowerPlaceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flowerplace";
	//I don't expect anyone to really use this outside of me in hexfall

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection radius")
		public int DETECTION = 40;
		@BossParam(help = "how often it can use the ability")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "delay before ability becomes online")
		public int DELAY = 2 * 20;
		@BossParam(help = "radius")
		public int RADIUS = 4;
		@BossParam(help = "flower placement chance")
		public double FLOWER_CHANCE = 0.5;
		@BossParam(help = "works with any block but just use a flower vro")
		public Material FLOWER = Material.BLUE_ORCHID;
		@BossParam(help = "Sound played when the explosion hits a player")
		public SoundsList SOUND_FLOWER = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f))
			.add(new SoundsList.CSound(Sound.BLOCK_FLOWERING_AZALEA_PLACE, 0.6f, 1.0f))
			.build();
		@BossParam(help = "Particles to spawn on plant.")
		public ParticlesList PARTICLES_FLOWER = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.COMPOSTER, 2, 0.1, 0.3, 0.1, 0.1))
			.build();

	}

	public FlowerPlaceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		FlowerPlaceBoss.Parameters p = FlowerPlaceBoss.Parameters.getParameters(boss, identityTag, new FlowerPlaceBoss.Parameters());

		Spell spell = new Spell() {

			private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
				Material.SHORT_GRASS,
				Material.BLUE_ORCHID,
				Material.COMMAND_BLOCK,
				Material.CHAIN_COMMAND_BLOCK,
				Material.REPEATING_COMMAND_BLOCK,
				Material.BEDROCK,
				Material.BARRIER,
				Material.SPAWNER,
				Material.CHEST,
				Material.TRAPPED_CHEST,
				Material.END_PORTAL,
				Material.END_PORTAL_FRAME,
				Material.LIGHT
			);

			@Override
			public void run() {
				final World world = mBoss.getWorld();
				ArrayList<Block> blocks = new ArrayList<>();

				//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
				for (int x = -p.RADIUS; x <= p.RADIUS; x++) {
					int zDelta = (int) Math.round(Math.sqrt(p.RADIUS * p.RADIUS - x * x)); // choose only block in a circle
					for (int z = -zDelta; z <= zDelta; z++) {
						Block lowerBlock = world.getBlockAt(mBoss.getLocation().clone().add(x, -2, z));
						for (int y = -1; y <= 2; y++) {
							Block currentBlock = world.getBlockAt(mBoss.getLocation().clone().add(x, y, z));
							if (!lowerBlock.getType().isAir() && currentBlock.getType().isAir()) {
								blocks.add(lowerBlock);
								break;
							}
							lowerBlock = currentBlock;
						}
					}
				}

				for (Block b : blocks) {
					if (b == null) {
						continue;
					}

					Material material = b.getType();
					Block setToAir = null;
					if (ItemUtils.CARPETS.contains(material)) {
						setToAir = b;
						b = b.getRelative(BlockFace.DOWN);
					}
					if (!mIgnoredMats.contains(material) && !BlockUtils.containsWater(b) && !(b.getBlockData() instanceof Bed) && b.isCollidable() && FastUtils.RANDOM.nextDouble() < p.FLOWER_CHANCE && !ZoneUtils.hasZoneProperty(b.getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
						world.getBlockAt(b.getLocation().clone().add(0, 1, 0)).setType(p.FLOWER);
						p.PARTICLES_FLOWER.spawn(mBoss, b.getLocation());
						if (setToAir != null) {
							setToAir.setType(Material.AIR);
						}

					}
				}
				p.SOUND_FLOWER.play(mBoss.getLocation());
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}
		};
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}

}