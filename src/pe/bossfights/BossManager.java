package pe.bossfights;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import pe.bossfights.bosses.Boss;
import pe.bossfights.bosses.CAxtal;
import pe.bossfights.bosses.Masked_1;
import pe.bossfights.bosses.Masked_2;
import pe.bossfights.utils.Utils;
import pe.bossfights.utils.Utils.ArgumentException;

public class BossManager implements Listener, CommandExecutor
{
	Plugin mPlugin;

	Map<UUID, Boss> mBosses = new HashMap<UUID, Boss>();

	public BossManager(Plugin plugin)
	{
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender send, Command command, String label, String[] args)
	{
		if (args.length < 4)
			return false;

		Location endLoc;
		Entity targetEntity;

		try
		{
			targetEntity = Utils.calleeEntity(send);
			endLoc = Utils.getLocation(targetEntity.getLocation(), args[1], args[2], args[3]);
		}
		catch (ArgumentException ex)
		{
			send.sendMessage(ChatColor.RED + ex.getMessage());
			return false;
		}

		if (!(targetEntity instanceof LivingEntity))
		{
			send.sendMessage(ChatColor.RED + "Target entity is not a LivingEntity!");
			return false;
		}

		send.sendMessage(args[0].toLowerCase());
		send.sendMessage(Masked_1.identityTag);
		Boss boss;
		switch (args[0].toLowerCase())
		{
		case CAxtal.identityTag:
			boss = new CAxtal(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		case Masked_1.identityTag:
			boss = new Masked_1(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		case Masked_2.identityTag:
			boss = new Masked_2(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		default:
			//TODO helpful message
			return false;
		}

		/* Set up boss health / armor / etc */
		mBosses.put(targetEntity.getUniqueId(), boss);
		boss.init();

		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities)
		{
			if (!(entity instanceof LivingEntity))
				continue;

			// TODO: Sanity check (this should never happen)
			/*
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null)
			{
				// TODO WARNING - this should never happen
			}
			*/

			Set<String> tags = entity.getScoreboardTags();
			if (tags != null && !tags.isEmpty()) {
				if (tags.contains(CAxtal.identityTag)) {
					// TODO: Read these from the entity directly
					Location spawnLoc = entity.getLocation();
					Location endLoc = entity.getLocation();
					mBosses.put(entity.getUniqueId(), new CAxtal(mPlugin, (LivingEntity)entity, spawnLoc, endLoc));
				} else if (tags.contains(Masked_1.identityTag)) {
					// TODO: Read these from the entity directly
					Location spawnLoc = entity.getLocation();
					Location endLoc = entity.getLocation();
					mBosses.put(entity.getUniqueId(), new Masked_1(mPlugin, (LivingEntity)entity, spawnLoc, endLoc));
				} else if (tags.contains(Masked_2.identityTag)) {
					// TODO: Read these from the entity directly
					Location spawnLoc = entity.getLocation();
					Location endLoc = entity.getLocation();
					mBosses.put(entity.getUniqueId(), new Masked_2(mPlugin, (LivingEntity)entity, spawnLoc, endLoc));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkUnloadEvent(ChunkUnloadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities)
		{
			if (!(entity instanceof LivingEntity))
				continue;

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null)
			{
				boss.unload();
				mBosses.remove(entity.getUniqueId());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDeathEvent(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity))
			return;
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null)
		{
			boss.death();
			boss.unload();

			// TODO: Un-tagify drops
		}
	}
}
