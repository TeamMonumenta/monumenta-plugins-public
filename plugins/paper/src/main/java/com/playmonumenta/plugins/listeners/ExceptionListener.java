package com.playmonumenta.plugins.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.destroystokyo.paper.exception.ServerSchedulerException;

public class ExceptionListener implements Listener {

	private final Plugin mPlugin;

	public ExceptionListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void serverExceptionEvent(ServerExceptionEvent event) {
		ServerException exception = event.getException();

		if (exception instanceof ServerSchedulerException) {
			ServerSchedulerException schedException = (ServerSchedulerException)exception;

			BukkitTask task = schedException.getTask();
			mPlugin.getLogger().warning("Caught exception in " + (task.isSync() ? "sync" : "async") +
			                            " task from " + task.getOwner().getName() +
										" in class " + task.getClass().getName() +
										" - killing it");
			if (!task.isCancelled()) {
				task.cancel();
			}
		}
		exception.printStackTrace();
	}

}
