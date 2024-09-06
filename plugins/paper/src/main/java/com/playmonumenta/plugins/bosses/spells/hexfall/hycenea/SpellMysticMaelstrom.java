package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellMysticMaelstrom extends Spell {

	private static final String ABILITY_NAME = "Mystic Maelstrom";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRadius;
	private final int mDamage;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellMysticMaelstrom(Plugin plugin, LivingEntity boss, int range, double radius, int damage, int castTime, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mDamage = damage;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		for (Player viewer : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			viewer.playSound(viewer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1f, 2f);
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mChargeUp.getTime() % 10 == 0) {
					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {

						Location pGround = LocationUtils.fallToGround(p.getLocation(), mSpawnLoc.getY());

						if (HexfallUtils.playersInBossInXZRange(pGround, mRadius, true).stream().filter(player -> !p.equals(player)).toList().isEmpty()) {
							new PPCircle(Particle.REDSTONE, pGround.add(0, 0.25, 0), mRadius)
								.count(35)
								.delta(0.1, 0.05, 0.1)
								.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
								.spawnAsBoss();
							new PPCircle(Particle.FLAME, pGround.add(0, 0.25, 0), mRadius)
								.count(15)
								.delta(0.1, 0.05, 0.1)
								.spawnAsBoss();
							p.playSound(p, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 1f);
						} else {
							new PPCircle(Particle.REDSTONE, pGround.add(0, 0.25, 0), mRadius)
								.count(35)
								.delta(0.1, 0.05, 0.1)
								.data(new Particle.DustOptions(Color.fromRGB(85, 85, 85), 1.65f))
								.spawnAsBoss();
							new PPCircle(Particle.ELECTRIC_SPARK, pGround.add(0, 0.25, 0), mRadius)
								.count(15)
								.delta(0.1, 0.05, 0.1)
								.spawnAsBoss();

							p.playSound(p, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 1.5f);
						}
					}
				}

				if (mChargeUp.nextTick()) {

					BukkitRunnable resolveRunnable = new BukkitRunnable() {
						int mT = 0;
						final int ANIM_TIME = 10;
						final int MAX_HEIGHT = 5;

						@Override
						public void run() {
							if (mBoss.isDead() || !mBoss.isValid()) {
								mChargeUp.reset();
								this.cancel();
								return;
							}


							if (mT == 0) {
								for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
									p.playSound(p, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 1f, 1f);
								}
							}

							if (mT >= ANIM_TIME) {
								this.cancel();

								Set<Player> playersHit = new HashSet<>();
								for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
									Location pGround = LocationUtils.fallToGround(p.getLocation(), mSpawnLoc.getY());
									playersHit.addAll(HexfallUtils.playersInBossInXZRange(pGround, mRadius, true).stream().filter(player -> !p.equals(player)).toList());
								}

								for (Player playerHit : playersHit) {
									DamageUtils.damage(mBoss, playerHit, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);

								}
							}

							for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
								Location pGround = LocationUtils.fallToGround(p.getLocation(), mSpawnLoc.getY());

								new PPCircle(Particle.REDSTONE, pGround.add(0, MAX_HEIGHT - (mT / 2f), 0), mRadius)
									.count(25)
									.delta(0.1, 0.05, 0.1)
									.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
									.spawnAsBoss();
								new PPCircle(Particle.SQUID_INK, pGround.add(0, MAX_HEIGHT - (mT / 2f), 0), mRadius)
									.count(25)
									.delta(0.1, 0.05, 0.1)
									.spawnAsBoss();
								PPCircle indicator2 = new PPCircle(Particle.REDSTONE, p.getLocation(), 0)
									.ringMode(true)
									.count(2)
									.delta(0.25, 0.1, 0.25)
									.data(new Particle.DustOptions(Color.fromRGB(85, 85, 85), 1.65f));
								for (double r = 1; r < mRadius; r++) {
									indicator2.radius(r).location(pGround).spawnAsBoss();
								}
							}

							mT++;
						}
					};
					resolveRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(resolveRunnable);

					mChargeUp.remove();
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
