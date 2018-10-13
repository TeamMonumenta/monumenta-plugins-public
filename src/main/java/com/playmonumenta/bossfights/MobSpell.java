package com.playmonumenta.bossfights;

import com.playmonumenta.bossfights.spells.SpellAxtalMeleeMinions;
import com.playmonumenta.bossfights.spells.SpellAxtalRangedFlyingMinions;
import com.playmonumenta.bossfights.spells.SpellAxtalTntThrow;
import com.playmonumenta.bossfights.spells.SpellAxtalWitherAoe;
import com.playmonumenta.bossfights.spells.SpellBlockBreak;
import com.playmonumenta.bossfights.spells.SpellDetectionCircle;
import com.playmonumenta.bossfights.spells.SpellMaskedFrostNova;
import com.playmonumenta.bossfights.spells.SpellMaskedShadowGlade;
import com.playmonumenta.bossfights.spells.SpellMaskedSummonBlazes;
import com.playmonumenta.bossfights.spells.SpellPushPlayersAway;
import com.playmonumenta.bossfights.spells.SpellTpBehindRandomPlayer;
import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.bossfights.utils.Utils.ArgumentException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MobSpell implements CommandExecutor {
	Plugin plugin;

	public MobSpell(Plugin pl) {
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender send, Command command, String label, String[] args) {
		if (args.length < 1) {
			return false;
		}
		return spellCall(send, args);
	}

	public boolean spellCall(CommandSender send, String[] args) {
		String input = args[0].toLowerCase();
		String usage = null; // Default is just the command name

		try {
			switch (input) {
			case "axtal_wither_aoe":
				usage = "axtal_wither_aoe <radius> <power>";
				Utils.assertArgCount(args, 2);
				(new SpellAxtalWitherAoe(plugin,
				                         Utils.calleeEntity(send),
				                         Utils.parseInt(args[1], 0, 2000),
				                         Utils.parseInt(args[2], 0, 5))).run();
				break;
			case "axtal_melee_minions":
				usage = "axtal_melee_minions <count> <scope> <repeats> <nearbyRadius> <maxNearby>";
				Utils.assertArgCount(args, 5);
				(new SpellAxtalMeleeMinions(plugin,
				                            Utils.calleeEntity(send),
				                            Utils.parseInt(args[1], 0, 64),
				                            Utils.parseInt(args[2], 0, 32),
				                            Utils.parseInt(args[3], 0, 5),
				                            Utils.parseInt(args[4], 0, 1000),
				                            Utils.parseInt(args[5], 0, 1000))).run();
				break;
			case "axtal_ranged_flying_minions":
				usage = "axtal_ranged_flying_minions <count> <scope> <repeats>";
				Utils.assertArgCount(args, 3);
				(new SpellAxtalRangedFlyingMinions(plugin,
				                                   Utils.calleeEntity(send),
				                                   Utils.parseInt(args[1], 0, 64),
				                                   Utils.parseInt(args[2], 0, 32),
				                                   Utils.parseInt(args[3], 0, 5))).run();
				break;
			case "axtal_tnt_throw":
				usage = "axtal_tnt_throw <count> <cooldown>";
				Utils.assertArgCount(args, 2);
				(new SpellAxtalTntThrow(plugin,
				                        Utils.calleeEntity(send),
				                        Utils.parseInt(args[1], 0, 64),
				                        Utils.parseInt(args[2], 0, 60))).run();
				break;
			case "tp_behind":
				usage = "tp_behind";
				Utils.assertArgCount(args, 0);
				(new SpellTpBehindRandomPlayer(plugin,
											   Utils.calleeEntity(send),
											   Utils.parseInt(args[1], 0, 1000))).run();
				break;
			case "axtal_block_break":
			case "block_break":
				usage = "block_break";
				Utils.assertArgCount(args, 0);
				(new SpellBlockBreak(Utils.calleeEntity(send))).run();
				break;
			case "push_players_away":
				usage = "push_players_away <radius> <maxtime> - NOTE: When running via command, only maxtime=0 will work";
				Utils.assertArgCount(args, 2);
				(new SpellPushPlayersAway(Utils.calleeEntity(send),
				                          Utils.parseInt(args[1], 0, 64),
				                          Utils.parseInt(args[2], 0, 0))).run();
				break;
			case "detection_circle":
				usage = "detection_circle <centerx> <centery> <centerz> <radius> <duration> <targetx> <targety> <targetz>";
				Utils.assertArgCount(args, 8);
				(new SpellDetectionCircle(plugin,
				                          Utils.getLocation(Utils.calleeEntity(send).getLocation(), args[1], args[2], args[3]),
				                          Utils.parseInt(args[4], 0, 2000),
				                          Utils.parseInt(args[5], 0, 65535),
				                          Utils.getLocation(Utils.calleeEntity(send).getLocation(), args[6], args[7], args[8]))).run();
				break;
			case "masked_summon_blazes":
				usage = "masked_summon_blazes";
				Utils.assertArgCount(args, 0);
				(new SpellMaskedSummonBlazes(plugin, Utils.calleeEntity(send))).run();
				break;
			case "masked_shadow_glade":
				usage = "masked_shadow_glade <count>";
				Utils.assertArgCount(args, 1);
				(new SpellMaskedShadowGlade(plugin,
				                            Utils.calleeEntity(send).getLocation(),
				                            Utils.parseInt(args[1], 1, 4))).run();
				break;
			case "masked_frost_nova":
				usage = "masked_frost_nova <radius> <time>";
				Utils.assertArgCount(args, 2);
				(new SpellMaskedFrostNova(plugin,
				                          Utils.calleeEntity(send),
				                          Utils.parseInt(args[1], 0, 2000),
				                          Utils.parseInt(args[2], 0, 500))).run();
				break;
			default:
				send.sendMessage("Unknown spell: '" + args[0] + "'");
			}
		} catch (ArgumentException ex) {
			send.sendMessage(ChatColor.RED + ex.getMessage());
			if (usage != null) {
				send.sendMessage(ChatColor.RED + "Usage: " + usage);
			}

			return false;
		}
		return true;
	}
}
