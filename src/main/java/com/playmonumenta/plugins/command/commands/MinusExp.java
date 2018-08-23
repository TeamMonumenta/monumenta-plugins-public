package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MinusExp extends AbstractPlayerCommand {

	public MinusExp(Plugin plugin) {
		super(
		    "minusexp",
		    "Subtracts an equivalent amount of levels worth of experience",
		    plugin
		);
	}

	/**
	 * /minusexp <levels>
	 *
	 * @param parser the {@link ArgumentParser} specific to the command
	 */
	@Override
	protected void configure(ArgumentParser parser) {
		parser.addArgument("levels")
		.help("number of levels to subtract")
		.type(Integer.class)
		.nargs("?");
	}

	@Override
	protected boolean run(CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();
		final Integer levels = context.getNamespace().getInt("levels");

		_setTotalExp(player, _getTotalExp(player) - _levelToExp(levels));

		return true;
	}

	private float _levelToExp(int level) {
		float levelF = (float)level;

		if (level <= 0) {
			return 0.0F;
		} else if (level <= 16) {
			return levelF * levelF + 6.0F * levelF;
		} else if (level <= 31) {
			return 2.5F * levelF * levelF - 40.5F * levelF + 360.0F;
		} else {
			return 4.5F * levelF * levelF - 162.5F * levelF + 2220.0F;
		}
	}

	private float _getTotalExp(Player player) {
		int level = player.getLevel();
		float exp = _levelToExp(level);

		// Get the component from current progression to the next level
		exp += (_levelToExp(level + 1) - _levelToExp(level)) * player.getExp();

		return exp;
	}

	private void _setTotalExp(Player player, float exp) {
		if (exp < 0) {
			exp = 0.0F;
		}

		int level = 0;
		while (_levelToExp(level + 1) < exp) {
			level++;
		}

		player.setLevel(level);
		player.setExp((exp - _levelToExp(level)) / (_levelToExp(level + 1) - _levelToExp(level)));
	}
}
