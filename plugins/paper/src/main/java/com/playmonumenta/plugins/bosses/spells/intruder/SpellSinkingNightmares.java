package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSinkingNightmares extends Spell {
	private static final String SPELL_NAME = "Sinking Nightmares";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final IntruderBoss.Dialogue mDialogue;

	private static final int RANGE = 24;
	private static final double RADIUS = 4;
	private static final int DAMAGE = 110;
	private static final int TELEGRAPH_DURATION = 30;
	private static final int CHARGE_TIME = 4 * 20;
	private static final int DURATION = 12 * 20;
	private static final DamageEvent.DamageType damageType = DamageEvent.DamageType.MAGIC;

	private final ChargeUpManager mChargeUpManager;

	public SpellSinkingNightmares(Plugin plugin, LivingEntity boss, Location center, IntruderBoss.Dialogue dialogue) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;

		mDialogue = dialogue;
		mChargeUpManager = new ChargeUpManager(mBoss, CHARGE_TIME, Component.text("Preparing ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
	}

	@Override
	public void run() {
		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(CHARGE_TIME);
		mChargeUpManager.setTitle(Component.text("Preparing ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));

		mBoss.setAI(false);
		mBoss.setInvulnerable(true);

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Location location = mBoss.getLocation();
			location.setY(mCenter.getY() + 4);
			mBoss.teleport(location);
		}, CHARGE_TIME));

		mDialogue.dialogue(40, List.of(
			"THE CONNECTION. IS FADING.",
			"YET. THERE IS STILL TIME.",
			"YOUR MIND. WILL. BE MINE.")
		);

		mActiveTasks.add(new BukkitRunnable() {
			boolean mStarted = false;

			@Override
			public void run() {
				if (!mStarted) {
					if (mChargeUpManager.nextTick()) {
						mChargeUpManager.setTime(DURATION);
						mChargeUpManager.setChargeTime(DURATION);
						mChargeUpManager.setTitle(Component.text("Unleashing  ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
						mStarted = true;
					}
				} else {
					if (mChargeUpManager.previousTick()) {
						mChargeUpManager.remove();
						this.cancel();
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						Location location = mBoss.getLocation();
						location.setY(mCenter.getY());
						mBoss.teleport(location);

					} else if ((mChargeUpManager.getTime() + 1) % 10 == 0) {
						List<Player> players = IntruderBoss.playersInRange(mBoss.getLocation());
						for (int i = 0; i < 4 - players.size(); i++) {
							summonNightmare(LocationUtils.randomLocationInCircle(mCenter, RANGE));
						}
						players.forEach(player -> {
							Location location = LocationUtils.randomLocationInCircle(player.getLocation(), RADIUS / 1.5);
							location.setY(mCenter.getY());
							summonNightmare(location);
						});
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}


	public void summonNightmare(Location location) {
		Location loc = location.clone();
		new BukkitRunnable() {
			private int mTicks = 0;
			private final World mBossWorld = mBoss.getWorld();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(20)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(1, 0, mTicks == 20 ? 0.5 : -0.5)
						.extra(0.1)
						.spawnAsBoss();
					new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(30)
						.rotateDelta(true)
						.directionalMode(true)
						.spawnAsBoss();
				}
				if (mTicks % 10 == 0) {
					mBossWorld.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.66f);
					mBossWorld.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.7f);
					new PPCircle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(12)
						.spawnAsBoss();
				}
				if (mTicks == TELEGRAPH_DURATION - 10) {
					new PPLightning(Particle.DUST_COLOR_TRANSITION, loc)
						.count(2)
						.data(new Particle.DustTransition(Color.BLACK, Color.GRAY, 2.4f))
						.hopsPerBlock(1).hopXZ(1).hopY(0).maxWidth(1).height(16)
						.duration(10)
						.spawnAsBoss();
				}
				if (mTicks >= TELEGRAPH_DURATION) {
					// starfall
					new PPCircle(Particle.SQUID_INK, loc.clone().add(0, 0.15, 0), 0.5)
						.count(20)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(0.15, 0, 0)
						.extra(RADIUS)
						.spawnAsBoss();
					mBossWorld.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.2f);
					mBossWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 0.5f);
					new PartialParticle(Particle.FLASH, loc).minimumCount(1).spawnAsBoss();

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc, RADIUS, RADIUS);
					hitbox.getHitPlayers(true).forEach(player -> {
						BossUtils.blockableDamage(mBoss, player, damageType, DAMAGE, loc);
						MovementUtils.knockAway(loc, player, 0.5f, 0.65f);
					});

					this.cancel();
				}
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return CHARGE_TIME + DURATION + 2 * 20;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
