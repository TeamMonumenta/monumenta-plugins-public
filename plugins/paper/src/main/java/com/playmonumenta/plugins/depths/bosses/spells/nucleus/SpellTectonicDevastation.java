package com.playmonumenta.plugins.depths.bosses.spells.nucleus;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellTectonicDevastation extends Spell {

	public static final String TAG = "HitByShatter";
	public static final int TELEGRAPH_DURATION = 50;
	public static final Material TELEGRAPH_TYPE = Material.STRIPPED_WARPED_HYPHAE;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	public int mCooldownTicks;
	public Nucleus mBossInstance;

	private final List<Block> mChangedBlocks = new ArrayList<>();

	private final Location mStartLoc;

	public SpellTectonicDevastation(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
		mBossInstance = bossInstance;
	}

	@Override
	public boolean canRun() {
		return mBossInstance.mIsHidden;
	}

	@Override
	public void run() {
		cast(0);

		new BukkitRunnable() {
			@Override
			public void run() {
				cast(40);
			}
		}.runTaskLater(mPlugin, 4 * 20);

		new BukkitRunnable() {
			@Override
			public void run() {
				cast(80);
			}
		}.runTaskLater(mPlugin, 8 * 20);
	}

	public void cast(int offset) {

		//mBoss.setAI(false);
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, 80, true);
		players.removeIf(p -> p.getGameMode() != GameMode.SURVIVAL);
		Player target = null;

		//Choose random target
		if (players.size() == 1) {
			target = players.get(0);
		} else if (players.size() > 1) {
			target = players.get(FastUtils.RANDOM.nextInt(players.size()));
		}

		Player tar = target;
		if (tar != null) {
			Vector dir = LocationUtils.getDirectionTo(tar.getLocation(), mStartLoc).setY(0).normalize();
			mBoss.teleport(mBoss.getLocation().setDirection(dir));
		}

		Location loc = mStartLoc.clone();

		loc.setYaw(loc.getYaw() + offset);

		mChangedBlocks.clear();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 0;

			@Override
			public void run() {
				mT += 2;
				mPitch += 0.025f;

				//Play shatter sound
				if (mT % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3, mPitch);
				}

				//Every half-second, do visuals
				if (mT % 10 == 0) {
					//Creates 4 cones in 4 different directions
					for (int dir = 0; dir < 360; dir += 120) {
						Vector vec;
						//The degree range is 60 degrees for 30 blocks radius
						for (double degree = 60; degree < 120; degree += 5) {
							for (double r = 0; r < 30; r++) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								Location l = loc.clone().add(vec);

								l.subtract(0, 1, 0);
								//Spawns stripped warped hyphae as a warning at a 1/3 rate, will try to climb 1 block up or down if needed
								if (l.getBlock().getType() != TELEGRAPH_TYPE) {
									if (FastUtils.RANDOM.nextInt(3) == 0 || mT == 20 * 2) {
										while (l.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR && l.getBlockY() <= mStartLoc.getBlockY() + 3) {
											l.add(0, 1, 0);
										}
										if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
											l.subtract(0, 1, 0);
										}
										//Once it leaves the arena, stop iterating
										if ((l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
											|| l.distance(mStartLoc) > 30) {
											continue;
										}
										//Move up one block if on barrier or bedrock level
										if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
											l.add(0, 1, 0);
										}
										if (TemporaryBlockChangeManager.INSTANCE.changeBlock(l.getBlock(), TELEGRAPH_TYPE, TELEGRAPH_DURATION - mT + FastUtils.randomIntInRange(0, 10))) {
											mChangedBlocks.add(l.getBlock());
										}
									}
								}
							}
						}
					}
				}

				//End shatter, deal damage, show visuals
				if (mT >= TELEGRAPH_DURATION) {
					//mBoss.setAI(true);
					Mob mob = (Mob) mBoss;
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), Nucleus.detectionRange, true);
					players.removeIf(p -> p.getGameMode() != GameMode.SURVIVAL);
					if (players.size() > 1) {
						Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
						if (mob.getTarget() != null) {
							while (player.getUniqueId().equals(mob.getTarget().getUniqueId())) {
								player = players.get(FastUtils.RANDOM.nextInt(players.size()));
							}
							mob.setTarget(player);
						}
					}
					this.cancel();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 0.5f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.5f);
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<>();

					for (double r = 0; r < 30; r++) {
						for (int dir = 0; dir < 360; dir += 120) {
							for (double degree = 60; degree < 120; degree += 5) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								Location l = loc.clone().add(vec);
								//1.5 -> 15
								BoundingBox box = BoundingBox.of(l, 0.65, 15, 0.65);
								boxes.add(box);
							}
						}
					}

					//Damage player by 40% hp in cone after warning is over (2 seconds) and knock player away
					Hitbox hitbox = Hitbox.unionOfAABB(boxes, world);
					for (Player player : hitbox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, 30, null, false, false, "Tectonic Devastation");
						player.addScoreboardTag(TAG);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> player.removeScoreboardTag(TAG), 100);
						player.setVelocity(player.getVelocity().setY(1.0f));
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, "Tectonic", new PercentHeal(6 * 20, -1.00));
						player.sendActionBar(Component.text("You cannot heal for 6s", NamedTextColor.RED));
						PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.BAD_OMEN, 6 * 20, 1));
					}

					for (LivingEntity le : hitbox.getHitMobs(mBoss)) {
						le.damage(12);
						le.setVelocity(le.getVelocity().setY(1.0f));
					}
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, TELEGRAPH_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks + (10 * 20);
	}
}
