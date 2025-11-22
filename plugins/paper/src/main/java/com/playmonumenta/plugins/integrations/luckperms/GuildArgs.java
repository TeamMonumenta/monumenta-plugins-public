package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.Nullable;

public abstract class GuildArgs {
	protected final String mLabel;
	protected final List<Argument<?>> mGuildArgs = new ArrayList<>();

	protected GuildArgs(String label) {
		mLabel = label;
	}

	public String label() {
		return mLabel;
	}

	public List<Argument<?>> getArgs() {
		return new ArrayList<>(mGuildArgs);
	}

	public abstract @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args);

	public static List<GuildArgs> getArgVariants(String prefix, String suffix) {
		List<GuildArgs> results = new ArrayList<>();

		results.add(new CalleeGuildArgs(prefix, suffix));
		results.add(new WorldArgs(prefix, suffix));
		results.add(new SelectorGuildArgs(prefix, suffix));
		results.add(new PlotIdConstantArgs(prefix, suffix));
		results.add(new PlotIdScoreArgs(prefix, suffix));
		results.add(new GuildTagArgs(prefix, suffix));
		results.add(new GuildNameArgs(prefix, suffix));

		return results;
	}

	private static final class CalleeGuildArgs extends GuildArgs {
		private CalleeGuildArgs(String prefix, String suffix) {
			super(prefix + "own_guild" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));
		}

		@Override
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			CommandSender callee = CommandUtils.getCallee(sender);
			if (!(callee instanceof Player player)) {
				return null;
			}
			return LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(player));
		}
	}

	private static final class WorldArgs extends GuildArgs {
		private WorldArgs(String prefix, String suffix) {
			super(prefix + "guild_plot" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));
		}

		@Override
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			Long plotId = GuildPlotUtils.getGuildPlotNumber(sender.getWorld());
			if (plotId == null || plotId == 0) {
				return null;
			}

			for (Group guild : LuckPermsIntegration.getLoadedGuilds()) {
				if (plotId.equals(LuckPermsIntegration.getGuildPlotId(guild))) {
					return guild;
				}
			}
			return null;
		}
	}

	private static final class SelectorGuildArgs extends GuildArgs {
		private final EntitySelectorArgument.OnePlayer mSelectorArg;

		private SelectorGuildArgs(String prefix, String suffix) {
			super(prefix + "selector_guild" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));

			mSelectorArg = new EntitySelectorArgument.OnePlayer(prefix + "selector" + suffix);
			mGuildArgs.add(mSelectorArg);
		}

		@Override
		@SuppressWarnings("DataFlowIssue")
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			Player player = args.getByArgument(mSelectorArg);
			return LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(player));
		}
	}

	private static final class PlotIdConstantArgs extends GuildArgs {
		private final IntegerArgument mPlotIdArg;

		private PlotIdConstantArgs(String prefix, String suffix) {
			super(prefix + "plot_id_constant" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));

			mPlotIdArg = new IntegerArgument(prefix + "plot_id" + suffix);
			mGuildArgs.add(mPlotIdArg);
		}

		@Override
		@SuppressWarnings("DataFlowIssue")
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			CommandSender callee = CommandUtils.getCallee(sender);
			if (!(callee instanceof Player player)) {
				return null;
			}

			int guildPlotId = args.getByArgument(mPlotIdArg);
			for (Group testGuildGroup : LuckPermsIntegration.getRelevantGuilds(player, true, false)) {
				Group testGuild = LuckPermsIntegration.getGuildRoot(testGuildGroup);
				Long testGuildPlotId = LuckPermsIntegration.getGuildPlotId(testGuild);
				if (testGuildPlotId != null && testGuildPlotId == guildPlotId) {
					return testGuild;
				}
			}
			return null;
		}
	}

	private static final class PlotIdScoreArgs extends GuildArgs {
		private final ScoreHolderArgument.Single mPlotIdScoreHolderArg;
		private final ObjectiveArgument mPlotIdScoreObjectiveArg;

		private PlotIdScoreArgs(String prefix, String suffix) {
			super(prefix + "plot_id_score" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));

			mPlotIdScoreHolderArg = new ScoreHolderArgument.Single(prefix + "plot_id_score_holder" + suffix);
			mGuildArgs.add(mPlotIdScoreHolderArg);
			mPlotIdScoreObjectiveArg = new ObjectiveArgument(prefix + "plot_id_objective" + suffix);
			mGuildArgs.add(mPlotIdScoreObjectiveArg);
		}

		@Override
		@SuppressWarnings("DataFlowIssue")
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			CommandSender callee = CommandUtils.getCallee(sender);
			if (!(callee instanceof Player player)) {
				return null;
			}

			String scoreHolder = args.getByArgument(mPlotIdScoreHolderArg);
			Objective scoreObjective = args.getByArgument(mPlotIdScoreObjectiveArg);
			int guildPlotId = ScoreboardUtils.getScoreboardValue(scoreHolder, scoreObjective).orElse(0);

			for (Group testGuildGroup : LuckPermsIntegration.getRelevantGuilds(player, true, false)) {
				Group testGuild = LuckPermsIntegration.getGuildRoot(testGuildGroup);
				Long testGuildPlotId = LuckPermsIntegration.getGuildPlotId(testGuild);
				if (testGuildPlotId != null && testGuildPlotId == guildPlotId) {
					return testGuild;
				}
			}
			return null;
		}
	}

	private static final class GuildTagArgs extends GuildArgs {
		private final TextArgument mGuildTagArg;

		private GuildTagArgs(String prefix, String suffix) {
			super(prefix + "guild_tag" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));

			mGuildTagArg = new TextArgument(prefix + "tag" + suffix);
			mGuildTagArg.replaceSuggestions(GuildArguments.TAG_SUGGESTIONS);
			mGuildArgs.add(mGuildTagArg);
		}

		@Override
		@SuppressWarnings("DataFlowIssue")
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			CommandSender callee = CommandUtils.getCallee(sender);
			if (!(callee instanceof Player player)) {
				return null;
			}

			String guildTag = args.getByArgument(mGuildTagArg);
			for (Group testGuildGroup : LuckPermsIntegration.getRelevantGuilds(player, true, false)) {
				Group testGuild = LuckPermsIntegration.getGuildRoot(testGuildGroup);
				if (testGuild == null) {
					continue;
				}
				String testGuildTag = LuckPermsIntegration.getGuildPlainTag(testGuild);
				if (guildTag.equals(testGuildTag)) {
					return testGuild;
				}
			}
			return null;
		}
	}

	private static final class GuildNameArgs extends GuildArgs {
		private final TextArgument mGuildNameArg;

		private GuildNameArgs(String prefix, String suffix) {
			super(prefix + "guild_name" + suffix);
			mGuildArgs.add(new LiteralArgument(mLabel));

			mGuildNameArg = new TextArgument(prefix + "name" + suffix);
			mGuildNameArg.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS);
			mGuildArgs.add(mGuildNameArg);
		}

		@Override
		@SuppressWarnings("DataFlowIssue")
		public @Nullable Group getGuild(NativeProxyCommandSender sender, CommandArguments args) {
			CommandSender callee = CommandUtils.getCallee(sender);
			if (!(callee instanceof Player player)) {
				return null;
			}

			String guildTag = args.getByArgument(mGuildNameArg);
			for (Group testGuildGroup : LuckPermsIntegration.getRelevantGuilds(player, true, false)) {
				Group testGuild = LuckPermsIntegration.getGuildRoot(testGuildGroup);
				if (testGuild == null) {
					continue;
				}
				String testGuildTag = LuckPermsIntegration.getRawGuildName(testGuild);
				if (guildTag.equals(testGuildTag)) {
					return testGuild;
				}
			}
			return null;
		}
	}
}
