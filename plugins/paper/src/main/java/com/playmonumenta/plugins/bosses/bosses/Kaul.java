package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellArachnopocolypse;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthsWrath;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellGroundSurge;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellKaulsJudgement;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStorm;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellLightningStrike;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPutridPlague;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellVolcanicDemise;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

/* Woah it's Kaul! */

/*
Phase 1 :
Attacks :
Raise Jungle
Arachnopocalypse
Putrid plague
Earth’s Wrath


Phase 2 :
Earth’s wrath
Putrid Plague
Raise Jungle
Kaul’s Judgement

Phase 2.5 (50% health) :
Summons a powerful Primordial Elemental that is invulnerable and immovable until out of the ground. Players will have 15 seconds to prepare for the elemental’s arrival. Kaul will not be attacking or casting any abilities (except for his passives) during this time. (512 health)

Elemental’s Abilities:
Normal Block break passive
Raise Jungle (Kaul’s ability), however the timer for raising them will be 30 seconds instead of 40.
Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that spans out 5 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.
Stone Blast: After 1 second, fires at all players a powerful block breaking bolt. Intersecting with a player causes 15 damage and applies Weakness II and Slowness II. Intersecting with a block causes a TNT explosion to happen instead. The bolt will stop travelling if it hits a player or a block.
Once the elemental is dead, Kaul returns to the fight. The elemental will meld into the ground for later return in Phase 3.5

Phase 3:
Earth’s Wrath
Putrid plague
Volcanic demise
Kaul’s Judgement

Phase 3.5 (20% health [Let’s make this even harder shall we?]) :
The Primordial Elemental from Phase 2.5 returns, however he is completely invulnerable to all attacks, and gains Strength I for the rest of the fight. The elemental will remain active until the end of the fight.
The elemental will lose his “Raise Jungle” ability, but will still possess the others.

 *
 */
/*
 * Base Spells:
 * /
 * Volcanic Demise (Magma cream): Kaul shoots at each player at the
 * same time, casting particle fireballs at each player with a large
 * hit radius. On contact with a player, deals 20 damage and ignites
 * the player for 10 seconds. If the player shields the fireball, the
 * shield takes 50% durability damage and is put on cooldown for 30
 * seconds (The player is still set on fire). On contact with a block,
 * explodes and leaves a fiery aura that deals 10 damage and ignites
 * for 5 seconds (+3 seconds for fire duration if a player is already on
 * fire) to players who stand in it. The aura lasts 5 seconds.
 * (Aka Disco Inferno)
 */
public class Kaul extends BossAbilityGroup {
	public static final String identityTag = "boss_kaul";
	public static final int detectionRange = 50;
	private static final String primordial = "{CustomName:\"{\\\"text\\\":\\\"§6Primordial Elemental\\\"}\",Health:120.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Boots\\\"}\"},Damage:0}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Leggings\\\"}\"},Damage:0}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Vest\\\"}\"},Damage:0}},{id:\"minecraft:brown_terracotta\",Count:1b,tag:{Enchantments:[{lvl:8s,id:\"minecraft:projectile_protection\"}],AttributeModifiers:[{UUIDMost:-4385518367071189805L,UUIDLeast:-8720188027200143741L,Amount:16.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:2698543145384691203L,UUIDLeast:-5523831565464878560L,Amount:0.15d,Slot:\"head\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}}],Attributes:[{Base:768.0d,Name:\"generic.maxHealth\"}],Tags:[\"Boss\",\"boss_kaulprimoridal\"],Team:\"kaulele\"}";
	private static final String immortal = "{CustomName:\"{\\\"text\\\":\\\"§6Immortal Elemental\\\"}\",Health:120.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Boots\\\"}\"},Damage:0}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Leggings\\\"}\"},Damage:0}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:8608560,Name:\"{\\\"text\\\":\\\"§fHobnailed Vest\\\"}\"},Damage:0}},{id:\"minecraft:brown_terracotta\",Count:1b,tag:{Enchantments:[{lvl:8s,id:\"minecraft:projectile_protection\"}],AttributeModifiers:[{UUIDMost:-4385518367071189805L,UUIDLeast:-8720188027200143741L,Amount:16.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:2698543145384691203L,UUIDLeast:-5523831565464878560L,Amount:0.15d,Slot:\"head\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}}],Attributes:[{Base:768.0d,Name:\"generic.maxHealth\"}],Tags:[\"Boss\",\"boss_kaulimmortal\"],Team:\"kaulele\"}";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private boolean defeated = false;
	private boolean cooldown = false;
	private boolean primordialPhase = false;
	private int hits = 0;
	private final Random rand = new Random();

