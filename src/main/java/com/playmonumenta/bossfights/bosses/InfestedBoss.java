package com.playmonumenta.bossfights.bosses;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellRunAction;
import com.playmonumenta.bossfights.spells.SpellDelayedAction;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class InfestedBoss extends BossAbilityGroup
{
	public static final String identityTag = "boss_infested";
	public static final int detectionRange = 30;

	LivingEntity mBoss;
	Plugin mPlugin;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new InfestedBoss(plugin, boss);
	}

	public InfestedBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;
		mPlugin = plugin;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> mBoss.getLocation().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 1, 0.2, 0.2, 0.2, 0))
		);

		// Boss effectively does nothing
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void death()
	{
		// Spell triggered when the boss dies
		new SpellDelayedAction(mPlugin, mBoss.getLocation(), 60,
				  // Sound effect when boss dies
				  (Location loc) ->
					{
						loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, 1f, 0.65f);
					},
				  // Particles while maggots incubate
				  (Location loc) ->
					{
						//TODO: Change this to a darker more appropriate particle
						loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.6, 0.6, 0.6, 0);
					},
				  // Maggots spawn
				  (Location loc) ->
					{
						loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, 1f, 0.1f);
						loc.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, -1, 0), 50, 0.6, 0.6, 0.6, 0);
						//TODO: Raise location up to avoid spawning in blocks?
						for (int i = 0; i < 4; i++) {
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "summon minecraft:silverfish " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " {CustomName:\"Maggot\",Health:16.0f,Attributes:[{Base:16.0d,Name:\"generic.maxHealth\"}],ActiveEffects:[{Ambient:1b,ShowParticles:1b,Duration:72000,Id:5b,Amplifier:1b}]}");
						}
					}
		).run();
	}

}
