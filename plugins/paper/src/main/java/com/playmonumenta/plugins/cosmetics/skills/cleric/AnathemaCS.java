package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AnathemaCS extends CrusadeCS {

	public static final String NAME = "Anathema";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Emotions embodied into curses can be rather... damaging.",
			"And hatred happens to be the deadliest."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CORAL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	Particle.DustOptions RED_DARK = new Particle.DustOptions(Color.fromRGB(180, 0, 80), 0.65f);
	Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(210, 0, 90), 0.7f);

	@Override
	public void crusadeTag(Entity enemy) {
		Location center = enemy.getLocation().add(0, enemy.getHeight() + 1.0, 0);
		Vector front = center.getDirection().clone().setY(0).normalize().multiply(0.75);
		Vector vBottom = VectorUtils.rotateTargetDirection(front, 0, 90);
		Vector vRight = VectorUtils.rotateTargetDirection(vBottom, 72, 0);
		Vector vTopRight = VectorUtils.rotateTargetDirection(vBottom, 144, 0);
		Vector vTopLeft = VectorUtils.rotateTargetDirection(vBottom, 216, 0);
		Vector vLeft = VectorUtils.rotateTargetDirection(vBottom, 288, 0);
		Location topLeft = center.clone().add(vTopLeft);
		Location topRight = center.clone().add(vTopRight);
		Location right = center.clone().add(vRight);
		Location bottom = center.clone().add(vBottom);
		Location left = center.clone().add(vLeft);
		new PPLine(Particle.REDSTONE, bottom, topLeft).countPerMeter(12).delta(0.01).extra(0).groupingDistance(0).data(RED).spawnAsEnemyBuff();
		new PPLine(Particle.REDSTONE, topLeft, right).countPerMeter(12).delta(0.01).extra(0).groupingDistance(0).data(RED_DARK).spawnAsEnemyBuff();
		new PPLine(Particle.REDSTONE, right, left).countPerMeter(12).delta(0.01).extra(0).groupingDistance(0).data(RED_DARK).spawnAsEnemyBuff();
		new PPLine(Particle.REDSTONE, left, topRight).countPerMeter(12).delta(0.01).extra(0).groupingDistance(0).data(RED_DARK).spawnAsEnemyBuff();
		new PPLine(Particle.REDSTONE, topRight, bottom).countPerMeter(12).delta(0.01).extra(0).groupingDistance(0).data(RED).spawnAsEnemyBuff();
	}
}
