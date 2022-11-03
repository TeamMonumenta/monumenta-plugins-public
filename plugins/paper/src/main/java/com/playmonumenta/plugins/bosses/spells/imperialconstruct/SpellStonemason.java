package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellStonemason extends Spell {

	private final String ABILITY_NAME = "Stonemason";
	private final int CAST_TIME = 20 * 3;

	private LivingEntity mBoss;
	private Plugin mPlugin;
	private int mRadius = 6;
	private int mDamage;
	private ChargeUpManager mChargeUp;
	private Location mStartLoc;
	private int mRange;

	public SpellStonemason(LivingEntity boss, Plugin plugin, Location startLoc, int range, int damage) {
		mBoss = boss;
		mPlugin = plugin;
		mStartLoc = startLoc;
		mRange = range;
		mDamage = damage;
		mChargeUp = new ChargeUpManager(mBoss, CAST_TIME, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, mRange);
	}

	@Override
	public void run() {

		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);
		World world = mBoss.getWorld();
		mChargeUp.setTime(0);
		List<Location> locs = new ArrayList<>();
		for (Player p : players) {
			locs.add(p.getLocation());
		}

		BukkitRunnable testRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick(2)) {
					for (double degree = 0; degree < 360; degree += 15) {
						if (FastUtils.RANDOM.nextDouble() < 0.8) {
							double radian = Math.toRadians(degree);
							double cos = FastUtils.cos(radian);
							double sin = FastUtils.sin(radian);

							for (Location loc : locs) {
								loc.add(cos * mRadius, 0.5, sin * mRadius);
								world.spawnParticle(Particle.SQUID_INK, loc, 1, 0, 0, 0, 0);
								loc.subtract(cos * mRadius, 0.5, sin * mRadius);
							}
						}
					}

					for (Location loc : locs) {
						Location tempLoc = loc.clone();
						for (int y = 0; y >= -15; y--) {
							tempLoc.set(loc.getX(), loc.getY() + y, loc.getZ());

							if (!tempLoc.getBlock().getType().isAir()) {
								loc.set(tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
								break;
							}
						}

						world.spawnParticle(Particle.LAVA, loc, 20, 2, 0.1, 2, 0.25);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 2);
						world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 1.5f, 0);

						for (Player p : PlayerUtils.playersInRange(loc, mRadius, true)) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, true, true, "Stonemason");
							MovementUtils.knockAway(loc, p, 0f, 1f, false);
							world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1.5f, 1);
						}

						BukkitRunnable runnable2 = new BukkitRunnable() {
							int mTicks = 0;
							@Override
							public void run() {
								if (mTicks > 10) {
									this.cancel();
									return;
								}

								com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
								scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");
								try {
									scriptedQuestsPlugin.mGrowableManager.grow("constructpillar", loc, 1, 7, true);
								} catch (Exception e) {
									mPlugin.getLogger().warning("Failed to grow scripted quests structure 'constructpillar': " + e.getMessage());
									e.printStackTrace();
								}

								mTicks += 1;
							}
						};
						runnable2.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnable2);
					}

					this.cancel();
				} else {
					if (mChargeUp.getTime() % 6 == 0) {
						for (double degree = 0; degree < 360; degree += 15) {
							double radian = Math.toRadians(degree);
							double cos = FastUtils.cos(radian);
							double sin = FastUtils.sin(radian);

							for (Location loc : locs) {
								loc.add(cos * mRadius, 0.5, sin * mRadius);
								if (FastUtils.RANDOM.nextDouble() < 0.5) {
									world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0);
								} else {
									world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0);
								}
								loc.subtract(cos * mRadius, 0.5, sin * mRadius);
							}
						}
					}

					for (Location loc : locs) {
						world.spawnParticle(Particle.BLOCK_DUST, loc, 10, 2, 0.1, 2, 0.25, Material.BONE_BLOCK.createBlockData());
						world.spawnParticle(Particle.LAVA, loc, 5, 2, 0.1, 2, 0.25);

						if (mChargeUp.getTime() % 6 == 0) {
							world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1, 0f);
						}
					}
				}
			}
		};
		testRunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(testRunnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}
}
