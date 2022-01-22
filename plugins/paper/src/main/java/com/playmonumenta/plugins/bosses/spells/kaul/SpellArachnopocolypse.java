package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SpellArachnopocolypse extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mLoc;
	private double mDetectRange;
	private boolean mCooldown = false;
	private ChargeUpManager mChargeUp;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, Location loc, double detectRange) {
		mPlugin = plugin;
		mBoss = boss;
		mLoc = loc;
		mDetectRange = detectRange;
		mChargeUp = new ChargeUpManager(mBoss, 45, ChatColor.GREEN + "Channeling " + ChatColor.DARK_GREEN + "Arachnopocalypse...",
			BarColor.GREEN, BarStyle.SEGMENTED_10, 50);
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);
		new BukkitRunnable() {

			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mDetectRange, true);
				players.removeIf(p -> p.getLocation().getY() >= 61);
				int amount = 10 + (5 * (players.size()));
				if (players.size() == 1) {
					amount = 18;
				}
				int a = amount;
				mChargeUp.setChargeTime(a);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0);
				new BukkitRunnable() {

					@Override
					public void run() {

						riseSpider(getRandomLocation(mLoc, 32));
						if (mChargeUp.nextTick()) {
							this.cancel();
							mChargeUp.reset();
						}
					}

				}.runTaskTimer(mPlugin, 0, 2);
			}

		}.runTaskLater(mPlugin, 30);

	}

	private static final String[] SPIDER_SUMMONS = {
	        "BlackrootBroodmother",
	        "BlackrootArachnid",
	        "ShieldbreakerSpider",
	        "BlackrootMonster"
	};

	public void riseSpider(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
		world.spawnParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData());
		LibraryOfSoulsIntegration.summon(loc.clone().add(0, 1, 0), SPIDER_SUMMONS[FastUtils.RANDOM.nextInt(SPIDER_SUMMONS.length)]);
	}

	private Location getRandomLocation(Location origin, double range) {
		Location loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		while (loc.getBlock().getType().isSolid()) {
			loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		}
		return loc;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20;
	}

	@Override
	public int castTicks() {
		return 20 * 5;
	}

}
