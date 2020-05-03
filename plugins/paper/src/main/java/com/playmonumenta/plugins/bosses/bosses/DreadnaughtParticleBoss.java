package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.abilities.delves.twisted.Dreadful;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadnaughtParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class DreadnaughtParticleBoss extends BossAbilityGroup {

	public static final String identityTag = Dreadful.DREADFUL_DREADNAUGHT_TAG;
	public static final int detectionRange = 40;

	private static final String DREADLING_SUMMON_COMMAND = "summon minecraft:spider ";
	private static final String DREADLING_1_SUMMON_COMMAND_DATA = " {HurtByTimestamp:0,Attributes:[{Base:20.0d,Name:\"generic.maxHealth\"},{Base:0.4d,Name:\"generic.movementSpeed\"},{Base:6.0d,Name:\"generic.attackDamage\"}],HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Tags:[\"boss_dreadling\"],Health:40.0f,Silent:1b,HandItems:[{},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"{\\\"text\\\":\\\"Dreadling\\\"}\",ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}]}";
	private static final String DREADLING_2_SUMMON_COMMAND_DATA = " {HurtByTimestamp:0,Attributes:[{Base:40.0d,Name:\"generic.maxHealth\"},{Base:0.4d,Name:\"generic.movementSpeed\"},{Base:11.0d,Name:\"generic.attackDamage\"}],HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Tags:[\"boss_dreadling\"],Health:40.0f,Silent:1b,HandItems:[{},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"{\\\"text\\\":\\\"Dreadling\\\"}\",ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}]}";

	LivingEntity mBoss;

	private final String mDreadlingSummonCommandData;
	private double mDamageCounter = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadnaughtParticleBoss(plugin, boss);
	}

	public DreadnaughtParticleBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		List<Spell> passiveSpells = Arrays.asList(
			new SpellDreadnaughtParticle(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);

		mDreadlingSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? DREADLING_2_SUMMON_COMMAND_DATA : DREADLING_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		mDamageCounter += event.getDamage();

		if (mDamageCounter >= 80) {
			mDamageCounter -= 80;

			Location loc = event.getEntity().getLocation();
			String command = DREADLING_SUMMON_COMMAND + loc.getX() + " " + loc.getY() + " " + loc.getZ() + mDreadlingSummonCommandData;
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

			loc.add(0, 1, 0);

			World world = event.getEntity().getWorld();
			world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1, 0.5f);
			world.spawnParticle(Particle.FLAME, loc, 50, 3, 1, 3, 0);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 1, 3, 0);
		}
	}
}
