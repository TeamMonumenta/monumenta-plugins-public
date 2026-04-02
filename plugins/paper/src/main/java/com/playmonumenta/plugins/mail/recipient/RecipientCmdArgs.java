package com.playmonumenta.plugins.mail.recipient;

import com.playmonumenta.plugins.mail.MailCache;
import com.playmonumenta.plugins.mail.MailMan;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;

public abstract class RecipientCmdArgs {
	public enum ArgTarget {
		CALLER,
		CALLEE,
		ARG,
	}

	public final ArgTarget mTarget;
	protected final List<Argument<?>> mRecipientArgs = new ArrayList<>();

	protected RecipientCmdArgs(ArgTarget target) {
		mTarget = target;
	}

	public abstract String label();

	public List<Argument<?>> recipientArgs() {
		return new ArrayList<>(mRecipientArgs);
	}

	public abstract CompletableFuture<Recipient> getRecipient(CommandSender sender, CommandArguments args);

	public CompletableFuture<MailCache> getRecipientCache(CommandSender sender, CommandArguments args) {
		CompletableFuture<MailCache> future = new CompletableFuture<>();
		getRecipient(sender, args).whenComplete((recipient, ex) -> {
			if (ex != null) {
				future.completeExceptionally(ex);
				return;
			}

			MailCache mailCache = MailMan.recipientMailCache(recipient);
			// Might as well ensure this is fully initialized if we have to get it async anyway
			mailCache.awaitInitialization().whenComplete((unused, ex2) -> {
				if (ex2 != null) {
					future.completeExceptionally(ex2);
					return;
				}
				future.complete(mailCache);
			});
		});

		return future;
	}
}
