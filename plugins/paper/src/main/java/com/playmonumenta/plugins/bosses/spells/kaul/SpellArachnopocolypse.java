package com.playmonumenta.plugins.bosses.spells.kaul;

import java.util.List;

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

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellArachnopocolypse extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mLoc;
	private double mDetectRange;
	private boolean mCooldown = false;

	public SpellArachnopocolypse(Plugin plugin, LivingEntity boss, Location loc, double detectRange) {
		mPlugin = plugin;
		mBoss = boss;
		mLoc = loc;
		mDetectRange = detectRange;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);
		new BukkitRunnable() {

			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mDetectRange);
				players.removeIf(p -> p.getLocation().getY() >= 61);
				int amount = 10 + (5 * (players.size()));
				if (players.size() == 1) {
					amount = 18;
				}
				int a = amount;
				world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 10, 1);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						riseSpider(getRandomLocation(mLoc, 32));
						if (mTicks >= a) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 2);
			}

		}.runTaskLater(mPlugin, 30);

	}

	public void riseSpider(Location loc) {
		int num = FastUtils.RANDOM.nextInt(3);//5
		String summon = null;
		if (num == 0) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Corrupted Spider\\\"}\",Health:23.0f,Attributes:[{Base:23,Name:\"generic.maxHealth\"},{Base:0.36d,Name:\"generic.movementSpeed\"},{Base:40,Name:\"generic.followRange\"}],Tags:[\"boss_force\"],HandDropChances:[-327.67f,0.085f],ActiveEffects:[{Duration:199980,Id:8,Amplifier:3}],HandItems:[{id:\"minecraft:wooden_sword\",tag:{Enchantments:[{lvl:2,id:\"minecraft:knockback\"}]},Count:1b},{}]}";
		} else if (num == 1) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Shieldcrusher Spider\\\"}\",Health:27.0f,Attributes:[{Base:27,Name:\"generic.maxHealth\"},{Base:40,Name:\"generic.followRange\"}],HandDropChances:[-327.67f,0.085f],ActiveEffects:[{Duration:222220,Id:26,Amplifier:0}],HandItems:[{id:\"minecraft:wooden_axe\",tag:{AttributeModifiers:[{UUIDMost:339242,UUIDLeast:52922,Amount:2,AttributeName:\"generic.attackDamage\",Operation:0,Name:\"generic.attackDamage\"}]},Count:1b},{}]}";
		} else if (num == 2) {
			summon = "summon minecraft:spider " + loc.getX() + " " + (loc.getY() + 1) + " " + loc.getZ() + " {CustomName:\"{\\\"text\\\":\\\"Monstrous Spider\\\"}\",Health:19.0f,Attributes:[{Base:19,Name:\"generic.maxHealth\"},{Base:0.24d,Name:\"generic.movementSpeed\"},{Base:40,Name:\"generic.followRange\"},{Base:10,Name:\"generic.attackDamage\"},{Base:1,Name:\"generic.knockbackResistance\"}],ActiveEffects:[{Duration:199980,Id:11,Amplifier:1}]}";
		}

		String toSummon = summon;
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;
				loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 2, 0.4, 0.1, 0.4, 0.25, Material.DIRT.createBlockData());

				if (mTicks >= 20) {
					this.cancel();
					loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
					loc.getWorld().spawnParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData());
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), toSummon);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private Location getRandomLocation(Location origin, double range) {
		Location loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		while (loc.getBlock().getType().isSolid()) {
			loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		}
		return loc;
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
