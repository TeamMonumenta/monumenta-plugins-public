package pe.bossfights;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
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
import org.bukkit.World;

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

		if (!(targetEntity instanceof Damageable))
		{
			send.sendMessage(ChatColor.RED + "Target entity is not damageable!");
			return false;
		}

		Boss boss;
		switch (args[0].toLowerCase())
		{
		case "caxtal":
			boss = new CAxtal(mPlugin, (Damageable)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		case "masked_1":
			boss = new Masked_1(mPlugin, (Damageable)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		case "masked_2":
			boss = new Masked_2(mPlugin, (Damageable)targetEntity, targetEntity.getLocation(), endLoc);
			break;
		default:
			//TODO helpful message
			return false;
		}

		/* Set up boss health / armor / etc */
		boss.init();

		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities)
		{
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null)
			{
				// TODO WARNING - this should never happen
			}

			// Check if boss
			// Read data from items
			// Instantiate boss
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkUnloadEvent(ChunkUnloadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities)
		{
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
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null)
		{
			boss.death();
			boss.unload();

			// TODO: Un-tagify drops
		}
	}
}
