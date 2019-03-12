package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.BossBarManager.BossHealthAction;
import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellBaseCharge;
import com.playmonumenta.bossfights.spells.SpellDelayedAction;
import com.playmonumenta.bossfights.spells.SpellTpBehindRandomPlayer;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;

public class CrownbearerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_crownbearer";
	public static final int detectionRange = 60;

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private final Random rand = new Random();
	private final String sotf = "{HurtByTimestamp:274,Attributes:[{Base:24.0d,Name:\"generic.maxHealth\"},{Base:0.0d,Name:\"generic.knockbackResistance\"},{Base:0.28d,Name:\"generic.movementSpeed\"},{Base:2.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:35.0d,Name:\"generic.followRange\"},{Base:3.0d,Name:\"generic.attackDamage\"},{Base:0.024851952672861142d,Name:\"zombie.spawnReinforcements\"}],Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,InWaterTime:-1,FallDistance:0.0f,DeathTime:0s,WorldUUIDMost:-1041596277173696703L,HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Spigot.ticksLived:496,ConversionTime:-1,Motion:[0.0d,-0.0784000015258789d,0.0d],Leashed:0b,Health:22.1462f,Bukkit.updateLevel:2,LeftHanded:0b,Paper.AAAB:[-1132.582911225707d,182.0d,-1069.7724767288335d,-1131.9829112018651d,183.95000004768372d,-1069.1724767049916d],Air:300s,OnGround:1b,Dimension:0,Rotation:[93.105225f,0.0f],Paper.ShouldBurnInDay:1b,HandItems:[{id:\"minecraft:wooden_sword\",Count:1b,tag:{Enchantments:[{lvl:1,id:\"minecraft:unbreaking\"}],Damage:0}},{id:\"minecraft:jungle_sapling\",Count:1b}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],Profession:5,CustomName:\"{\\\"text\\\":\\\"Son of the Forest\\\"}\",Passengers:[{shake:0b,xTile:0,Invulnerable:0b,PortalCooldown:0,FallDistance:0.0f,WorldUUIDMost:-1041596277173696703L,zTile:0,yTile:0,id:\"minecraft:potion\",Spigot.ticksLived:496,Motion:[0.0d,-0.05000000074505806d,0.0d],UUIDLeast:-6032565129635564387L,Potion:{id:\"minecraft:lingering_potion\",Count:1b,tag:{CustomPotionEffects:[{Duration:240,Id:5,Amplifier:0},{Duration:20,Id:7,Amplifier:1},{Duration:200,Id:9,Amplifier:0}],Potion:\"minecraft:awkward\"}},Bukkit.updateLevel:2,inGround:0b,Paper.AAAB:[-1132.407911213786d,183.4625000357628d,-1069.5974767169125d,-1132.157911213786d,183.7125000357628d,-1069.3474767169125d],Air:0s,OnGround:0b,Dimension:0,Rotation:[0.0f,0.0f],UUIDMost:-9057466973911038820L,Pos:[-1132.282911213786d,183.4625000357628d,-1069.4724767169125d],Fire:0s,WorldUUIDLeast:-7560693509725274339L,Paper.Origin:[-1128.2814645405856d,183.0d,-1067.2047218221555d]}],Pos:[-1132.282911213786d,182.0d,-1069.4724767169125d],CanBreakDoors:0b,Fire:-1s,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:3060485},Damage:0}},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:3060485},Damage:0}},{id:\"minecraft:grass\",Count:1b}],CanPickUpLoot:0b,HurtTime:0s,Paper.FromMobSpawner:1b,WorldUUIDLeast:-7560693509725274339L,DrownedConversionTime:-1,Paper.Origin:[-1128.2814645405856d,183.0d,-1067.2047218221555d]}";


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CrownbearerBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public CrownbearerBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		World world = mSpawnLoc.getWorld();
		mBoss.addScoreboardTag("Boss");

		SpellBaseCharge charge = new SpellBaseCharge(plugin, mBoss, 20, 25,
			(Player player) -> {
				boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
			},
			// Warning particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0);
			},
			// Charge attack sound/particles at boss location
			(Player player) -> {
				boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
			},
			// Attack hit a player
			(Player player) -> {
				player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 20, 1, 1, 1, 0);
				player.damage(14, boss);
			},
			// Attack particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.75, 0.75, 0.75, 0);
			},
			// Ending particles on boss
			() -> {
				boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 150, 2, 2, 2, 0);
			}
		);

		SpellBaseCharge multidash = new SpellBaseCharge(plugin, mBoss, 20, 25, false, 3, 15,
			(Player player) -> {
				boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
			},
			// Warning particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0);
			},
			// Charge attack sound/particles at boss location
			(Player player) -> {
				boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
			},
			// Attack hit a player
			(Player player) -> {
				player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 20, 1, 1, 1, 0);
				player.damage(7, boss);
			},
			// Attack particles
			(Location loc) -> {
				loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.75, 0.75, 0.75, 0);
			},
			// Ending particles on boss
			() -> {
				boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 150, 2, 2, 2, 0);
			}
		);

		SpellDelayedAction aoe = new SpellDelayedAction(plugin, mBoss.getLocation(), 40,
			//Start
			(Location loc) -> {
				mBoss.removePotionEffect(PotionEffectType.SLOW);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1));

			},
			//Warning
			(Location loc) -> {
				world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation().add(0, 1, 0), 25, 4, 4,
				4, 0.175);
				world.spawnParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 5,
				4, 4, 4, 0.05);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
			},
			//End
			(Location loc) -> {
				world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation().add(0, 1, 0), 150, 4, 4,
				4, 0.175);
				world.spawnParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 100,
				4, 4, 4, 0.175);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
				boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 2f);
				for (Player player : Utils.playersInRange(mBoss.getLocation(), 4)) {
					player.damage(16, mBoss);
					Utils.KnockAway(mBoss.getLocation(), player, 0.2f);
				}
			}
		);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			charge, new SpellTpBehindRandomPlayer(plugin, mBoss, 20 * 6)
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			multidash, new SpellTpBehindRandomPlayer(plugin, mBoss, 20 * 5), aoe
		));

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			Utils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"So my identity has been revealed? No matter, I'll take out you and the King in one fell swoop!\",\"color\":\"white\"}]");
		});
		events.put(75, mBoss -> {
			Utils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Don't underestimate me! After you fall, so will the King, and all of Sierhaven with him!\",\"color\":\"white\"}]");
		});
		events.put(50, mBoss -> {
			// TODO: Summon mobs here
			new BukkitRunnable() {
				int t = 0;
				int summon_radius = 5;
				@Override
				public void run() {
					t++;
					Location loc = mBoss.getLocation().add(rand.nextInt(summon_radius), 1.5, rand.nextInt(summon_radius));
					summonSOTF(loc);
					world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45,
					0.25, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45,
					0, 0.15);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
					if (t >= 4) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 30, 10);
			Utils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Sons of the Forest, come to me! Let us conquer this place once and for all!\",\"color\":\"white\"}]");
		});
		events.put(30, mBoss -> {
			knockback(plugin, 6);
			changePhase(phase2Spells, null, (LivingEntity entity) -> {

			});
			Utils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Agh! This battle ends here and now! I will not let you stall this any longer!\",\"color\":\"white\"}]");
		});
		events.put(20, mBoss -> {
			new BukkitRunnable() {
				int t = 0;
				int summon_radius = 5;
				@Override
				public void run() {
					t++;
					Location loc = mBoss.getLocation().add(rand.nextInt(summon_radius), 1.5, rand.nextInt(summon_radius));
					summonSOTF(loc);
					world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45,
					0.25, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45,
					0, 0.15);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
					if (t >= 5) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 15, 7);
			Utils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"My allies, aid me! Let us finish this fight!\",\"color\":\"white\"}]");
		});

		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BarColor.GREEN, BarStyle.SEGMENTED_10, events);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, bossBar);
	}

	private void summonSOTF(Location loc) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:zombie_villager " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + sotf);
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
	public void death() {
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.death master @s ~ ~ ~ 100 0.8");
		Utils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Damn you... The King... Must meet... His...\",\"color\":\"white\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 512;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0) {
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Onyx Crownbearer\",\"color\":\"gold\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The King's Assassinator\",\"color\":\"dark_red\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 1.25");
	}

}
