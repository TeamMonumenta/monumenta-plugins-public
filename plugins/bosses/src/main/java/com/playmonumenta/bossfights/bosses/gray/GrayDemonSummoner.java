package com.playmonumenta.bossfights.bosses.gray;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;

public class GrayDemonSummoner extends GrayStrongSummonerBase {
	public static final String identityTag = "boss_gray_summ_demon";
	public static final int detectionRange = 35;
	private static final String nbt = "{Health:50.0f,Tags:[\"boss_gray_summoned\"],Attributes:[{Base:50.0d,Name:\"generic.maxHealth\"}],AbsorptionAmount:0.0f,HandItems:[{id:\"minecraft:stone_axe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§5§lErebus\\\"}\"},AttributeModifiers:[{UUIDMost:4546668228276800624L,UUIDLeast:-9220563185721679875L,Amount:0.2d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"},{UUIDMost:2132509948532640819L,UUIDLeast:-4693094300401352144L,Amount:14.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:fermented_spider_eye\",Count:1b,tag:{HideFlags:1,display:{Name:\"{\\\"text\\\":\\\"§c§lDemonic Heart\\\"}\"}}}],CustomName:\"{\\\"text\\\":\\\"Conjured Demon\\\"}\",ArmorItems:[{id:\"minecraft:diamond_boots\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§bSummoner2Boots\\\"}\"},Damage:0}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:13915937,Name:\"{\\\"text\\\":\\\"§bSummoner2Pants\\\"}\"},Damage:0}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:11546150,Name:\"{\\\"text\\\":\\\"§bSummoner2Chest\\\"}\"},Damage:0}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"a7024851-83e4-3575-8a62-20405cf05aa3\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRlNTU2OWUzM2YzNzVhYWEyM2U5MTc2ZjkxZjE5MmMzNDUyOTNlMjgyMjc1NjdjNzMwYzY2NmYzOGI4ZGUifX19\"}]}}}}]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayDemonSummoner(plugin, boss);
	}

	public GrayDemonSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, EntityType.HUSK, nbt);
	}
}
