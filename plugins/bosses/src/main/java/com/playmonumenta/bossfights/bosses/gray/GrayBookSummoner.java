package com.playmonumenta.bossfights.bosses.gray;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;

public class GrayBookSummoner extends GraySwarmSummonerBase {
	public static final String identityTag = "boss_gray_summ_book";
	public static final int detectionRange = 35;
	private static final String nbt = "{IsBaby:1b,Health:8.0f,Attributes:[{Base:8.0d,Name:\"generic.maxHealth\"}],Silent:1b,AbsorptionAmount:0.0f,CustomName:\"{\\\"text\\\":\\\"§dAnimated Text\\\"}\",CanBreakDoors:0b,ArmorItems:[{},{},{},{id:\"minecraft:book\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§b§lTome of Aquatic Shards\\\"}\"},Enchantments:[{lvl:1s,id:\"minecraft:power\"}],AttributeModifiers:[{UUIDMost:-7795826339782442390L,UUIDLeast:-8977556070605263741L,Amount:2.0d,Slot:\"head\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}],DeathLootTable:\"minecraft:empty\",Tags:[\"boss_gray_summoned\"],ActiveEffects:[{Ambient:0b,ShowIcon:0b,ShowParticles:0b,Duration:199709,Id:14b,Amplifier:0b}]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayBookSummoner(plugin, boss);
	}

	public GrayBookSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, "minecraft:zombie", nbt);
	}
}
