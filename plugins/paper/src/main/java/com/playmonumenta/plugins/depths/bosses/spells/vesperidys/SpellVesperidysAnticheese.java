package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.vesperidys.VesperidysBlockPlacerBoss;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.VoidCorruption;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SpellVesperidysAnticheese extends Spell {

	public static final int ANTI_BLOCK_ZONE_DISTANCE = 19;

	private int mVoidTicks = 0;
	private int mBlockTicks = 0;
	private int mChainTicks = 0;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Vesperidys mVesperidys;

	private final List<Player> mWarnedPlayers = new ArrayList<>();
	private final List<Block> mMarked = new ArrayList<>();
	public final List<Block> mIgnored = new ArrayList<>();

	public SpellVesperidysAnticheese(Plugin plugin, LivingEntity boss, Location spawnLoc, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mVesperidys = vesperidys;
	}

	@Override
	public void run() {
		if (mVesperidys.mPhase == 0 || mVesperidys.mDefeated || mVesperidys.mDead) {
			return;
		}

		mVoidTicks += 5;
		mChainTicks += 5;
		mBlockTicks += 5;

		List<Player> players = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true);

		// Shackled (Y > 20, yoinked to the boss)
		if (mChainTicks >= 40) {
			mChainTicks = 0;
			for (Player player : players) {
				if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
					&& (player.getLocation().getY() - mSpawnLoc.getY()) > 20) {
					Location l = player.getEyeLocation();
					new PartialParticle(Particle.SQUID_INK, l, 10, 0.1, 0.1, 0.1, 0.25).spawnAsEntityActive(mBoss);

					mVesperidys.dealPercentageAndCorruptionDamage(player, 0.1, "The Void");
					player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 0.5f);

					// Draw tether line from Player to Boss.
					Location playerLoc = player.getLocation();
					Vector dir = LocationUtils.getVectorTo(mBoss.getLocation(), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 40; i++) {
						pLoc.add(dir);
						new PartialParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0)
							.extra(0)
							.spawnForPlayer(ParticleCategory.BOSS, player);

						if (pLoc.distance(mBoss.getLocation()) < 0.5) {
							break;
						}
					}

					MovementUtils.pullTowardsNormalized(mBoss.getLocation(), player, 0.5f);
				}
			}
		}

		if (mVoidTicks >= 5) {
			mVoidTicks = 0;

			// Grave Checkers
			for (ArmorStand armorStand : mSpawnLoc.getNearbyEntitiesByType(ArmorStand.class, Vesperidys.detectionRange)) {
				if (DepthsUtils.isDepthsGrave(armorStand)
					&& (Math.abs(armorStand.getLocation().getX() - mSpawnLoc.getX()) > 18
					|| Math.abs(armorStand.getLocation().getY() - mSpawnLoc.getY()) > 5
					|| Math.abs(armorStand.getLocation().getZ() - mSpawnLoc.getZ()) > 18)) {
					List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getShuffledPlatforms(null);
					Vesperidys.Platform selectedPlatform = null;

					// Prioritizes platforms which doesn't have adds on it (including boss itself).
					for (Vesperidys.Platform platform : platforms) {
						selectedPlatform = platform;
						if (platform.getMobsOnPlatform().size() <= 0 && platform.getPlayersOnPlatform().size() <= 0) {
							break;
						}
					}

					Location newLoc;
					if (selectedPlatform == null) {
						// Failsafe. Should not happen, ever.
						newLoc = mVesperidys.mSpawnLoc;
					} else {
						newLoc = selectedPlatform.getCenter().add(0, 2, 0);
					}

					armorStand.teleport(newLoc);
				}
			}

			// Kill all XP Bottles. (It was messing up teleportation of some delve mobs)
			for (Entity e : mSpawnLoc.getWorld().getNearbyEntitiesByType(ThrownExpBottle.class, mSpawnLoc, Vesperidys.detectionRange)) {
				e.remove();
			}

			// Delve Mob Build Platform Checkers
			for (LivingEntity le : EntityUtils.getNearbyMobs(mSpawnLoc, Vesperidys.detectionRange)) {
				if (ScoreboardUtils.checkTag(le, "delve_mob")) {
					mPlugin.mBossManager.manuallyRegisterBoss(le, new VesperidysBlockPlacerBoss(mPlugin, le, mVesperidys, 0));
				}
			}

			for (Player player : players) {
				if (!mPlugin.mEffectManager.hasEffect(player, VoidCorruption.class) && !player.isDead()) {
					mPlugin.mEffectManager.addEffect(player, "VesperidysVoidCorruption", new VoidCorruption(Integer.MAX_VALUE, mPlugin, mVesperidys, mBoss, Math.min(50, mVesperidys.corruptionEqulibrium())));
				}

				if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
					&& (Math.abs(player.getLocation().getX() - mSpawnLoc.getX()) > 18
					|| Math.abs(player.getLocation().getY() - mSpawnLoc.getY()) > 5
					|| Math.abs(player.getLocation().getZ() - mSpawnLoc.getZ()) > 18)
					&& (player.getLocation().add(0, -1, 0).getBlock().getType() != Material.AIR || (player.getLocation().getY() - mSpawnLoc.getY() < -5))) {
					Location l = player.getEyeLocation();
					new PartialParticle(Particle.SQUID_INK, l, 10, 0.1, 0.1, 0.1, 0.25).spawnAsEntityActive(mBoss);
					mVesperidys.dealPercentageAndCorruptionDamage(player, 0.3, "The Void");

					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 2 * 20, 0));
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5, 0));
					mPlugin.mEffectManager.addEffect(player, "VesperidysVoidWeakness", new PercentDamageDealt(6 * 20, -0.1));
					if (!mWarnedPlayers.contains(player)) {
						mWarnedPlayers.add(player);
						player.sendMessage(Component.text("As you fall away from the arena, you were brought back from the endless abyss.", NamedTextColor.DARK_GRAY));
					}

					List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getShuffledPlatforms(null);
					Vesperidys.Platform selectedPlatform = null;

					// Prioritizes platforms which doesn't have adds on it (including boss itself).
					for (Vesperidys.Platform platform : platforms) {
						selectedPlatform = platform;
						if (platform.getMobsOnPlatform().size() <= 0 && platform.getPlayersOnPlatform().size() <= 0) {
							break;
						}
					}

					Location newLoc;
					if (selectedPlatform == null) {
						// Failsafe. Should not happen, ever. Forcefully load middle platform lol.
						newLoc = mVesperidys.mSpawnLoc.clone().add(0, 1, 0);
						Vesperidys.Platform centrePlatform = mVesperidys.mPlatformList.getPlatform(0, 0);
						if (centrePlatform != null) {
							centrePlatform.generateInstantFull();
						}
					} else {
						newLoc = selectedPlatform.getCenter().add(0, 1, 0);
					}

					player.teleport(newLoc);
				}
			}
		}

		// Block Updates
		if (mBlockTicks >= 40) {
			mBlockTicks = 0;
			blockTick(false);
		}
	}

	public void blockTick(boolean instantClear) {
		ArrayList<Block> platformBlocks = new ArrayList<>();
		ArrayList<Block> nonMechanicalBlocks = new ArrayList<>();

		// Gets all blocks that are in Platforms.
		for (Vesperidys.Platform platform : mVesperidys.mPlatformList.getAllPlatforms()) {
			platformBlocks.addAll(platform.mBlocks);
		}

		// Remove any marked blocks from the last Block Update.
		for (Block block : mMarked) {
			if (!BlockUtils.isMechanicalBlock(block.getType()) && !platformBlocks.contains(block) && !mIgnored.contains(block)) {
				block.setType(Material.AIR);
			}
		}
		mMarked.clear();

		// Add in all blocks to be checked.
		for (int x = -ANTI_BLOCK_ZONE_DISTANCE; x <= ANTI_BLOCK_ZONE_DISTANCE; x++) {
			for (int y = -12; y <= 25; y++) {
				for (int z = -ANTI_BLOCK_ZONE_DISTANCE; z <= ANTI_BLOCK_ZONE_DISTANCE; z++) {
					Block block = mSpawnLoc.clone().add(x, y, z).getBlock();
					if (!BlockUtils.isMechanicalBlock(block.getType()) && !platformBlocks.contains(block) && !mIgnored.contains(block)) {
						if (y < -5 || y > 5) {
							new PartialParticle(Particle.SQUID_INK, block.getLocation(), 5, 0.5, 0.5, 0.5, 0).spawnAsBoss();
							block.setType(Material.AIR);
						} else {
							nonMechanicalBlocks.add(block);
						}
					}
				}
			}
		}

		// Mark blocks with a 1/3 rate.
		for (Block block : nonMechanicalBlocks) {
			if (instantClear) {
				new PartialParticle(Particle.SQUID_INK, block.getLocation(), 5, 0.5, 0.5, 0.5, 0).spawnAsBoss();
				block.setType(Material.AIR);
			} else if (FastUtils.randomIntInRange(0, 2) == 0) {
				new PartialParticle(Particle.SQUID_INK, block.getLocation(), 5, 0.5, 0.5, 0.5, 0).spawnAsBoss();
				if (DepthsUtils.isIce(block.getType())) {
					block.setType(Material.FROSTED_ICE);
					BlockData blockData = block.getState().getBlockData();
					if (blockData instanceof Ageable frostedIce) {
						frostedIce.setAge(3);
						block.setBlockData(frostedIce);
					}
				} else {
					block.setType(Material.STRIPPED_CRIMSON_HYPHAE);
				}
				mMarked.add(block);
			}
		}
	}

	public void antiCheeseCooldown() {
		mBlockTicks = -5 * 20;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
