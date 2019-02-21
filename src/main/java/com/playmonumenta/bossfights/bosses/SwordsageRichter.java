package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.BossBarManager.BossHealthAction;
import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellAGoshDamnAirCombo;
import com.playmonumenta.bossfights.spells.SpellBaseBolt;
import com.playmonumenta.bossfights.spells.SpellBladeDance;
import com.playmonumenta.bossfights.spells.SpellConditionalTeleport;
import com.playmonumenta.bossfights.spells.SpellProjectileDeflection;
import com.playmonumenta.bossfights.spells.SpellWindWalk;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;


public class SwordsageRichter extends BossAbilityGroup {
	public static final String identityTag = "boss_swordsagerichter";
	public static final int detectionRange = 60;
	private static final Particle.DustOptions BOLT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private final Random rand = new Random();

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
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		World world = mSpawnLoc.getWorld();

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBladeDance(plugin, mBoss),
			new SpellWindWalk(plugin, mBoss),
			new SpellBaseBolt(plugin, mBoss, (int)(20 * 2.5), 30, 1.4, 20, 0.5, false,
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
			                          player.damage(15, boss);
			                          player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 1));
			                          player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 0));
			                      }
			                      world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.175);
			                  })
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
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"This is the challenger you speak of, master? Very well, let's get this over with quickly.\",\"color\":\"white\"}]"));
		});

		events.put(75, mBoss -> {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Not bad so far, but at this rate you shouldn't even bother learning my path.\",\"color\":\"white\"}]"));
		});

		events.put(50, mBoss -> {
			// Spawn adds
			summonLivingBlades(plugin, mBoss);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Let's make this more interesting shall we? Living Blades, cut down this infidel!\",\"color\":\"white\"}]"));
		});

		events.put(30, mBoss -> {
			super.changePhase(phase2Spells, passiveSpells,
			                  (LivingEntity entity) -> {
			                      knockback(plugin, 7);
			                  });
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"Agh! I won't lose to a weakling like you!\",\"color\":\"white\"}]"));
		});

		events.put(15, mBoss -> {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"This is impossible! How are you still standing!?\",\"color\":\"white\"}]"));
		});

		events.put(8, mBoss -> {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), Utils.getExecuteCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Richter] \",\"color\":\"gold\"},{\"text\":\"DAMN YOU! This match ends here! I won't allow myself to be beaten like this! NEVER!\",\"color\":\"white\"}]"));
			List<Player> players = Utils.playersInRange(mBoss.getLocation(), detectionRange);
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
								int t = 0;
								boolean attacked = false;
								@Override
								public void run() {
									t++;
									if (t >= 20 * 2 && !attacked) {
										attacked = true;
										for (Player player : players) {
											player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
											player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
											world.spawnParticle(Particle.FLAME, player.getLocation(), 200, 0, 0, 0, 0.25);
											world.spawnParticle(Particle.CLOUD, player.getLocation(), 100, 0, 0, 0, 0.25);
											world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 200, 4, 4, 4, 0);
										}
									} else {
										float pitch = t / 20;
										double offset = 2.5 - pitch;
										for (Player player : players) {
											Location loc = player.getLocation().add(0, 1, 0);
											player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, pitch);
											world.spawnParticle(Particle.SWEEP_ATTACK, loc, 20, offset, offset, offset, 0);
											world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 40, offset, offset, offset, 0);
										}
									}

									if (t >= 20 * 2.1) {
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
		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, bossBar);
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
			int summon_radius = 5;

			final String mobdata;
			switch (t) {
				case 3:
					// Swing Living Sword
					mobdata = "{Tags:[\"boss_livingblade\"],Silent:1b,Health:30f,CustomName:\"{\\\"text\\\":\\\"Swift Living Blade\\\"}\",HandItems:[{id:\"minecraft:iron_sword\",Count:1b,tag:{Damage:4}},{}],ActiveEffects:[{Id:14b,Amplifier:1b,Duration:120000,ShowParticles:0b}],Attributes:[{Name:generic.maxHealth,Base:30},{Name:generic.movementSpeed,Base:0.425}]}";
					break;
				case 2:
					// Fiery Living Sword
					mobdata = "{Tags:[\"boss_livingblade\"],Silent:1b,Health:40f,CustomName:\"{\\\"text\\\":\\\"Fiery Living Blade\\\"}\",HandItems:[{id:\"minecraft:golden_sword\",Count:1b,tag:{HideFlags:4,Unbreakable:1b,Damage:6,Enchantments:[{id:\"minecraft:fire_aspect\",lvl:2}]}},{}],ActiveEffects:[{Id:14b,Amplifier:1b,Duration:120000,ShowParticles:0b}],Attributes:[{Name:generic.maxHealth,Base:40},{Name:generic.movementSpeed,Base:0.325}]}";
					break;
				default:
					// Heavy Living Sword
					mobdata = "{Tags:[\"boss_livingblade\"],Silent:1b,Health:80f,CustomName:\"{\\\"text\\\":\\\"Heavy Living Blade\\\"}\",HandItems:[{id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:16}},{}],ActiveEffects:[{Id:14b,Amplifier:1b,Duration:120000,ShowParticles:0b}],Attributes:[{Name:generic.maxHealth,Base:80},{Name:generic.knockbackResistance,Base:0.5},{Name:generic.movementSpeed,Base:0.3}]}";
					break;
			}

			new BukkitRunnable() {
				Location loc = mBoss.getLocation().add(rand.nextInt(summon_radius), 1.5, rand.nextInt(summon_radius));
				double rotation = 0;
				double radius = 4;
				@Override
				public void run() {
					if (mBoss.isDead()) {
						this.cancel();
						return;
					}

					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(rotation + (72 * i));
						loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
						world.spawnParticle(Particle.SPELL_INSTANT, loc, 3, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 5, 0.1, 0.1, 0.1, 0.15);
						loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
					}
					rotation += 8;
					radius -= 0.25;
					if (radius <= 0) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.25f);
						world.spawnParticle(Particle.SPELL_INSTANT, loc, 50, 0.1, 0.1, 0.1, 1);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 150, 0.1, 0.1, 0.1, 1);
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:zombie " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + mobdata);
					}
				}
			}.runTaskTimer(plugin, t * 10, 1);
		}
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
		for (Player player : Utils.playersInRange(mBoss.getLocation(), r)) {
			Utils.KnockAway(mBoss.getLocation(), player, 0.45f);
		}
		new BukkitRunnable() {
			double rotation = 0;
			Location loc = mBoss.getLocation();
			double radius = 0;
			double y = 2.5;
			double yminus = 0.35;

			@Override
			public void run() {

				radius += 1;
				for (int i = 0; i < 15; i += 1) {
					rotation += 24;
					double radian1 = Math.toRadians(rotation);
					loc.add(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);
					world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 3, 0.1, 0.1, 0.1, 0.1);
					loc.subtract(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);

				}
				y -= y * yminus;
				yminus += 0.02;
				if (yminus >= 1) {
					yminus = 1;
				}
				if (radius >= r) {
					this.cancel();
				}

			}

		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 650;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0) {
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.addScoreboardTag("Boss");
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Richter\",\"color\":\"aqua\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Expert Swordsage\",\"color\":\"dark_aqua\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

}
