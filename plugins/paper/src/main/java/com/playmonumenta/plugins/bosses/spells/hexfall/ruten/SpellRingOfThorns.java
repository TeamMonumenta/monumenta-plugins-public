package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellRingOfThorns extends Spell {

	private static final String ABILITY_NAME = "Ring of Thorns";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRadius;
	private final int mDamage;
	private final int mTargetCount;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;
	private final BlockData mDripStoneData = Material.DRIPSTONE_BLOCK.createBlockData();
	private final BlockData mJungleLeavesData = Material.JUNGLE_LEAVES.createBlockData();

	public SpellRingOfThorns(Plugin plugin, LivingEntity boss, int radius, int damage, int castTime, int targetCount, int range, int cooldown, Location centerLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mDamage = damage;
		mTargetCount = targetCount;
		mCooldown = cooldown;
		mSpawnLoc = centerLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		final List<Player> targets = HexfallUtils.getPlayersInRuten(mSpawnLoc).stream().limit(mTargetCount).collect(Collectors.toList());

		BukkitRunnable runnable = new BukkitRunnable() {

			List<Player> mPlayers = targets;

			@Override
			public void run() {
				mPlayers = mPlayers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());

				if (mChargeUp.getTime() % 4 == 0) {
					for (Player target : mPlayers) {
						Location fixY = target.getLocation().clone();
						fixY.setY(mSpawnLoc.getY());
						new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc, Math.min(fixY.distance(mSpawnLoc), Ruten.arenaRadius))
							.countPerMeter(1)
							.data(mDripStoneData)
							.ringMode(true)
							.spawnAsBoss();
						new PPCircle(Particle.DUST_COLOR_TRANSITION, mSpawnLoc, Math.min(fixY.distance(mSpawnLoc), Ruten.arenaRadius))
							.countPerMeter(1)
							.data(new Particle.DustTransition(
								Color.fromRGB(77, 51, 44),
								Color.BLACK,
								1.5f
							))
							.ringMode(true)
							.spawnAsBoss();
						target.playSound(target, Sound.BLOCK_WOOD_FALL, SoundCategory.HOSTILE, 1f, 2f);
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					mPlayers.forEach(p -> {
						p.playSound(p.getLocation(), Sound.BLOCK_MOSS_BREAK, 10.5f, 1);
						p.playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK, 12.5f, 0.75f);
					});

					for (Player target : mPlayers) {
						Location fixY = target.getLocation().clone();
						fixY.setY(mSpawnLoc.getY());
						double distanceFromCenter = Math.min(fixY.distance(mSpawnLoc), Ruten.arenaRadius);
						double innerRad = distanceFromCenter - mRadius;
						double outerRad = distanceFromCenter + mRadius;

						for (double i = innerRad; i < outerRad; i += 0.25) {
							new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(0, 0.5, 0), i)
								.data(mJungleLeavesData)
								.ringMode(true)
								.count(3)
								.spawnAsBoss();
							new PPCircle(Particle.BLOCK_CRACK, mSpawnLoc.clone().add(0, 0.5, 0), i)
								.data(mDripStoneData)
								.ringMode(true)
								.count(3)
								.spawnAsBoss();
						}

						for (Player p : mPlayers.stream().filter(player -> LocationUtils.xzDistance(player.getLocation(), mSpawnLoc) <= outerRad && LocationUtils.xzDistance(player.getLocation(), mSpawnLoc) >= innerRad)
							.collect(Collectors.toList())) {
							if (!p.equals(target)) {
								DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, false, true, ABILITY_NAME);
								MovementUtils.knockAway(mSpawnLoc, p, 0, .4f, true);
							} else if (mPlayers.size() == 1) {
								p.sendMessage(Component.text("Alone, Ru'Ten concentrates all its anima directly on you...").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC));
								DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, false, true, ABILITY_NAME);
								MovementUtils.knockAway(mSpawnLoc, p, 0, .4f, true);
							}

						}
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
