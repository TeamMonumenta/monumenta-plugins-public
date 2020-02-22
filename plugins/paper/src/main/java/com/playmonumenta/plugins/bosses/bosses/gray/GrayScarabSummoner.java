package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class GrayScarabSummoner extends GraySwarmSummonerBase {
	public static final String identityTag = "boss_gray_summ_bug";
	public static final int detectionRange = 35;
	private static final String nbt = "{HurtByTimestamp:0,Leashed:0b,IsBaby:1b,Health:27.0f,Bukkit.updateLevel:2,Attributes:[{Base:27.0d,Name:\"generic.maxHealth\"}],Silent:1b,OnGround:1b,Dimension:0,PortalCooldown:0,AbsorptionAmount:0.0f,CustomName:\"{\\\"text\\\":\\\"§eScarab\\\"}\",CanBreakDoors:0b,WorldUUIDMost:-1041596277173696703L,ArmorItems:[{},{},{},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"28dc0a33-2f66-43ab-8085-a15f77c62fc2\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTMwYWMxZjljNjQ5Yzk5Y2Q2MGU0YmZhNTMzNmNjMTg1MGYyNzNlYWI5ZjViMGI3OTQwZDRkNGQ3ZGM4MjVkYyJ9fX0=\"}]}},AttributeModifiers:[{UUIDMost:7476590459543701375L,UUIDLeast:-7971836069524391014L,Amount:6.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}],DeathLootTable:\"minecraft:empty\",Spigot.ticksLived:271,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:0b,ShowIcon:0b,ShowParticles:0b,Duration:199709,Id:14b,Amplifier:0b}],Tags:[\"boss_gray_summoned\"]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayScarabSummoner(plugin, boss);
	}

	public GrayScarabSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, EntityType.ZOMBIE, nbt);
	}
}
