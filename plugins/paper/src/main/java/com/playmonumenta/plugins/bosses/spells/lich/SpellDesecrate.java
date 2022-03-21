package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
Desecrate - After a 1.5 second delay, each player within 12 blocks of the lich are dealt 20 damage, knockbacked,
and stunned for 3 seconds (Slowness 10, Weakness 10, Negative Jump Boost, Silenced, Potentially put all items on CD?)

A secondary ring will grow from lich to a radius of 15. If marked player(s) are within the secondary circle,
they are dealt an additional 50 damage and the boss is healed for 3% per marked player(s) health -new

Players below 25% max health when the additional damage is dealt are instead killed instantly. -new
 */
public class SpellDesecrate extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private ChargeUpManager mChargeUp;
	private PartialParticle mSmoke;
	private PartialParticle mWitch;
	private PartialParticle mEnch;
	private PartialParticle mHeart;

	public SpellDesecrate(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = new ChargeUpManager(mBoss, 50, ChatColor.YELLOW + "Channeling Desecrate...", BarColor.YELLOW, BarStyle.SOLID, 50);
		mSmoke = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.35, 0, 0.35, 0.005);
		mWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 3, 0.4, 0.4, 0.4, 0);
		mEnch = new PartialParticle(Particle.ENCHANTMENT_TABLE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1);
		mHeart = new PartialParticle(Particle.HEART, mBoss.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.1);
	}

	@Override
	public void run() {
		mChargeUp.setTime(0);
		World world = mBoss.getWorld();

		PPCircle indicator = new PPCircle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 0).ringMode(true).count(12).delta(0.2, 0, 0.2);
		PPCircle indicator2 = new PPCircle(Particle.SPELL_WITCH, mBoss.getLocation(), 0).ringMode(true).count(12).delta(0.2, 0, 0.2);

		PPCircle indicator3 = new PPCircle(Particle.SMOKE_LARGE, mBoss.getLocation(), 0).ringMode(true).count(15).delta(0.2, 0, 0.2);
		PPCircle indicator4 = new PPCircle(Particle.DRAGON_BREATH, mBoss.getLocation(), 0).ringMode(true).count(15).delta(0.2, 0.2, 0.2);

		BukkitRunnable runA = new BukkitRunnable() {
			double mRadius = 12;

			@Override
			public void run() {
				float fTick = mChargeUp.getTime();
				float ft = fTick / 25;
				mSmoke.location(mBoss.getLocation()).spawnAsBoss();
				mWitch.location(mBoss.getLocation().add(0, 1, 0)).spawnAsBoss();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 7, 0.5f + ft);
				Location loc = mBoss.getLocation();

				indicator.radius(mRadius).location(loc).spawnAsBoss();
				indicator2.radius(mRadius).location(loc).spawnAsBoss();

				mRadius -= 0.24;
				mChargeUp.nextTick();
				if (mRadius <= 0 || Lich.phase3over()) {
					List<Player> players = Lich.playersInRange(loc, 12, true);
					players.removeAll(SpellDimensionDoor.getShadowed());
					mChargeUp.setTitle(ChatColor.YELLOW + "Casting Desecrate...");
					mChargeUp.setColor(BarColor.RED);
					for (Player player : players) {
						mSmoke.location(player.getLocation()).spawnAsBoss();
						mWitch.location(player.getLocation().add(0, 1, 0)).spawnAsBoss();
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 6));
						player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 3, -10));
						player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 3, 10));
						AbilityUtils.silencePlayer(player, 3 * 20);
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 15, null, false, true, "Desecrate");
						MovementUtils.knockAway(mBoss.getLocation(), player, 0.1f, false);
					}

					BukkitRunnable runB = new BukkitRunnable() {
						double mRadius = 0;
						boolean mBool = true;
						final Location mBossLoc = loc;
						@Override
						public void run() {
							mChargeUp.setProgress(1 - mRadius / 15.0d);
							List<Player> toRemove = new ArrayList<Player>();
							for (Player p : players) {
								Location pLoc = p.getLocation().add(0, 1, 0);
								Location shiftBossLoc = mBossLoc.clone().add(0, 1.6f, 0);
								Location endLoc = shiftBossLoc.clone();
								Location pHoriLoc = new Location(world, pLoc.getX(), mBossLoc.getY(), pLoc.getZ());

								Vector baseVect = LocationUtils.getVectorTo(pLoc, endLoc);
								baseVect = baseVect.normalize().multiply(0.5);
								//line
								if (mBool) {
									mBool = false;
									for (int i = 0; i < 80; i++) {
										mEnch.location(endLoc).spawnAsBoss();
										endLoc.add(baseVect);

										//stop line
										if (shiftBossLoc.distance(endLoc) >= shiftBossLoc.distance(pLoc)) {
											break;
										}
									}
								} else {
									mBool = true;
								}

								//escape option
								if (pHoriLoc.distance(mBossLoc) > 15 || SpellDimensionDoor.getShadowed().contains(p)) {
									toRemove.add(p);
									p.removePotionEffect(PotionEffectType.SLOW);
									p.removePotionEffect(PotionEffectType.WEAKNESS);
									p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.8f);
									PotionEffect jumpPotionEffect = p.getPotionEffect(PotionEffectType.JUMP);
									if (jumpPotionEffect != null && jumpPotionEffect.getAmplifier() < 0) {
										p.removePotionEffect(PotionEffectType.JUMP);
									}
								}
								//fail to escape
								if (pHoriLoc.distance(mBossLoc) < mRadius) {
									DamageUtils.damage(mBoss, p, DamageType.MAGIC, 65, null, false, true, "Desecrate");
									MovementUtils.knockAway(mBoss.getLocation(), p, 0.5f, false);
									world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.75f);
									world.playSound(mBossLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 0.5f);
									mHeart.location(mBoss.getEyeLocation()).spawnAsBoss();
									double newHealth = mBoss.getHealth() * 1.03;
									mBoss.setHealth(Math.min(newHealth, EntityUtils.getMaxHealth(mBoss)));
									toRemove.add(p);
								}
							}
							players.removeAll(toRemove);
							mRadius += 0.2;
							indicator3.radius(mRadius).location(loc).spawnAsBoss();
							indicator4.radius(mRadius).location(loc).spawnAsBoss();
							if (mRadius >= 15 || Lich.phase3over()) {
								this.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(ChatColor.YELLOW + "Charging Desecrate...");
								mChargeUp.setColor(BarColor.YELLOW);
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
					this.cancel();
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
