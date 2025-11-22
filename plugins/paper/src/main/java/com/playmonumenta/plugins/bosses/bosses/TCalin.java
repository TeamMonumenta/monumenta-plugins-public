package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.bosses.spells.SpellBaseBolt;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellDelayedAction;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class TCalin extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_tcalin";
	public static final int detectionRange = 60;

	public TCalin(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);
		final World world = mSpawnLoc.getWorld();
		mBoss.addScoreboardTag("Boss");
		SpellBaseCharge charge = new SpellBaseCharge(mPlugin, mBoss, 20, 25, 160, true,
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(mBoss);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1f, 1.5f);
			},
			// Warning particles
			(Location loc) -> new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0).spawnAsEntityActive(boss),
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 100, 2, 2, 2, 0).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation(), 80, 1, 1, 1, 0).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 20, 1, 1, 1, 0.15).spawnAsEntityActive(mBoss);
				world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 0.85f);
				BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, 14);
				MovementUtils.knockAway(mBoss.getLocation(), player, 0.25f, 0.4f);
			},
			// Attack particles
			(Location loc) -> new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).minimumCount(1).spawnAsEntityActive(boss),
			// Ending particles on boss
			() -> new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 150, 2, 2, 2, 0).spawnAsEntityActive(boss)
		);

		SpellBaseBolt bolt = new SpellBaseBolt(mPlugin, mBoss, (int) (20 * 2.25), 20 * 5, 1.15, detectionRange, 0.5, true, true, 2, 10, null) {
			// Charge-up action
			@Override
			protected void tickAction(Entity entity, int tick) {
				float t = tick / 15.0f;
				if (tick == 1) {
					GlowingManager.startGlowing(mBoss, NamedTextColor.RED, (int) (20 * 2.25), GlowingManager.BOSS_SPELL_PRIORITY);
				}
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 1, 0.35, 0, 0.35, 0.05).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.BLOCK_CRACK, mBoss.getLocation().add(0, 1, 0), 5, 0.25, 0.45, 0.25,
					0.5, Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, t);
				mBoss.removePotionEffect(PotionEffectType.SLOW);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 1));
			}

			@Override
			protected void castAction(Entity entity) {
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 1f);
				new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 80, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 30, 0.2, 0.45, 0.2,
					0.1).spawnAsEntityActive(mBoss);
			}

			@Override
			protected void particleAction(Location loc) {
				new PartialParticle(Particle.BLOCK_CRACK, loc, 10, 0.35, 0.35, 0.35, 0.25,
					Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.2, 0.2, 0.2, 0.125).spawnAsEntityActive(mBoss);
			}

			@Override
			protected void intersectAction(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) {
				if (!blocked && player != null) {
					BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 12, prevLoc);
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 7, 1));
				}
				new PartialParticle(Particle.BLOCK_CRACK, loc, 125, 0.35, 0.35, 0.35, 1,
					Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1.25f);
			}
		};

		SpellDelayedAction aoe = new SpellDelayedAction(mPlugin, mBoss.getLocation(), 20 * 3,
			// Start
			(Location loc) -> {
				mBoss.removePotionEffect(PotionEffectType.SLOW);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1));
				new BukkitRunnable() {
					float mJ = 0;
					double mRotation = 0;
					double mRadius = 5;

					@Override
					public void run() {
						Location loc = mBoss.getLocation();
						mJ++;
						new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 2, 0.25, 0.45, 0.25).spawnAsEntityActive(mBoss);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 0.5f + (mJ / 20));
						for (int i = 0; i < 5; i++) {
							double radian1 = Math.toRadians(mRotation + (72 * i));
							loc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
							new PartialParticle(Particle.SPELL_WITCH, loc, 6, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.SPELL_MOB, loc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
							loc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						}
						mRotation += 10;
						mRadius -= 0.1;
						if (mRadius <= 0 || mBoss.isDead()) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			},
			// Warning
			(Location loc) -> {

			},
			// End
			(Location loc) -> {
				new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
				knockback(5);
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 5, true)) {
					BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 16);
				}
			}
		);

		SpellManager activeSpells = new SpellManager(Arrays.asList(bolt, charge));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(bolt, charge, aoe));

		List<Spell> passiveSpells = List.of(
			new SpellBaseAura(mBoss, 8, 5,
				// Summon particles on boss
				(final Entity launcher) ->
					new PartialParticle(Particle.FALLING_DUST, launcher.getLocation().add(0, 1, 0)).count(2)
						.delta(1).data(Material.ANVIL.createBlockData()).spawnAsEntityActive(launcher),
				// Effect to apply to each player
				(final Player player) ->
					player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, true)),
				false
			)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> sendDialogue("Thank you for foolishly opening the way into this town, hero."));

		events.put(50, mBoss -> {
			sendDialogue("Know the true power of the Sons of the Forest!");
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1f);
			new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);
			mBoss.teleport(mSpawnLoc);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 0f);
			new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
			super.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			new BukkitRunnable() {
				float mJ = 0;
				double mRotation = 0;
				double mRadius = 10;
				final Location mLoc = mBoss.getLocation();

				@Override
				public void run() {
					mJ++;
					new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 2, 0.25, 0.45, 0.25, 0).spawnAsEntityActive(mBoss);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 0.5f + (mJ / 20));
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(mRotation + (72 * i));
						mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						new PartialParticle(Particle.SPELL_WITCH, mLoc, 6, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SPELL_MOB, mLoc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 10;
					mRadius -= 0.25;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1, 1.5f);
						new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45,
							0.25, 0.15).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1,
							0.10).spawnAsEntityActive(mBoss);
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						changePhase(SpellManager.EMPTY, passiveSpells, null);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> changePhase(phase2Spells, passiveSpells, null), 20 * 10);
					}
				}

			}.runTaskTimer(mPlugin, 30, 1);
		});

		events.put(5, mBoss -> sendDialogue("This cannot be!"));

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, events);
		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, bossBar);
	}

	private void knockback(double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.4f, false);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYMinus = 0.35;

			@Override
			public void run() {
				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SPELL_WITCH, mLoc, 2, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 2, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

				}
				mY -= mY * mYMinus;
				mYMinus += 0.02;
				if (mYMinus >= 1) {
					mYMinus = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 5, 0.8f);
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		final int baseHealth = 512;
		final double bossTargetHp = baseHealth * BossUtils.healthScalingCoef(players.size(), 0.5, 0.5);
		EntityUtils.setMaxHealthAndHealth(mBoss, bossTargetHp);

		for (Player player : players) {
			MessagingUtils.sendBoldTitle(player, Component.text("T'Calin", NamedTextColor.GREEN), Component.text("Forest Battlemage", NamedTextColor.DARK_GREEN));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 1.25f);
		}
	}

	private void sendDialogue(String msg) {
		PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
			.sendMessage(Component.text("[T'Calin] ", NamedTextColor.GOLD)
				.append(Component.text(msg, NamedTextColor.WHITE)));
	}
}
