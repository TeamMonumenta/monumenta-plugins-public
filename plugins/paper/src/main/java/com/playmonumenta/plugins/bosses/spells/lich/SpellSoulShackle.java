package com.playmonumenta.plugins.bosses.spells.lich;

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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class SpellSoulShackle extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private int mCeiling;
	private ChargeUpManager mChargeUp;
	private List<Player> mGotHit = new ArrayList<Player>();

	public SpellSoulShackle(Plugin plugin, LivingEntity boss, Location loc, double r, int ceil) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = r;
		mCeiling = ceil;
		mChargeUp = new ChargeUpManager(mBoss, 20, ChatColor.YELLOW + "Charging Soul Shackle...", BarColor.YELLOW, BarStyle.SOLID, 50);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 6.0f, 1.0f);
		new PartialParticle(Particle.PORTAL, mBoss.getLocation().add(0, 5, 0), 100, 0.1, 0.1, 0.1, 1.5).spawnAsBoss();

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
						((Projectile) b).setShooter(mBoss);
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
		if (event.getEntity() instanceof ShulkerBullet && event.getHitEntity() instanceof Player && !mGotHit.contains(event.getHitEntity())) {
			mGotHit.add((Player) event.getHitEntity());
			event.setCancelled(true);
			event.getEntity().remove();

			World world = mBoss.getWorld();
			Player p = (Player) event.getHitEntity();
			Location pLoc = p.getLocation().add(0, 1.5, 0);
			p.sendMessage(ChatColor.AQUA
	                   + "You got chained by Hekawt! Don't move outside of the ring!");

			BossUtils.bossDamage(mBoss, p, 35, null, "Soul Shackle");
			AbilityUtils.silencePlayer(p, 5 * 20);
			new PartialParticle(Particle.END_ROD, pLoc, 40, 1, 1, 1, 0).spawnAsBoss();
			world.playSound(pLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7f, 0.5f);

			PPGroundCircle indicator = new PPGroundCircle(Particle.END_ROD, pLoc, 36, 0, 0, 0, 0).init(3, true);

			BukkitRunnable run = new BukkitRunnable() {
				BossBar mBar = Bukkit.getServer().createBossBar(ChatColor.RED + "Soul Shackle Duration", BarColor.RED, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
				int mINC = 0;

				@Override
				public void run() {
					//chain function
					mINC++;
					if (SpellDimensionDoor.getShadowed().contains(p)) {
						this.cancel();
						mBar.setVisible(false);
						mBar.removePlayer(p);
						return;
					}

					if (mINC % 10 == 0) {
						p.removePotionEffect(PotionEffectType.LEVITATION);
						for (double n = -1; n < 2; n += 1) {
							Location mColumn = pLoc.clone().add(0, n, 0);
							new PartialParticle(Particle.FIREWORKS_SPARK, mColumn, 1, 0, 0, 0, 0).spawnAsBoss();
						}

						// check HORIZONTAL distance to allow jump boost effects
						Location pGroundLoc = p.getLocation();
						Location pCheckLoc = pLoc.clone();
						pGroundLoc.setY(mCenter.getY());
						pCheckLoc.setY(mCenter.getY());
						if (pGroundLoc.distance(pCheckLoc) > 3) {
							p.sendMessage(ChatColor.AQUA + "I shouldn't leave this ring.");
							world.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 2.0f, 1.0f);
							BossUtils.bossDamagePercent(mBoss, p, 0.15, null, "Soul Shackle");
							MovementUtils.knockAway(pCheckLoc, p, -0.7f);
						}

						Location endLoc = pLoc.clone();
						Vector baseVect = LocationUtils.getDirectionTo(p.getLocation(), pLoc);

						for (int inc = 0; inc < 100; inc++) {
							world.spawnParticle(Particle.END_ROD, endLoc, 1, 0.1, 0.1, 0.1, 0);
							endLoc.add(baseVect.clone().multiply(0.5));
							if (endLoc.distance(pLoc) > p.getLocation().distance(pLoc)) {
								break;
							}
						}

						indicator.location(pLoc).spawnAsBoss();
					}

					//boss bar
					mBar.setVisible(true);
					mBar.addPlayer(p);
					double progress = 1.0d - mINC / (20.0d * 5.0d);
					if (progress > 0) {
						mBar.setProgress(progress);
					}

					// cancel
					if (mINC >= 20 * 5 || p.isDead() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						mBar.setVisible(false);
						mBar.removePlayer(p);
						mGotHit.remove(p);
					}
				}
			};
			run.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(run);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
