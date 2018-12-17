package com.playmonumenta.bossfights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.playmonumenta.bossfights.bosses.AuraLargeFatigueBoss;
import com.playmonumenta.bossfights.bosses.AuraLargeHungerBoss;
import com.playmonumenta.bossfights.bosses.AuraLargeSlownessBoss;
import com.playmonumenta.bossfights.bosses.AuraLargeWeaknessBoss;
import com.playmonumenta.bossfights.bosses.AuraSmallFatigueBoss;
import com.playmonumenta.bossfights.bosses.AuraSmallHungerBoss;
import com.playmonumenta.bossfights.bosses.AuraSmallSlownessBoss;
import com.playmonumenta.bossfights.bosses.AuraSmallWeaknessBoss;
import com.playmonumenta.bossfights.bosses.Azacor;
import com.playmonumenta.bossfights.bosses.AzacorNormal;
import com.playmonumenta.bossfights.bosses.BlockBreakBoss;
import com.playmonumenta.bossfights.bosses.BossAbilityGroup;
import com.playmonumenta.bossfights.bosses.CAxtal;
import com.playmonumenta.bossfights.bosses.ChargerBoss;
import com.playmonumenta.bossfights.bosses.FireResistantBoss;
import com.playmonumenta.bossfights.bosses.FireballBoss;
import com.playmonumenta.bossfights.bosses.FlameNovaBoss;
import com.playmonumenta.bossfights.bosses.GenericBoss;
import com.playmonumenta.bossfights.bosses.HiddenBoss;
import com.playmonumenta.bossfights.bosses.InfestedBoss;
import com.playmonumenta.bossfights.bosses.InvisibleBoss;
import com.playmonumenta.bossfights.bosses.Masked_1;
import com.playmonumenta.bossfights.bosses.Masked_2;
import com.playmonumenta.bossfights.bosses.Orangyboi;
import com.playmonumenta.bossfights.bosses.PlayerTargetBoss;
import com.playmonumenta.bossfights.bosses.PulseLaserBoss;
import com.playmonumenta.bossfights.bosses.TpBehindBoss;
import com.playmonumenta.bossfights.bosses.Virius;
import com.playmonumenta.bossfights.bosses.WeaponSwitchBoss;
import com.playmonumenta.bossfights.utils.SerializationUtils;

public class BossManager implements Listener {
	Plugin mPlugin;
	Map<UUID, Boss> mBosses;

	@FunctionalInterface
	public interface StatelessBossConstructor {
		BossAbilityGroup construct(Plugin plugin, LivingEntity entity) throws Exception;
	}

	@FunctionalInterface
	public interface StatefulBossConstructor {
		BossAbilityGroup construct(Plugin plugin, LivingEntity entity, Location spawnLoc, Location endLoc);
	}

	@FunctionalInterface
	public interface BossDeserializer {
		BossAbilityGroup deserialize(Plugin plugin, LivingEntity entity) throws Exception;
	}

