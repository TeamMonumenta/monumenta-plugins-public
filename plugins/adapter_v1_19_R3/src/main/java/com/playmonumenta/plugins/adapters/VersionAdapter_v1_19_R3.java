package com.playmonumenta.plugins.adapters;

import com.google.gson.JsonObject;
import io.papermc.paper.adventure.PaperAdventure;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.*;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.scoreboard.CraftScoreboard;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftVector;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VersionAdapter_v1_19_R3 implements VersionAdapter {
	private interface TypedField<O, F> {
		F get(O object);

		void set(O object, F value);
	}

	@SuppressWarnings("unchecked")
	private static <O, F> TypedField<O, F> getField(Class<O> clazz, String field) {
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return new TypedField<>() {
				@Override
				public F get(O object) {
					try {
						return (F) f.get(object);
					} catch (IllegalAccessException e) {
						// Should not happen as the field is set to be accessible
						throw new RuntimeException(e);
					}
				}

				@Override
				public void set(O object, F value) {
					try {
						f.set(object, value);
					} catch (IllegalAccessException e) {
						// Should not happen as the field is set to be accessible
						throw new RuntimeException(e);
					}
				}
			};
		} catch (NoSuchFieldException e) {
			// Should only happen if Minecraft is updated.
			// See linkie.
			throw new RuntimeException(e);
		}
	}

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.19.4&search=targetType&translateMode=none
	@SuppressWarnings("rawtypes")
	private static final TypedField<NearestAttackableTargetGoal, Class<?>> NEAREST_ATTACKABLE_TARGET_GOAL_TARGET_TYPE
		= getField(NearestAttackableTargetGoal.class, "a");

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.19.4&search=broadcast&translateMode=none&allowClasses=false&allowMethods=false
	private static final TypedField<ServerEntity, Consumer<Packet<?>>> SERVER_ENTITY_BROADCAST_FIELD
		= getField(ServerEntity.class, "g");

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.19.4&search=attackStrengthTicker&translateMode=none
	private static final TypedField<net.minecraft.world.entity.LivingEntity, Integer> LIVING_ENTITY_ATTACK_STRENGTH_TICKER
		= getField(net.minecraft.world.entity.LivingEntity.class, "aO");

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.19.4&search=TARGETING_CONDITIONS&translateMode=none
	private static final TypedField<WitherBoss, TargetingConditions> WITHER_TARGETING_CONDITIONS
		= getField(WitherBoss.class, "cd");

	// https://linkie.shedaniel.dev/mappings?namespace=mojang_raw&version=1.19.4&search=playerScores&translateMode=none
	private static final TypedField<Scoreboard, Map<String, Map<Objective, Score>>> SCOREBOARD_PLAYER_SCORES
		= getField(Scoreboard.class, "j");

	final Logger mLogger;

	public VersionAdapter_v1_19_R3(Logger logger) {
		mLogger = logger;
	}

	public void removeAllMetadata(Plugin plugin) {
		CraftServer server = (CraftServer) plugin.getServer();
		server.getEntityMetadata().removeAll(plugin);
		server.getPlayerMetadata().removeAll(plugin);
		server.getWorldMetadata().removeAll(plugin);
		for (World world : Bukkit.getWorlds()) {
			((CraftWorld) world).getBlockMetadata().removeAll(plugin);
		}
	}

	public void resetPlayerIdleTimer(Player player) {
		CraftPlayer p = (CraftPlayer) player;
		ServerPlayer playerHandle = p.getHandle();
		playerHandle.resetLastActionTime();
	}

	private static class CustomDamageSource extends DamageSource {
		private final boolean mBlockable;
		@Nullable
		private final String mKilledUsingMsg;
		@Nullable
		private final net.minecraft.world.entity.Entity mDamager;

		public CustomDamageSource(Holder<DamageType> type, @Nullable net.minecraft.world.entity.Entity damager, boolean blockable, @Nullable String killedUsingMsg) {
			super(type, damager, damager);
			mDamager = damager;
			mBlockable = blockable;
			mKilledUsingMsg = killedUsingMsg;
		}

		@Override
		public @Nullable Vec3 getSourcePosition() {
			return mBlockable ? super.getSourcePosition() : null;
		}

		@Override
		public Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity killed) {
			if (this.mDamager == null) {
				// death.attack.magic=%1$s was killed by magic
				String s = "death.attack.magic";
				return Component.translatable(s, killed.getDisplayName());
			} else if (mKilledUsingMsg == null || mKilledUsingMsg.isEmpty()) {
				// death.attack.mob=%1$s was killed by %2$s
				String s = "death.attack.mob";
				return Component.translatable(s, killed.getDisplayName(), this.mDamager.getDisplayName());
			} else {
				// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
				String s = "death.attack.indirectMagic.item";
				return Component.translatable(s, killed.getDisplayName(), this.mDamager.getDisplayName(), mKilledUsingMsg);
			}
		}
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg) {
		String id = "monumenta:custom"; // datapack damage types
		Holder<DamageType> type = ((CraftLivingEntity) damagee).getHandle().getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(id))).get(); // this will throw if not present in the datapack
		DamageSource reason = new CustomDamageSource(type, damager == null ? null : ((CraftLivingEntity) damager).getHandle(), blockable, killedUsingMsg);

		((CraftLivingEntity) damagee).getHandle().hurt(reason, (float) amount);
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T duplicateEntity(T entity) {
		T newEntity = (T) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

		CompoundTag nbttagcompound = ((CraftEntity) entity).getHandle().saveWithoutId(new CompoundTag());
		nbttagcompound.remove("UUID");
		nbttagcompound.remove("UUIDMost");
		nbttagcompound.remove("UUIDLeast");

		((CraftEntity) newEntity).getHandle().load(nbttagcompound);

		return newEntity;
	}

	public @Nullable Entity getEntityById(World world, int entityId) {
		net.minecraft.world.entity.Entity entity = ((CraftWorld) world).getHandle().getEntity(entityId);
		return entity == null ? null : entity.getBukkitEntity();
	}

	public Vector getActualDirection(Entity entity) {
		Vector vector = new Vector();

		double pitch = ((CraftEntity) entity).getHandle().getXRot();
		double yaw = ((CraftEntity) entity).getHandle().getYRot();

		vector.setY(-Math.sin(Math.toRadians(pitch)));

		double xz = Math.cos(Math.toRadians(pitch));

		vector.setX(-xz * Math.sin(Math.toRadians(yaw)));
		vector.setZ(xz * Math.cos(Math.toRadians(yaw)));

		return vector;
	}

	private static Method getMethod(Class<?> clazz, String method, Class<?>... arguments) {
		try {
			Method m = clazz.getDeclaredMethod(method, arguments);
			m.setAccessible(true);
			return m;
		} catch (NoSuchMethodException e) {
			// Should only happen if Minecraft is updated.
			// Check the documentation of where this is used for how to find the new name.
			throw new RuntimeException(e);
		}
	}

	private static @Nullable Object invokeMethod(Method method, Object target, @Nullable Object... args) {
		try {
			return method.invoke(target, args);
		} catch (IllegalAccessException e) {
			// Should not happen as the method is set to be accessible
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			// RuntimeException and Errors can happen, just throw them again (without the InvocationTargetException wrapper)
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			// This should not happen as long as the methods have no checked exceptions declared
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unboxing.of.nullable")
	public int getAttackCooldown(LivingEntity entity) {
		return LIVING_ENTITY_ATTACK_STRENGTH_TICKER.get(((CraftLivingEntity) entity).getHandle());
	}

	public void setAttackCooldown(LivingEntity entity, int newCooldown) {
		LIVING_ENTITY_ATTACK_STRENGTH_TICKER.set(((CraftLivingEntity) entity).getHandle(), newCooldown);
	}

	// Update the code in releaseActiveItem() below before updating this, as this may not even be used anymore.
	// unobfuscated field name: updatingUsingItem
	private static final Method tickActiveItemStack = getMethod(net.minecraft.world.entity.LivingEntity.class, "D");

	public void releaseActiveItem(LivingEntity entity, boolean clearActiveItem) {
		net.minecraft.world.entity.LivingEntity nmsEntity = ((CraftLivingEntity) entity).getHandle();
		if (clearActiveItem) {
			nmsEntity.releaseUsingItem();
		} else {
			// This code is adapted from releaseActiveItem(), without the call to clearActiveItem() (can't use exactly because of private fields)
			ItemStack activeItem = nmsEntity.getUseItem();
			if (!activeItem.isEmpty()) {
				activeItem.releaseUsing(nmsEntity.level, nmsEntity, nmsEntity.getUseItemRemainingTicks());
				if (activeItem.useOnRelease()) {
					invokeMethod(tickActiveItemStack, nmsEntity);
				}
			}
		}
	}

	public void stunShield(Player player, int ticks) {
		player.setCooldown(Material.SHIELD, ticks);
		if (player.getActiveItem() != null && player.getActiveItem().getType() == Material.SHIELD) {
			releaseActiveItem(player, true);
		}
	}


	private static final Method getJumpPowerMethod = getMethod(net.minecraft.world.entity.LivingEntity.class, "eQ");

	@Override
	public double getJumpVelocity(LivingEntity entity) {
		net.minecraft.world.entity.LivingEntity e = ((CraftLivingEntity) entity).getHandle();
		float getJumpPower = (float) invokeMethod(getJumpPowerMethod, e);
		// getJumpFactor + getJumpBoostPower
		return getJumpPower + e.getJumpBoostPower();
	}

	@Override
	public void cancelStrafe(Mob mob) {
		((CraftMob) mob).getHandle().setXxa(0);
		((CraftMob) mob).getHandle().setZza(0);
	}

	@Override
	public Entity spawnWorldlessEntity(EntityType type, World world) {
		Optional<net.minecraft.world.entity.EntityType<?>> entityTypes = net.minecraft.world.entity.EntityType.byString(type.name().toLowerCase(Locale.ROOT));
		if (entityTypes.isEmpty()) {
			throw new IllegalArgumentException("Invalid entity type " + type.name());
		}
		net.minecraft.world.entity.Entity entity = entityTypes.get().create(((CraftWorld) world).getHandle());
		if (entity == null) {
			throw new IllegalArgumentException("Unspawnable entity type " + type.name());
		}
		return entity.getBukkitEntity();
	}

	@Override
	public void disablePerching(Parrot parrot) {
		((CraftParrot) parrot).getHandle().goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof LandOnOwnersShoulderGoal);
	}

	@Override
	public void setAggressive(Creature entity, DamageAction action) {
		//0 is default. Would be like, a negative number but unfortunately -100^2 is a positive number
		setAggressive(entity, action, 0);
	}

	@Override
	public void setAggressive(Creature entity, DamageAction action, double attackRange) {
		PathfinderMob mob = ((CraftCreature) entity).getHandle();
		mob.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(mob, action, true, attackRange));
		mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, net.minecraft.world.entity.player.Player.class, true));
	}

	@Override
	public void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate, double attackRange) {
		PathfinderMob entityCreature = ((CraftCreature) entity).getHandle();

		//removing panic mode
		Optional<WrappedGoal> oldGoal = entityCreature.goalSelector.getAvailableGoals().stream().filter(task -> task.getGoal() instanceof PanicGoal).findFirst();
		if (oldGoal.isPresent()) {
			WrappedGoal goal = oldGoal.get();
			entityCreature.goalSelector.removeGoal(goal);
		}

		//removing others NearestAttackableTargetGoal
		List<WrappedGoal> list = entityCreature.targetSelector.getAvailableGoals().stream().filter(task -> task.getGoal() instanceof NearestAttackableTargetGoal).toList();
		for (WrappedGoal wrapped : list) {
			entityCreature.targetSelector.removeGoal(wrapped);
		}


		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(entityCreature, action, true) {
			@Override
			protected double getAttackReachSqr(net.minecraft.world.entity.LivingEntity target) {
				double x = mob.getX();
				double y = mob.getY() + 1;
				double z = mob.getZ();
				if (target.distanceToSqr(x, y, z) <= attackRange * attackRange) {
					return Double.POSITIVE_INFINITY;
				} else {
					return Double.NEGATIVE_INFINITY;
				}
			}
		});
		entityCreature.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entityCreature, net.minecraft.world.entity.LivingEntity.class, 10, false, false, entityLiving -> predicate.test(getLivingEntity(entityLiving))));
	}

	@Override
	public void setHuntingCompanion(Creature entity, DamageAction action, double attackRange) {
		PathfinderMob entityCreature = ((CraftCreature) entity).getHandle();

		//removing panic mode
		Optional<WrappedGoal> oldGoal = entityCreature.goalSelector.getAvailableGoals().stream().filter(task -> task.getGoal() instanceof PanicGoal).findFirst();
		if (oldGoal.isPresent()) {
			WrappedGoal goal = oldGoal.get();
			entityCreature.goalSelector.removeGoal(goal);
		}

		//removing others NearestAttackableTargetGoal
		List<WrappedGoal> list = entityCreature.targetSelector.getAvailableGoals().stream().filter(task -> task.getGoal() instanceof NearestAttackableTargetGoal).toList();
		for (WrappedGoal wrapped : list) {
			entityCreature.targetSelector.removeGoal(wrapped);
		}
		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(entityCreature, action, true) {
			@Override
			protected double getAttackReachSqr(net.minecraft.world.entity.LivingEntity target) {
				double x = mob.getX();
				double y = mob.getY() + 1;
				double z = mob.getZ();
				if (target.distanceToSqr(x, y, z) <= attackRange * attackRange) {
					return Double.POSITIVE_INFINITY;
				} else {
					return Double.NEGATIVE_INFINITY;
				}
			}
		});

	}

	private LivingEntity getLivingEntity(net.minecraft.world.entity.LivingEntity nmsEntity) {
		try {
			return nmsEntity.getBukkitLivingEntity();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void setAttackRange(Creature entity, double attackRange) {
		PathfinderMob mob = ((CraftCreature) entity).getHandle();
		Optional<WrappedGoal> oldGoal = mob.goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof MeleeAttackGoal).findFirst();
		if (oldGoal.isPresent()) {
			WrappedGoal goal = oldGoal.get();
			mob.goalSelector.getAvailableGoals().remove(goal);
			mob.goalSelector.addGoal(goal.getPriority(), new CustomPathfinderGoalMeleeAttack18(mob, 1.0, true, attackRange));
		}
	}

	@Override
	public Class<?> getResourceKeyClass() {
		return ResourceKey.class;
	}

	@Override
	public Object createDimensionTypeResourceKey(String namespace, String key) {
		return ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(namespace, key));
	}

	@Override
	// Cannot check resource key type, but this should only ever be called with the proper type anyway
	@SuppressWarnings("unchecked")
	public @Nullable World getWorldByResourceKey(Object key) {
		if (!(key instanceof ResourceKey<?> resourceKey)) {
			return null;
		}
		ServerLevel level = MinecraftServer.getServer().getLevel(((ResourceKey<Level>) resourceKey));
		return level == null ? null : level.getWorld();
	}

	@Override
	public void runConsoleCommandSilently(String command) {
		// bypasses dispatchServerCommand and ServerCommandEvent
		MinecraftServer.getServer().getCommands().performCommand(MinecraftServer.getServer().getCommands().getDispatcher().parse(command, MinecraftServer.getServer().createCommandSourceStack().withSuppressedOutput()), command);
	}

	public boolean hasCollision(World world, BoundingBox aabb) {
		return !((CraftWorld) world).getHandle().noCollision(new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()));
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		return hasCollisionWithBlocks(world, aabb, loadChunks, mat -> mat != Material.SCAFFOLDING);
	}

	@Override
	public boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks, Predicate<Material> checkedTypes) {
		return io.papermc.paper.util.CollisionUtil.getCollisionsForBlocksOrWorldBorder(((CraftWorld) world).getHandle(), null,
			new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()), null, loadChunks, false, false, true,
			(state, pos) -> checkedTypes.test(state.getBukkitMaterial()));
	}

	@Override
	public Set<Block> getCollidingBlocks(World world, BoundingBox aabb, boolean loadChunks) {
		List<AABB> collisions = new ArrayList<>();
		io.papermc.paper.util.CollisionUtil.getCollisionsForBlocksOrWorldBorder(((CraftWorld) world).getHandle(), null,
			new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()), collisions, loadChunks, false, false, false,
			(state, pos) -> state.getBukkitMaterial() != Material.SCAFFOLDING);
		Set<Block> result = new HashSet<>();
		for (AABB collision : collisions) {
			// This assumes that block collision centers are within their block.
			Vec3 center = collision.getCenter();
			result.add(world.getBlockAt((int) Math.floor(center.x), (int) Math.floor(center.y), (int) Math.floor(center.z)));
		}
		return result;
	}


	private static Class<?> getNearestAttackableTargetGoalTargetType(NearestAttackableTargetGoal<?> goal) {
		return NEAREST_ATTACKABLE_TARGET_GOAL_TARGET_TYPE.get(goal);
	}

	static {
		// make withers only attack players and not other mobs
		WITHER_TARGETING_CONDITIONS.get(null).selector(le -> le instanceof Player);
	}

	@Override
	public void mobAIChanges(Mob mob) {
		Set<WrappedGoal> availableGoals = ((CraftMob) mob).getHandle().goalSelector.getAvailableGoals();
		Set<WrappedGoal> availableTargetGoals = ((CraftMob) mob).getHandle().targetSelector.getAvailableGoals();
		if (mob instanceof Fox) {
			// prevent foxes running from players, wolves, and polar bears
			availableGoals.removeIf(goal -> goal.getGoal() instanceof AvoidEntityGoal);
		} else if (mob instanceof AbstractSkeleton) {
			// prevent skeletons running away from wolves
			availableGoals.removeIf(goal -> goal.getGoal() instanceof AvoidEntityGoal);
			if (mob instanceof WitherSkeleton) {
				// prevent wither skeletons from attacking piglins
				availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
					&& AbstractPiglin.class.isAssignableFrom(getNearestAttackableTargetGoalTargetType(natg)));
			}
			if (mob.getScoreboardTags().contains("boss_winged")) {
				// prevent skeletons from strafing if using boss_winged
				availableGoals.removeIf(goal -> goal.getGoal() instanceof RangedBowAttackGoal);
				availableGoals.add(new WrappedGoal(2, new CustomNoStrafeRangedBowAttackGoal((PathfinderMob) ((CraftMob) mob).getHandle(), 0, 20, 32)));
			}
		} else if (mob instanceof IronGolem) {
			// prevent iron golems defending villages and attacking mobs
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof DefendVillageTargetGoal);
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.Mob.class);
		} else if (mob instanceof Drowned) {
			// prevent drowneds from attacking mobs
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& net.minecraft.world.entity.Mob.class.isAssignableFrom(getNearestAttackableTargetGoalTargetType(natg)));
		} else if (mob instanceof Evoker) {
			// disable vexes and fangs on evokers with the proper tags
			if (mob.getScoreboardTags().contains("boss_evoker_no_vex")) {
				availableGoals.removeIf(goal -> "net.minecraft.world.entity.monster.EntityEvoker$c".equals(goal.getGoal().getClass().getName())); // EvokerSummonSpellGoal
			}
			if (mob.getScoreboardTags().contains("boss_evoker_no_fangs")) {
				availableGoals.removeIf(goal -> "net.minecraft.world.entity.monster.EntityEvoker$a".equals(goal.getGoal().getClass().getName())); // EvokerAttackSpellGoal
			}
		} else if (mob instanceof Wolf) {
			// prevent wolves from attacking animals and skeletons
			availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
				&& net.minecraft.world.entity.monster.AbstractSkeleton.class.isAssignableFrom(getNearestAttackableTargetGoalTargetType(natg)));
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
			availableTargetGoals.add(new WrappedGoal(2, new NearestAttackableTargetGoal<>(((CraftMob) mob).getHandle(), net.minecraft.world.entity.player.Player.class, false, false)));
		} else if (mob instanceof Spider) {
			// allow spiders to target and attack even with something riding them or if it's too bright
			availableGoals.removeIf(goal -> goal.getGoal().getClass().getDeclaringClass() == net.minecraft.world.entity.monster.Spider.class);
			double attackRange = mob instanceof CaveSpider ? 1.5 : 1.8; // lower than vanilla, even lower for cave spiders
			availableGoals.add(new WrappedGoal(4, new CustomPathfinderGoalMeleeAttack18((PathfinderMob) ((CraftMob) mob).getHandle(), 1.0D, true, attackRange)));
			availableTargetGoals.add(new WrappedGoal(2, new NearestAttackableTargetGoal<>(((CraftMob) mob).getHandle(), net.minecraft.world.entity.player.Player.class, false, false)));
			// disable leaping if desired
			if (mob.getScoreboardTags().contains("boss_spider_no_leap")) {
				availableGoals.removeIf(goal -> goal.getGoal() instanceof LeapAtTargetGoal);
			}
		} else if (mob instanceof Bee) {
			// for bees used as aggressive mobs, disable the peaceful behaviours of pollination and using bee hives
			if (mob.getScoreboardTags().contains("boss_targetplayer") || mob.getScoreboardTags().contains("boss_generictarget")) {
				availableGoals.removeIf(goal -> goal.getGoal().getClass().getSimpleName().equals("d") // BeeEnterHiveGoal
					|| goal.getGoal().getClass().getSimpleName().equals("k") // BeePollinateGoal
					|| goal.getGoal().getClass().getSimpleName().equals("i") // BeeLocateHiveGoal
					|| goal.getGoal().getClass().getSimpleName().equals("e") // BeeGoToHiveGoal
					|| goal.getGoal().getClass().getSimpleName().equals("f") // BeeGoToKnownFlowerGoal
					|| goal.getGoal().getClass().getSimpleName().equals("g") // BeeGrowCropGoal
				);
			}
		}

		// Prevent certain creepers from exploding
		if (mob instanceof Creeper && mob.getScoreboardTags().contains("boss_creeper_no_swell")) {
			availableGoals.removeIf(goal -> goal.getGoal() instanceof SwellGoal);
		}

		// prevent all mobs from attacking iron golems and turtles
		availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
			&& (getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.animal.IronGolem.class
			|| getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.animal.Turtle.class
			|| getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.npc.AbstractVillager.class));
	}

	@Override
	public Object toVanillaChatComponent(net.kyori.adventure.text.Component component) {
		return Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
	}

	@Override
	public boolean isSameItem(@Nullable org.bukkit.inventory.ItemStack item1, @Nullable org.bukkit.inventory.ItemStack item2) {
		return item1 == item2
			|| item1 instanceof CraftItemStack craftItem1 && item2 instanceof CraftItemStack craftItem2
			&& craftItem1.handle == craftItem2.handle;
	}

	@Override
	public void forceDismountVehicle(Entity entity) {
		((CraftEntity) entity).getHandle().stopRiding(true);
	}

	@Override
	public org.bukkit.inventory.ItemStack getUsedProjectile(Player player, org.bukkit.inventory.ItemStack weapon) {
		return CraftItemStack.asCraftMirror(((CraftPlayer) player).getHandle().getProjectile(CraftItemStack.asNMSCopy(weapon)));
	}

	@Override
	public net.kyori.adventure.text.Component getDisplayName(org.bukkit.inventory.ItemStack item) {
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
		Scoreboard nmsScoreboard = ((CraftScoreboard) scoreboard).getHandle();

		JsonObject data = new JsonObject();
		final var playerScores = SCOREBOARD_PLAYER_SCORES.get(nmsScoreboard);

		if (playerScores == null) {
			throw new IllegalArgumentException("Scoreboard#playerScores is null");
		}

		Map<Objective, Score> scores = playerScores.get(scoreHolder);
		if (scores == null) {
			// No scores for this player
			return data;
		}

		for (Map.Entry<Objective, Score> entry : scores.entrySet()) {
			data.addProperty(entry.getKey().getName(), entry.getValue().getScore());
		}

		return data;
	}

	public void resetScoreHolderScores(String scoreHolder, org.bukkit.scoreboard.Scoreboard scoreboard) {
		Scoreboard nmsScoreboard = ((CraftScoreboard) scoreboard).getHandle();
		nmsScoreboard.resetPlayerScore(scoreHolder, null);
	}

	@Override
	public void disableRangedAttackGoal(LivingEntity entity) {
		((CraftMob) entity).getHandle().goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof RangedAttackGoal);
	}

	@Override
	public void forceSyncEntityPositionData(Entity entity) {
		final var mcEntity = ((CraftEntity) entity).getHandle();
		final var tracker = mcEntity.tracker;

		if (tracker != null) {
			SERVER_ENTITY_BROADCAST_FIELD.get(tracker.serverEntity).accept(new ClientboundTeleportEntityPacket(mcEntity));
		}
	}
}
