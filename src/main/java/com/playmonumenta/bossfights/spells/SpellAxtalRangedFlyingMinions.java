package com.playmonumenta.bossfights.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

// TODO: This entire spell is probably broken...
public class SpellAxtalRangedFlyingMinions implements Spell {
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mCount;
	private int mScope;
	private int mRepeats;
	private Random mRand = new Random();

	public SpellAxtalRangedFlyingMinions(Plugin plugin, Entity launcher, int count, int scope, int repeats) {
		mPlugin = plugin;
		mLauncher = launcher;
		mCount = count;
		mScope = scope;
		mRepeats = repeats;
	}

	@Override
	public void run() {
		animation();
		spawn();
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	private void spawn() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable() {
			@Override
			public void run() {
				int nb_to_spawn = mCount + (mRand.nextInt(2 * mScope) - mScope);
				for (int j = 0; j < nb_to_spawn; j++) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:skeleton " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " {HurtByTimestamp:0,Attributes:[{Base:0.0d,Name:\"generic.knockbackResistance\"},{Base:0.25d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:16.0d,Name:\"generic.followRange\"},{Base:2.0d,Name:\"generic.attackDamage\"},{Base:25.0d,Name:\"generic.maxHealth\"}],Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,FallDistance:0.0f,DeathTime:0s,WorldUUIDMost:-1041596277173696703L,HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Spigot.ticksLived:434,Motion:[0.0d,-0.0784000015258789d,0.0d],Leashed:0b,Health:25.0f,Bukkit.updateLevel:2,LeftHanded:0b,Air:300s,OnGround:1b,Dimension:0,Rotation:[0.0f,3.0153077f],HandItems:[{id:\"minecraft:bow\",Count:1b,tag:{ench:[{lvl:5s,id:48s},{lvl:1s,id:49s}]},Damage:0s},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"Strong Lost Soul\",Pos:[-1027.5d,82.0d,-1382.5d],Fire:-1s,ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{ench:[{lvl:5s,id:4s},{lvl:10s,id:3s}],display:{color:11579568},AttributeModifiers:[]},Damage:0s},{id:\"minecraft:skull\",Count:1b,Damage:0s}],CanPickUpLoot:0b,DeathLootTable:\"ayylmao\",HurtTime:0s,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:1b,ShowParticles:1b,Duration:11566,Id:5b,Amplifier:0b},{Ambient:1b,ShowParticles:1b,Duration:999566,Id:14b,Amplifier:0b}]}");
				}
				for (Entity skelly : mLauncher.getNearbyEntities(0.2, 5, 0.2)) {
					if (skelly.getType() == EntityType.SKELETON) {
						double x = Math.cos((double)mRand.nextInt(628) / 100);
						double z = Math.sin((double)mRand.nextInt(628) / 100);
						skelly.setVelocity(new Vector(x, 0f, z));
						((LivingEntity)skelly).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, mRand.nextInt(100) + 40, 1));
					}
				}
			}
		};
		for (int i = 0; i < mRepeats; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, single_spawn, 40 + 15 * i);
		}
	}

	private void animation() {
		Location loc = mLauncher.getLocation();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable() {
			@Override
			public void run() {
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				Location particleLoc = new Location(loc.getWorld(), 0, 0, 0);
				mLauncher.teleport(loc);
				centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_GHAST_AMBIENT, 1f, 0.5f);
				for (int j = 0; j < 5; j++) {
					while (particleLoc.distance(centerLoc) > 2) {
						particleLoc.setX(loc.getX() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setZ(loc.getZ() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
						particleLoc.setY(loc.getY() + ((double)(mRand.nextInt(4000) - 2000) / 1000));
					}
					particleLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, particleLoc, 4, 0, 0, 0, 0.01);
					particleLoc.setX(0);
					particleLoc.setY(0);
					particleLoc.setZ(0);
				}
			}
		};
		for (int i = 0; i < (40 + mRepeats * 15) / 3; i++) {
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, i * 3);
		}
	}
}