	static Map<String, StatelessBossConstructor> mStatelessBosses;
	static Map<String, StatefulBossConstructor> mStatefulBosses;
	static Map<String, BossDeserializer> mBossDeserializers;
	static {
		/* Stateless bosses are those that have no end location set where a redstone block would be spawned when they die */
		mStatelessBosses = new HashMap<String, StatelessBossConstructor>();
		mStatelessBosses.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> new GenericBoss(p, e));
		mStatelessBosses.put(HiddenBoss.identityTag, (Plugin p, LivingEntity e) -> new HiddenBoss(p, e));
		mStatelessBosses.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> new InvisibleBoss(p, e));
		mStatelessBosses.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> new FireResistantBoss(p, e));
		mStatelessBosses.put(BlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> new BlockBreakBoss(p, e));
		mStatelessBosses.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> new PulseLaserBoss(p, e));
		mStatelessBosses.put(WeaponSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> new WeaponSwitchBoss(p, e));
		mStatelessBosses.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> new ChargerBoss(p, e));
		mStatelessBosses.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> new InfestedBoss(p, e));
		mStatelessBosses.put(FireballBoss.identityTag, (Plugin p, LivingEntity e) -> new FireballBoss(p, e));
		mStatelessBosses.put(TpBehindBoss.identityTag, (Plugin p, LivingEntity e) -> new TpBehindBoss(p, e));
		mStatelessBosses.put(FlameNovaBoss.identityTag, (Plugin p, LivingEntity e) -> new FlameNovaBoss(p, e));
		mStatelessBosses.put(PlayerTargetBoss.identityTag, (Plugin p, LivingEntity e) -> new PlayerTargetBoss(p, e));
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
		mStatefulBosses.put(Azacor.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new Azacor(p, e, s, l));
		mStatefulBosses.put(AzacorNormal.identityTag, (Plugin p, LivingEntity e, Location s, Location l) -> new AzacorNormal(p, e, s, l));

		/* All bosses have a deserializer which gives the boss back their abilities when chunks re-load */
		mBossDeserializers = new HashMap<String, BossDeserializer>();
		mBossDeserializers.put(GenericBoss.identityTag, (Plugin p, LivingEntity e) -> GenericBoss.deserialize(p, e));
		mBossDeserializers.put(InvisibleBoss.identityTag, (Plugin p, LivingEntity e) -> InvisibleBoss.deserialize(p, e));
		mBossDeserializers.put(HiddenBoss.identityTag, (Plugin p, LivingEntity e) -> HiddenBoss.deserialize(p, e));
		mBossDeserializers.put(FireResistantBoss.identityTag, (Plugin p, LivingEntity e) -> FireResistantBoss.deserialize(p, e));
		mBossDeserializers.put(BlockBreakBoss.identityTag, (Plugin p, LivingEntity e) -> BlockBreakBoss.deserialize(p, e));
		mBossDeserializers.put(PulseLaserBoss.identityTag, (Plugin p, LivingEntity e) -> PulseLaserBoss.deserialize(p, e));
		mBossDeserializers.put(WeaponSwitchBoss.identityTag, (Plugin p, LivingEntity e) -> WeaponSwitchBoss.deserialize(p, e));
		mBossDeserializers.put(ChargerBoss.identityTag, (Plugin p, LivingEntity e) -> ChargerBoss.deserialize(p, e));
		mBossDeserializers.put(InfestedBoss.identityTag, (Plugin p, LivingEntity e) -> InfestedBoss.deserialize(p, e));
		mBossDeserializers.put(FireballBoss.identityTag, (Plugin p, LivingEntity e) -> FireballBoss.deserialize(p, e));
		mBossDeserializers.put(TpBehindBoss.identityTag, (Plugin p, LivingEntity e) -> TpBehindBoss.deserialize(p, e));
		mBossDeserializers.put(FlameNovaBoss.identityTag, (Plugin p, LivingEntity e) -> FlameNovaBoss.deserialize(p, e));
		mBossDeserializers.put(PlayerTargetBoss.identityTag, (Plugin p, LivingEntity e) -> PlayerTargetBoss.deserialize(p, e));
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
		mBossDeserializers.put(Azacor.identityTag, (Plugin p, LivingEntity e) -> Azacor.deserialize(p, e));
		mBossDeserializers.put(AzacorNormal.identityTag, (Plugin p, LivingEntity e) -> AzacorNormal.deserialize(p, e));
	}

	public BossManager(Plugin plugin) {
		mPlugin = plugin;
		mBosses = new HashMap<UUID, Boss>();

		/* When starting up, look for bosses in all current world entities */
		for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
			if (!(entity instanceof LivingEntity)) {
				continue;
			}

			ProcessEntity((LivingEntity)entity);
		}
	}

	public void createBoss(LivingEntity targetEntity, String requestedTag) throws Exception {
		StatelessBossConstructor stateless = mStatelessBosses.get(requestedTag);
		if (stateless != null) {
			createBossInternal(targetEntity, stateless.construct(mPlugin, targetEntity));
		}
	}

	public void createBoss(LivingEntity targetEntity, String requestedTag, Location endLoc) throws Exception {
		StatefulBossConstructor stateful = mStatefulBosses.get(requestedTag);
		if (stateful != null) {
			createBossInternal(targetEntity, stateful.construct(mPlugin, targetEntity, targetEntity.getLocation(), endLoc));
		} else {
			createBoss(targetEntity, requestedTag);
		}
	}

	private void createBossInternal(LivingEntity targetEntity, BossAbilityGroup ability) throws Exception {
		/* Set up boss health / armor / etc */
		ability.init();

		Boss boss = mBosses.get(targetEntity.getUniqueId());
		if (boss == null) {
			boss = new Boss(ability);
		} else {
			boss.add(ability);
		}

		mBosses.put(targetEntity.getUniqueId(), boss);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();

		if (!(entity instanceof LivingEntity)) {
			return;
		}

		ProcessEntity((LivingEntity)entity);
	}

	private void ProcessEntity(LivingEntity entity) {
		/* This should never happen */
		if (mBosses.get(entity.getUniqueId()) != null) {
			mPlugin.getLogger().log(Level.WARNING, "ProcessEntity: Attempted to add boss that was already tracked!");
			return;
		}

		Set<String> tags = entity.getScoreboardTags();
		if (tags != null && !tags.isEmpty()) {
			Boss boss = null;
			/*
			 * Note - it is important to make a copy here to avoid concurrent modification exception
			 * which happens while iterating and the boss constructor changes the tags on the mob
			 */
			for (String tag : new ArrayList<String>(tags)) {
				BossDeserializer deserializer = mBossDeserializers.get(tag);
				if (deserializer != null) {
					BossAbilityGroup ability;
					try {
						ability = deserializer.deserialize(mPlugin, entity);
					} catch (Exception ex) {
						mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
						continue;
					}

					if (boss == null) {
						boss = new Boss(ability);
						mBosses.put(entity.getUniqueId(), boss);
					} else {
						boss.add(ability);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof LivingEntity)) {
				continue;
			}

			ProcessEntity((LivingEntity)entity);
		}
	}

	public void unload(LivingEntity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.unload();
			mBosses.remove(entity.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void ChunkUnloadEvent(ChunkUnloadEvent event) {
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity)) {
				continue;
			}

			unload((LivingEntity)entity);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void EntityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) {
			return;
		}

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.death();
			boss.unload();

			/*
			 * Remove special serialization data from drops. Should not be
			 * necessary since loaded bosses already have this data stripped
			 */
			SerializationUtils.stripSerializationDataFromDrops(event);
		}
	}

	public void unloadAll() {
		for (Map.Entry<UUID, Boss> entry : mBosses.entrySet()) {
			entry.getValue().unload();
		}
		mBosses.clear();
	}
}
