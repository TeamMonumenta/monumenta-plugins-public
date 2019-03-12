package com.playmonumenta.bossfights.spells.spells_kaul;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

/*
 * Kaul’s Judgement: (Stone Brick)Give a Tellraw to ¼ (min 2) of the
 * players as a warning then teleport them after 1-2 seconds. The players
 * selected are teleported to a mini-dungeon* of some kind. In order to
 * get out and back to the fight they must traverse the dungeon.
 * (players in the mini dungeon can’t get focused by Kaul’s attacks,
 * like his passive) with a timer, to be determined to the dungeon’s length
 * (Players that got banished get strength 1 and speed 1 for 30s if they survived)
 * (triggers once in phase 2 , and twice in phase 3)
 */
public class SpellKaulsJudgement extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private LivingEntity mTp = null;
	private Random rand = new Random();
	private int uses = 0;
	private final int mMax;
	private boolean onCooldown = false;
	private boolean mPhase3;

	private static final String KAULS_JUDGEMENT_TP_TAG = "KaulsJudgementTPTag";
	private static final String KAULS_JUDGEMENT_TAG = "KaulsJudgementTag";
	private static final String KAULS_JUDGEMENT_MOB_SPAWN_TAG = "KaulsJudgementMobSpawn";
	private static final String KAULS_JUDGEMENT_MOB_TAG = "deleteelite";
	private static final String mob = "{ArmorDropChances:[-327.67f,-327.67f,-327.67f,-327.67f],CustomName:\"{\\\"text\\\":\\\"§6Stoneborn Immortal\\\"}\",IsBaby:0,Health:30.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:10395294},Damage:0}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:6191160},Damage:0}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:10395294},Damage:0}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"86d08d4a-3cc6-46fa-61cf-c54d58373c70\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk4ZDNjMzFmOWJjNTc1Mzk4MzdkMTc2NTdiOWJhZTczNWE4OTZmMWEwNzc4MTZlZDk1NzU3YzE1NDJhYTMifX19\"}]}}}}],Attributes:[{Base:30,Name:\"generic.maxHealth\"},{Base:0.21d,Name:\"generic.movementSpeed\"},{Base:0.0f,Name:\"zombie.spawnReinforcements\"},{Base:60,Name:\"generic.followRange\"}],HandDropChances:[-327.67f,-327.67f],PersistenceRequired:1b,Tags:[\"Elite\",\"deleteelite\"],ActiveEffects:[{Duration:1999980,Id:5,Amplifier:0},{Duration:1999980,Id:11,Amplifier:4}],HandItems:[{id:\"minecraft:shield\",Count:1b,tag:{BlockEntityTag:{id:\"minecraft:banner\",Patterns:[{Pattern:\"tt\",Color:7},{Pattern:\"cs\",Color:7},{Pattern:\"flo\",Color:7},{Pattern:\"gru\",Color:7}],Base:8},Enchantments:[{lvl:30s,id:\"minecraft:sharpness\"}],AttributeModifiers:[{UUIDMost:-1113277120483211494L,UUIDLeast:-5231964292071775789L,Amount:0.2d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}},{id:\"minecraft:shield\",Count:1b,tag:{BlockEntityTag:{id:\"minecraft:banner\",Patterns:[{Pattern:\"tt\",Color:7},{Pattern:\"cs\",Color:7},{Pattern:\"flo\",Color:7},{Pattern:\"gru\",Color:7}],Base:8},Enchantments:[{lvl:30s,id:\"minecraft:sharpness\"}],AttributeModifiers:[{UUIDMost:-1113277120483211494L,UUIDLeast:-5231964292071775789L,Amount:0.2d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}}]}";
	public SpellKaulsJudgement(Plugin plugin, LivingEntity boss, double range, int max, boolean phase3) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mMax = max;
		mPhase3 = phase3;
		for (Entity e : boss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_TP_TAG) && e instanceof LivingEntity) {
				mTp = (LivingEntity) e;
				break;
			}
		}
	}

	@Override
	public void run() {
		uses++;

		onCooldown = true;
		World world = mBoss.getWorld();
		new BukkitRunnable() {

			@Override
			public void run() {
				onCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 90);
		for (Entity e : mBoss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_TAG)) {
				e.remove();
			}
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				for (Entity e : mBoss.getWorld().getEntities()) {
					if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_SPAWN_TAG)) {
						Location loc = e.getLocation();
						world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0.3, 0.45, 0.3, 1);
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:zombie " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " " + mob);
					}
				}
			}

		}.runTaskLater(mPlugin, 50);
		new BukkitRunnable() {

			@Override
			public void run() {
				for (Entity e : mBoss.getWorld().getEntities()) {
					if (e.getScoreboardTags().contains(KAULS_JUDGEMENT_MOB_TAG)) {
						e.remove();
					}
				}
			}

		}.runTaskLater(mPlugin, 20 * 55);
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		for (Player player : players) {
			player.sendMessage(ChatColor.DARK_GREEN + "IT IS TIME FOR JUDGEMENT TO COME.");
		}
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 10, 2);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0);
		double amount = Math.ceil(players.size() / 2);
		if (amount < 2) {
			amount++;
		}
		List<Player> judge = new ArrayList<Player>();
		for (int i = 0; i < amount; i++) {
			Player player = players.get(rand.nextInt(players.size()));
			if (!judge.contains(player)) {
				judge.add(player);
			} else {
				amount++;
			}
		}
		for (Player player : judge) {
			judge(player);
		}
	}

	public void judge(Player player) {
		int time = mPhase3 ? 20 * 45 : 20 * 48;
		new BukkitRunnable() {
			World world = player.getWorld();
			Location loc = player.getLocation();
			int t = 0;
			@Override
			public void run() {
				t++;
				world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 2, 0.4, 0.4, 0.4, 0);
				world.spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0);
				if (t >= 20 * 2) {
					this.cancel();
					player.addScoreboardTag(KAULS_JUDGEMENT_TAG);
					world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
					world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
					world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
					player.teleport(mTp);
					player.playSound(mTp.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
					player.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
					player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
					player.sendMessage(ChatColor.AQUA + "What happened!? You need to find your way out of here quickly!");
					player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ESCAPE", "", 1, 20 * 3, 1);
					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.5, 0), 1, 0.4, 0.4, 0.4, 0);

							if (player.isDead() || t >= time) {
								player.setInvulnerable(true);
								if (player.isDead()) {
									player.spigot().respawn();
									new BukkitRunnable() {

										@Override
										public void run() {
											player.setInvulnerable(false);
										}

									}.runTaskLater(mPlugin, 20 * 6);
								} else {
									new BukkitRunnable() {

										@Override
										public void run() {
											player.setInvulnerable(false);
										}

									}.runTaskLater(mPlugin, 20 * 3);
								}
								player.teleport(loc);
								world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0);
								world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
								world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
								player.removeScoreboardTag(KAULS_JUDGEMENT_TAG);
								new BukkitRunnable() {

									@Override
									public void run() {
										world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1, 0.2f);
										world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 50, 0.25, 0.45, 0.25, 0.15);
										world.spawnParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0, Material.ANVIL.createBlockData());
										double health = player.getHealth() - (player.getMaxHealth() - 10);
										if (health <= 0) {
											player.damage(100, mBoss);
										} else {
											player.setHealth(health);
										}
										player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 1));
										player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 2));
										player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 30, 1));
										player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "SUCH FAILURE.");
									}

								}.runTaskLater(mPlugin, 40);
								this.cancel();
								return;
							}

							if (!player.getScoreboardTags().contains(KAULS_JUDGEMENT_TAG)) {
								this.cancel();
								player.setInvulnerable(true);
								new BukkitRunnable() {

									@Override
									public void run() {
										player.setInvulnerable(false);
									}

								}.runTaskLater(mPlugin, 20 * 3);
								player.sendMessage(ChatColor.AQUA + "You escaped! You feel much more invigorated from your survival!");
								player.teleport(loc);
								world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
								world.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 60, 0, 0.4, 0, 1);
								world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0, 0.4, 0, 0.15);
								player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 0));
								player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0));
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean canRun() {
		return mTp != null && uses < mMax && !onCooldown;
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 16;
	}

	@Override
	public int castTime() {
		return 20 * 4;
	}

}
