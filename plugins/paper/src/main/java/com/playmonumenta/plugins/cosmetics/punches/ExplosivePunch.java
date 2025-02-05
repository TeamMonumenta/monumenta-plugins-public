package com.playmonumenta.plugins.cosmetics.punches;

import com.playmonumenta.networkchat.RemotePlayerListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ExplosivePunch implements PlayerPunch {
	public static final String NAME = "Explosive";

	@Override
	public void run(Player bully, Player victim) {
		World world = victim.getWorld();
		Location loc = victim.getLocation();

		new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1).spawnAsPlayerActive(bully);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5, 0.05).spawnAsPlayerActive(bully);

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);

		victim.setVelocity(new Vector(0, 2.5, 0));
	}

	@Override
	public void broadcastPunchMessage(Player bully, Player victim, List<Player> playersInWorld, boolean isRemotePunch) {
		for (Player player : playersInWorld) {
			player.sendMessage(
				RemotePlayerListener.getPlayerComponent(bully.getUniqueId())
					.append(Component.text((isRemotePunch ? " remotely punched " : " punched "), NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
					.append(RemotePlayerListener.getPlayerComponent(victim.getUniqueId()))
					.append(Component.text(" into the sky!", NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
			);
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}
}
