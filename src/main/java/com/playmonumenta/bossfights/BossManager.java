package com.playmonumenta.bossfights;

import java.util.HashMap;
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

import com.playmonumenta.bossfights.bosses.Boss;
import com.playmonumenta.bossfights.bosses.CAxtal;
import com.playmonumenta.bossfights.bosses.ChargerBoss;
import com.playmonumenta.bossfights.bosses.GenericBoss;
import com.playmonumenta.bossfights.bosses.InfestedBoss;
import com.playmonumenta.bossfights.bosses.InvisibleBoss;
import com.playmonumenta.bossfights.bosses.FireResistantBoss;
import com.playmonumenta.bossfights.bosses.Masked_1;
import com.playmonumenta.bossfights.bosses.Masked_2;
import com.playmonumenta.bossfights.bosses.PulseLaserBoss;
import com.playmonumenta.bossfights.bosses.Virius;
import com.playmonumenta.bossfights.bosses.Orangyboi;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.bossfights.utils.Utils.ArgumentException;

public class BossManager implements Listener, CommandExecutor
{
	Plugin mPlugin;
	Map<UUID, Boss> mBosses;

	public BossManager(Plugin plugin)
	{
		mPlugin = plugin;
		mBosses = new HashMap<UUID, Boss>();

		/* When starting up, look for bosses in all current world entities */
		for (Entity entity : Bukkit.getWorlds().get(0).getEntities())
		{
			if (!(entity instanceof LivingEntity))
				continue;

			ProcessEntity((LivingEntity)entity);
		}
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

		Boss boss;
		switch (args[0].toLowerCase())
		{
		case GenericBoss.identityTag:
			boss = new GenericBoss(mPlugin, (LivingEntity)targetEntity);
			break;
		case InvisibleBoss.identityTag:
			boss = new InvisibleBoss(mPlugin, (LivingEntity)targetEntity);
			break;
		case FireResistantBoss.identityTag:
			boss = new FireResistantBoss(mPlugin, (LivingEntity)targetEntity);
			break;
		case PulseLaserBoss.identityTag:
			boss = new PulseLaserBoss(mPlugin, (LivingEntity)targetEntity);
			break;
		case ChargerBoss.identityTag:
			boss = new ChargerBoss(mPlugin, (LivingEntity)targetEntity);
			break;
		case InfestedBoss.identityTag:
			boss = new InfestedBoss(mPlugin, (LivingEntity)targetEntity);
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
		case Virius.identityTag:
			boss = new Virius(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		case Orangyboi.identityTag:
			boss = new Orangyboi(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		default:
			send.sendMessage(ChatColor.RED + "Invalid boss name!");
			send.sendMessage(ChatColor.RED + "Valid options are: [" + GenericBoss.identityTag + "," +
			                 InvisibleBoss.identityTag + "," + FireResistantBoss.identityTag + "," + CAxtal.identityTag + "," +
			                 Masked_1.identityTag + "," + Masked_2.identityTag + "," + InfestedBoss.identityTag + "," + ChargerBoss.identityTag + "," + PulseLaserBoss.identityTag + "," + Virius.identityTag + "," +
							 Orangyboi.identityTag + "]");
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

		ProcessEntity((LivingEntity)entity);
	}

	private void ProcessEntity(LivingEntity entity)
	{
		/* This should never happen */
		if (mBosses.get(entity.getUniqueId()) != null)
		{
			mPlugin.getLogger().log(Level.WARNING, "ProcessEntity: Attempted to add boss that was already tracked!");
			return;
		}

		Set<String> tags = entity.getScoreboardTags();
		if (tags != null && !tags.isEmpty())
		{
			Boss boss = null;
			try
			{
				if (tags.contains(GenericBoss.identityTag))
					boss = GenericBoss.deserialize(mPlugin, entity);
				else if (tags.contains(InvisibleBoss.identityTag))
					boss = InvisibleBoss.deserialize(mPlugin, entity);
				else if (tags.contains(FireResistantBoss.identityTag))
					boss = FireResistantBoss.deserialize(mPlugin, entity);
				else if (tags.contains(CAxtal.identityTag))
					boss = CAxtal.deserialize(mPlugin, entity);
				else if (tags.contains(Masked_1.identityTag))
					boss = Masked_1.deserialize(mPlugin, entity);
				else if (tags.contains(Masked_2.identityTag))
					boss = Masked_2.deserialize(mPlugin, entity);
				else if (tags.contains(InfestedBoss.identityTag))
					boss = InfestedBoss.deserialize(mPlugin, entity);
				else if (tags.contains(ChargerBoss.identityTag))
					boss = ChargerBoss.deserialize(mPlugin, entity);
				else if (tags.contains(PulseLaserBoss.identityTag))
					boss = PulseLaserBoss.deserialize(mPlugin, entity);
				else if (tags.contains(Virius.identityTag))
					boss = Virius.deserialize(mPlugin, entity);
				else if (tags.contains(Orangyboi.identityTag))
					boss = Orangyboi.deserialize(mPlugin, entity);
			}
			catch (Exception ex)
			{
				mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
			}

			if (boss != null)
				mBosses.put(entity.getUniqueId(), boss);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event)
	{
		for (Entity entity : event.getChunk().getEntities())
		{
			if (!(entity instanceof LivingEntity))
				continue;

			ProcessEntity((LivingEntity)entity);
		}
	}

	public void unload(LivingEntity entity)
	{
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null)
		{
			boss.unload();
			mBosses.remove(entity.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void ChunkUnloadEvent(ChunkUnloadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities)
		{
			if (!(entity instanceof LivingEntity))
				continue;

			unload((LivingEntity)entity);
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
