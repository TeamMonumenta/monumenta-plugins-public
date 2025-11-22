package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellAGoshDamnAirCombo;
import com.playmonumenta.plugins.bosses.spells.SpellBaseBolt;
import com.playmonumenta.plugins.bosses.spells.SpellBladeDance;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellProjectileDeflection;
import com.playmonumenta.plugins.bosses.spells.SpellWindWalk;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public final class SwordsageRichter extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_swordsagerichter";
	public static final int detectionRange = 60;

	private static final String SLOWNESS_SRC = "SwordsageRichterSlowness";
	private static final String WEAKNESS_SRC = "SwordsageRichterWeakness";
	private static final int DEBUFF_DURATION = 20 * 6;
	private static final Particle.DustOptions BOLT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	public SwordsageRichter(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);
		World world = mSpawnLoc.getWorld();

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBladeDance(mPlugin, mBoss),
			new SpellWindWalk(mPlugin, mBoss),
			new SpellBaseBolt(mPlugin, mBoss, (int) (20 * 2.5), 30, 1.4, 20, 0.5, false, false, 1, 1, null) {
				@Override
				protected void tickAction(Entity entity, int tick) {
					float t = tick / 10f;
					if (tick == 1) {
						GlowingManager.startGlowing(boss, NamedTextColor.RED, (int) (20 * 2.5), GlowingManager.BOSS_SPELL_PRIORITY);
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, boss.getLocation().add(0, 1, 0), 3, 0.35, 0.45, 0.35, 0.005).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SWEEP_ATTACK, boss.getLocation().add(0, 1, 0), 3, 0.35, 0.45, 0.35, 0.005).spawnAsEntityActive(boss);
					world.playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2, t);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(20 * 2, -0.3));
				}

				@Override
				protected void castAction(Entity entity) {
					world.playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2, 0);
					new PartialParticle(Particle.EXPLOSION_NORMAL, boss.getLocation().add(0, 1, 0), 30, 0.2, 0, 0.2, 0.15).spawnAsEntityActive(boss);
				}

				@Override
				protected void particleAction(Location loc) {
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 2);
					new PartialParticle(Particle.CLOUD, loc, 3, 0.05, 0.05, 0.05, 0.03).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.REDSTONE, loc, 40, 0.25, 0.25, 0.25, BOLT_COLOR).spawnAsEntityActive(boss);
				}

				@Override
				protected void intersectAction(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) {
					if (!blocked && player != null) {
						BossUtils.blockableDamage(boss, player, DamageType.PROJECTILE, 15, prevLoc);
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
							new PercentSpeed(DEBUFF_DURATION, -0.3, SLOWNESS_SRC));
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, WEAKNESS_SRC,
							new PercentDamageDealt(DEBUFF_DURATION, -0.1));
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.175).spawnAsEntityActive(boss);
				}
			}
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellBladeDance(mPlugin, mBoss),
			new SpellWindWalk(mPlugin, mBoss),
			new SpellAGoshDamnAirCombo(mPlugin, mBoss, 20, 20)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellConditionalTeleport(mBoss, mSpawnLoc, b -> mSpawnLoc.distance(b.getLocation()) > 80),
			new SpellProjectileDeflection(mBoss)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			for (Player player : PlayerUtils.playersInRange(spawnLoc, detectionRange, true)) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "This is the challenger you speak of, master? Very well, let's get this over with quickly.");
			}
		});

		events.put(75, mBoss -> {
			for (Player player : PlayerUtils.playersInRange(spawnLoc, detectionRange, true)) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "Not bad so far, but at this rate you shouldn't even bother learning my path.");
			}
		});

		events.put(50, mBoss -> {
			// Spawn adds
			summonLivingBlades(mBoss);
			for (Player player : PlayerUtils.playersInRange(spawnLoc, detectionRange, true)) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "Let's make this more interesting shall we? Living Blades, cut down this outsider!");
			}
		});

		events.put(30, mBoss -> {
			super.changePhase(phase2Spells, passiveSpells, (LivingEntity entity) -> knockback(7));
			for (Player player : PlayerUtils.playersInRange(spawnLoc, detectionRange, true)) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "Agh! I won't lose to a weakling like you!");
			}
		});

		events.put(15, mBoss -> {
			for (Player player : PlayerUtils.playersInRange(spawnLoc, detectionRange, true)) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "This is impossible! How are you still standing!?");
			}
		});

		events.put(8, mBoss -> {
			final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);

			for (Player player : players) {
				com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Richter", "DAMN YOU! This match ends here! I won't allow myself to be beaten like this! NEVER!");
			}
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);
			knockback(10);

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mBoss.remove();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 2, 1);
				new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(boss);

				new BukkitRunnable() {
					int mT = 0;
					boolean mAttacked = false;

					@Override
					public void run() {
						mT++;
						if (mT >= 20 * 2 && !mAttacked) {
							mAttacked = true;
							for (Player player : players) {
								player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);
								player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.5f);
								new PartialParticle(Particle.FLAME, player.getLocation(), 200, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
								new PartialParticle(Particle.CLOUD, player.getLocation(), 100, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
								new PartialParticle(Particle.SWEEP_ATTACK, player.getLocation(), 200, 4, 4, 4, 0).spawnAsEntityActive(boss);
							}
						} else {
							float pitch = mT / 20f;
							double offset = 2.5 - pitch;
							for (Player player : players) {
								Location loc = player.getLocation().add(0, 1, 0);
								player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, pitch);
								new PartialParticle(Particle.SWEEP_ATTACK, loc, 20, offset, offset, offset, 0).spawnAsEntityActive(boss);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, offset, offset, offset, 0).spawnAsEntityActive(boss);
							}
						}

						if (mT >= 20 * 2.1) {
							this.cancel();
							mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						}
					}
				}.runTaskTimer(mPlugin, 20, 2);
			}, 20 * 2);
		});
		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mBoss.getHealth() - event.getFinalDamage(true) <= 0) {
			event.setCancelled(true);
			event.setFlatDamage(0);
		}
	}

	private void summonLivingBlades(LivingEntity mBoss) {
		World world = mBoss.getWorld();

		if (mBoss.isDead()) {
			return;
		}

		for (int t = 0; t < 3; t++) {
			int summonRadius = 5;

			final String mobdata = switch (t) {
				case 2 -> "SwiftLivingBlade";
				case 1 -> "FieryLivingBlade";
				default -> "HeavyLivingBlade";
			};

			new BukkitRunnable() {
				final Location mLoc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(summonRadius), 1.5, FastUtils.RANDOM.nextInt(summonRadius));
				double mRotation = 0;
				double mRadius = 4;

				@Override
				public void run() {
					if (mBoss.isDead()) {
						this.cancel();
						return;
					}

					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(mRotation + (72 * i));
						mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						new PartialParticle(Particle.SPELL_INSTANT, mLoc, 3, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.CRIT_MAGIC, mLoc, 5, 0.1, 0.1, 0.1, 0.15).spawnAsEntityActive(mBoss);
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 8;
					mRadius -= 0.25;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 1.25f);
						new PartialParticle(Particle.SPELL_INSTANT, mLoc, 50, 0.1, 0.1, 0.1, 1).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.CRIT_MAGIC, mLoc, 150, 0.1, 0.1, 0.1, 1).spawnAsEntityActive(mBoss);
						LibraryOfSoulsIntegration.summon(mLoc, mobdata);
					}
				}
			}.runTaskTimer(mPlugin, t * 10L, 1);
		}
	}

	private void knockback(double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.45f, false);
		}
		new BukkitRunnable() {
			final Location mLoc = mBoss.getLocation();
			double mRotation = 0;
			double mRadius = 0;
			double mY = 2.5;
			double mDelta = 0.35;

			@Override
			public void run() {
				mRadius++;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SWEEP_ATTACK, mLoc, 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 3, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

				}
				mY -= mY * mDelta;
				mDelta += 0.02;
				if (mDelta >= 1) {
					mDelta = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void init() {
		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		final int baseHealth = 650;
		final double bossTargetHp = baseHealth * BossUtils.healthScalingCoef(players.size(), 0.5, 0.5);
		mBoss.addScoreboardTag("Boss");
		EntityUtils.setMaxHealthAndHealth(mBoss, bossTargetHp);

		for (Player player : players) {
			MessagingUtils.sendBoldTitle(player, Component.text("Richter", NamedTextColor.AQUA), Component.text("Expert Swordsage", NamedTextColor.DARK_AQUA));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}
}
