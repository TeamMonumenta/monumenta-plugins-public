package com.playmonumenta.plugins.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Base class for command.
 */
public abstract class AbstractCommand implements CommandExecutor {
	private final String mName;
	private final String mDescription;
	protected final Plugin mPlugin;

	/**
	 * Create the command.
	 *
	 * @param name        the command name
	 * @param description the command description
	 * @param plugin      the plugin object
	 */
	public AbstractCommand(String name, String description, Plugin plugin) {
		this.mName = name;
		this.mDescription = description;
		this.mPlugin = plugin;
	}

	/**
	 * Configure the command's {@link ArgumentParser}.
	 *
	 * @param parser the {@link ArgumentParser} specific to the command
	 */
	protected abstract void configure(final ArgumentParser parser);

	/**
	 * Validate preconditions to running the command.
	 *
	 * @return true if preconditions are true
	 */
	protected boolean validate(final CommandContext context) {
		return true;
	}

	/**
	 * Executes when the user runs this specific command.
	 *
	 * @param context contains all command related data
	 * @throws Exception if something goes wrong
	 */
	protected abstract boolean run(final CommandContext context) throws Exception;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		final ArgumentParser parser = initParser(sender, mName, mDescription);

		configure(parser);

		final Namespace namespace;
		try {
			namespace = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			sendArgumentParserError(sender, parser, e);
			return false;
		}

		final CommandContext context = new CommandContext(sender, command, namespace);

		if (!validate(context)) {
			// Assume any messages or logging is handled within the method
			return false;
		}

		try {
			return run(context);
		} catch (Exception e) {
			onError(context, e);
			return false;
		}
	}

	/**
	 * Getter for the plugin name
	 *
	 * @return plugin name
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Method is called if there is an issue running the command.
	 *
	 * @param context the context data for the command
	 * @param e       the exception that was thrown when running the command
	 */
	protected void onError(final CommandContext context, final Throwable e) {
		sendErrorMessage(context, "An error occurred while attempting to run this command");
		mPlugin.getLogger().log(Level.WARNING, e, () -> "An error occurred for '" + context.getSender().getName() + "' when running the command: " + mName);
	}

	/**
	 * Send the argument parsing error message to the sender.
	 *
	 * @param sender the source of the command
	 * @param e      the parser exception
	 */
	private void sendArgumentParserError(CommandSender sender, ArgumentParser parser, ArgumentParserException e) {
		StringWriter out = new StringWriter();

		parser.handleError(e, new PrintWriter(out));

		sender.sendMessage(ChatColor.RED + out.toString().replaceAll("\\r\\n?", "\n"));
	}

	/**
	 * Build the argument parser.
	 *
	 * @param name        the command name
	 * @param description the command description
	 * @return the parser
	 */
	private static ArgumentParser initParser(final CommandSender sender, final String name, final String description) {
		final ArgumentParser parser = ArgumentParsers
		                              .newFor(name)
		                              .addHelp(false) // Replace with custom help below so we can return to sender instead of console
		                              .defaultFormatWidth(Integer.MAX_VALUE) // Minecraft client wraps for us and we don't know it's width for certain
		                              .build()
		                              .description(description);

		// Add custom help argument
		parser.addArgument("-h", "--help")
		.help("show this help message and exit")
		.action(new ArgumentAction() {
			@Override
			public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
				StringWriter out = new StringWriter();

				parser.printHelp(new PrintWriter(out));
				sender.sendMessage(ChatColor.YELLOW + out.toString().replaceAll("\\r\\n?", "\n"));

				throw new HelpScreenException(parser);
			}

			@Override
			public void onAttach(Argument arg) {
			}

			@Override
			public boolean consumeArgument() {
				return false;
			}
		})
		.setDefault(Arguments.SUPPRESS);

		return parser;
	}

	/**
	 * Helper method for sending messages.
	 *
	 * @param context the context data for the command
	 * @param message the message to send to the sender
	 */
	protected static void sendMessage(final CommandContext context, String message) {
		context.getSender().sendMessage(message);
	}

	/**
	 * Helper method for sending error messages.
	 *
	 * @param context the context data for the command
	 * @param message the message to send to the sender
	 */
	protected static void sendErrorMessage(final CommandContext context, String message) {
		context.getSender().sendMessage(ChatColor.RED + message);
	}
}
