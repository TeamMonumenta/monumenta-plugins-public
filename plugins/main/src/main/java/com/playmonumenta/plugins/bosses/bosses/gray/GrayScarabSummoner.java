package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class GrayScarabSummoner extends GraySwarmSummonerBase {
	public static final String identityTag = "boss_gray_summ_bug";
	public static final int detectionRange = 35;
	private static final String nbt = "{HurtByTimestamp:0,Leashed:0b,IsBaby:1b,Health:14.0f,Bukkit.updateLevel:2,Attributes:[{Base:14.0d,Name:\"generic.maxHealth\"}],Silent:1b,OnGround:1b,Dimension:0,PortalCooldown:0,AbsorptionAmount:0.0f,CustomName:\"{\\\"text\\\":\\\"§eScarab\\\"}\",CanBreakDoors:0b,WorldUUIDMost:-1041596277173696703L,ArmorItems:[{},{},{},{id:\"minecraft:brown_mushroom\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§eScarab Teeth\\\"}\"},AttributeModifiers:[{UUIDMost:6479497315394079057L,UUIDLeast:-5714715579073658916L,Amount:2.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}],DeathLootTable:\"minecraft:empty\",Spigot.ticksLived:271,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:0b,ShowIcon:0b,ShowParticles:0b,Duration:199709,Id:14b,Amplifier:0b}],Tags:[\"boss_gray_summoned\"]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayScarabSummoner(plugin, boss);
	}

	public GrayScarabSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, EntityType.ZOMBIE, nbt);
	}
}
