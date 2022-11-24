package com.playmonumenta.plugins.adapters;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftMob;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftParrot;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class VersionAdapter_v1_18_R2 implements VersionAdapter {

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

	private static class CustomDamageSource extends EntityDamageSource {
		private final boolean mBlockable;
		private final String mKilledUsingMsg;

		public CustomDamageSource(net.minecraft.world.entity.Entity damager, boolean blockable, @Nullable String killedUsingMsg) {
			super("custom", damager);
			mBlockable = blockable;
			if (killedUsingMsg == null || killedUsingMsg.isEmpty()) {
				// We don't want to see "Player was killed by Mob using ", so just get rid of the message if it's nothing
				mKilledUsingMsg = null;
			} else {
				mKilledUsingMsg = killedUsingMsg;
			}
		}

		@Override
		public @Nullable Vec3 getSourcePosition() {
			return mBlockable ? super.getSourcePosition() : null;
		}

		@Override
		public Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity entityliving) {
			if (mKilledUsingMsg == null) {
				String s = "death.attack.mob";
				return new TranslatableComponent(s, entityliving.getDisplayName(), this.entity.getDisplayName());
			} else {
				// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
				String s = "death.attack.indirectMagic.item";
				return new TranslatableComponent(s, entityliving.getDisplayName(), this.entity.getDisplayName(), mKilledUsingMsg);
			}
		}
	}

	public void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg) {
		DamageSource reason = damager == null ? DamageSource.GENERIC : new CustomDamageSource(((CraftLivingEntity) damager).getHandle(), blockable, killedUsingMsg);

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

	private static Field getField(Class<?> clazz, String field) {
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			// Should only happen if Minecraft is updated.
			// Check the documentation of where this is used for how to find the new name.
			throw new RuntimeException(e);
		}
	}

	private static @Nullable Object getFieldValue(Field field, Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
	}

	private static void setFieldValue(Field field, Object target, @Nullable Object value) {
		try {
			field.set(target, value);
		} catch (IllegalAccessException e) {
			// Should not happen as the field is set to be accessible
			throw new RuntimeException(e);
		}
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

	// unobfuscated field name: attackStrengthTicker
	private static final Field attackCooldownField = getField(net.minecraft.world.entity.LivingEntity.class, "aQ");

	@SuppressWarnings("unboxing.of.nullable")
	public int getAttackCooldown(LivingEntity entity) {
		return (int) getFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle());
	}

	public void setAttackCooldown(LivingEntity entity, int newCooldown) {
		setFieldValue(attackCooldownField, ((CraftLivingEntity) entity).getHandle(), newCooldown);
	}

	// Update the code in releaseActiveItem() below before updating this, as this may not even be used anymore.
	private static final Method tickActiveItemStack = getMethod(net.minecraft.world.entity.LivingEntity.class, "A");

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
	public int getEntityTypeRegistryId(Entity entity) {
		return Registry.ENTITY_TYPE.getId(((CraftEntity) entity).getHandle().getType());
	}

	@Override
	public void disablePerching(Parrot parrot) {
		((CraftParrot) parrot).getHandle().goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof LandOnOwnersShoulderGoal);
	}

	@Override
	public void setAggressive(Creature entity, DamageAction action) {
		PathfinderMob mob = ((CraftCreature) entity).getHandle();
		mob.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(mob, action));
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


		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(entityCreature, action) {
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
		entityCreature.goalSelector.addGoal(0, new CustomMobAgroMeleeAttack18(entityCreature, action) {
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

	public void setAttackRange(Creature entity, double attackRange, double attackHeight) {
		PathfinderMob mob = ((CraftCreature) entity).getHandle();
		Optional<WrappedGoal> oldGoal = mob.goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof MeleeAttackGoal).findFirst();
		if (oldGoal.isPresent()) {
			WrappedGoal goal = oldGoal.get();
			mob.goalSelector.getAvailableGoals().remove(goal);
			mob.goalSelector.addGoal(goal.getPriority(), new CustomPathfinderGoalMeleeAttack18(mob, 1.0, true, attackRange, attackHeight));
		}
	}

	@Override
	public Class<?> getResourceKeyClass() {
		return ResourceKey.class;
	}

	@Override
	public Object createDimensionTypeResourceKey(String namespace, String key) {
		return ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(namespace, key));
	}

	@Override
	public void executeCommandAsBlock(Block block, String command) {
		CommandBlockEntity tileEntity = new CommandBlockEntity(((CraftBlock) block).getPosition(), ((CraftBlockState) block.getState()).getHandle());
		tileEntity.setLevel(((CraftBlock) block).getHandle().getMinecraftWorld());
		Bukkit.dispatchCommand(tileEntity.getCommandBlock().getBukkitSender(tileEntity.getCommandBlock().createCommandSourceStack()), command);
	}

	public boolean hasCollision(World world, BoundingBox aabb) {
		return !((CraftWorld) world).getHandle().noCollision(new AABB(aabb.getMinX(), aabb.getMinY(), aabb.getMinZ(), aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()));
	}

	private static final Field targetTypeField = getField(NearestAttackableTargetGoal.class, "a"); // unobfuscated field name: targetType

	private static Class<?> getNearestAttackableTargetGoalTargetType(NearestAttackableTargetGoal<?> goal) {
		return (Class<?>) getFieldValue(targetTypeField, goal);
	}

	private static final Field WITHER_TARGETING_CONDITIONS = getField(WitherBoss.class, "cg"); // unobfuscated field name: TARGETING_CONDITIONS

	static {
		// make withers only attack players and not other mobs
		((TargetingConditions) getFieldValue(WITHER_TARGETING_CONDITIONS, null)).selector(le -> le instanceof Player);
	}

	@Override
	public void mobAIChanges(Mob mob) {
		Set<WrappedGoal> availableGoals = ((CraftMob) mob).getHandle().goalSelector.availableGoals;
		Set<WrappedGoal> availableTargetGoals = ((CraftMob) mob).getHandle().targetSelector.availableGoals;
		if (mob instanceof Fox || mob instanceof AbstractSkeleton) {
			// prevent foxes running from players, wolves, and polar bears, and skeletons running away from wolves
			availableGoals.removeIf(goal -> goal.getGoal() instanceof AvoidEntityGoal);
			if (mob instanceof WitherSkeleton) {
				// prevent wither skeletons from attacking piglins
				availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
					                                      && AbstractPiglin.class.isAssignableFrom(getNearestAttackableTargetGoalTargetType(natg)));
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
			availableGoals.add(new WrappedGoal(4, new MeleeAttackGoal((PathfinderMob) ((CraftMob) mob).getHandle(), 1.0D, true)));
			availableTargetGoals.add(new WrappedGoal(2, new NearestAttackableTargetGoal<>(((CraftMob) mob).getHandle(), net.minecraft.world.entity.player.Player.class, false, false)));
			// disable leaping if desired
			if (mob.getScoreboardTags().contains("boss_spider_no_leap")) {
				availableGoals.removeIf(goal -> goal.getGoal() instanceof LeapAtTargetGoal);
			}
		}
		// prevent all mobs from attacking iron golems and turtles
		availableTargetGoals.removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?> natg
			                                      && (getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.animal.IronGolem.class
				                                          || getNearestAttackableTargetGoalTargetType(natg) == net.minecraft.world.entity.animal.Turtle.class));
	}

	@Override
	public Object toVanillaChatComponent(net.kyori.adventure.text.Component component) {
		return Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
	}

}
