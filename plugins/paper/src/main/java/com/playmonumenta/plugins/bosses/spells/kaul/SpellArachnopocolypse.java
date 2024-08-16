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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellArachnopocolypse extends Spell {
	private static final String SPELL_NAME = "Arachnopocolypse";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mDetectRange;
	private boolean mCooldown = false;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, double detectRange, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDetectRange = detectRange;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(mBoss, 45, Component.text("Channeling ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_GREEN)),
			BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, 50);
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
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, mDetectRange, true);
					players.removeIf(p -> p.getLocation().getY() >= 61);

					HashMap<String, Vector> spiderLocations = new HashMap<>();
					spiderLocations.put("EarthVassal", new Vector(-15, -8, -15));
					spiderLocations.put("AirVassal", new Vector(-15, -8, 15));
					spiderLocations.put("FireVassal", new Vector(15, -8, -15));
					spiderLocations.put("WaterVassal", new Vector(15, -8, 15));

					world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 10, 1);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0).spawnAsEntityActive(mBoss);

					double health = 80 * BossUtils.healthScalingCoef(players.size(), 0.5, 0.5);

					for (String los : spiderLocations.keySet()) {
						riseSpider(mSpawnLoc.clone().add(spiderLocations.get(los)), los, (int) health);
					}

					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				mActiveRunnables.remove(this);
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void riseSpider(Location loc, String los, int health) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1, 1f);
		new PartialParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData()).spawnAsEntityActive(mBoss);
		Spider spider = (Spider) LibraryOfSoulsIntegration.summon(loc.clone().add(0, 1, 0), los);
		if (spider != null) {
			EntityUtils.setAttributeBase(spider, Attribute.GENERIC_MAX_HEALTH, health);
			spider.setHealth(health);
		}
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
