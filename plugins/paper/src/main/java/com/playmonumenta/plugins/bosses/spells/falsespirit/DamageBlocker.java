package com.playmonumenta.plugins.bosses.spells.falsespirit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class DamageBlocker extends Spell {

	private static final double MAX_DEFLECT_VELOCITY = 3.0;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private List<Player> mWarned = new ArrayList<Player>();
	private GatesOfHell mHell;
	private GatesOfHell mCeilingHell;

	public DamageBlocker(Plugin plugin, LivingEntity boss, GatesOfHell hell, GatesOfHell ceilingHell) {
		mPlugin = plugin;
		mBoss = boss;
		mHell = hell;
		mCeilingHell = ceilingHell;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, 20 * 5);
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		if (proj.getShooter() instanceof Player) {
			Player player = (Player) proj.getShooter();
			if (player.getLocation().distance(mBoss.getLocation()) > 7) {
				//Do not do damage if farther than 7 blocks away
				if (!(proj instanceof Trident)) {
					Projectile deflected = (Projectile) mBoss.getWorld().spawnEntity(proj.getLocation().subtract(proj.getVelocity().normalize()), proj.getType());
					deflected.setShooter(mBoss);
					if (deflected instanceof Arrow && proj instanceof Arrow) {
						Arrow arrow = (Arrow) deflected;
						Arrow projec = (Arrow) proj;
						((Arrow) deflected).setCritical(((Arrow) proj).isCritical());
						if (projec.getBasePotionData() != null) {
							arrow.setBasePotionData((projec).getBasePotionData());
							for (PotionEffect effect : (projec).getCustomEffects()) {
								arrow.addCustomEffect(effect, true);
							}
						}

					}
					deflected.setVelocity(LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), deflected.getLocation()).multiply(Math.max(MAX_DEFLECT_VELOCITY, proj.getVelocity().length())));
					proj.remove();
				}
			}
		}
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Projectile) {
			Projectile proj = (Projectile) event.getDamager();
			if (proj.getShooter() instanceof Player) {
				Player player = (Player) proj.getShooter();
				if (player.getLocation().distance(mBoss.getLocation()) > 7 || mHell.checkPortals() || mCeilingHell.checkPortals()) {
					if (!mWarned.contains(player) && (mHell.checkPortals() || mCeilingHell.checkPortals())) {
						player.sendMessage(ChatColor.DARK_RED + "Foolish. I am made of nothing. Your attacks shall do nothing to me while my gates are powered.");
						mWarned.add(player);
					} else if (!mWarned.contains(player)) {
						player.sendMessage(ChatColor.GOLD + "[Bhairavi]" + ChatColor.WHITE + " You must get closer! It's turning your attacks to nothing!");
						mWarned.add(player);
					}
					event.setCancelled(true);
					player.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 2);
					mBoss.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, proj.getLocation(), 10, 0, 0, 0, 0.1);
				}
			}
		}

		if (event.getDamager() instanceof Player) {
			Player player = (Player) event.getDamager();
			if (player.getLocation().distance(mBoss.getLocation()) > 7 || mHell.checkPortals() || mCeilingHell.checkPortals()) {
				if (!mWarned.contains(player) && (mHell.checkPortals() || mCeilingHell.checkPortals())) {
					player.sendMessage(ChatColor.DARK_RED + "Foolish. I am made of nothing. Your attacks shall do nothing to me while my gates are powered.");
					mWarned.add(player);
				} else if (!mWarned.contains(player)) {
					player.sendMessage(ChatColor.GOLD + "[Bhairavi]" + ChatColor.WHITE + " You must get closer! It's turning your attacks to nothing!");
					mWarned.add(player);
				}
				event.setCancelled(true);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 2);
				mBoss.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 10, 0, 0, 0, 0.1);
			} else {
				player.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 10, 1.5f);
			}
		}
	}

	@Override
	public void run() {
		Location bossLoc = mBoss.getLocation();
		if (mHell.checkPortals() || mCeilingHell.checkPortals()) {
			//Always block with portals up
			Vector vec;
			for (int y = 0; y < 2; y++) {
				for (double degree = 0; degree < 360; degree += 40) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * 1, y, FastUtils.sin(radian1) * 1);
					vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

					Location l = bossLoc.clone().add(vec);
					mBoss.getWorld().spawnParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0);
				}
			}
		} else {
			//Show particles when player is further than 7 blocks to indicate no damage will be done
			for (Player player : PlayerUtils.playersInRange(bossLoc, FalseSpirit.detectionRange)) {
				if (bossLoc.distance(player.getLocation()) > 7) {
					Vector vec;
					for (int y = 0; y < 2; y++) {
						for (double degree = 0; degree < 360; degree += 40) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(FastUtils.cos(radian1) * 1, y, FastUtils.sin(radian1) * 1);
							vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

							Location l = bossLoc.clone().add(vec);
							player.spawnParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0);
						}
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 4;
	}


}
