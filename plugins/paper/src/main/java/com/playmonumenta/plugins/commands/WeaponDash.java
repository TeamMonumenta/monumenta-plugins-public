package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.particle.PartialParticle;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WeaponDash extends GenericCommand {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.weapondash");

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		DoubleArgument horizontalArg = new DoubleArgument("horizontal");
		DoubleArgument verticalArg = new DoubleArgument("vertical");
		IntegerArgument durationArg = new IntegerArgument("duration", 0);

		new CommandAPICommand("weapondash")
			.withPermission(perms)
			.withArguments(playerArg)
			.withArguments(horizontalArg)
			.withArguments(verticalArg)
			.withArguments(durationArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				Location playerLoc = player.getLocation();
				playerLoc.setPitch(0);
				Vector v = playerLoc.getDirection();
				v.multiply(args.getByArgument(horizontalArg));
				v.setY(args.getByArgument(verticalArg));
				player.setVelocity(v);
				player.setNoDamageTicks(args.getByArgument(durationArg));
				player.getNoDamageTicks();


				player.getWorld().playSound(playerLoc, Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1, 2);
				player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1, 1.5f);
				new PartialParticle(Particle.CLOUD, playerLoc, 15, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(player);
			})
			.register();
	}
}
