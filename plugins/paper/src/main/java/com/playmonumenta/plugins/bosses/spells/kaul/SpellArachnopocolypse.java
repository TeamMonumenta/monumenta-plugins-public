package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellArachnopocolypse extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mDetectRange;
	private boolean mCooldown = false;
	private Location mSpawnLoc;
	private ChargeUpManager mChargeUp;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, double detectRange, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDetectRange = detectRange;
		mSpawnLoc = spawnLoc;
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

		//30 ticks charge time
		mChargeUp.setTime(15);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, mDetectRange, true);
					players.removeIf(p -> p.getLocation().getY() >= 61);

					HashMap<String, Vector> spiderLocations = new HashMap<String, Vector>();
					spiderLocations.put("EarthVassal", new Vector(-15, -8, -15));
					spiderLocations.put("AirVassal", new Vector(-15, -8, 15));
					spiderLocations.put("FireVassal", new Vector(15, -8, -15));
					spiderLocations.put("WaterVassal", new Vector(15, -8, 15));

					world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0).spawnAsEntityActive(mBoss);

					double health = 80 * BossUtils.healthScalingCoef(players.size(), 0.5, 0.5);

					for (String los : spiderLocations.keySet()) {
						riseSpider(mSpawnLoc.clone().add(spiderLocations.get(los)), los, (int) health);
					}

					this.cancel();
					return;
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);

	}

	private void riseSpider(Location loc, String los, int health) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
		new PartialParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData()).spawnAsEntityActive(mBoss);
		Spider spider = (Spider) LibraryOfSoulsIntegration.summon(loc.clone().add(0, 1, 0), los);
		EntityUtils.setAttributeBase(spider, Attribute.GENERIC_MAX_HEALTH, health);
		spider.setHealth(health);
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
