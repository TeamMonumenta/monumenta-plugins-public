package com.playmonumenta.bossfights.spells;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellCyanSummon extends Spell {

	private final LivingEntity mBoss;

	public SpellCyanSummon(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		for (int i = 0; i < 3; i++) {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
			                                   "summon minecraft:zombie_pigman " + mBoss.getLocation().getX() + " " + mBoss.getLocation().getY() + " " + mBoss.getLocation().getZ() +
											   " {Anger:32767s,CustomName:\"{\\\"text\\\":\\\"Seeking Flesh\\\"}\",IsBaby:1b,Health:25.0f,ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:13458534,Name:\"{\\\"text\\\":\\\"§fBurnt Cloak\\\"}\"}}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"ef8b6b38-5b8c-4eed-88ec-d6d556bf59f6\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODUyZWVhZjg5OGQ0YzczNTk3NTEwOTljZTc2ZDI0NjYyYmVmYWE4OTEwM2UzYjJiZjAyN2MwYWE4NTFkOSJ9fX0=\"}]}}}}],Attributes:[{Base:25.0d,Name:\"generic.maxHealth\"}],Team:\"mobs\",HandItems:[{id:\"minecraft:fire_coral\",Count:1b,tag:{Enchantments:[{lvl:1s,id:\"minecraft:knockback\"}],AttributeModifiers:[{UUIDMost:6839568357755078786L,UUIDLeast:-6973455746412718128L,Amount:8.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:-7496085604184011549L,UUIDLeast:-5185622562047912854L,Amount:-0.12d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}},{id:\"minecraft:fire_coral\",Count:1b,tag:{Enchantments:[{lvl:1s,id:\"minecraft:knockback\"}],AttributeModifiers:[{UUIDMost:6839568357755078786L,UUIDLeast:-6973455746412718128L,Amount:8.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:-7496085604184011549L,UUIDLeast:-5185622562047912854L,Amount:-0.12d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}}]}"
			                                  );
		}
	}

	public int duration() {
		return 20 * 20; //20 seconds
	}
}
