package com.playmonumenta.plugins.commands;

public class TellMiniMessage {
	// TODO: tellmini is moved to the mixins repo due to a bug in commandapi
	/*private static final String PERMISSION = "monumenta.command.tellmini";
	private static final String NAME = "tellmini";

	private static CommandAPICommand generateSubcommand(String name, BiConsumer<Player, Component> send) {
		return new CommandAPICommand(name).withArguments(
			new EntitySelectorArgument.ManyPlayers("recipients"),
			new GreedyStringArgument("message").setOptional(true)
		).executes((sender, args) -> {
			if (sender instanceof NativeProxyCommandSender nativeProxyCommandSender) {
				sender = nativeProxyCommandSender.getCallee(); // don't use get caller probably
			}

			Collection<Player> recipients = Objects.requireNonNull(args.getUnchecked("recipients"));
			String title = args.getUnchecked("message");
			Component parsed = title == null ? Component.empty() : MessagingUtils.fromMiniMessage(title);
			for (Player recipient : recipients) {
				try {
					send.accept(recipient, PaperComponents.resolveWithContext(parsed, sender, recipient, false));
				} catch (IOException e) {
					MMLog.warning("tellmini: failed to resolveWithContext:", e);
					return;
				}
			}
		});
	}*/

	public static void register() {/*
		new CommandAPICommand(NAME)
			.withPermission(PERMISSION)
			.withSubcommands(
				generateSubcommand("title", (r, m) -> r.sendTitlePart(TitlePart.TITLE, m)),
				generateSubcommand("subtitle", (r, m) -> r.sendTitlePart(TitlePart.SUBTITLE, m)),
				generateSubcommand("actionbar", Audience::sendActionBar),
				generateSubcommand("msg", Audience::sendMessage)
			).register();

		generateSubcommand(NAME, Audience::sendMessage)
			.withPermission(PERMISSION)
			.register();*/
	}
}
