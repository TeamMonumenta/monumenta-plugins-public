package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSoulShackle extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private int mCeiling;
	private ChargeUpManager mChargeUp;
	private List<Player> mGotHit = new ArrayList<Player>();
	private PartialParticle mPortal;
	private PartialParticle mRod;
	private PartialParticle mSpark;

	public SpellSoulShackle(Plugin plugin, LivingEntity boss, Location loc, double r, int ceil) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = r;
		mCeiling = ceil;
		mChargeUp = new ChargeUpManager(mBoss, 20, ChatColor.YELLOW + "Charging Soul Shackle...", BarColor.YELLOW, BarStyle.SOLID, 50);
		mPortal = new PartialParticle(Particle.PORTAL, mBoss.getLocation(), 100, 0.1, 0.1, 0.1, 1.5);
		mRod = new PartialParticle(Particle.END_ROD, mBoss.getLocation(), 40, 1, 1, 1, 0);
		mSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 1, 0, 0, 0, 0);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 6.0f, 1.0f);
		mPortal.location(mBoss.getLocation().add(0, 5, 0)).spawnAsBoss();

		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p) || p.getLocation().getY() >= mCenter.getY() + mCeiling);

		//summon all bullets after 0.5s
		BukkitRunnable run = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
					world.playSound(mBoss.getLocation().add(0, 5, 0), Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.HOSTILE, 3.0f, 1.0f);
					Location cornerloc = mBoss.getLocation().add(-1, 5, -1);
					for (int i = 0; i < players.size(); i++) {
						Location spawnloc = cornerloc.clone().add(FastUtils.RANDOM.nextInt(2), 0, FastUtils.RANDOM.nextInt(2));
						EntityUtils.summonEntityAt(spawnloc, EntityType.SHULKER_BULLET, "{Target:[I;1234,1234,1234,1234],Motion:[0.0,0.5,0.0],TYD:-1d}");
					}

					//grab all bullets summoned
					List<Entity> bullets = new ArrayList<Entity>(mBoss.getLocation().add(0, 5, 0).getNearbyEntities(2f, 2f, 2f));
					bullets.removeIf(e -> !e.getType().equals(EntityType.SHULKER_BULLET));

					//set target of each bullet in list
					for (int i = 0; i < players.size(); i++) {
						Player p = players.get(i);
						ShulkerBullet b = (ShulkerBullet) bullets.get(i);
						b.setTarget(p);
						b.setShooter(mBoss);
					}
					mChargeUp.reset();
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	// soul shackle player lock
	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof ShulkerBullet)) {
			return;
		}
		event.setCancelled(true);
		event.getEntity().remove();

		if (!(event.getHitEntity() instanceof Player p)) {
			return;
		}
		if (mGotHit.contains(p)) {
			return;
		}
		mGotHit.add(p);

		World world = mBoss.getWorld();
		Location pLoc = p.getLocation().add(0, 1.5, 0);
		p.sendMessage(ChatColor.AQUA
			              + "You got chained by Hekawt! Don't move outside of the ring!");

		DamageUtils.damage(mBoss, p, DamageType.MAGIC, 27, null, false, true, "Soul Shackle");
		AbilityUtils.silencePlayer(p, 5 * 20);
		mRod.location(pLoc).spawnAsBoss();
		world.playSound(pLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.7f, 0.5f);
		BossBar bar = Bukkit.getServer().createBossBar(ChatColor.RED + "Soul Shackle Duration", BarColor.RED, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		bar.setVisible(true);
		bar.addPlayer(p);

		PPCircle indicator = new PPCircle(Particle.END_ROD, pLoc, 3).ringMode(true).count(36);

		BukkitRunnable run = new BukkitRunnable() {

			int mINC = 0;

			@Override
			public void run() {
				//chain function
				mINC++;
				if (SpellDimensionDoor.getShadowed().contains(p)) {
					this.cancel();
					bar.setVisible(false);
					bar.removePlayer(p);
					return;
				}

				if (mINC % 10 == 0) {
					p.removePotionEffect(PotionEffectType.LEVITATION);
					for (double n = -1; n < 2; n += 1) {
						Location mColumn = pLoc.clone().add(0, n, 0);
						mSpark.location(mColumn).spawnAsBoss();
					}

					// check HORIZONTAL distance to allow jump boost effects
					Location pGroundLoc = p.getLocation();
					Location pCheckLoc = pLoc.clone();
					pGroundLoc.setY(mCenter.getY());
					pCheckLoc.setY(mCenter.getY());
					if (pGroundLoc.distance(pCheckLoc) > 3) {
						p.sendMessage(ChatColor.AQUA + "I shouldn't leave this ring.");
						world.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2.0f, 1.0f);
						BossUtils.bossDamagePercent(mBoss, p, 0.15, "Soul Shackle");
						MovementUtils.knockAway(pCheckLoc, p, -0.75f, false);
					}

					Location endLoc = pLoc.clone();
					Vector baseVect = LocationUtils.getDirectionTo(p.getLocation(), pLoc);

					for (int inc = 0; inc < 100; inc++) {
						new PartialParticle(Particle.END_ROD, endLoc, 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
						endLoc.add(baseVect.clone().multiply(0.5));
						if (endLoc.distance(pLoc) > p.getLocation().distance(pLoc)) {
							break;
						}
					}

					indicator.location(pLoc).spawnAsBoss();
				}

				//boss bar
				double progress = 1.0d - mINC / (20.0d * 5.0d);
				if (progress > 0) {
					bar.setProgress(progress);
				}

				// cancel
				if (mINC >= 20 * 5 || p.isDead() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
					bar.setVisible(false);
					bar.removePlayer(p);
					mGotHit.remove(p);
					this.cancel();
				}
			}
		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);

		//do not put this in activerunnables or else the boss bar will linger when phase change
		new BukkitRunnable() {

			@Override
			public void run() {
				if (run.isCancelled()) {
					bar.setVisible(false);
					bar.removeAll();
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
