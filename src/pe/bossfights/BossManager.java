package pe.bossfights;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import pe.bossfights.bosses.Boss;
import pe.bossfights.bosses.CAxtal;
import pe.bossfights.bosses.GenericBoss;
import pe.bossfights.bosses.Masked_1;
import pe.bossfights.bosses.Masked_2;
import pe.bossfights.utils.SerializationUtils;
import pe.bossfights.utils.Utils;
import pe.bossfights.utils.Utils.ArgumentException;

public class BossManager implements Listener, CommandExecutor
{
	Plugin mPlugin;
	Map<UUID, Boss> mBosses;

	public BossManager(Plugin plugin)
	{
		mPlugin = plugin;
		mBosses = new HashMap<UUID, Boss>();

		/* When starting up, look for bosses in all current world entities */
		ProcessEntities(Bukkit.getWorlds().get(0).getEntities());
	}

	@Override
	public boolean onCommand(CommandSender send, Command command, String label, String[] args)
	{
		if (args.length != 4)
		{
			send.sendMessage(ChatColor.RED + "This command requires exactly four arguments!");
			return false;
		}

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
		case GenericBoss.identityTag:
			boss = new GenericBoss(mPlugin, (LivingEntity)targetEntity);
			break;
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
			send.sendMessage(ChatColor.RED + "Invalid boss name!");
			send.sendMessage(ChatColor.RED + "Valid options are: [" + GenericBoss.identityTag + "," +
			                 CAxtal.identityTag + "," + Masked_1.identityTag + "," + Masked_2.identityTag + "]");
			return false;
		}

		/* Set up boss health / armor / etc */
		mBosses.put(targetEntity.getUniqueId(), boss);
		boss.init();

		return true;
	}


	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event)
	{
		Entity entity = event.getEntity();

		if (!(entity instanceof LivingEntity))
			return;

		/* This should never happen */
		if (mBosses.get(entity.getUniqueId()) != null)
			mPlugin.getLogger().log(Level.SEVERE, "EntitySpawnEvent: Boss spawned that is already tracked!");

		Set<String> tags = entity.getScoreboardTags();
		if (tags != null && !tags.isEmpty())
		{
			Boss boss = null;
			if (tags.contains(GenericBoss.identityTag))
				boss = new GenericBoss(mPlugin, (LivingEntity)entity);

			if (boss != null)
			{
				mBosses.put(entity.getUniqueId(), boss);
				boss.init();
			}
		}
	}

	private void ProcessEntities(List<Entity> entities)
	{
		for (Entity entity : entities)
		{
			if (!(entity instanceof LivingEntity))
				continue;

			/* This should never happen */
			if (mBosses.get(entity.getUniqueId()) != null)
				mPlugin.getLogger().log(Level.SEVERE, "ProcessEntities: Adding boss that is already tracked!");

			Set<String> tags = entity.getScoreboardTags();
			if (tags != null && !tags.isEmpty())
			{
				Boss boss = null;
				try
				{
					if (tags.contains(GenericBoss.identityTag))
						boss = GenericBoss.deserialize(mPlugin, (LivingEntity)entity);
					else if (tags.contains(CAxtal.identityTag))
						boss = CAxtal.deserialize(mPlugin, (LivingEntity)entity);
					else if (tags.contains(Masked_1.identityTag))
						boss = Masked_1.deserialize(mPlugin, (LivingEntity)entity);
					else if (tags.contains(Masked_2.identityTag))
						boss = Masked_2.deserialize(mPlugin, (LivingEntity)entity);
				}
				catch (Exception ex)
				{
					mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
				}

				if (boss != null)
					mBosses.put(entity.getUniqueId(), boss);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event)
	{
		ProcessEntities(Arrays.asList(event.getChunk().getEntities()));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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

	@EventHandler(priority = EventPriority.HIGHEST)
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

			/*
			 * Remove special serialization data from drops. Should not be
			 * necessary since loaded bosses already have this data stripped
			 */
			SerializationUtils.stripSerializationDataFromDrops(event);
		}
	}

	public void unloadAll()
	{
		for (Map.Entry<UUID, Boss> entry : mBosses.entrySet())
			entry.getValue().unload();
		mBosses.clear();
	}
}
