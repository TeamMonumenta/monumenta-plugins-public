package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDelayedAction;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.EntityUtils;

public class CorruptInfestedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_corruptinfested";
	public static final int detectionRange = 30;

	LivingEntity mBoss;
	Plugin mPlugin;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CorruptInfestedBoss(plugin, boss);
	}

	public CorruptInfestedBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		mPlugin = plugin;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> mBoss.getLocation().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 1, 0.2, 0.2, 0.2, 0))
		);

		// Boss effectively does nothing
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		// Spell triggered when the boss dies
		new SpellDelayedAction(mPlugin, mBoss.getLocation(), 25,
		                       // Sound effect when boss dies
		                       (Location loc) -> {
		                           loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, 1f, 0.65f);
		                       },
		                       // Particles while maggots incubate
		                       (Location loc) -> {
		                           //TODO: Change this to a darker more appropriate particle
		                           loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.6, 0.6, 0.6, 0);
		                       },
		                       // Maggots spawn
		                       (Location loc) -> {
		                           loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, 1f, 0.1f);
		                           loc.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, -1, 0), 20, 0.6, 0.6, 0.6, 0);
		                           //TODO: Raise location up to avoid spawning in blocks?
		                           for (int i = 0; i < 2; i++) {
									   EntityUtils.summonEntityAt(loc, EntityType.PIG_ZOMBIE, "{Anger:32767s,CustomName:\"{\\\"text\\\":\\\"Fire Imp\\\"}\",Passengers:[{Potion:{id:\"minecraft:splash_potion\",Count:1b,tag:{CustomPotionEffects:[{Ambient:0b,ShowIcon:1b,ShowParticles:1b,Duration:1200,Id:12b,Amplifier:0b}],Potion:\"minecraft:empty\"}},id:\"minecraft:potion\"}],IsBaby:1b,Health:30.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Boots\\\"}\"}}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Pants\\\"}\"}}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:5643800,Name:\"{\\\"text\\\":\\\"§fInfernal Tunic\\\"}\"}}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"ca9c48a8-6147-390d-858d-4a4e9807d067\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmQ0YWJiYjRiZWM0NjlkN2E3NWNkNzQ2ZTIyMmIwYThiZWRmOTc0MzVhNjFkN2IwODNkZjc5ZDk0MDY0YiJ9fX0=\"}]}}}}],Attributes:[{Base:30.0d,Name:\"generic.maxHealth\"}],id:\"minecraft:zombie_pigman\",HandItems:[{id:\"minecraft:blaze_powder\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§4§lBlazing Soul\\\"}\"},Enchantments:[{lvl:1s,id:\"minecraft:unbreaking\"}],AttributeModifiers:[{UUIDMost:6643163485254798509L,UUIDLeast:-9135644038788542450L,Amount:-0.08d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"},{UUIDMost:-2899840319788137997L,UUIDLeast:-6474403868551417057L,Amount:10.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:blaze_powder\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§4§lBlazing Soul\\\"}\"},Enchantments:[{lvl:1s,id:\"minecraft:unbreaking\"}],AttributeModifiers:[{UUIDMost:6643163485254798509L,UUIDLeast:-9135644038788542450L,Amount:-0.08d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"},{UUIDMost:-2899840319788137997L,UUIDLeast:-6474403868551417057L,Amount:10.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}}]}");
		                           }
		                       }).run();
	}

}
