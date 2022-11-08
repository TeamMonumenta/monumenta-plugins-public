package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFloor extends Spell {

	private int mTimer = 0;
	private int mDuration;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCurrentLoc;

	private List<LivingEntity> mDamaged = new ArrayList<>();


	public SpellFloor(Plugin plugin, LivingEntity boss, int duration, Location currentLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDuration = duration;
		mCurrentLoc = currentLoc.clone();
	}

	@Override
	public void run() {
		new PartialParticle(Particle.PORTAL, mCurrentLoc.clone().subtract(0, 8, 0), 200, 25, 1, 25, 0).spawnAsBoss();

		mTimer += 2;
		if (mTimer >= mDuration) {
			mTimer = 0;

			List<Player> players = PlayerUtils.playersInRange(mCurrentLoc, ImperialConstruct.detectionRange, true);

			for (Player p : players) {
				if (mDamaged.contains(p)) {
					continue;
				}
				Location loc = p.getLocation();
				double height = loc.getY();

				if (height - mCurrentLoc.getY() > 7 && p.isOnGround()) {
					BossUtils.bossDamagePercent(mBoss, p, 0.7, p.getLocation(), "Mechanical Void");
					Vector dir = mCurrentLoc.toVector().clone().subtract(p.getLocation().toVector());
					dir.normalize();
					dir.multiply(1.5f);

					p.setVelocity(dir);
					mDamaged.add(p);
					new BukkitRunnable() {
						@Override
						public void run() {
							mDamaged.remove(p);
						}
					}.runTaskLater(mPlugin, 0);
				}

				if (p.getLocation().distance(mCurrentLoc) > 31) {
					p.sendMessage(ChatColor.GRAY + "The mechanical void pushes you back in.");
					p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 1, 0);
					new PartialParticle(Particle.FIREWORKS_SPARK, p.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15);
					new PartialParticle(Particle.DRAGON_BREATH, p.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.2);

					BossUtils.bossDamagePercent(mBoss, p, 0.6, p.getLocation(), "Mechanical Void");
					Vector dir = mCurrentLoc.toVector().clone().subtract(p.getLocation().toVector());
					dir.normalize();
					dir.multiply(1.5f);

					p.setVelocity(dir);

					mDamaged.add(p);
					new BukkitRunnable() {
						@Override
						public void run() {
							mDamaged.remove(p);
						}
					}.runTaskLater(mPlugin, 0);
				}
				if (p.getLocation().getY() - mCurrentLoc.getY() < -6) {
					p.sendMessage(ChatColor.GRAY + "The mechanical void sends you back up.");

					p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 1, 0);
					new PartialParticle(Particle.FIREWORKS_SPARK, p.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15);
					new PartialParticle(Particle.DRAGON_BREATH, p.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.2);

					BossUtils.bossDamagePercent(mBoss, p, 0.4, p.getLocation(), "Mechanical Void");
					p.setVelocity(p.getVelocity().setY(2));
					p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 7, 0, false, true));

					mDamaged.add(p);
					new BukkitRunnable() {
						@Override
						public void run() {
							mDamaged.remove(p);
						}
					}.runTaskLater(mPlugin, 2);
				}
			}
		}
	}

	public void setLocation(Location loc) {
		//Set a few seconds of delay so players get go down
		mCurrentLoc = loc.clone();
		mTimer = -20 * 5; // was -150
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
