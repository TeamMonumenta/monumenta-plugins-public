package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

public class SpellFinalParticle extends Spell {

	private Plugin mPlugin;
	private Location mCenter;
	private double mRange;
	private LivingEntity mBoss;
	private int mCylRadius = 8;
	private boolean mPTick = true;
	private List<Player> mWarned = new ArrayList<Player>();
	private FallingBlock mBlock;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<Player>();
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);

	public SpellFinalParticle(Plugin plugin, LivingEntity boss, Location loc, double range, FallingBlock block) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
		mBlock = block;
	}

	@Override
	public void run() {
		mBlock.setTicksLived(1);
		World world = mBoss.getWorld();
		//smoke ring particle
		PPGroundCircle indicator = new PPGroundCircle(Particle.REDSTONE, mCenter, 30, 0.1, 0.1, 0.1, 0, BLACK).init(mCylRadius, true);
		if (mPTick) {
			mPTick = false;
			for (int j = 0; j < 20; j += 1.5) {
				indicator.location(mCenter.clone().add(0, j, 0)).spawnAsBoss();
			}
		} else {
			mPTick = true;
		}

		//ball particle above lich
		Location ballLoc = mBoss.getLocation().add(0, 4.5, 0);
		new PartialParticle(Particle.REDSTONE, ballLoc, 20, 0.5, 0.5, 0.5, 0, BLACK).spawnAsBoss();
		//hurt players within the circle
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		for (Player p : mPlayers) {
			Location pGroundLoc = p.getLocation();
			pGroundLoc.setY(mCenter.getY());
			if (pGroundLoc.distance(mCenter) < 8) {
				if (!mWarned.contains(p)) {
					mWarned.add(p);
					p.sendMessage(ChatColor.AQUA + "Looks like a dense cloud of miasma formed at the center of the arena.");
				}
				int ndt = p.getNoDamageTicks();
				p.setNoDamageTicks(0);
				BossUtils.bossDamagePercent(mBoss, p, 0.05);
				p.setNoDamageTicks(ndt);
				//death bloom nod >:3
				p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 15, 2));
				p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 15, 2));
				p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 15, 1));
				p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 0));
			}
		}
		//kill all projectiles close to boss
		//prevent easy shooting of corner crystals
		BukkitRunnable run = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				// 1 tick overlap
				if (mT > 6 || Lich.bossDead()) {
					this.cancel();
				}
				Collection<AbstractArrow> projs = mBoss.getLocation().getNearbyEntitiesByType(AbstractArrow.class, 8);
				for (AbstractArrow proj : projs) {
					Location loc = proj.getLocation();
					Vector dir = LocationUtils.getDirectionTo(ballLoc, loc).multiply(0.5);
					double dist = proj.getLocation().distance(ballLoc);
					for (int k = 0; k < 40; k++) {
						Location pLoc = loc.clone().add(dir.multiply(k));
						if (pLoc.distance(loc) > dist) {
							break;
						}
						new PartialParticle(Particle.REDSTONE, pLoc, 2, 0.1, 0.1, 0.1, 0, BLACK).spawnAsBoss();
					}
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1, 1);
					proj.remove();
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
