package com.playmonumenta.bossfights.spells.spells_kaul;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

public class SpellArachnopocolypse extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private double mDetectRange;
	private Random rand = new Random();
	private boolean cooldown = false;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, double range, double detectRange) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDetectRange = detectRange;
	}

	@Override
	public boolean canRun() {
		return !cooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		cooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				cooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);
		new BukkitRunnable() {

			@Override
			public void run() {
				List<Player> players = Utils.playersInRange(mBoss.getLocation(), mDetectRange);

				int amount = 10 + (5 * (players.size()));
				if (players.size() == 1) {
					amount = 18;
				}
				int a = amount;
				world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0);
				new BukkitRunnable() {
					int t = 0;
					@Override
					public void run() {
						t++;
						riseSpider(getRandomLocation(loc, mRange));
						if (t >= a) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 2);
			}

		}.runTaskLater(mPlugin, 30);

	}

	public void riseSpider(Location loc) {
		int num = rand.nextInt(5);
		String summon = null;
		if (num == 0) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Corrupted Spider\\\"}\",Health:20.0f,Attributes:[{Base:20,Name:\"generic.maxHealth\"},{Base:0.36d,Name:\"generic.movementSpeed\"},{Base:25,Name:\"generic.followRange\"}],HandDropChances:[-327.67f,0.085f],ActiveEffects:[{Duration:199980,Id:8,Amplifier:3}],HandItems:[{id:\"minecraft:wooden_sword\",tag:{Enchantments:[{lvl:2,id:\"minecraft:knockback\"}]},Count:1b},{}]}";
		} else if (num == 1) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Shieldcrusher Spiders\\\"}\",Health:24.0f,Attributes:[{Base:24,Name:\"generic.maxHealth\"}],HandDropChances:[-327.67f,0.085f],ActiveEffects:[{Duration:222220,Id:26,Amplifier:0}],HandItems:[{id:\"minecraft:wooden_axe\",tag:{AttributeModifiers:[{UUIDMost:339242,UUIDLeast:52922,Amount:2,AttributeName:\"generic.attackDamage\",Operation:0,Name:\"generic.attackDamage\"}]},Count:1b},{}]}";
		} else if (num == 2) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Monstrous Spider\\\"}\",Health:16.0f,Attributes:[{Base:16,Name:\"generic.maxHealth\"},{Base:0.24d,Name:\"generic.movementSpeed\"},{Base:10,Name:\"generic.attackDamage\"},{Base:1,Name:\"generic.knockbackResistance\"}],ActiveEffects:[{Duration:199980,Id:11,Amplifier:1}]}";
		} else if (num == 3) {
			summon = "summon minecraft:cave_spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ();
		} else if (num == 4) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {Passengers:[{Passengers:[{Passengers:[{CustomName:\"{\\\"text\\\":\\\"Queen Spider\\\"}\",Health:14.0f,Attributes:[{Base:14,Name:\"generic.maxHealth\"}],id:\"minecraft:cave_spider\"}],Potion:{id:\"minecraft:splash_potion\",tag:{CustomPotionEffects:[{Duration:120,Id:19,Amplifier:2}],Potion:\"minecraft:strong_poison\"},Count:1},id:\"minecraft:potion\"}],Attributes:[{Base:4,Name:\"generic.attackDamage\"}],id:\"minecraft:spider\"}],Attributes:[{Base:0.2d,Name:\"generic.movementSpeed\"},{Base:4,Name:\"generic.attackDamage\"}]}";
		}

		String toSummon = summon;
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.1, 0.4, 0.25,
				                             Material.DIRT.createBlockData());

				if (t >= 20) {
					this.cancel();
					loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
					loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData());
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), toSummon);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private Location getRandomLocation(Location origin, double range) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		return origin.clone().add(rand.nextDouble(-range, range), 0, rand.nextDouble(-range, range));
	}

	@Override
	public int duration() {
		return 20 * 20;
	}

	@Override
	public int castTime() {
		return 20 * 5;
	}

}
