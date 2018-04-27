package mmbf.main;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import mmms.spells.AxtalBlockBreak;
import mmms.spells.AxtalDeathRay;
import mmms.spells.AxtalMeleeMinions;
import mmms.spells.AxtalRangedFlyingMinions;
import mmms.spells.AxtalSneakup;
import mmms.spells.AxtalTntThrow;
import mmms.spells.AxtalWitherAoe;
import mmms.spells.CommandSpell;
import mmms.spells.DetectionCircle;
import pe.bossfights.commands.MaskedEldritchBeam;
import pe.bossfights.commands.MaskedFrostNova;
import pe.bossfights.commands.MaskedShadowGlade;
import pe.bossfights.commands.MaskedSummonBlazes;

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
			(new AxtalBlockBreak()).onSpell(send, args);
			break;
		case "axtal_death_ray":
			(new AxtalDeathRay(plugin)).onSpell(send, args);
			break;
		case "detection_circle":
			(new DetectionCircle(plugin)).onSpell(send, args);
			break;
		case "masked_summon_blazes":
			(new MaskedSummonBlazes(plugin)).onSpell(send, args);
			break;
		case "masked_shadow_glade":
			(new MaskedShadowGlade(plugin)).onSpell(send, args);
			break;
		case "masked_eldritch_beam":
			(new MaskedEldritchBeam(plugin)).onSpell(send, args);
			break;
		case "masked_frost_nova":
			(new MaskedFrostNova(plugin)).onSpell(send, args);
			break;
		default:
			send.sendMessage("Unknown spell: '" + args[0] + "'");
		}
		return true;
	}
}
