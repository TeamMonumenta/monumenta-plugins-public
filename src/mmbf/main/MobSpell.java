package mmbf.main;

import mmms.spells.DetectionCircle;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.bossfights.spells.SpellAxtalDeathRay;
import pe.bossfights.spells.SpellAxtalMeleeMinions;
import pe.bossfights.spells.SpellAxtalRangedFlyingMinions;
import pe.bossfights.spells.SpellAxtalSneakup;
import pe.bossfights.spells.SpellAxtalTntThrow;
import pe.bossfights.spells.SpellAxtalWitherAoe;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellMaskedEldritchBeam;
import pe.bossfights.spells.SpellMaskedFrostNova;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.utils.CommandUtils;
import pe.bossfights.utils.CommandUtils.ArgumentException;

public class MobSpell implements CommandExecutor
{
	Main plugin;

	public MobSpell(Main pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender send, Command command, String label, String[] args)
	{
		if (args.length < 1)
			return (false);
		return spellCall(send, args);
	}

	public boolean spellCall(CommandSender send, String[] args)
	{
		String input = args[0].toLowerCase();
		String usage = null; // Default is just the command name

		try
		{
			switch (input)
			{
			case "axtal_wither_aoe":
				usage = "axtal_wither_aoe <radius> <power>";
				CommandUtils.assertArgCount(args, 2);
				(new SpellAxtalWitherAoe(plugin,
				                         CommandUtils.calleeEntity(send),
				                         CommandUtils.parseInt(args[1], 0, 2000),
				                         CommandUtils.parseInt(args[2], 0, 5))).run();
				break;
			case "axtal_melee_minions":
				usage = "axtal_melee_minions <count> <scope> <repeats>";
				CommandUtils.assertArgCount(args, 3);
				(new SpellAxtalMeleeMinions(plugin,
				                            CommandUtils.calleeEntity(send),
				                            CommandUtils.parseInt(args[1], 0, 64),
				                            CommandUtils.parseInt(args[2], 0, 32),
				                            CommandUtils.parseInt(args[3], 0, 5))).run();
				break;
			case "axtal_ranged_flying_minions":
				usage = "axtal_ranged_flying_minions <count> <scope> <repeats>";
				CommandUtils.assertArgCount(args, 3);
				(new SpellAxtalRangedFlyingMinions(plugin,
				                                   CommandUtils.calleeEntity(send),
				                                   CommandUtils.parseInt(args[1], 0, 64),
				                                   CommandUtils.parseInt(args[2], 0, 32),
				                                   CommandUtils.parseInt(args[3], 0, 5))).run();
				break;
			case "axtal_tnt_throw":
				usage = "axtal_tnt_throw <count> <cooldown>";
				CommandUtils.assertArgCount(args, 2);
				(new SpellAxtalTntThrow(plugin,
				                        CommandUtils.calleeEntity(send),
				                        CommandUtils.parseInt(args[1], 0, 64),
				                        CommandUtils.parseInt(args[2], 0, 60))).run();
				break;
			case "axtal_sneakup":
				usage = "axtal_sneakup";
				CommandUtils.assertArgCount(args, 0);
				(new SpellAxtalSneakup(plugin, CommandUtils.calleeEntity(send))).run();
				break;
			case "axtal_block_break":
			case "block_break":
				usage = "block_break";
				CommandUtils.assertArgCount(args, 0);
				(new SpellBlockBreak(CommandUtils.calleeEntity(send))).run();
				break;
			case "axtal_death_ray":
				usage = "axtal_death_ray";
				CommandUtils.assertArgCount(args, 0);
				(new SpellAxtalDeathRay(plugin, CommandUtils.calleeEntity(send))).run();
				break;
			case "detection_circle":
				//TODO UPGRADE
				(new DetectionCircle(plugin)).onSpell(send, args);
				break;
			case "masked_summon_blazes":
				usage = "masked_summon_blazes";
				CommandUtils.assertArgCount(args, 0);
				(new SpellMaskedSummonBlazes(plugin, CommandUtils.calleeEntity(send))).run();
				break;
			case "masked_shadow_glade":
				usage = "masked_shadow_glade <count>";
				CommandUtils.assertArgCount(args, 1);
				(new SpellMaskedShadowGlade(plugin,
				                            CommandUtils.calleeEntity(send).getLocation(),
				                            CommandUtils.parseInt(args[1], 1, 4))).run();
				break;
			case "masked_eldritch_beam":
				usage = "masked_eldritch_beam";
				CommandUtils.assertArgCount(args, 0);
				(new SpellMaskedEldritchBeam(plugin, CommandUtils.calleeEntity(send))).run();
				break;
			case "masked_frost_nova":
				usage = "masked_frost_nova <radius> <time>";
				CommandUtils.assertArgCount(args, 2);
				(new SpellMaskedFrostNova(plugin,
				                          CommandUtils.calleeEntity(send),
				                          CommandUtils.parseInt(args[1], 0, 2000),
				                          CommandUtils.parseInt(args[2], 0, 500))).run();
				break;
			default:
				send.sendMessage("Unknown spell: '" + args[0] + "'");
			}
		}
		catch (ArgumentException ex)
		{
			send.sendMessage(ChatColor.RED + ex.getMessage());
			if (usage != null)
				send.sendMessage(ChatColor.RED + "Usage: " + usage);

			return false;
		}
		return true;
	}
}
