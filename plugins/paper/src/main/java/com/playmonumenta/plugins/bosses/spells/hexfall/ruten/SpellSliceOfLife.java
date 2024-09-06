package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSliceOfLife extends Spell {

	private static final String ABILITY_NAME = "Slice of Life";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCastTime;
	private final int mDamage;
	private final int mRange;
	private final int mCooldown;
	private final double mAngle;
	private final double mLockInPercentage;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellSliceOfLife(Plugin plugin, LivingEntity boss, int castTime, int damage, int range, int cooldown, Location centerLoc, int angle, double lockInPercentage) {
		mPlugin = plugin;
		mBoss = boss;
		mCastTime = castTime;
		mDamage = damage;
		mRange = range;
		mCooldown = cooldown;
		mSpawnLoc = centerLoc;
		mAngle = angle;
		mLockInPercentage = lockInPercentage;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange * 2);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mChargeUp.reset();

		EntityUtils.selfRoot(mBoss, mCastTime);

		List<Player> players = HexfallUtils.getPlayersInRuten(mSpawnLoc);
		if (players.isEmpty()) {
			return;
		}
		Collections.shuffle(players);
		Player initialTarget = players.get(0);

		for (Player p : players) {
			p.sendMessage(Component.text("A safe haven surrounds ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC).append(Component.text(initialTarget.getName()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			final Player mTarget = initialTarget;
			Vector mVec = LocationUtils.getDirectionTo(LocationUtils.fallToGround(mTarget.getLocation(), mBoss.getLocation().getY()), mBoss.getLocation()).setY(0);

			@Override
			public void run() {

				if (mChargeUp.getTime() % 20 == 0 && mChargeUp.getTime() <= mChargeUp.getChargeTime() * mLockInPercentage) {

					if (HexfallUtils.playerInBoss(mTarget)) {
						mVec = LocationUtils.getDirectionTo(LocationUtils.fallToGround(mTarget.getLocation(), mBoss.getLocation().getY()), mBoss.getLocation()).setY(0);
					}

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(-mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.PUMPKIN.createBlockData())
						.spawnAsBoss();

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.PUMPKIN.createBlockData())
						.spawnAsBoss();

					mBoss.setRotation(mVec.toLocation(mBoss.getWorld()).getYaw(), mBoss.getLocation().getPitch());

					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.HOSTILE, 1.5f, 1.2f);
						player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, SoundCategory.HOSTILE, 1.3f, 1.2f);
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1f, 0.6f);
					}
				}

				if (mChargeUp.getTime() % 4 == 0 && mChargeUp.getTime() > mChargeUp.getChargeTime() * mLockInPercentage) {

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(-mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.FIRE_CORAL_BLOCK.createBlockData())
						.spawnAsBoss();

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.FIRE_CORAL_BLOCK.createBlockData())
						.spawnAsBoss();

					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.5f, 1.2f);
					}
				}

				if (mChargeUp.nextTick()) {

					world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1f, 1.5f);

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(-mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.FIRE_CORAL_BLOCK.createBlockData())
						.spawnAsBoss();

					new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mVec.clone().rotateAroundY(Math.toRadians(mAngle / 2)), mRange)
						.countPerMeter(10)
						.data(Material.FIRE_CORAL_BLOCK.createBlockData())
						.spawnAsBoss();

					new PPCircle(Particle.BLOCK_DUST, mBoss.getLocation(), mRange)
						.countPerMeter(5)
						.ringMode(true)
						.data(Material.FIRE_CORAL_BLOCK.createBlockData())
						.spawnAsBoss();

					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1f);
						player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, SoundCategory.HOSTILE, 1f, 2f);

						Vector vec = LocationUtils.getDirectionTo(player.getLocation(), mBoss.getLocation());
						double playerDegrees = VectorUtils.vectorToRotation(vec)[0] % 360;
						double coneDegrees = VectorUtils.vectorToRotation(mVec)[0] % 360;
						if (Math.abs(playerDegrees - coneDegrees) > mAngle / 2 && LocationUtils.xzDistance(player.getLocation(), mBoss.getLocation()) <= mRange) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
						}
					}

					mVec.rotateAroundY(Math.toRadians(mAngle / 2));

					Location l = mBoss.getLocation();

					for (double degree = mAngle; degree < 360; degree += 2) {
						mVec.rotateAroundY(Math.toRadians(2));

						if (degree % 10 == 0) {
							new PPLine(Particle.SWEEP_ATTACK, mBoss.getLocation(), mBoss.getLocation().clone().add(mVec.clone().multiply(mRange).setY(0)))
								.count(10)
								.spawnAsBoss();
							new PPLine(Particle.BLOCK_DUST, mBoss.getLocation(), mBoss.getLocation().clone().add(mVec.clone().multiply(mRange).setY(0)))
								.count(10)
								.data(Material.DIRT.createBlockData())
								.spawnAsBoss();
							world.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 2f);
						}
					}

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