	private static final String LIGHTNING_STORM_TAG = "KaulLightningStormTag";
	private static final String PUTRID_PLAGUE_TAG_RED = "KaulPutridPlagueRed";
	private static final String PUTRID_PLAGUE_TAG_BLUE = "KaulPutridPlagueBlue";
	private static final String PUTRID_PLAGUE_TAG_YELLOW = "KaulPutridPlagueYellow";
	private static final String PUTRID_PLAGUE_TAG_GREEN = "KaulPutridPlagueGreen";
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);
	private LivingEntity mCenter;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Kaul(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Kaul(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");
		for (Entity e : boss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(LIGHTNING_STORM_TAG) && e instanceof LivingEntity) {
				mCenter = (LivingEntity) e;
				break;
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange)) {
					if (player.isSleeping()) {
						BossUtils.bossDamage(mBoss, player, 22);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 1));
						player.sendMessage(ChatColor.DARK_GREEN + "THE JUNGLE FORBIDS YOU TO DREAM.");
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1, 0.85f);
					}
				}
				if (defeated || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
		SpellManager phase1Spells = new SpellManager(
		    Arrays.asList(new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 9, 20 * 10, mCenter.getLocation().getY()),
		                  new SpellPutridPlague(mPlugin, mBoss, detectionRange, false, mCenter.getLocation()),
		                  new SpellEarthsWrath(mPlugin, mBoss, mCenter.getLocation().getY()),
		                  new SpellArachnopocolypse(mPlugin, mBoss, mCenter.getLocation(), detectionRange)));

		Spell judgement = SpellKaulsJudgement.getInstance(mSpawnLoc);

		SpellManager phase2Spells = new SpellManager(
		    Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange / 2, false, mCenter.getLocation()),
		                  new SpellEarthsWrath(mPlugin, mBoss, mCenter.getLocation().getY()),
		                  new SpellRaiseJungle(mPlugin, mBoss, 10, detectionRange, 20 * 8, 20 * 10, mCenter.getLocation().getY()),
		                  new SpellGroundSurge(mPlugin, mBoss, detectionRange),
		                  judgement));

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange);

		SpellManager phase3Spells = new SpellManager(
		    Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange / 2, true, mCenter.getLocation()),
		                  new SpellEarthsWrath(mPlugin, mBoss, mCenter.getLocation().getY()),
		                  new SpellVolcanicDemise(plugin, mBoss, 40D, mCenter.getLocation()),
		                  new SpellGroundSurge(mPlugin, mBoss, detectionRange),
		                  judgement));

		SpellManager phase4Spells = new SpellManager(
			    Arrays.asList(new SpellPutridPlague(mPlugin, mBoss, detectionRange / 2, true, mCenter.getLocation()),
			                  new SpellEarthsWrath(mPlugin, mBoss, mCenter.getLocation().getY()),
			                  new SpellVolcanicDemise(plugin, mBoss, 40D, mCenter.getLocation()),
			                  new SpellGroundSurge(mPlugin, mBoss, detectionRange)));

		List<UUID> hit = new ArrayList<UUID>();

		List<UUID> cd = new ArrayList<UUID>();
		SpellPlayerAction action = new SpellPlayerAction(mBoss, detectionRange, (Player player) -> {
			Vector loc = player.getLocation().toVector();
			if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mCenter.getLocation().toVector(), 42)) {
				if (player.getLocation().getY() >= 61 || cd.contains(player.getUniqueId())) {
					return;
				}
				// Damage has no direction so can't be blocked */
				if (BossUtils.bossDamagePercent(mBoss, player, 0.4, (Location)null)) {
					/* Player survived the damage */
					MovementUtils.knockAway(mSpawnLoc, player, -2.5f, 0.85f);
					world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1, 1.3f);
					world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 80, 0.25, 0.45, 0.25, 0.15);
					cd.add(player.getUniqueId());
					new BukkitRunnable() {
						@Override
						public void run() {
							cd.remove(player.getUniqueId());
						}
					}.runTaskLater(mPlugin, 10);
				}
				if (player.getLocation().getBlock().isLiquid()) {
					if (!hit.contains(player.getUniqueId())) {
						hit.add(player.getUniqueId());
						player.sendMessage(ChatColor.AQUA + "That hurt! It seems like the water is extremely corrosive. Best to stay out of it.");
					}
				} else if (!loc.isInSphere(mCenter.getLocation().toVector(), 42)) {
					player.sendMessage(ChatColor.AQUA + "You feel a powerful force pull you back in fiercely. It seems there's no escape from this fight.");
				}
			}
		});

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
			}),
			new SpellLightningStrike(plugin, boss, mCenter.getLocation(), detectionRange, 20 * 18, 3),
			new SpellLightningStorm(boss, detectionRange),
			new SpellPurgeNegatives(mBoss, 20 * 6),
			new SpellConditionalTeleport(mBoss, spawnLoc,
										 b -> b.getLocation().getBlock().getType() == Material.BEDROCK
										 || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
										 || b.getLocation().getBlock().getType() == Material.LAVA
										 || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
			}),
			new SpellLightningStrike(plugin, boss, mCenter.getLocation(), detectionRange, 20 * 12, 3),
			new SpellLightningStorm(boss, detectionRange),
			new SpellPurgeNegatives(mBoss, 20 * 3),
			new SpellConditionalTeleport(mBoss, spawnLoc,
										 b -> b.getLocation().getBlock().getType() == Material.BEDROCK
										 || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
										 || b.getLocation().getBlock().getType() == Material.LAVA
										 || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase3PassiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
				world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, 0.1);
				world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, RED_COLOR);
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.BLUE_WOOL.createBlockData());
			}),
			new SpellLightningStrike(plugin, boss, mCenter.getLocation(), detectionRange, 20 * 10, 2),
			new SpellLightningStorm(boss, detectionRange),
			new SpellPurgeNegatives(mBoss, 2),
			new SpellConditionalTeleport(mBoss, spawnLoc,
										 b -> b.getLocation().getBlock().getType() == Material.BEDROCK
										 || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
										 || b.getLocation().getBlock().getType() == Material.LAVA
										 || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		List<Spell> phase4PassiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.GREEN_CONCRETE.createBlockData());
				world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, 0.1);
				world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35, 0.45,
				0.35, RED_COLOR);
				world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 2, 0.35,
				0.45, 0.35, Material.BLUE_WOOL.createBlockData());
			}),
			new SpellLightningStrike(plugin, boss, mCenter.getLocation(), detectionRange, 20 * 6, 2),
			new SpellLightningStorm(boss, detectionRange),
			new SpellPurgeNegatives(mBoss, 2),
			new SpellConditionalTeleport(mBoss, spawnLoc,
										 b -> b.getLocation().getBlock().getType() == Material.BEDROCK
										 || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
										 || b.getLocation().getBlock().getType() == Material.LAVA
										 || b.getLocation().getBlock().getType() == Material.WATER), action
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			if (players.size() == 1) {
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL NOT ALLOW A LONE MORTAL LIKE YOU TO LIVE. PERISH, FOOLISH USUPRER!\",\"color\":\"dark_green\"}]");
			} else {
				PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL TAKE YOUR PRESENCE NO MORE. PERISH, USUPRERS.\",\"color\":\"dark_green\"}]");
			}
		});

		events.put(75, mBoss -> {
			forceCastSpell(SpellArachnopocolypse.class);
		});

		// Phase 2
		events.put(66, mBoss -> {
			changePhase(null, null, null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));

			new BukkitRunnable() {
				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {
						Random rand = new Random();
						Location loc = mBoss.getLocation();
						float j = 0;
						double rotation = 0;
						double radius = 10;

						@Override
						public void run() {
							j++;
							world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 3, 0.5f + (j / 25));
							for (int i = 0; i < 5; i++) {
								double radian1 = Math.toRadians(rotation + (72 * i));
								loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
								world.spawnParticle(Particle.SPELL_WITCH, loc, 6, 0.25, 0.25, 0.25, 0);
								world.spawnParticle(Particle.BLOCK_DUST, loc, 4, 0.25, 0.25, 0.25, 0.25,
								Material.COARSE_DIRT.createBlockData());
								loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
							}
							world.spawnParticle(Particle.SPELL_WITCH, mCenter.getLocation().add(0, 3, 0), 20, 8, 5, 8,
							                    0);
							rotation += 8;
							radius -= 0.25;

							if (mBoss.isDead() || !mBoss.isValid()) {
								this.cancel();
							}

							if (radius <= 0) {
								this.cancel();
								Location loc = mCenter.getLocation().subtract(0, 0.5, 0);
								changePhase(null, phase2PassiveSpells, null);
								new BukkitRunnable() {
									int t = 0;
									double rotation = 0;
									double radius = 0;

									@Override
									public void run() {
										t++;
										radius = t;
										world.spawnParticle(Particle.SPELL_WITCH, mCenter.getLocation().add(0, 3, 0), 20, 8, 5, 8, 0);
										world.spawnParticle(Particle.SMOKE_NORMAL, mCenter.getLocation().add(0, 3, 0), 10, 8, 5, 8, 0);
										for (int i = 0; i < 36; i++) {
											double radian1 = Math.toRadians(rotation + (10 * i));
											loc.add(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
											world.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.4, 0.4, 0.4, 0);
											world.spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.4, 0.4, 0.25,
											                    Material.COARSE_DIRT.createBlockData());
											loc.subtract(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
										}
										for (Block block : LocationUtils.getEdge(loc.clone().subtract(t, 0, t),
										                                 loc.clone().add(t, 0, t))) {
											if (rand.nextInt(6) == 1 && block.getType() == Material.SMOOTH_SANDSTONE
											    && block.getLocation().add(0, 1.5, 0).getBlock()
											    .getType() == Material.AIR) {
												block.setType(Material.SMOOTH_RED_SANDSTONE);
											}
										}
										if (t >= 40) {
											this.cancel();
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
								for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange)) {
									player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1,
									                 0.75f);
								}
								new BukkitRunnable() {

									@Override
									public void run() {
										mBoss.setInvulnerable(false);
										mBoss.setAI(true);
										teleport(mSpawnLoc);
										mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
										new BukkitRunnable() {

											@Override
											public void run() {
												changePhase(phase2Spells, phase2PassiveSpells, null);
											}

										}.runTaskLater(mPlugin, 20 * 10);
									}

								}.runTaskLater(mPlugin, 20 * 2);
							}
						}

					}.runTaskTimer(mPlugin, 30, 1);
				}

			}.runTaskLater(mPlugin, 20 * 2);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE JUNGLE WILL DEVOUR YOU. ALL RETURNS TO ROT.\",\"color\":\"dark_green\"}]");
		});

		// Forcecast Raise Jungle
		events.put(60, mBoss -> {
			super.forceCastSpell(SpellRaiseJungle.class);
		});

		// Phase 2.5
		events.put(50, mBoss -> {
			changePhase(null, passiveSpells, null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			teleport(mSpawnLoc.clone().add(0, 5, 0));
			primordialPhase = true;
			new BukkitRunnable() {
				Location loc = mSpawnLoc;
				double rotation = 0;
				double radius = 10;

				@Override
				public void run() {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(rotation + (72 * i));
						loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
						world.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.BLOCK_DUST, loc, 3, 0.1, 0.1, 0.1, 0.25,
						Material.DIRT.createBlockData());
						loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
					}
					rotation += 8;
					radius -= 0.15;
					if (radius <= 0) {
						this.cancel();
						world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0);
						world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 50, 0.1, 0.1, 0.1, 1);
						world.spawnParticle(Particle.BLOCK_CRACK, loc, 150, 0.1, 0.1, 0.1, 0.5,
						                    Material.DIRT.createBlockData());
						LivingEntity miniboss = spawnPrimordial(loc);
						new BukkitRunnable() {

							@Override
							public void run() {
								if (miniboss == null) {
									this.cancel();
								}

								if (miniboss.isDead() || !miniboss.isValid()) {
									this.cancel();
									mBoss.setInvulnerable(false);
									mBoss.setAI(true);
									teleport(mSpawnLoc);
									mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
									primordialPhase = false;
									new BukkitRunnable() {

										@Override
										public void run() {
											changePhase(phase2Spells, passiveSpells, null);
										}

									}.runTaskLater(mPlugin, 20 * 10);
								}

								if (mBoss.isDead() || !mBoss.isValid()) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 20);
					}
					if (mBoss.isDead()) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 0, 1);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE EARTH AND JUNGLE ARE ENTWINED. PRIMORDIAL, HEWN FROM SOIL AND STONE, END THEM.\",\"color\":\"dark_green\"}]");
		});

		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(40,  mBoss -> {
			forceCastSpell(SpellKaulsJudgement.class);
		});

		// Phase 3
		events.put(33, mBoss -> {
			changePhase(null, passiveSpells, null);
			knockback(plugin, 10);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			new BukkitRunnable() {

				@Override
				public void run() {
					List<ArmorStand> points = new ArrayList<ArmorStand>();
					for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
						if ((e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)
						    || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) && e instanceof ArmorStand) {
							points.add((ArmorStand) e);
						}
					}

					if (!points.isEmpty()) {
						teleport(mSpawnLoc.clone().add(0, 5, 0));
						for (ArmorStand point : points) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 5, 0.75f);
							new BukkitRunnable() {
								Location loc = point.getLocation().add(0, 15, 0);
								Vector dir = LocationUtils.getDirectionTo(mBoss.getLocation().add(0, 1, 0), loc);
								float t = 0;

								@Override
								public void run() {
									t++;
									if (t % 2 == 0) {
										world.spawnParticle(Particle.SPELL_WITCH, mCenter.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0);
									}
									world.spawnParticle(Particle.FLAME, mCenter.getLocation().add(0, 3, 0), 10, 8, 5, 9, 0);
									world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1.25, 0), 16, 0.35, 0.45, 0.35, 0);
									world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1.25, 0), 1, 0.35, 0.45, 0.35, 0);
									if (t == 1) {
										loc.getWorld().createExplosion(loc, 6, true);
										loc.getWorld().createExplosion(loc.clone().subtract(0, 4, 0), 6, true);
									}
									loc.add(dir.clone().multiply(0.35));
									if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)) {
										world.spawnParticle(Particle.FALLING_DUST, loc, 9, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData());
										world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.4, 0.4, 0.4, Material.BLUE_WOOL.createBlockData());
										world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.4, 0.4, 0.4, 0.1);
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)) {
										world.spawnParticle(Particle.REDSTONE, loc, 15, 0.4, 0.4, 0.4, RED_COLOR);
										world.spawnParticle(Particle.FALLING_DUST, loc, 10, 0.4, 0.4, 0.4, Material.RED_WOOL.createBlockData());
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)) {
										world.spawnParticle(Particle.FLAME, loc, 10, 0.3, 0.3, 0.3, 0.1);
										world.spawnParticle(Particle.SMOKE_LARGE, loc, 3, 0.4, 0.4, 0.4, 0);
									} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) {
										world.spawnParticle(Particle.FALLING_DUST, loc, 9, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData());
										world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.4, 0.4, 0.4, Material.GREEN_TERRACOTTA.createBlockData());
										world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.4, 0.4, 0.4, 0.1);
									}
									if (loc.distance(mSpawnLoc.clone().add(0, 5, 0)) < 1.25 || loc.distance(mBoss.getLocation().add(0, 1, 0)) < 1.25) {
										this.cancel();
										hits++;
									}

									if (mBoss.isDead() || !mBoss.isValid()) {
										this.cancel();
									}

									if (hits >= 4) {
										this.cancel();
										world.spawnParticle(Particle.SPELL_WITCH, mCenter.getLocation().add(0, 3, 0), 25, 6, 5, 6, 1);
										world.spawnParticle(Particle.FLAME, mCenter.getLocation().add(0, 3, 0), 40, 6, 5, 6, 0.1);
										mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.02);
										mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 9999, 0));
										changePhase(null, phase3PassiveSpells, null);
										world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175);
										world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25);
										world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25);
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_GENERIC_EXPLODE, 5, 0.9f);
										world.playSound(mBoss.getLocation().add(0, 1, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, 5, 0f);

										Random rand = new Random();
										new BukkitRunnable() {
											Location loc = mCenter.getLocation().subtract(0, 0.5, 0);
											double rotation = 0;
											double radius = 0;
											int t = 0;

											@Override
											public void run() {
												t++;
												radius = t;
												for (int i = 0; i < 36; i++) {
													double radian1 = Math.toRadians(rotation + (10 * i));
													loc.add(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
													world.spawnParticle(Particle.FLAME, loc, 2, 0.25, 0.25, 0.25, 0.1);
													world.spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.25, 0.25, 0.25, 0.25, Material.COARSE_DIRT.createBlockData());
													loc.subtract(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
												}
												for (Block block : LocationUtils.getEdge(loc.clone().subtract(t, 0, t), loc.clone().add(t, 0, t))) {
													if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
														block.setType(Material.NETHERRACK);
														if (rand.nextInt(3) == 1) {
															block.setType(Material.MAGMA_BLOCK);
														}
													} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
														block.setType(Material.SMOOTH_RED_SANDSTONE);
													}
												}
												if (t >= 40) {
													this.cancel();
												}
											}

										}.runTaskTimer(mPlugin, 0, 1);
										new BukkitRunnable() {

											@Override
											public void run() {
												mBoss.setInvulnerable(false);
												mBoss.setAI(true);
												teleport(mSpawnLoc);
												mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
												new BukkitRunnable() {

													@Override
													public void run() {
														changePhase(phase3Spells, phase3PassiveSpells, null);
													}

												}.runTaskLater(mPlugin, 20 * 10);
											}

										}.runTaskLater(mPlugin, 20 * 3);
									}
								}

							}.runTaskTimer(mPlugin, 40, 1);
						}
					}
				}

			}.runTaskLater(mPlugin, 20 * 2);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"YOU ARE NOT ANTS, BUT PREDATORS. YET THE JUNGLE'S WILL IS MANIFEST; DEATH COMES TO ALL.\",\"color\":\"dark_green\"}]");
		});

		//Force-cast Kaul's Judgement if it hasn't been casted yet.
		events.put(25,  mBoss -> {
			forceCastSpell(SpellKaulsJudgement.class);
		});

		// Phase 3.5
		events.put(20, mBoss -> {
			new BukkitRunnable() {
				Location loc = mSpawnLoc;
				double rotation = 0;
				double radius = 5;

				@Override
				public void run() {
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(rotation + (72 * i));
						loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
						world.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
						world.spawnParticle(Particle.BLOCK_DUST, loc, 4, 0.2, 0.2, 0.2, 0.25,
						Material.COARSE_DIRT.createBlockData());
						loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
					}
					rotation += 8;
					radius -= 0.25;
					if (radius <= 0) {
						this.cancel();
						world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0);
						world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 150, 0.1, 0.1, 0.1, 1);
						LivingEntity miniboss = spawnImmortal(loc);
						miniboss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(miniboss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.01);
						miniboss.setInvulnerable(true);
						miniboss.setCustomNameVisible(true);
						new BukkitRunnable() {

							@Override
							public void run() {

								if (mBoss.isDead() || !mBoss.isValid() || defeated) {
									this.cancel();
									if (!miniboss.isDead()) {
										miniboss.setHealth(0);
									}
								}
							}

						}.runTaskTimer(mPlugin, 0, 20);
					}
					if (mBoss.isDead()) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 0, 1);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"PRIMORDIAL, RETURN, NOW AS UNDYING AND EVERLASTING AS THE MOUNTAIN.\",\"color\":\"dark_green\"}]");
		});

		events.put(10, mBoss -> {
			changePhase(phase4Spells, phase4PassiveSpells, null);
			forceCastSpell(SpellVolcanicDemise.class);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE VALLEY RUNS RED WITH BLOOD TODAY. LET THIS BLASPHEMY END. PREDATORS, FACE THE FULL WILL OF THE JUNGLE. COME.\",\"color\":\"dark_green\"}]");
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange + 30, BarColor.RED, BarStyle.SEGMENTED_10, events);

		//Construct the boss with a delay to prevent the passives from going off during the dialogue
		new BukkitRunnable() {

			@Override
			public void run() {
				constructBoss(plugin, identityTag, mBoss, phase1Spells, passiveSpells, detectionRange, bossBar, 20 * 10);
			}

		}.runTaskLater(mPlugin, (20 * 10) + 1);
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.55f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
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
					mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 4, 0.2, 0.2, 0.2, 0.25,
					                               Material.COARSE_DIRT.createBlockData());
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 3, 0.1, 0.1, 0.1, 0.1);
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

	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		/* Boss deals AoE damage when melee'ing a player */
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity().getLocation().distance(mBoss.getLocation()) <= 2) {
			if (!cooldown) {
				cooldown = true;
				new BukkitRunnable() {
					@Override
					public void run() {
						cooldown = false;
					}
				}.runTaskLater(mPlugin, 20);
				UUID uuid = event.getEntity().getUniqueId();
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4)) {
					if (!player.getUniqueId().equals(uuid)) {
						BossUtils.bossDamage(mBoss, player, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				world.spawnParticle(Particle.DAMAGE_INDICATOR, mBoss.getLocation(), 30, 2, 2, 2, 0.1);
				world.spawnParticle(Particle.SWEEP_ATTACK, mBoss.getLocation(), 10, 2, 2, 2, 0.1);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0);
			}
		}

		if (event.getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 30);
			}
		}
	}

	private LivingEntity spawnPrimordial(Location loc) {
		LivingEntity entity = null;
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:wither_skeleton " + loc.getX()
		                                   + " " + loc.getY() + " " + loc.getZ() + " " + primordial);
		for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.65, 0.65, 0.65)) {
			if (e instanceof LivingEntity && !(e instanceof Player)) {
				entity = (LivingEntity) e;
				break;
			}
		}
		return entity;
	}

	private LivingEntity spawnImmortal(Location loc) {
		LivingEntity entity = null;
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:wither_skeleton " + loc.getX()
		                                   + " " + loc.getY() + " " + loc.getZ() + " " + immortal);
		for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.65, 0.65, 0.65)) {
			if (e instanceof LivingEntity && !(e instanceof Player)) {
				entity = (LivingEntity) e;
				break;
			}
		}
		return entity;
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		Spell spell = event.getSpell();
		if (spell != null && spell.castTime() > 0) {
			mBoss.setInvulnerable(true);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 12));
			mBoss.setAI(false);
			new BukkitRunnable() {

				@Override
				public void run() {
					teleport(mSpawnLoc.clone().add(0, 5, 0));
					new BukkitRunnable() {

						@Override
						public void run() {
							// If the Primordial Elemental is active, don't allow other abilities to turn Kaul's AI back on
							if (!primordialPhase) {
								mBoss.setInvulnerable(false);
								mBoss.setAI(true);
								mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
								teleport(mSpawnLoc);
								List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange);
								if (players.size() > 0) {
									Player newTarget = players.get(rand.nextInt(players.size()));
									((Mob) mBoss).setTarget(newTarget);
								}
							}
						}

					}.runTaskLater(mPlugin, spell.castTime());
				}

			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange);
		if (players.size() <= 0) {
			return;
		}
		String[] dio = new String[] {
			"AS ALL RETURNS TO ROT, SO TOO HAS THIS ECHO FALLEN.",
			"DO NOT THINK THIS ABSOLVES YOUR BLASPHEMY. RETURN HERE AGAIN, AND YOU WILL PERISH.",
			"NOW... THE JUNGLE... MUST SLEEP...",
		};
		defeated = true;
		knockback(mPlugin, 10);

		for (Player player : players) {
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 40, 10));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
		}
		changePhase(null, null, null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		World world = mBoss.getWorld();
		mBoss.removePotionEffect(PotionEffectType.GLOWING);
		Random rand = new Random();
		for (Entity ent : mSpawnLoc.getNearbyLivingEntities(detectionRange)) {
			if (!ent.getUniqueId().equals(mBoss.getUniqueId()) && ent instanceof WitherSkeleton && !ent.isDead()) {
				ent.remove();
			}
		}
		new BukkitRunnable() {
			Location loc = mCenter.getLocation().subtract(0, 0.5, 0);
			double rotation = 0;
			double radius = 0;
			int t = 0;

			@Override
			public void run() {
				t++;
				radius = t;
				for (int i = 0; i < 36; i++) {
					double radian1 = Math.toRadians(rotation + (10 * i));
					loc.add(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
					world.spawnParticle(Particle.CLOUD, loc, 3, 0.25, 0.25, 0.25, 0.025, null, true);
					world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.4, 0.25, 0.4, 0.25, null, true);
					loc.subtract(Math.cos(radian1) * radius, 1, Math.sin(radian1) * radius);
				}
				for (Block block : LocationUtils.getEdge(loc.clone().subtract(t, 0, t), loc.clone().add(t, 0, t))) {
					if (block.getType() == Material.MAGMA_BLOCK) {
						block.setType(Material.OAK_LEAVES);
						if (rand.nextInt(5) == 1) {
							block.setType(Material.GLOWSTONE);
						}
					} else if (block.getType() == Material.SMOOTH_RED_SANDSTONE || block.getType() == Material.NETHERRACK) {
						block.setType(Material.GRASS_BLOCK);
						if (rand.nextInt(3) == 1) {
							Block b = block.getLocation().add(0, 1.5, 0).getBlock();
							if (!b.getType().isSolid()) {
								b.setType(Material.GRASS);
							}
						}
					}
				}
				if (t >= 40) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		String[] d = dio;
		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + d[t].toUpperCase() + "\",\"color\":\"dark_green\"}]");
				t++;
				if (t == d.length) {
					this.cancel();
					teleport(mSpawnLoc);
					new BukkitRunnable() {
						int t = 0;

						@Override
						public void run() {
							if (t <= 0) {
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
							}
							t++;
							if (t <= 60) {
								mBoss.teleport(mBoss.getLocation().subtract(0, 0.05, 0));
								mBoss.getWorld().spawnParticle(Particle.BLOCK_DUST, mSpawnLoc, 7, 0.3, 0.1, 0.3, 0.25,
								                               Material.COARSE_DIRT.createBlockData());
							} else {
								mBoss.getEquipment().clear();
								mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 27, 0));
								mBoss.setAI(false);
								mBoss.setSilent(true);
								mBoss.setInvulnerable(true);
								if (t >= 100) {
									this.cancel();
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 100 0.8");
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"VICTORY\",\"color\":\"green\",\"bold\":true}]");
									PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Kaul, Soul of the Jungle\",\"color\":\"dark_green\",\"bold\":true}]");
									mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
									mBoss.remove();
								}
							}
						}

					}.runTaskTimer(mPlugin, 30, 1);
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 6);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 2048;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2 + 25;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);
		EntityEquipment equips = mBoss.getEquipment();
		ItemStack[] armorc = equips.getArmorContents();
		ItemStack m = equips.getItemInMainHand();
		ItemStack o = equips.getItemInOffHand();
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.getEquipment().clear();
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 100, 0));
				mBoss.setAI(false);
				mBoss.setSilent(true);
				mBoss.setInvulnerable(true);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 100, 10));
				World world = mBoss.getWorld();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 0f);
				world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
				world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
				String[] dio = new String[] {
					"THE JUNGLE'S WILL IS UNASSAILABLE, YET YOU SCURRY ACROSS MY SHRINE LIKE ANTS.",
					"IS THE DEFILEMENT OF THE DREAM NOT ENOUGH!?",
				};

				new BukkitRunnable() {
					int t = 0;
					int index = 0;

					@Override
					public void run() {
						if (t == 0) {
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3, 0f);
						}

						if (t % (20 * 4) == 0) {
							if (index < dio.length) {
								PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio[index].toUpperCase() + "\",\"color\":\"dark_green\"}]");
								index++;
							}
						}
						t++;

						if (t >= (20 * 8)) {
							this.cancel();
							mBoss.setAI(true);
							mBoss.setSilent(false);
							mBoss.setInvulnerable(false);
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color white");
							mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
							mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99999, 0));
							mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 9999, 0));
							world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 0f);
							world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
							world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
							mBoss.getEquipment().setArmorContents(armorc);
							mBoss.getEquipment().setItemInMainHand(m);
							mBoss.getEquipment().setItemInOffHand(o);

							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Kaul\",\"color\":\"dark_green\",\"bold\":true}]");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Soul of the Jungle\",\"color\":\"green\",\"bold\":true}]");
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 5, 0f);
						}

					}

				}.runTaskTimer(mPlugin, 40, 1);
			}

		}.runTaskLater(mPlugin, 1);
	}
}
