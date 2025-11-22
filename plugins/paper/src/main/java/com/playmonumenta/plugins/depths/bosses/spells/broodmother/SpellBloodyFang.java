package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
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

public class SpellBloodyFang extends Spell {

	public static final String SPELL_NAME = "Bloody Fang";
	public static final Vector[] MODEL_OFFSETS = {
		new Vector(2, 0, 0),
		new Vector(0, 0, 2),
		new Vector(-2, 0, 0),
		new Vector(0, 0, -2),
		new Vector(0, -4, 0)
	};
	public final ChargeUpManager mChargeUp;
	public static final Color START_COLOR = Color.WHITE;
	public static final Color END_COLOR = Color.RED;
	public static final Particle.DustOptions END_DUST_OPTIONS = new Particle.DustOptions(END_COLOR, 2f);
	public static final int CAST_TIME = 50;
	public static final int CAST_TIME_A15_DECREASE = 10;
	public static final int TELEGRAPH_COUNT = 5;
	public static final float TELEGRAPH_SOUND_PITCH = 1;
	public static final float TELEGRAPH_SOUND_PITCH_INCREASE = 0.2f;
	public static final int SPAWN_HEIGHT = Broodmother.GROUND_Y_LEVEL + 20;
	public static final int FALL_TIME_TICKS = 24;
	public static final int FALL_DISTANCE = SPAWN_HEIGHT - Broodmother.GROUND_Y_LEVEL;
	public static final int COOLDOWN = 200;
	public static final int INTERNAL_COOLDOWN = 400;
	public static final double DAMAGE = 70;
	public static final double DAMAGE_RADIUS = 5;
	public static final int FANG_COUNT = 1;
	public static final int FANG_COUNT_A15_INCREASE = 2;
	public static final int FANG_INTERVAL = 10;
	public static final List<Material> GROUND_QUAKE_BLOCKS = List.of(Material.TERRACOTTA, Material.PACKED_MUD, Material.BROWN_MUSHROOM_BLOCK, Material.DRIPSTONE_BLOCK);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mFinalCooldown;
	private final int mFinalFangCount;
	private final int mFinalCastTime;
	private final int mFinalTelegraphModulo;

	private boolean mOnCooldown = false;

	public SpellBloodyFang(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mPlugin = Plugin.getInstance();

		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mFinalFangCount = getFangCount(party);
		mFinalCastTime = getCastTime(party);
		mFinalTelegraphModulo = mFinalCastTime / TELEGRAPH_COUNT;

		mChargeUp = new ChargeUpManager(mBoss, mFinalCastTime,
			Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.RED, TextDecoration.BOLD)),
			BossBar.Color.RED, BossBar.Overlay.PROGRESS, 100);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		// Telegraph the fang, damocles sword
		new BukkitRunnable() {
			int mTicks = 0;
			float mCurrentPitch = TELEGRAPH_SOUND_PITCH;

			@Override
			public void run() {
				mChargeUp.nextTick();
				if (mTicks % mFinalTelegraphModulo == 0) {
					List<Location> centers = PlayerUtils.playersInRange(mBoss.getLocation(), 100, true).stream().map(Player::getLocation).toList();
					for (Location center : centers) {
						// Circle on the ground
						Location groundCenter = center.clone();
						groundCenter.setY(Broodmother.GROUND_Y_LEVEL);
						new PPCircle(Particle.REDSTONE, groundCenter, DAMAGE_RADIUS).data(END_DUST_OPTIONS).countPerMeter(1).spawnAsBoss();
						// Fang
						Location fangCenter = center.clone();
						fangCenter.setY(SPAWN_HEIGHT);
						drawFang(fangCenter);
					}
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 8f, mCurrentPitch);
					mCurrentPitch += TELEGRAPH_SOUND_PITCH_INCREASE;
				}
				if (mTicks >= mFinalCastTime) {
					// Drop it like it's hot
					new BukkitRunnable() {
						int mRuns = 0;

						@Override
						public void run() {
							dropFang();
							mRuns++;
							if (mRuns >= mFinalFangCount) {
								cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, FANG_INTERVAL);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, INTERNAL_COOLDOWN);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void dropFang() {
		List<Location> centers = mBoss.getLocation().getNearbyPlayers(60).stream().filter(p -> !p.getGameMode().equals(GameMode.SPECTATOR)).map(Player::getLocation).toList();
		for (Location center : centers) {
			Location startCenter = center.clone();
			startCenter.setY(SPAWN_HEIGHT);
			// Telegraph
			Location telegraphLoc = center.clone();
			telegraphLoc.setY(Broodmother.GROUND_Y_LEVEL);
			new PPCircle(Particle.REDSTONE, telegraphLoc, DAMAGE_RADIUS).data(END_DUST_OPTIONS).countPerMeter(1).spawnAsBoss();
			new BukkitRunnable() {
				int mTicks = 0;
				final Location mCenter = startCenter;
				final double mFallAmount = (double) FALL_DISTANCE / (double) FALL_TIME_TICKS;

				@Override
				public void run() {
					drawFang(mCenter);
					mCenter.add(0, -mFallAmount, 0);

					mTicks++;
					if (mTicks >= FALL_TIME_TICKS) {
						// Do damage
						mBoss.getWorld().playSound(mCenter, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1f, 0.5f);
						mBoss.getWorld().playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 0.7f);
						new PartialParticle(Particle.CLOUD, mCenter, 125).delta(0.5).extra(0.5).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.BLOCK_CRACK, mCenter, 125).delta((DAMAGE_RADIUS - 1) / 2, 0, (DAMAGE_RADIUS - 1) / 2)
							.data(Material.DIRT.createBlockData()).extra(0.5).spawnAsEntityActive(mBoss);
						// Circle on the ground again
						new PPCircle(Particle.REDSTONE, mCenter, DAMAGE_RADIUS).data(END_DUST_OPTIONS).countPerMeter(1).spawnAsBoss();
						// Block quake, making sure it spawns at the correct height of the floor
						mCenter.setY(Broodmother.GROUND_Y_LEVEL);
						DisplayEntityUtils.groundBlockQuake(mCenter, DAMAGE_RADIUS, GROUND_QUAKE_BLOCKS, new Display.Brightness(8, 8), 0.02);
						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mCenter, FALL_DISTANCE, DAMAGE_RADIUS);
						List<Player> hitPlayers = hitbox.getHitPlayers(true);
						for (Player hitPlayer : hitPlayers) {
							DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.PROJECTILE_SKILL, DAMAGE, null, true, false, SPELL_NAME);
						}
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void drawFang(Location center) {
		int units = 10;
		for (int i = 0; i < MODEL_OFFSETS.length - 1; i++) {
			ParticleUtils.drawLine(center.clone().add(MODEL_OFFSETS[i]), center.clone().add(MODEL_OFFSETS[4]), units,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1).extra(0).data(
					new Particle.DustOptions(ParticleUtils.getTransition(START_COLOR, END_COLOR, t / (double) units), 2)
				).spawnAsBoss()
			);
		}
	}

	private int getFangCount(@Nullable DepthsParty party) {
		int fangCount = FANG_COUNT;
		if (party != null && party.getAscension() >= 15) {
			fangCount += FANG_COUNT_A15_INCREASE;
		}
		return fangCount;
	}

	private int getCastTime(@Nullable DepthsParty party) {
		int castTime = CAST_TIME;
		if (party != null && party.getAscension() >= 15) {
			castTime -= CAST_TIME_A15_DECREASE;
		}
		return castTime;
	}
}
