package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDelayedAction;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class InfestedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_infested";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new InfestedBoss(plugin, boss);
	}

	public InfestedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellRunAction(() -> new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 1, 0.2, 0.2, 0.2, 0).spawnAsEntityActive(boss))
		);

		// Boss effectively does nothing
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		// Spell triggered when the boss dies
		new SpellDelayedAction(mPlugin, mBoss.getLocation(), 25,
			// Sound effect when boss dies
			(Location loc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_DEATH, SoundCategory.HOSTILE, 1f, 0.65f);
			},
			// Particles while maggots incubate
			(Location loc) -> {
				//TODO: Change this to a darker more appropriate particle
				new PartialParticle(Particle.CLOUD, loc, 1, 0.6, 0.6, 0.6, 0).spawnAsEntityActive(mBoss);
			},
			// Maggots spawn
			(Location loc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.HOSTILE, 1f, 0.1f);
				new PartialParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, -1, 0), 20, 0.6, 0.6, 0.6, 0).spawnAsEntityActive(mBoss);
				//TODO: Raise location up to avoid spawning in blocks?
				for (int i = 0; i < 4; i++) {
					LibraryOfSoulsIntegration.summon(loc, "Maggot");
				}
			}).run();
	}

}
