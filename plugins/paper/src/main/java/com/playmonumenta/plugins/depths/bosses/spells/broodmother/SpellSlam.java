package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAbstractCircleAttack;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellSlam extends Spell {

	public static final String SPELL_NAME = "Body Slam";
	public static final int COOLDOWN = 200;
	public static final int INTERNAL_COOLDOWN = 800;
	public static final int CAST_TIME = 130;
	public static final int CAST_TIME_A15_DECREASE = 30;
	public static final double ATTACK_RADIUS = 40;
	public static final int TICKS_BEFORE_CAST_END_JUMP = 20;
	public static final int TICKS_BEFORE_CAST_END_GALLOP = 40;
	public static final int WARNING_SOUND_LOWER_BOUND = 50;
	public static final int WARNING_SOUND_MODULO = 20;
	public static final double DAMAGE = 120;
	public static final int TELEGRAPH_UNITS = 180;
	public static final int TELEGRAPH_PULSES = 8;
	public static final double PARTICLE_SPEED = 2;
	public static final List<Material> GROUND_QUAKE_BLOCKS = List.of(Material.TERRACOTTA, Material.PACKED_MUD, Material.BROWN_MUSHROOM_BLOCK, Material.DRIPSTONE_BLOCK);

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final SpellBaseAbstractCircleAttack.CircleInfo mSlam;
	private final SpellBaseAbstractCircleAttack mSlamAttack;
	private final ChargeUpManager mChargeUp;
	private final int mFinalCooldown;
	private final int mFinalCastTime;
	private @Nullable BukkitRunnable mQuakeRunnable = null;

	private boolean mOnCooldown = false;

	public SpellSlam(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mPlugin = Plugin.getInstance();

		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mFinalCastTime = getCastTime(party);

		mSlam = new SpellBaseAbstractCircleAttack.CircleInfo(mBoss.getLocation().subtract(0, 1, 0), ATTACK_RADIUS);
		mSlamAttack = new SpellBaseAbstractCircleAttack(
			mSlam, TELEGRAPH_UNITS, TELEGRAPH_PULSES, mFinalCastTime, PARTICLE_SPEED, Particle.SOUL_FIRE_FLAME, DamageEvent.DamageType.MELEE_SKILL, 0, true,
			true, SPELL_NAME, Particle.CRIT, mPlugin, mBoss,
			(bosss) -> {
				bosss.getWorld().playSound(bosss.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 12f, 0);
				new PartialParticle(Particle.CRIT, mSlam.getCenter().clone().add(0, 0.1, 0), 100).extra(0)
					.delta(mSlam.getRadius() / 2.0).spawnAsEntityActive(mBoss);
			}
		);

		mChargeUp = new ChargeUpManager(mBoss, mFinalCastTime,
			Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.BLUE, TextDecoration.BOLD)),
			BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_20, 100);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
				}
				if (mTicks >= WARNING_SOUND_LOWER_BOUND && mTicks <= mFinalCastTime - TICKS_BEFORE_CAST_END_GALLOP && mTicks % WARNING_SOUND_MODULO == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 12f, 0);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 12f, 0);
				}
				if (mTicks >= mFinalCastTime - TICKS_BEFORE_CAST_END_GALLOP && mTicks < mFinalCastTime - TICKS_BEFORE_CAST_END_JUMP) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_GALLOP, 9f, 2f);
				}
				if (mTicks == mFinalCastTime - TICKS_BEFORE_CAST_END_JUMP) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 9f, 0f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 9f, 0f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 9f, 0f);
				}
				if (mTicks == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 9f, 1.5f);
				}
				mTicks++;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);

		mSlamAttack.run();
		// Play particles at slam time
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (int i = 0; i < 225; i++) {
				double deltaX = (Math.random() * 12) - 6;
				double deltaZ = (Math.random() * 12) - 6;
				Vector center = mSlam.getCenter().toVector().setY(0);
				Vector velocity = center.subtract(new Vector(center.getX() + deltaX, 0, center.getZ() + deltaZ)).normalize().multiply(-1);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mSlam.getCenter().clone().add(deltaX, 0.1, deltaZ), 1).extra(1.5)
					.directionalMode(true).delta(velocity.getX(), 0, velocity.getZ()).spawnAsEntityActive(mBoss);
			}
			new PartialParticle(Particle.EXPLOSION_HUGE, mSlam.getCenter(), 1).extra(0).spawnAsEntityActive(mBoss);
			// Send the quake
			doQuake();
		}, mFinalCastTime);

		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, INTERNAL_COOLDOWN);
	}

	private void doQuake() {
		DisplayEntityUtils.groundBlockQuake(mSlam.getCenter(), mSlam.getRadius(), GROUND_QUAKE_BLOCKS, new Display.Brightness(8, 8), 0.015);
		// Follow the quake and deal damage at its level.
		// Seems to be travelling at ~0.67 blocks per tick, and takes 3 seconds (60 ticks) to reach the 40 block radius.
		mQuakeRunnable = new BukkitRunnable() {
			final Location mSlamLoc = mBoss.getLocation().subtract(0, 1, 0);
			final ArrayList<UUID> mHitPlayers = new ArrayList<>();
			final double mRadiusIncrease = 0.75;
			final int mTicksToComplete = (int) Math.floor(ATTACK_RADIUS / mRadiusIncrease);
			final double mHitSize = 2;

			double mCurrRadius = 1;
			int mTicks = 0;

			@Override
			public void run() {
				// Only target players within mHitSize / 2 of the current radius, so subtract the smaller circle
				// from the bigger circle. Also remove players that have already been hit.
				List<Player> playersToExclude = new Hitbox.UprightCylinderHitbox(mSlamLoc, mHitSize, mCurrRadius - (mHitSize / 2)).getHitPlayers(true);
				List<Player> totalPlayers = new Hitbox.UprightCylinderHitbox(mSlamLoc, mHitSize, mCurrRadius + (mHitSize / 2)).getHitPlayers(true);
				totalPlayers.removeIf(p -> playersToExclude.contains(p) || mHitPlayers.contains(p.getUniqueId()));
				totalPlayers.forEach(this::hitPlayer);

				mTicks++;
				mCurrRadius += mRadiusIncrease;
				if (mTicks >= mTicksToComplete) {
					cancel();
				}
			}

			public void hitPlayer(Player player) {
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, DAMAGE, null, true, false, SPELL_NAME);
				player.setVelocity(player.getVelocity().add(new Vector(0, 0.75, 0)));
				mHitPlayers.add(player.getUniqueId());
			}
		};
		mQuakeRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	public void stopQuake() {
		if (mQuakeRunnable != null && !mQuakeRunnable.isCancelled()) {
			mQuakeRunnable.cancel();
			mQuakeRunnable = null;
		}
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	@Override
	public int castTicks() {
		return mFinalCastTime;
	}

	private int getCastTime(@Nullable DepthsParty party) {
		int castTime = CAST_TIME;
		if (party != null && party.getAscension() >= 15) {
			castTime -= CAST_TIME_A15_DECREASE;
		}
		return castTime;
	}
}
