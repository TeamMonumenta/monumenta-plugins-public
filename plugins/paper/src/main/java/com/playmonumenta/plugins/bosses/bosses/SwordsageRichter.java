package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;


public class SwordsageRichter extends BossAbilityGroup {
	public static final String identityTag = "boss_swordsagerichter";
	public static final int detectionRange = 60;
	private static final Particle.DustOptions BOLT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new SwordsageRichter(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public SwordsageRichter(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);
		World world = mSpawnLoc.getWorld();

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBladeDance(plugin, mBoss),
			new SpellWindWalk(plugin, mBoss),
			new SpellBaseBolt(plugin, mBoss, (int)(20 * 2.5), 30, 1.4, 20, 0.5, false, false, 1, 1,
			                  (Entity entity, int tick) -> {
			                      float t = tick / 10;
			                      world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 3, 0.35, 0.45, 0.35, 0.005);
			                      world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation().add(0, 1, 0), 3, 0.35, 0.45, 0.35, 0.005);
			                      world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2, t);
			                      mBoss.removePotionEffect(PotionEffectType.SLOW);
			                      mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 1));
			                  },

			                  (Entity entity) -> {
			                      world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2, 0);
			                      world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 30, 0.2, 0, 0.2, 0.15);
			                  },

			                  (Location loc) -> {
			                      world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 2);
			                      world.spawnParticle(Particle.CLOUD, loc, 3, 0.05, 0.05, 0.05, 0.03);
			                      world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
			                      world.spawnParticle(Particle.REDSTONE, loc, 40, 0.25, 0.25, 0.25, BOLT_COLOR);
			                  },

			                  (Player player, Location loc, boolean blocked) -> {
			                      if (!blocked) {
			                          BossUtils.bossDamage(mBoss, player, 15);
			                          player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 1));
			                          player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 0));
			                      }
			                      world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.175);
			                  },
							  null)
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellBladeDance(plugin, mBoss),
			new SpellWindWalk(plugin, mBoss),
			new SpellAGoshDamnAirCombo(plugin, mBoss, 20, 20)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
			new SpellProjectileDeflection(mBoss)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"This is the challenger you speak of, master? Very well, let's get this over with quickly.\",\"color\":\"white\"}]");
		});

		events.put(75, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Not bad so far, but at this rate you shouldn't even bother learning my path.\",\"color\":\"white\"}]");
		});

		events.put(50, mBoss -> {
			// Spawn adds
			summonLivingBlades(plugin, mBoss);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Let's make this more interesting shall we? Living Blades, cut down this infidel!\",\"color\":\"white\"}]");
		});

		events.put(30, mBoss -> {
			super.changePhase(phase2Spells, passiveSpells,
			                  (LivingEntity entity) -> {
			                      knockback(plugin, 7);
			                  });
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Agh! I won't lose to a weakling like you!\",\"color\":\"white\"}]");
		});

		events.put(15, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"This is impossible! How are you still standing!?\",\"color\":\"white\"}]");
		});

		events.put(8, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"DAMN YOU! This match ends here! I won't allow myself to be beaten like this! NEVER!\",\"color\":\"white\"}]");
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);


			knockback(plugin, 10);
			new BukkitRunnable() {
				@Override
				public void run() {
					mBoss.remove();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 2, 1);
					world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);

					world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);

					new BukkitRunnable() {
						@Override
						public void run() {

							new BukkitRunnable() {
								int mT = 0;
								boolean mAttacked = false;
								@Override
								public void run() {
									mT++;
									if (mT >= 20 * 2 && !mAttacked) {
										mAttacked = true;
										for (Player player : players) {
											player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
											player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
											world.spawnParticle(Particle.FLAME, player.getLocation(), 200, 0, 0, 0, 0.25);
											world.spawnParticle(Particle.CLOUD, player.getLocation(), 100, 0, 0, 0, 0.25);
											world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 200, 4, 4, 4, 0);
										}
									} else {
										float pitch = mT / 20;
										double offset = 2.5 - pitch;
										for (Player player : players) {
											Location loc = player.getLocation().add(0, 1, 0);
											player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, pitch);
											world.spawnParticle(Particle.SWEEP_ATTACK, loc, 20, offset, offset, offset, 0);
											world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 40, offset, offset, offset, 0);
										}
									}

									if (mT >= 20 * 2.1) {
										this.cancel();
										mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
									}
								}

							}.runTaskTimer(plugin, 0, 2);

						}

					}.runTaskLater(plugin, 20 * 1);
				}

			}.runTaskLater(plugin, 20 * 2);
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (mBoss.getHealth() - event.getFinalDamage() <= 0) {
			event.setCancelled(true);
			event.setDamage(0);
		}
	}

	private void summonLivingBlades(Plugin plugin, LivingEntity mBoss) {
		World world = mBoss.getWorld();

		if (mBoss.isDead()) {
			return;
		}

		for (int t = 0; t < 3; t++) {
			int summonRadius = 5;

			final String mobdata;
			switch (t) {
				case 3:
					// Swift Living Sword
					mobdata = "SwiftLivingBlade";
					break;
				case 2:
					// Fiery Living Sword
					mobdata = "FieryLivingBlade";
					break;
				default:
					// Heavy Living Sword
					mobdata = "HeavyLivingBlade";
					break;
			}

			new BukkitRunnable() {
				Location mLoc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(summonRadius), 1.5, FastUtils.RANDOM.nextInt(summonRadius));
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
						world.spawnParticle(Particle.SPELL_INSTANT, mLoc, 3, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 5, 0.1, 0.1, 0.1, 0.15);
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 8;
					mRadius -= 0.25;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.25f);
						world.spawnParticle(Particle.SPELL_INSTANT, mLoc, 50, 0.1, 0.1, 0.1, 1);
						world.spawnParticle(Particle.CRIT_MAGIC, mLoc, 150, 0.1, 0.1, 0.1, 1);
						LibraryOfSoulsIntegration.summon(mLoc, mobdata);
					}
				}
			}.runTaskTimer(plugin, t * 10, 1);
		}
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.45f);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mDelta = 0.35;

			@Override
			public void run() {

				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.SWEEP_ATTACK, mLoc, 1, 0.1, 0.1, 0.1, 0);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 3, 0.1, 0.1, 0.1, 0.1);
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

		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 650;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		mBoss.addScoreboardTag("Boss");
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Richter\",\"color\":\"aqua\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Expert Swordsage\",\"color\":\"dark_aqua\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

}
