package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.abilities.delves.twisted.Dreadful;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadnaughtParticle;

public class DreadnaughtParticleBoss extends BossAbilityGroup {

	public static final String identityTag = Dreadful.DREADFUL_DREADNAUGHT_TAG;
	public static final int detectionRange = 40;

	private static final String SUMMON_COMMAND_1 = "summon minecraft:spider ";
	private static final String SUMMON_COMMAND_2 = " {HurtByTimestamp:0,Attributes:[{Base:40.0d,Name:\"generic.maxHealth\"},{Base:0.4d,Name:\"generic.movementSpeed\"},{Base:10.0d,Name:\"generic.attackDamage\"}],Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,FallDistance:0.0f,DeathTime:0s,WorldUUIDMost:-1041596277173696703L,HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Spigot.ticksLived:25,Tags:[\"boss_dreadling\"],Motion:[0.0d,-0.0784000015258789d,0.0d],Leashed:0b,Health:40.0f,Bukkit.updateLevel:2,Silent:1b,LeftHanded:0b,Paper.SpawnReason:\"SPAWNER_EGG\",Air:300s,OnGround:1b,Dimension:0,Rotation:[23.142958f,-20.76723f],HandItems:[{},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"{\\\"text\\\":\\\"Dreadling\\\"}\",Pos:[-958.5d,8.0d,-1476.5d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,HurtTime:0s,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}],Paper.Origin:[-958.5d,8.0d,-1476.5d]}";

	LivingEntity mBoss;

	private double mDamageCounter = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LivingBladeBoss(plugin, boss);
	}

	public DreadnaughtParticleBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		List<Spell> passiveSpells = Arrays.asList(
			new SpellDreadnaughtParticle(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		mDamageCounter += event.getDamage();

		if (mDamageCounter >= 80) {
			mDamageCounter -= 80;

			Location loc = event.getEntity().getLocation();
			String command = SUMMON_COMMAND_1 + loc.getX() + " " + loc.getY() + " " + loc.getZ() + SUMMON_COMMAND_2;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

			loc.add(0, 1, 0);

			World world = event.getEntity().getWorld();
			world.spawnParticle(Particle.FLAME, loc, 50, 3, 1, 3, 0);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 1, 3, 0);
		}
	}
}
