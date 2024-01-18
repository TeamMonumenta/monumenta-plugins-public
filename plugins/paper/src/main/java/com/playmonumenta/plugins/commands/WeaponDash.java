package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.particle.PartialParticle;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WeaponDash extends GenericCommand {
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.weapondash");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new DoubleArgument("horizontal"));
		arguments.add(new DoubleArgument("vertical"));
		arguments.add(new IntegerArgument("duration"));

		new CommandAPICommand("weapondash")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Location playerLoc = player.getLocation();
				playerLoc.setPitch(0);
				Vector v = playerLoc.getDirection();
				v.multiply((double) args[1]);
				v.setY((double) args[2]);
				player.setVelocity(v);
				player.setNoDamageTicks((int) args[3]);
				player.getNoDamageTicks();


				player.getWorld().playSound(playerLoc, Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1, 2);
				player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1, 1.5f);
				new PartialParticle(Particle.CLOUD, playerLoc, 15, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(player);
			})
			.register();
	}
}
