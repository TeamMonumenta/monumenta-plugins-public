package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class GrayBookSummoner extends GraySwarmSummonerBase {
	public static final String identityTag = "boss_gray_summ_book";
	public static final int detectionRange = 35;
	private static final String nbt = "{HurtByTimestamp:0,Leashed:0b,IsBaby:1b,Health:27.0f,Bukkit.updateLevel:2,Attributes:[{Base:27.0d,Name:\"generic.maxHealth\"}],Silent:1b,OnGround:1b,Dimension:0,PortalCooldown:0,AbsorptionAmount:0.0f,CustomName:\"{\\\"text\\\":\\\"§dAnimated Text\\\"}\",CanBreakDoors:0b,WorldUUIDMost:-1041596277173696703L,ArmorItems:[{},{},{},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"ef76f48e-0f9e-4c0d-bce3-d341eb10b7e8\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2VlYTM0NTkwOGQxN2RjNDQ5NjdkMWRjZTQyOGYyMmYyYjE5Mzk3MzcwYWJlYjc3YmRjMTJlMmRkMWNiNiJ9fX0=\"}]}},AttributeModifiers:[{UUIDMost:-9083280520086207940L,UUIDLeast:-5838966267974544850L,Amount:6.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}],DeathLootTable:\"minecraft:empty\",Spigot.ticksLived:271,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:0b,ShowIcon:0b,ShowParticles:0b,Duration:199709,Id:14b,Amplifier:0b}],Tags:[\"boss_gray_summoned\"]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayBookSummoner(plugin, boss);
	}

	public GrayBookSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, EntityType.ZOMBIE, nbt);
	}
}
