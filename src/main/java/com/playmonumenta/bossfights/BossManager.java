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

import com.playmonumenta.bossfights.bosses.*;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;
import com.playmonumenta.bossfights.utils.Utils.ArgumentException;

public class BossManager implements Listener, CommandExecutor
{
	Plugin mPlugin;
	Map<UUID, Boss> mBosses;

@FunctionalInterface
public interface StatelessBossConstructor
{
	Boss construct(Plugin plugin, LivingEntity entity);
}

@FunctionalInterface
public interface StatefulBossConstructor
{
	Boss construct(Plugin plugin, LivingEntity entity, Location spawnLoc, Location endLoc);
}

@FunctionalInterface
public interface BossDeserializer
{
	Boss deserialize(Plugin plugin, LivingEntity entity) throws Exception;
}

static Map<String, StatelessBossConstructor> mStatelessBosses;
static Map<String, StatefulBossConstructor> mStatefulBosses;
static Map<String, BossDeserializer> mBossDeserializers;
static
{
	/* Stateless bosses are those that have no end location set where a redstone block would be spawned when they die */
	mStatelessBosses = new HashMap<String, StatelessBossConstructor>();
	mStatelessBosses.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> new GenericBoss(p, e));
	mStatelessBosses.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> new InvisibleBoss(p, e));
	mStatelessBosses.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> new FireResistantBoss(p, e));
	mStatelessBosses.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> new PulseLaserBoss(p, e));
	mStatelessBosses.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> new ChargerBoss(p, e));
	mStatelessBosses.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> new InfestedBoss(p, e));
	mStatelessBosses.put(AuraLargeFatigueBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraLargeFatigueBoss(p, e));
	mStatelessBosses.put(AuraLargeHungerBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraLargeHungerBoss(p, e));
	mStatelessBosses.put(AuraLargeSlownessBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraLargeSlownessBoss(p, e));
	mStatelessBosses.put(AuraLargeWeaknessBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraLargeWeaknessBoss(p, e));
	mStatelessBosses.put(AuraSmallFatigueBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraSmallFatigueBoss(p, e));
	mStatelessBosses.put(AuraSmallHungerBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraSmallHungerBoss(p, e));
	mStatelessBosses.put(AuraSmallSlownessBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraSmallSlownessBoss(p, e));
	mStatelessBosses.put(AuraSmallWeaknessBoss.identityTag, (Plugin p, LivingEntity e) -> new AuraSmallWeaknessBoss(p, e));

	/* Stateful bosses have a remembered spawn location and end location where a redstone block is set when they die */
	mStatefulBosses = new HashMap<String, StatefulBossConstructor>();
	mStatefulBosses.put(CAxtal.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new CAxtal(p, e, s, l));
	mStatefulBosses.put(Masked_1.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Masked_1(p, e, s, l));
	mStatefulBosses.put(Masked_2.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Masked_2(p, e, s, l));
	mStatefulBosses.put(Virius.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Virius(p, e, s, l));
	mStatefulBosses.put(Orangyboi.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Orangyboi(p, e, s, l));

	/* All bosses have a deserializer which gives the boss back their abilities when chunks re-load */
	mBossDeserializers = new HashMap<String, BossDeserializer>();
	mBossDeserializers.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> GenericBoss.deserialize(p, e));
	mBossDeserializers.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> InvisibleBoss.deserialize(p, e));
	mBossDeserializers.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> FireResistantBoss.deserialize(p, e));
	mBossDeserializers.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> PulseLaserBoss.deserialize(p, e));
	mBossDeserializers.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> ChargerBoss.deserialize(p, e));
	mBossDeserializers.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> InfestedBoss.deserialize(p, e));
	mBossDeserializers.put(AuraLargeFatigueBoss.identityTag, (Plugin p, LivingEntity e) -> AuraLargeFatigueBoss.deserialize(p, e));
	mBossDeserializers.put(AuraLargeHungerBoss.identityTag, (Plugin p, LivingEntity e) -> AuraLargeHungerBoss.deserialize(p, e));
	mBossDeserializers.put(AuraLargeSlownessBoss.identityTag, (Plugin p, LivingEntity e) -> AuraLargeSlownessBoss.deserialize(p, e));
	mBossDeserializers.put(AuraLargeWeaknessBoss.identityTag, (Plugin p, LivingEntity e) -> AuraLargeWeaknessBoss.deserialize(p, e));
	mBossDeserializers.put(AuraSmallFatigueBoss.identityTag, (Plugin p, LivingEntity e) -> AuraSmallFatigueBoss.deserialize(p, e));
	mBossDeserializers.put(AuraSmallHungerBoss.identityTag, (Plugin p, LivingEntity e) -> AuraSmallHungerBoss.deserialize(p, e));
	mBossDeserializers.put(AuraSmallSlownessBoss.identityTag, (Plugin p, LivingEntity e) -> AuraSmallSlownessBoss.deserialize(p, e));
	mBossDeserializers.put(AuraSmallWeaknessBoss.identityTag, (Plugin p, LivingEntity e) -> AuraSmallWeaknessBoss.deserialize(p, e));
	mBossDeserializers.put(CAxtal.identityTag, (Plugin p, LivingEntity e) -> CAxtal.deserialize(p, e));
	mBossDeserializers.put(Masked_1.identityTag, (Plugin p, LivingEntity e) -> Masked_1.deserialize(p, e));
	mBossDeserializers.put(Masked_2.identityTag, (Plugin p, LivingEntity e) -> Masked_2.deserialize(p, e));
	mBossDeserializers.put(Virius.identityTag, (Plugin p, LivingEntity e) -> Virius.deserialize(p, e));
	mBossDeserializers.put(Orangyboi.identityTag, (Plugin p, LivingEntity e) -> Orangyboi.deserialize(p, e));
}

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

		Boss boss = null;
		String requestedTag = args[0];

		StatelessBossConstructor stateless = mStatelessBosses.get(requestedTag);
		if (stateless != null) {
			boss = stateless.construct(mPlugin, (LivingEntity)targetEntity);
		} else {
			StatefulBossConstructor stateful = mStatefulBosses.get(requestedTag);
			if (stateful != null) {
				boss = stateful.construct(mPlugin, (LivingEntity)targetEntity, targetEntity.getLocation(), endLoc);
			}
		}

		if (boss == null) {
			send.sendMessage(ChatColor.RED + "Invalid boss name!");
			send.sendMessage(ChatColor.RED + "Valid options are: [" + String.join(",", mBossDeserializers.keySet()) + "]");
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
				for (String tag : tags) {
					BossDeserializer deserializer = mBossDeserializers.get(tag);
					if (deserializer != null) {
						boss = deserializer.deserialize(mPlugin, entity);
						/* TODO
						 *
						 * Currently each boss can only have one boss ability. This is a fairly substantial limitation -
						 * For example having a boss bar AND a charge attack isn't currently possible.
						 *
						 * Fixing this will require rethinking how the boss is tracked (map of a list of Boss?) and
						 * also figuring out it works if one of the Boss types has nonempty serialization data
						 */
						mBosses.put(entity.getUniqueId(), boss);
						break;
					}
				}
			}
			catch (Exception ex)
			{
				mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
			}
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
