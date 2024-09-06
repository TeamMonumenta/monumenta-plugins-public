package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellRazorVine extends Spell {

	private static final String ABILITY_NAME = "Razor Vine";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mDamage;
	private final int mCooldown;
	private final int mCastTime;
	private final double mHitboxSize;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellRazorVine(Plugin plugin, LivingEntity boss, int range, int cooldown, int damage, int castTime, double hitboxSize, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mDamage = damage;
		mCastTime = castTime;
		mHitboxSize = hitboxSize;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, mCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		EntityUtils.selfRoot(mBoss, mCastTime);

		BukkitRunnable runnable = new BukkitRunnable() {

			List<Player> mPlayers = HexfallUtils.getPlayersInRuten(mSpawnLoc);

			@Override
			public void run() {
				mPlayers = mPlayers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());

				if (mChargeUp.getTime() % 5 == 0) {
					for (Player player : mPlayers) {
						player.playSound(player.getLocation(), Sound.BLOCK_GRASS_FALL, SoundCategory.HOSTILE, 2f, 0f);
						player.playSound(player.getLocation(), Sound.BLOCK_WART_BLOCK_PLACE, SoundCategory.HOSTILE, 2f, 1f);
						player.playSound(player.getLocation(), Sound.BLOCK_WART_BLOCK_BREAK, SoundCategory.HOSTILE, 2f, 1.5f);
					}
				}

				if (mChargeUp.getTime() % 2 == 0) {
					for (Player player : mPlayers) {
						Vector dir = LocationUtils.getDirectionTo(player.getLocation(), mBoss.getLocation()).setY(0);

						for (int i = 1; i <= Ruten.arenaRadius * 2; i++) {
							Location shifted = mBoss.getLocation().clone().add(0, 1, 0).add(dir.clone().multiply(i));

							if (shifted.distanceSquared(mSpawnLoc) > Ruten.arenaRadius * Ruten.arenaRadius) {
								break;
							} else {
								new PartialParticle(Particle.COMPOSTER, shifted)
									.count(1)
									.spawnAsBoss();
							}
						}
					}
				}

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.remove();

					List<Player> hitPlayers = new ArrayList<>();
					for (Player player : mPlayers) {
						final Vector dir = LocationUtils.getDirectionTo(player.getLocation(), mBoss.getLocation()).setY(0).multiply(Ruten.arenaRadius * 2);
						BoundingBox mHitbox = BoundingBox.of(mBoss.getLocation().clone().add(0, mHitboxSize / 2, 0), mHitboxSize, mHitboxSize, mHitboxSize);
						final World world = mBoss.getWorld();

						world.playSound(mHitbox.getCenter().toLocation(world), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1, 0);
						world.playSound(mHitbox.getCenter().toLocation(world), Sound.BLOCK_AZALEA_BREAK, SoundCategory.HOSTILE, 1, 0);

						ParticleUtils.drawCurve(mHitbox.getCenter().toLocation(mBoss.getWorld()), 1, 500, dir.clone().normalize(),
							t -> 0.075 * t,
							t -> mHitboxSize * FastUtils.cosDeg(50 * t),
							t -> mHitboxSize * FastUtils.sinDeg(50 * t),
							(l, t) -> {
								if (l.distance(mSpawnLoc) < Ruten.arenaRadius) {
									new PartialParticle(Particle.CLOUD, l, 1).spawnAsBoss();
								}
							}
						);

						ParticleUtils.drawCurve(mHitbox.getCenter().toLocation(mBoss.getWorld()), 1, 500, dir.clone().normalize(),
							t -> 0.075 * t,
							t -> mHitboxSize * FastUtils.sinDeg(50 * t),
							t -> mHitboxSize * FastUtils.cosDeg(50 * t),
							(l, t) -> {
								if (l.distance(mSpawnLoc) < Ruten.arenaRadius) {
									new PartialParticle(Particle.CLOUD, l, 1).spawnAsBoss();
								}
							}
						);

						for (int i = 0; i < 50; i++) {
							mHitbox = mHitbox.shift(dir.clone().multiply(0.02));
							for (Player p : mPlayers) {
								if (!p.equals(player)) {
									if (p.getBoundingBox().overlaps(mHitbox) && !hitPlayers.contains(p)) {
										hitPlayers.add(p);
									}
								} else if (mPlayers.size() == 1) {
									if (p.getBoundingBox().overlaps(mHitbox) && !hitPlayers.contains(p)) {
										p.sendMessage(Component.text("Alone, Ru'Ten concentrates all its anima directly on you...").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC));
										hitPlayers.add(p);
									}
								}
							}
						}
					}

					for (Player toHitPlayer : hitPlayers) {
						DamageUtils.damage(mBoss, toHitPlayer, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
					}
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
