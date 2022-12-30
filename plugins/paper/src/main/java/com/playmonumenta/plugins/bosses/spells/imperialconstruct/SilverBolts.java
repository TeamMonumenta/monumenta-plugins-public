package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Wall;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SilverBolts extends SpellBaseSeekingProjectile {

	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER
	);

	private final LivingEntity mLauncher;
	private final Plugin mPlugin;

	private static final int COOLDOWN = 20 * 20;
	private static final int DELAY = 20 * 4;
	private static final double SPEED = 0.2;
	private static final double TURN_RADIUS = Math.PI / 16;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int) (DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.5;
	private static final int DAMAGE = 80;
	private static final int CAGE_DURATION = 10 * 20;
	private static final HashMap<Vector, BlockData> CAGE_LOCATIONS = new HashMap<>();

	static {
		BlockData deepslate = Material.DEEPSLATE_BRICKS.createBlockData();
		BlockData deepslateSlab = Material.DEEPSLATE_BRICK_SLAB.createBlockData();
		Wall wall = (Wall) Material.POLISHED_DEEPSLATE_WALL.createBlockData();
		wall.setUp(true);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				CAGE_LOCATIONS.put(new Vector(x, -1, z), deepslate);

				MultipleFacing bars = (MultipleFacing) Material.IRON_BARS.createBlockData();
				bars.setFace(BlockFace.NORTH, z >= 0 && x != 0);
				bars.setFace(BlockFace.SOUTH, z <= 0 && x != 0);
				bars.setFace(BlockFace.WEST, x >= 0 && z != 0);
				bars.setFace(BlockFace.EAST, x <= 0 && z != 0);

				if (!(x == 0 && z == 0)) {
					for (int y = 0; y <= 4; y++) {
						CAGE_LOCATIONS.put(new Vector(x, y, z), bars);
					}
				}

				// plus shape
				if (x * z == 0) {
					CAGE_LOCATIONS.put(new Vector(x, 5, z), deepslate);
					Wall wall2 = (Wall) wall.clone();
					wall2.setHeight(BlockFace.NORTH, z >= 0 && x == 0 ? Wall.Height.LOW : Wall.Height.NONE);
					wall2.setHeight(BlockFace.SOUTH, z <= 0 && x == 0 ? Wall.Height.LOW : Wall.Height.NONE);
					wall2.setHeight(BlockFace.WEST, x >= 0 && z == 0 ? Wall.Height.LOW : Wall.Height.NONE);
					wall2.setHeight(BlockFace.EAST, x <= 0 && z == 0 ? Wall.Height.LOW : Wall.Height.NONE);
					CAGE_LOCATIONS.put(new Vector(x, 6, z), wall2);
				} else {
					CAGE_LOCATIONS.put(new Vector(x, 5, z), deepslateSlab);
				}
			}
		}

		CAGE_LOCATIONS.put(new Vector(0, 7, 0), wall);
		CAGE_LOCATIONS.put(new Vector(0, 8, 0), Material.POLISHED_DEEPSLATE.createBlockData());
		CAGE_LOCATIONS.put(new Vector(0, 9, 0), deepslateSlab);
	}

	private final ChargeUpManager mChargeUp;

	public SilverBolts(LivingEntity boss, Plugin plugin) {
		super(plugin, boss, false, COOLDOWN, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, true, true, 0, false,
			// Get spell targets
			() -> {
				return PlayerUtils.playersInRange(boss.getLocation(), 50, true);
			},
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				// Do nothing, effects are in run() since we cannot access the bossbar here
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 2);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 3, 1.2f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				if (ticks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 0.25f, 1.5f);
				}
				new PartialParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(boss);
				new PartialParticle(Particle.ELECTRIC_SPARK, loc, 4, 0.25, 0.25, 0.25, 0.05).spawnAsEntityActive(boss);
			},
			// Hit Action
			(World world, @Nullable LivingEntity le, Location loc, @Nullable Location prevLoc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 1, 0);
				new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0).spawnAsEntityActive(boss);
				if (le instanceof Player player) {
					BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MAGIC, DAMAGE, "Silver Bolts", prevLoc);
					cage(player);
				}

				new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 1, 1, 1).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);
			});

		mPlugin = plugin;
		mLauncher = boss;

		mChargeUp = new ChargeUpManager(mLauncher, DELAY, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + "Silver Bolts",
			BarColor.YELLOW, BarStyle.SOLID, 60);
	}

	@Override
	public void run() {
		super.run();
		World world = mLauncher.getWorld();
		mChargeUp.reset();
		BukkitRunnable chargeUpRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					world.playSound(mLauncher.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1.5f, 1.5f);
					mLauncher.setGlowing(false);
					this.cancel();
				} else {
					mLauncher.setGlowing(true);
					if (mChargeUp.getTime() % 4 == 0) {
						Location loc = mLauncher.getLocation();
						new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mLauncher);
						new PartialParticle(Particle.SMOKE_NORMAL, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mLauncher);
					}
				}
			}
		};
		chargeUpRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(chargeUpRunnable);
	}

	private static void cage(Player player) {
		com.playmonumenta.plugins.Plugin plugin = com.playmonumenta.plugins.Plugin.getInstance();
		PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SLOW_DIGGING, CAGE_DURATION, 99, false, false, true));
		plugin.mEffectManager.addEffect(player, "SilverBoltsAntiHealEffect", new PercentHeal(CAGE_DURATION, -0.5));
		MessagingUtils.sendActionBarMessage(player, ChatColor.RED + "You have 50% reduced healing for 10s");

		Location loc = player.getLocation();
		HashMap<Block, BlockData> oldBlocks = new HashMap<>();
		CAGE_LOCATIONS.forEach((offset, data) -> {
			Block block = loc.clone().add(offset).getBlock();
			if (!IGNORED_MATS.contains(block.getType())) {
				oldBlocks.put(block, block.getBlockData());
				block.setBlockData(data);
			}
		});

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			oldBlocks.forEach((block, blockData) -> {
				if (!block.getType().isAir()) {
					block.setBlockData(blockData);
				}
			});
		}, SilverBolts.CAGE_DURATION);
	}
}
