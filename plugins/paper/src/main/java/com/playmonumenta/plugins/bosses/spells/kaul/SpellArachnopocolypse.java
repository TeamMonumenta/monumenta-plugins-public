package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.entity.Spider;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellArachnopocolypse extends Spell {
	private static final String SPELL_NAME = "Arachnopocolypse";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private boolean mCooldown = false;
	private final Location mSpiderCenter;
	private final ChargeUpManager mChargeUp;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mSpiderCenter = center.clone().add(0, 0.5, 0);
		mChargeUp = new ChargeUpManager(mBoss, 30, Component.text("Channeling ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_GREEN)),
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

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				for (SpellPutridPlague.Pillar pillar : SpellPutridPlague.Pillar.values()) {
					Location location = mSpiderCenter.clone().add(pillar.getSpiderOffset());
					new PartialParticle(Particle.CRIT, location, 8 * mChargeUp.getTime() / mChargeUp.getChargeTime(), 0.2, 0.2, 0.2).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_NORMAL, location, 8, 0.4, 0.4, 0.4).spawnAsBoss();
				}

				if (mChargeUp.nextTick()) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 10, 1);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0).spawnAsEntityActive(mBoss);

					double health = 80 * BossUtils.healthScalingCoef(Kaul.getArenaParticipants(mSpiderCenter).size(), 0.5, 0.5);

					for (SpellPutridPlague.Pillar pillar : SpellPutridPlague.Pillar.values()) {
						Location location = mSpiderCenter.clone().add(pillar.getSpiderOffset());
						riseSpider(location, pillar.mSpiderLos, (int) health);
					}

					this.cancel();
					mChargeUp.reset();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void riseSpider(Location loc, String los, int health) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1, 1f);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData()).spawnAsEntityActive(mBoss);
		new PPSpiral(Particle.SPELL_WITCH, loc, 3)
			.count(120)
			.curveAngle(360)
			.spawnAsBoss();

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
