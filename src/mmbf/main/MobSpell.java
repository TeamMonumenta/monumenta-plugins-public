package mmbf.main;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.bossfights.spells.SpellBlockBreak;
import mmms.spells.AxtalDeathRay;
import mmms.spells.AxtalMeleeMinions;
import mmms.spells.AxtalRangedFlyingMinions;
import mmms.spells.AxtalSneakup;
import mmms.spells.AxtalTntThrow;
import mmms.spells.AxtalWitherAoe;
import mmms.spells.CommandSpell;
import mmms.spells.DetectionCircle;
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
			case "commandspell":
				(new CommandSpell()).onSpell(send, args);
				break;
			case "axtal_wither_aoe":
				(new AxtalWitherAoe(plugin)).onSpell(send, args);
				break;
			case "axtal_melee_minions":
				(new AxtalMeleeMinions(plugin)).onSpell(send, args);
				break;
			case "axtal_ranged_flying_minions":
				(new AxtalRangedFlyingMinions(plugin)).onSpell(send, args);
				break;
			case "axtal_tnt_throw":
				(new AxtalTntThrow(plugin)).onSpell(send, args);
				break;
			case "axtal_sneakup":
				(new AxtalSneakup(plugin)).onSpell(send, args);
				break;
			case "axtal_block_break":
			case "block_break":
				usage = "block_break";
				CommandUtils.assertArgCount(args, 0);
				(new SpellBlockBreak(CommandUtils.calleeEntity(send))).run();
				break;
			case "axtal_death_ray":
				(new AxtalDeathRay(plugin)).onSpell(send, args);
				break;
			case "detection_circle":
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
				CommandUtils.assertArgCount(args, 1);
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
