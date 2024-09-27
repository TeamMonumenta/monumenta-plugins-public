package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class HealerAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "healer";

	public HealerAction() {
		super(IDENTIFIER);
		addParameter("radius", 8.0);
		addParameter("healing_amount", 5.0);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		if (!AdvancementUtils.checkAdvancement(player, "monumenta:handbook/spawners_/healer_spawner")) {
			AdvancementUtils.grantAdvancement(player, "monumenta:handbook/spawners_/healer_spawner");
		}
		double radius = (double) getParameter(parameters, "radius");
		double healing = (double) getParameter(parameters, "healing_amount");
		Location spawnerLoc = BlockUtils.getCenterBlockLocation(spawner);
		Hitbox.SphereHitbox hitbox = new Hitbox.SphereHitbox(spawnerLoc, radius);
		hitbox.getHitMobs().forEach(mob -> {
			double max = EntityUtils.getMaxHealth(mob);
			mob.setHealth(Math.min(max, mob.getHealth() + healing));
			aesthetics(mob);
		});
		hitbox.getHitPlayers(true).forEach(p -> {
			double max = EntityUtils.getMaxHealth(p);
			p.setHealth(Math.min(max, p.getHealth() + healing));
			aesthetics(p);
		});
	}

	private void aesthetics(LivingEntity entity) {
		Location loc = LocationUtils.getHalfHeightLocation(entity);
		loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 0.6f, 1.25f);
		loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 0.6f, 2f);

		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 15).delta(0.25, 0.5, 0.25)
			.extra(0.3).spawnAsEnemy();
		new PartialParticle(Particle.HEART, loc, 5).delta(0.25, 0.5, 0.25).spawnAsEnemy();
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PartialParticle(Particle.HEART, blockLoc, 1).spawnAsEnemy();
	}
}
