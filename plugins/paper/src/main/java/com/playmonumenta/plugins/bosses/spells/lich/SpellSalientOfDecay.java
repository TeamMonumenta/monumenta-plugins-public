package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

/*
Salient of Decay - A straight piercing line appears from the lich at ⅓  targets, after 1 second
anyone within that line takes 16 damage every second for 6 seconds. Players are
also unable to heal for 4 seconds.
 */
public class SpellSalientOfDecay extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private static final Particle.DustOptions SALIENT_OF_DECAY_COLOR = new Particle.DustOptions(Color.fromRGB(3, 163, 116), 1f);
	private int mCap = 12;
	private ChargeUpManager mChargeUp;
	private int mTell = 40;
	private PartialParticle mDust;
	private PartialParticle mSmoke;
	private PartialParticle mWitch;

	public SpellSalientOfDecay(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = new ChargeUpManager(mBoss, mTell, ChatColor.YELLOW + "Channeling Salient of Decay...", BarColor.RED, BarStyle.SOLID, 50);
		mDust = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0, SALIENT_OF_DECAY_COLOR);
		mSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0.25);
		mWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 2, 0.25, 0.25, 0.25, 1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 5, 0.4f);
		List<Player> players = Lich.playersInRange(mBoss.getLocation(), 50, true);
		if (SpellDimensionDoor.getShadowed() != null && SpellDimensionDoor.getShadowed().size() > 0) {
			players.removeAll(SpellDimensionDoor.getShadowed());
		}
		List<Player> targets = new ArrayList<Player>();
		if (players.size() <= 2) {
			targets = players;
		} else {
			Collections.shuffle(players);
			targets = players.subList(0, (int) Math.min(players.size(), Math.max(2, Math.min(mCap, Math.ceil(players.size() / 3)))));
		}

		BukkitRunnable runA = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mChargeUp.nextTick();
				if (Lich.phase3over()) {
					this.cancel();
					mChargeUp.reset();
				} else if (mT >= mTell) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 5, 0.4f);
					this.cancel();
					mChargeUp.reset();
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);

		for (Player player : targets) {
			BukkitRunnable runB = new BukkitRunnable() {
				Vector mDir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), mBoss.getLocation().add(0, 1.25, 0));
				int mT = 0;
				@Override
				public void run() {
					mT += 2;
					Location loc = mBoss.getLocation().add(0, 1.25, 0);
					for (int i = 0; i < 40; i++) {
						loc.add(mDir.clone().multiply(0.75));
						mDust.location(loc).spawnAsBoss();
					}

					if (Lich.phase3over()) {
						this.cancel();
					} else if (mT >= mTell) {
						BoundingBox box = BoundingBox.of(mBoss.getLocation().add(0, 1.25, 0), 0.75, 0.75, 0.75);
						for (int i = 0; i < 40; i++) {
							box.shift(mDir.clone().multiply(0.75));
							Location bLoc = box.getCenter().toLocation(world);
							mWitch.location(bLoc).spawnAsBoss();
							mSmoke.location(bLoc).spawnAsBoss();
							Iterator<Player> it = players.iterator();
							while (it.hasNext()) {
								Player p = it.next();
								if (p.getBoundingBox().overlaps(box)) {
									AbilityUtils.increaseHealingPlayer(p, 20 * 6, -1.0, "Lich");
									p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 6, 0));
									BukkitRunnable runC = new BukkitRunnable() {
										int mT = 0;
										@Override
										public void run() {
											if (SpellDimensionDoor.getShadowed().contains(p)) {
												p.removePotionEffect(PotionEffectType.WITHER);
												this.cancel();
											}
											mT++;
											int ndt = p.getNoDamageTicks();
											p.setNoDamageTicks(0);
											Vector velocity = p.getVelocity();
											BossUtils.bossDamage(mBoss, p, 16, null, "Salient of Decay"); //16 dmg every sec, 6 seconds
											p.setVelocity(velocity);
											p.setNoDamageTicks(ndt);
											if (mT >= 6 || p.isDead()) {
												this.cancel();
											}
										}
									};
									runC.runTaskTimer(mPlugin, 0, 20);
									mActiveRunnables.add(runC);
									it.remove();
								}
							}
						}
						this.cancel();
					}
				}

			};
			runB.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runB);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}

}
