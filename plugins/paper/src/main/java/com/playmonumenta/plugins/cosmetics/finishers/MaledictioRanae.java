package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MaledictioRanae implements EliteFinisher {

	public static final String NAME = "Maledictio Ranae";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.6f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FROG_HURT, SoundCategory.PLAYERS, 1.8f, 1.0f);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1).spawnAsPlayerActive(p);
		new PartialParticle(Particle.SPELL_MOB, loc, 15, 0, 0.75, 0.25, 1).directionalMode(true).spawnAsPlayerActive(p);
		new PartialParticle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(0, 0.4, 0), 12, 0, 0.7, 0.2, 1).directionalMode(true).spawnAsPlayerActive(p);

		Frog frog = world.spawn(loc, Frog.class);
		frog.setVariant(Frog.Variant.COLD);
		frog.customName(Component.text(killedMob.getName()));
		EliteFinishers.modifyFinisherMob(frog, p, NamedTextColor.GREEN);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
			world.playSound(loc, Sound.ENTITY_FROG_AMBIENT, SoundCategory.PLAYERS, 1.8f, 1f), 5);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			// Jump away from the player
			world.playSound(frog.getLocation(), Sound.ENTITY_FROG_LONG_JUMP, SoundCategory.PLAYERS, 1.8f, 1.0f);
			Vector dir = VectorUtils.rotateTargetDirection(LocationUtils.getDirectionTo(p.getLocation(), frog.getLocation()).setY(0), 135 + 90 * Math.random(), 0).multiply(0.5);
			frog.teleport(frog.getLocation().setDirection(dir));
			frog.setVelocity(dir.setY(0.8));
		}, 20);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			frog::remove, 35);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SLIME_BALL;
	}
}
