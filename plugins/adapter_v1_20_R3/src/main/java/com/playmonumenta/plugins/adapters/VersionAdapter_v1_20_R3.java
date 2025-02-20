package com.playmonumenta.plugins.adapters;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.adapters.v1_20_R3.CustomDamageSource;
import com.playmonumenta.plugins.adapters.v1_20_R3.CustomMobAgroMeleeAttack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.util.CollisionUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftParticle;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftParrot;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.scoreboard.CraftScoreboard;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftVector;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VersionAdapter_v1_20_R3 implements VersionAdapter {
	public VersionAdapter_v1_20_R3(@SuppressWarnings("PMD.UnusedFormalParameter") Logger logger) {
	}

	@Override
	public void removeAllMetadata(Plugin plugin) {
		final var server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (final var world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	@Override
	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount,
								   boolean blockable, @Nullable String killedUsingMsg) {
		// this will throw if not present in the datapack
		final var type = ((CraftLivingEntity) damagee).getHandle()
			.level()
			.registryAccess()
			.registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("monumenta:custom"))).get();

		final var reason = new CustomDamageSource(
			type,
			damager == null ? null : ((CraftLivingEntity) damager).getHandle(),
			blockable,
			killedUsingMsg
		);

		((CraftLivingEntity) damagee).getHandle().hurt(reason, (float) amount);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		final var newEntity = (T) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

		final var tag = ((CraftEntity) entity).getHandle().saveWithoutId(new CompoundTag());
		tag.remove("UUID");
		tag.remove("UUIDMost");
		tag.remove("UUIDLeast");

		((CraftEntity) newEntity).getHandle().load(tag);

		return newEntity;
	}

	@Override
	public Vector getActualDirection(Entity entity) {
		final var vector = new Vector();

		double pitch = ((CraftEntity) entity).getHandle().getXRot();
		double yaw = ((CraftEntity) entity).getHandle().getYRot();

		vector.setY(-Math.sin(Math.toRadians(pitch)));

		double xz = Math.cos(Math.toRadians(pitch));

		vector.setX(-xz * Math.sin(Math.toRadians(yaw)));
		vector.setZ(xz * Math.cos(Math.toRadians(yaw)));

		return vector;
	}

	@Override
	public int getAttackCooldown(LivingEntity entity) {
		return ((CraftLivingEntity) entity).getHandle().attackStrengthTicker;
	}

	@Override
	public void setAttackCooldown(LivingEntity entity, int newCooldown) {
		((CraftLivingEntity) entity).getHandle().attackStrengthTicker = newCooldown;
	}

	@Override
	public void releaseActiveItem(LivingEntity entity, boolean clearActiveItem) {
		final var nmsEntity = ((CraftLivingEntity) entity).getHandle();
		if (clearActiveItem) {
			nmsEntity.releaseUsingItem();
		} else {
			// This code is adapted from releaseActiveItem(), without the call to clearActiveItem() (can't use exactly
			// because of private fields)
			final var activeItem = nmsEntity.getUseItem();
			if (!activeItem.isEmpty()) {
				activeItem.releaseUsing(nmsEntity.level(), nmsEntity, nmsEntity.getUseItemRemainingTicks());
				if (activeItem.useOnRelease()) {
					nmsEntity.updatingUsingItem();
				}
			}
		}
	}

	@Override
	public void stunShield(Player player, int ticks) {
		player.setCooldown(Material.SHIELD, ticks);
		if (player.getActiveItem().getType() == Material.SHIELD) {
			releaseActiveItem(player, true);
		}
	}

	@Override
	public void cancelStrafe(Mob mob) {
		((CraftMob) mob).getHandle().setXxa(0);
		((CraftMob) mob).getHandle().setZza(0);
	}

	@Override
	public void disablePerching(Parrot parrot) {
		((CraftParrot) parrot).getHandle()
			.goalSelector
			.getAvailableGoals()
			.removeIf(w -> w.getGoal() instanceof LandOnOwnersShoulderGoal);
	}

	@Override
	public void setAggressive(Creature entity, DamageAction action, double attackRange) {
		final var mob = ((CraftCreature) entity).getHandle();
		final var agro = CustomMobAgroMeleeAttack.builder(mob)
			.action(action)
			.requireSight(true)
			.attackRange(attackRange)
			.build();
		final var attack = new NearestAttackableTargetGoal<>(mob, net.minecraft.world.entity.player.Player.class,
			true);

		mob.goalSelector.addGoal(0, agro);
		mob.targetSelector.addGoal(2, attack);
	}

	@Override
	public void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate,
							double attackRange) {
		final var entityCreature = ((CraftCreature) entity).getHandle();

		setHuntingCompanion(entity, action, attackRange);

		final var goal = new NearestAttackableTargetGoal<>(
			entityCreature,
			net.minecraft.world.entity.LivingEntity.class, 10, false, false,
			entityLiving -> {
				try {
					return predicate.test(entityLiving.getBukkitLivingEntity());
				} catch (Exception e) {
					return predicate.test(null);
				}

			}
		);

		entityCreature.targetSelector.addGoal(2, goal);
	}

	@Override
	public void setHuntingCompanion(Creature entity, DamageAction action, double attackRange) {
		final var entityCreature = ((CraftCreature) entity).getHandle();

		// removing panic mode
		entityCreature.goalSelector
			.getAvailableGoals()
			.stream()
			.filter(task -> task.getGoal() instanceof PanicGoal)
			.findFirst()
			.ifPresent(goal -> entityCreature.goalSelector.removeGoal(goal));

		// Removing others NearestAttackableTargetGoal
		entityCreature.targetSelector
			.getAvailableGoals()
			.stream()
			.filter(task -> task.getGoal() instanceof NearestAttackableTargetGoal)
			.forEach(wrapped -> entityCreature.targetSelector.removeGoal(wrapped));

		final var goal = CustomMobAgroMeleeAttack.builder(entityCreature)
			.action(action)
			.requireSight(true)
			.attackRange(attackRange)
			.rangeChecker((mob, target, attackRangeSqr) -> {
				double x = mob.getX();
				double y = mob.getY() + 1;
				double z = mob.getZ();
				return target.distanceToSqr(x, y, z) <= attackRangeSqr;
			})
			.build();

		entityCreature.goalSelector.addGoal(0, goal);
	}

	@Override
	public void setAttackRange(Creature entity, double attackRange) {
		final var mob = ((CraftCreature) entity).getHandle();

		mob.goalSelector
			.getAvailableGoals()
			.stream()
			.filter(goal -> goal.getGoal() instanceof MeleeAttackGoal)
			.findFirst()
			.ifPresent(goal -> {
				mob.goalSelector.getAvailableGoals().remove(goal);
				mob.goalSelector.addGoal(
					goal.getPriority(),
					CustomMobAgroMeleeAttack.builder(mob)
						.pauseWhenMobIdle(true)
						.attackRange(attackRange)
						.build()
				);
			});
	}

	@Override
	public void runConsoleCommandSilently(String command) {
		// bypasses dispatchServerCommand and ServerCommandEvent
		final var server = MinecraftServer.getServer();
		final var commandSourceStack = server.createCommandSourceStack().withSuppressedOutput();
		final var parsed = server.getCommands().getDispatcher().parse(command, commandSourceStack);
		server.getCommands().performCommand(parsed, command);
	}

	public boolean hasCollision(World world, BoundingBox aabb) {
		final var bb = new AABB(
			aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(),
			aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()
		);

		return !((CraftWorld) world).getHandle().noCollision(bb);
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		return hasCollisionWithBlocks(world, aabb, loadChunks, mat -> mat != Material.SCAFFOLDING);
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks,
										  Predicate<Material> checkedTypes) {
		//boolean loadChunks, boolean collidesWithUnloaded, boolean checkBorder, boolean checkOnly
		// loadChunks, false, false, true
		return CollisionUtil.getCollisionsForBlocksOrWorldBorder(
			((CraftWorld) world).getHandle(),
			null,
			new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()),
			new ArrayList<>(),
			new ArrayList<>(),
			(loadChunks ? CollisionUtil.COLLISION_FLAG_LOAD_CHUNKS : 0) | CollisionUtil.COLLISION_FLAG_CHECK_ONLY,
			(state, pos) -> checkedTypes.test(state.getBukkitMaterial())
		);
	}

	@Override
	public Set<Block> getCollidingBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		List<AABB> collisions = new ArrayList<>();
		CollisionUtil.getCollisionsForBlocksOrWorldBorder(
			((CraftWorld) world).getHandle(),
			null,
			new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()),
			new ArrayList<>(),
			collisions,
			loadChunks ? CollisionUtil.COLLISION_FLAG_LOAD_CHUNKS : 0,
			(state, pos) -> state.getBukkitMaterial() != Material.SCAFFOLDING
		);

		Set<Block> result = new HashSet<>();
		for (final var collision : collisions) {
			// This assumes that block collision centers are within their block.
			Vec3 center = collision.getCenter();
			result.add(world.getBlockAt((int) Math.floor(center.x), (int) Math.floor(center.y),
				(int) Math.floor(center.z)));
		}
		return result;
	}

	static {
		WitherBoss.TARGETING_CONDITIONS.selector(le -> le instanceof Player);
	}

	@Override
	public void mobAIChanges(Mob mob) {
		final var availableGoals = ((CraftMob) mob).getHandle().goalSelector.getAvailableGoals();
		final var availableTargetGoals = ((CraftMob) mob).getHandle().targetSelector.getAvailableGoals();

		if (mob instanceof Fox || mob instanceof AbstractSkeleton) {
			// prevent foxes running from players, wolves, and polar bears, and skeletons running away from wolves
			availableGoals.removeIf(goal -> goal.getGoal() instanceof AvoidEntityGoal<?>);

			if (mob instanceof WitherSkeleton) {
				// prevent wither skeletons from attacking piglins
				availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg &&
					AbstractPiglin.class.isAssignableFrom(natg.targetType));
			}
		} else if (mob instanceof IronGolem) {
			// prevent iron golems defending villages and attacking mobs
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof DefendVillageTargetGoal);
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& natg.targetType == net.minecraft.world.entity.Mob.class);
		} else if (mob instanceof Drowned) {
			// prevent drowneds from attacking mobs
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& net.minecraft.world.entity.Mob.class.isAssignableFrom(natg.targetType));
		} else if (mob instanceof org.bukkit.entity.Evoker) {
			// disable vexes and fangs on evokers with the proper tags
			if (mob.getScoreboardTags().contains("boss_evoker_no_vex")) {
				availableGoals.removeIf(goal -> goal.getGoal() instanceof Evoker.EvokerSummonSpellGoal);
			}
			if (mob.getScoreboardTags().contains("boss_evoker_no_fangs")) {
				availableGoals.removeIf(goal -> goal.getGoal() instanceof Evoker.EvokerAttackSpellGoal);
			}
		} else if (mob instanceof Wolf) {
			// prevent wolves from attacking animals and skeletons
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& net.minecraft.world.entity.monster.AbstractSkeleton.class.isAssignableFrom(natg.targetType));
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NonTameRandomTargetGoal);
			// disable panicking and avoiding llamas
			availableGoals.removeIf(goal -> goal.getGoal().getClass().getDeclaringClass() == net.minecraft.world.entity.animal.Wolf.class);
		} else if (mob instanceof Enderman) {
			// remove all special enderman goals (freeze when looked at, attack staring player, take/drop block)
			availableGoals.removeIf(goal -> goal.getGoal().getClass().getDeclaringClass() == EnderMan.class);
			availableTargetGoals.removeIf(goal -> goal.getGoal().getClass().getDeclaringClass() == EnderMan.class);
		} else if (mob instanceof Wither) {
			// make withers only attack players and not other mobs
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal);
			availableTargetGoals.add(new WrappedGoal(2, new NearestAttackableTargetGoal<>(((CraftMob) mob).getHandle(),
				net.minecraft.world.entity.player.Player.class, false, false)));
		} else if (mob instanceof Spider) {
			// allow spiders to target and attack even with something riding them or if it's too bright
			availableGoals.removeIf(goal -> goal.getGoal().getClass().getDeclaringClass() == net.minecraft.world.entity.monster.Spider.class);
			double attackRange = mob instanceof CaveSpider ? 1.5 : 1.8; // lower than vanilla, even lower for cave
			// spiders
			availableGoals.add(new WrappedGoal(4,
				CustomMobAgroMeleeAttack.builder((PathfinderMob) ((CraftMob) mob).getHandle())
					.pauseWhenMobIdle(true)
					.attackRange(attackRange)
					.build()));
			availableTargetGoals.add(new WrappedGoal(2, new NearestAttackableTargetGoal<>(((CraftMob) mob).getHandle(),
				net.minecraft.world.entity.player.Player.class, false, false)));
			// disable leaping if desired
			if (mob.getScoreboardTags().contains("boss_spider_no_leap")) {
				availableGoals.removeIf(goal -> goal.getGoal() instanceof LeapAtTargetGoal);
			}
		} else if (mob instanceof org.bukkit.entity.Bee) {
			// for bees used as aggressive mobs, disable the peaceful behaviours of pollination and using bee hives
			if (mob.getScoreboardTags().contains("boss_targetplayer") || mob.getScoreboardTags().contains(
				"boss_generictarget")) {
				availableGoals.removeIf(goal ->
					goal.getGoal() instanceof Bee.BeeEnterHiveGoal
						|| goal.getGoal() instanceof Bee.BeePollinateGoal
						|| goal.getGoal() instanceof Bee.BeeLocateHiveGoal
						|| goal.getGoal() instanceof Bee.BeeGoToHiveGoal
						|| goal.getGoal() instanceof Bee.BeeGoToKnownFlowerGoal
						|| goal.getGoal() instanceof Bee.BeeGrowCropGoal
				);
			}
		}
		// prevent all mobs from attacking iron golems and turtles
		availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
			&& (natg.targetType == net.minecraft.world.entity.animal.IronGolem.class
			|| natg.targetType == Turtle.class
			|| natg.targetType == AbstractVillager.class));
	}

	@Override
	public Object toVanillaChatComponent(net.kyori.adventure.text.Component component) {
		return Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
	}

	@Override
	public boolean isSameItem(@Nullable ItemStack item1,
							  @Nullable ItemStack item2) {
		return item1 == item2
			|| item1 instanceof CraftItemStack craftItem1 && item2 instanceof CraftItemStack craftItem2
			&& craftItem1.handle == craftItem2.handle;
	}

	@Override
	public void forceDismountVehicle(Entity entity) {
		((CraftEntity) entity).getHandle().stopRiding(true);
	}

	@Override
	public ItemStack getUsedProjectile(Player player, ItemStack weapon) {
		return CraftItemStack.asCraftMirror(((CraftPlayer) player).getHandle().getProjectile(CraftItemStack.asNMSCopy(weapon)));
	}

	@Override
	public net.kyori.adventure.text.Component getDisplayName(ItemStack item) {
		return PaperAdventure.asAdventure(CraftItemStack.asNMSCopy(item).getHoverName());
	}

	@Override
	public void moveEntity(Entity entity, Vector movement) {
		((CraftEntity) entity).getHandle().move(MoverType.SELF, CraftVector.toNMS(movement));
	}

	@Override
	public void setEntityLocation(Entity entity, Vector target, float yaw, float pitch) {
		((CraftEntity) entity).getHandle().moveTo(target.getX(), target.getY(), target.getZ(), yaw, pitch);
	}

	public JsonObject getScoreHolderScoresAsJson(String scoreHolder, org.bukkit.scoreboard.Scoreboard scoreboard) {
		final var nmsScoreboard = ((CraftScoreboard) scoreboard).getHandle();

		final var data = new JsonObject();

		final var scores = nmsScoreboard.playerScores.get(scoreHolder);
		if (scores == null) {
			// No scores for this player
			return data;
		}

		scores.listRawScores().forEach((key, value) -> data.addProperty(key.getName(), value.value()));

		return data;
	}

	public void resetScoreHolderScores(String scoreHolder, org.bukkit.scoreboard.Scoreboard scoreboard) {
		Scoreboard nmsScoreboard = ((CraftScoreboard) scoreboard).getHandle();
		nmsScoreboard.resetSinglePlayerScore(() -> scoreHolder, null); // ???
	}

	@Override
	public void disableRangedAttackGoal(LivingEntity entity) {
		((CraftMob) entity).getHandle().goalSelector
			.getAvailableGoals()
			.removeIf(w -> w.getGoal() instanceof RangedAttackGoal);
	}

	@Override
	public void forceSyncEntityPositionData(Entity entity) {
		final var mcEntity = ((CraftEntity) entity).getHandle();
		final var tracker = mcEntity.tracker;

		if (tracker != null) {
			tracker.serverEntity.broadcast.accept(new ClientboundTeleportEntityPacket(mcEntity));
		}
	}

	private static final Map<UUID, Long> mFlushingPlayers = new ConcurrentHashMap<>();
	// Netty's default watermark is 64kb, but that throttles too much for us
	// TODO: increased hard limit to 25MB which should never be reached, but if it is, we should definitely drop packets
	private static final long mMaxQueueBytes = 25 * 1000 * 1024L; // 5 mb;
	private static final long mRetryQueueByes = 25 * 1000 * 1024L; // 256 kb
	private static final long mFlushQueueBytes = 32 * 1024L; // 32 kb;
	private static final long mTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(1);

	/**
	 * @return 0 if the packet was sent, 1 if the packet was dropped and should be requeued, -1 if the packet was dropped
	 */
	@Override
	public <T> int sendParticle(Particle particle, Player player, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data, boolean force) {
		Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
		if (channel == null || !channel.isActive()) {
			return -1;
		}
		ChannelPipeline pipeline = channel.pipeline();
		ChannelHandlerContext ctx = pipeline.context("packet_handler");
		if (ctx == null) {
			ctx = pipeline.lastContext();
		}
		if (ctx == null) {
			return -1;
		}
		final Channel ctxChannel = ctx.channel();
		if (ctxChannel == null) {
			return -1;
		}

		final int flushedQueueSize = ctxChannel.unsafe().outboundBuffer().size();
		final long unflushedQueueBytes = ctxChannel.unsafe().outboundBuffer().totalPendingWriteBytes();
		if (flushedQueueSize >= 8192 || unflushedQueueBytes >= mMaxQueueBytes) {
			// too many packets in the queue, drop this one
			return -1;
		}
		final UUID uuid = player.getUniqueId();
		final long currentTime = System.nanoTime();
		final long flushTime = mFlushingPlayers.getOrDefault(player.getUniqueId(), 0L);
		final boolean timeout = currentTime - flushTime >= mTimeoutNanos;
		// stop writing once the queue is too big and we are not flushing
		if (!timeout && unflushedQueueBytes >= mRetryQueueByes) {
			return 1;
		}
		// translate particles from legacy format (why bukkit)
		final ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(CraftParticle.createParticleParam(particle, data), force, x, y, z, (float) offsetX, (float) offsetY, (float) offsetZ, (float) extra, count);
		if (unflushedQueueBytes >= mFlushQueueBytes && timeout) {
			mFlushingPlayers.put(uuid, currentTime);
			ctxChannel.writeAndFlush(packet, ctx.voidPromise());
		} else {
			if (timeout) {
				mFlushingPlayers.remove(uuid);
			}
			ctxChannel.write(packet, ctx.voidPromise());
		}
		return 0;
	}


	@Override
	public Object replaceWorldNames(Object packet, Consumer<WorldNameReplacementToken> handler) {
		final CommonPlayerSpawnInfo info;

		if (packet instanceof ClientboundLoginPacket loginPacket) {
			info = loginPacket.commonPlayerSpawnInfo();
		} else if (packet instanceof ClientboundRespawnPacket respawnPacket) {
			info = respawnPacket.commonPlayerSpawnInfo();
		} else {
			throw new IllegalStateException();
		}

		final var currentWorldKey = info.dimension();

		final var token =
			new WorldNameReplacementToken(Objects.requireNonNull(MinecraftServer.getServer().getLevel(currentWorldKey)).getWorld());
		handler.accept(token);

		if (token.getKey() == null) {
			return packet;
		}

		final var replacedWorldKey = ResourceKey.create(Registries.DIMENSION,
			CraftNamespacedKey.toMinecraft(token.getKey()));

		final var newInfo = new CommonPlayerSpawnInfo(
			info.dimensionType(),
			replacedWorldKey,
			info.seed(),
			info.gameType(),
			info.previousGameType(),
			info.isDebug(),
			info.isFlat(),
			info.lastDeathLocation(),
			info.portalCooldown()
		);

		if (packet instanceof ClientboundLoginPacket loginPacket) {
			return new ClientboundLoginPacket(
				loginPacket.playerId(),
				loginPacket.hardcore(),
				loginPacket.levels().stream().map(key -> key.equals(currentWorldKey) ? replacedWorldKey : key).collect(Collectors.toSet()),
				loginPacket.maxPlayers(),
				loginPacket.chunkRadius(),
				loginPacket.simulationDistance(),
				loginPacket.reducedDebugInfo(),
				loginPacket.showDeathScreen(),
				loginPacket.doLimitedCrafting(),
				newInfo
			);
		} else {
			ClientboundRespawnPacket respawnPacket = (ClientboundRespawnPacket) packet;
			return new ClientboundRespawnPacket(
				newInfo,
				respawnPacket.dataToKeep()
			);
		}
	}

	@Override
	public void sendOpenSignPacket(Player player, int blockX, int blockY, int blockZ, boolean b) {
		((CraftPlayer) player).getHandle().connection.send(new ClientboundOpenSignEditorPacket(new BlockPos(blockX, blockY, blockZ), b));
	}
}
