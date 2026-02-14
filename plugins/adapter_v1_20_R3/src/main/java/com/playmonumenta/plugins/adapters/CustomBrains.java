package com.playmonumenta.plugins.adapters;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Credits to Njol
@SuppressWarnings({"unchecked", "UnusedReturnValue", "UnusedMethod"})
public class CustomBrains {

	// there's a bunch of protected method/fields we cant to use, which we can conveniently access without reflection by making subclasses
	private abstract static class PiglinAccessor extends Piglin {
		static ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = Piglin.MEMORY_TYPES;
		static ImmutableList<SensorType<? extends Sensor<? super Piglin>>> SENSOR_TYPES = Piglin.SENSOR_TYPES;

		private PiglinAccessor(EntityType<? extends Piglin> type, Level world) {
			super(type, world);
		}
	}

	private abstract static class PiglinAiAccessor extends PiglinAi {
		protected static @NotNull Brain<?> makeBrain(Piglin piglin, Brain<Piglin> brain) {
			return PiglinAi.makeBrain(piglin, brain);
		}
	}

	private abstract static class PiglinBruteAccessor extends PiglinBrute {
		static ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = PiglinBrute.MEMORY_TYPES;
		static ImmutableList<SensorType<? extends Sensor<? super PiglinBrute>>> SENSOR_TYPES = PiglinBrute.SENSOR_TYPES;

		private PiglinBruteAccessor(EntityType<? extends PiglinBrute> type, Level world) {
			super(type, world);
		}
	}

	private abstract static class PiglinBruteAiAccessor extends PiglinBruteAi {
		protected static @NotNull Brain<?> makeBrain(PiglinBrute piglinBrute, Brain<PiglinBrute> brain) {
			return PiglinBruteAi.makeBrain(piglinBrute, brain);
		}
	}

	private abstract static class HoglinAccessor extends Hoglin {
		static ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = Hoglin.MEMORY_TYPES;
		static ImmutableList<? extends SensorType<? extends Sensor<? super Hoglin>>> SENSOR_TYPES = Hoglin.SENSOR_TYPES;

		private HoglinAccessor(EntityType<? extends Hoglin> type, Level world) {
			super(type, world);
		}
	}

	private abstract static class HoglinAiAccessor extends HoglinAi {
		protected static @NotNull Brain<?> makeBrain(Brain<Hoglin> brain) {
			return HoglinAi.makeBrain(brain);
		}
	}

	public static CustomPiglinBrain makePiglinBrain(Piglin piglin, int minRangedDistance, int maxRangedDistance,
													int minShotDelay, int maxShotDelay, int attackSpeed) {
		Collection<? extends MemoryModuleType<?>> memories = PiglinAccessor.MEMORY_TYPES;
		Collection<? extends SensorType<? extends Sensor<? super Piglin>>> sensors = PiglinAccessor.SENSOR_TYPES;
		CustomPiglinBrain newBrain = new CustomPiglinBrain(piglin, memories, sensors, ImmutableList.of(),
			() -> Brain.codec(memories, sensors), minRangedDistance, maxRangedDistance, minShotDelay, maxShotDelay, attackSpeed);
		PiglinAiAccessor.makeBrain(piglin, newBrain);
		return newBrain;
	}

	public static CustomPiglinBruteBrain makePiglinBruteBrain(PiglinBrute piglinBrute, int attackSpeed) {
		Collection<? extends MemoryModuleType<?>> memories = PiglinBruteAccessor.MEMORY_TYPES;
		Collection<? extends SensorType<? extends Sensor<? super PiglinBrute>>> sensors = PiglinBruteAccessor.SENSOR_TYPES;
		CustomPiglinBruteBrain newBrain = new CustomPiglinBruteBrain(piglinBrute, memories, sensors, ImmutableList.of(),
			() -> Brain.codec(memories, sensors), attackSpeed);
		PiglinBruteAiAccessor.makeBrain(piglinBrute, newBrain);
		return newBrain;
	}

	public static CustomHoglinBrain makeHoglinBrain(Hoglin hoglin) {
		Collection<? extends MemoryModuleType<?>> memories = HoglinAccessor.MEMORY_TYPES;
		ImmutableList<? extends SensorType<? extends Sensor<? super Hoglin>>> sensors = HoglinAccessor.SENSOR_TYPES;
		CustomHoglinBrain newBrain = new CustomHoglinBrain(hoglin, memories, sensors, ImmutableList.of(), () -> Brain.codec(memories, sensors));
		HoglinAiAccessor.makeBrain(newBrain);
		return newBrain;
	}

	public static void dropAbstractPiglinAggro(AbstractPiglin piglin) {
		Brain<?> brain = piglin.getBrain();
		brain.setActiveActivityIfPossible(Activity.IDLE);
	}

	public static class CustomPiglinBrain extends Brain<Piglin> {

		private final Piglin piglin;
		private final int minRangedDistance;
		private final int maxRangedDistance;
		private final int minShotDelay;
		private final int maxShotDelay;
		private final int attackSpeed;

		public CustomPiglinBrain(Piglin piglin,
								 Collection<? extends MemoryModuleType<?>> memories,
								 Collection<? extends SensorType<? extends Sensor<? super Piglin>>> sensors,
								 ImmutableList memoryEntries, Supplier<Codec<Brain<Piglin>>> codecSupplier,
								 int minRangedDistance, int maxRangedDistance, int minShotDelay, int maxShotDelay,
								 int attackSpeed) {
			super(memories, sensors, memoryEntries, codecSupplier);
			this.piglin = piglin;
			this.minRangedDistance = minRangedDistance;
			this.maxRangedDistance = maxRangedDistance;
			this.minShotDelay = minShotDelay;
			this.maxShotDelay = maxShotDelay;
			this.attackSpeed = attackSpeed;
		}

		@Override
		public <U> void setMemoryWithExpiry(@NotNull MemoryModuleType<U> type, @NotNull U value, long expiry) {
			if (type == MemoryModuleType.AVOID_TARGET) {
				return;
			}
			super.setMemoryWithExpiry(type, value, expiry);
		}

		@Override
		public void addActivity(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super Piglin>> list) {
			if (activity.equals(Activity.CORE)) {
				// change PiglinAi.initCoreActivity
				super.addActivity(Activity.CORE, begin, ImmutableList.of(
					new LookAtTargetSink(45, 90),
					new MoveToTargetSink(),
					InteractWithDoor.create()
					// removed PiglinAi.babyAvoidNemesis()
					// removed PiglinAi.avoidZombified()
					// removed StopHoldingItemIfNoLongerAdmiring.create()
					// removed StartAdmiringItemIfSeen.create(120)
					// removed StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance)
					// removed StopBeingAngryIfTargetDead.create()
				));
			} else if (activity.equals(Activity.IDLE)) {
				// change PiglinAi.initIdleActivity
				super.addActivity(Activity.IDLE, begin, ImmutableList.copyOf(Stream.<BehaviorControl<? super Piglin>>of(
					// removed SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F)
					// changed: allow babies to attack as well
					StartAttacking.create(PiglinAi::findNearestValidAttackTarget),
					// removed BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create())
					// removed PiglinAi.avoidRepellent()
					// removed PiglinAi.babySometimesRideBabyHoglin()
					PiglinAi.createIdleLookBehaviors(),
					// changed: made optional
					piglin.getTags().contains("boss_no_pathfinding") ? null : PiglinAi.createIdleMovementBehaviors(),
					SetLookAndInteract.create(EntityType.PLAYER, 4)
				).filter(Objects::nonNull).toList()));
			} else {
				super.addActivity(activity, begin, list);
			}
		}

		@Override
		public void addActivityAndRemoveMemoryWhenStopped(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super Piglin>> tasks, @NotNull MemoryModuleType<?> memoryType) {
			if (activity.equals(Activity.FIGHT)) {
				// change PiglinAi.initFightActivity
				int extraRange = maxRangedDistance - ((ProjectileWeaponItem) Items.CROSSBOW).getDefaultProjectileRange();
				super.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, begin,
					ImmutableList.copyOf(Stream.<BehaviorControl<? super Piglin>>of(
						tasks.getFirst(), // task 0 is StopAttackingIfTargetInvalid (private access), re-use to avoid reflection
						// changed for configurable distance
						BehaviorBuilder.triggerIf(piglin -> piglin.isHolding(Items.CROSSBOW), BackUpIfTooClose.create(minRangedDistance, 0.75F)),
						// changed: made optional
						piglin.getTags().contains("boss_no_pathfinding") ? null : createSetWalkTargetFromAttackTargetIfTargetOutOfReach(e -> 1.0F, extraRange), // custom to change range
						// changed: configurable attack speed
						MeleeAttack.create(attackSpeed),
						// custom goal to extend range and change shooting delay
						new CustomCrossbowAttack<>(extraRange, minShotDelay, maxShotDelay)
						// removed RememberIfHoglinWasKilled.create()
						// removed EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)
					).filter(Objects::nonNull).toList()),
					memoryType);
			} else {
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin, tasks, memoryType);
			}
		}

		// Piglin should attack player wearing gold, & ignore wither skeletons/withers
		@Override
		public <U> @NotNull Optional<U> getMemory(@NotNull MemoryModuleType<U> type) {
			if (type == MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD || type == MemoryModuleType.NEAREST_VISIBLE_NEMESIS) {
				return (Optional<U>) super.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
			}
			if (type == MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT) {
				// prevent piglins from fleeing from hoglins by making them think there's no hoglins around
				return (Optional<U>) Optional.of(0);
			}
			return super.getMemory(type);
		}

		@Override
		public <U> Optional<U> getMemoryInternal(@NotNull MemoryModuleType<U> type) {
			if (type == MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD || type == MemoryModuleType.NEAREST_VISIBLE_NEMESIS) {
				return (Optional<U>) super.getMemoryInternal(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
			}
			return super.getMemoryInternal(type);
		}
	}

	public static class CustomPiglinBruteBrain extends Brain<PiglinBrute> {
		private final PiglinBrute piglinBrute;
		private final int attackSpeed;

		public CustomPiglinBruteBrain(PiglinBrute piglinBrute, Collection<? extends MemoryModuleType<?>> memories, Collection<? extends SensorType<? extends Sensor<? super PiglinBrute>>> sensors,
									  ImmutableList memoryEntries, Supplier<Codec<Brain<PiglinBrute>>> codecSupplier, int attackSpeed) {
			super(memories, sensors, memoryEntries, codecSupplier);
			this.piglinBrute = piglinBrute;
			this.attackSpeed = attackSpeed;
		}

		@Override
		public void addActivity(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super PiglinBrute>> list) {
			if (activity.equals(Activity.IDLE) && piglinBrute.getTags().contains("boss_no_pathfinding")) {
				// remove createIdleMovementBehaviors() if pathfinding is disabled
				super.addActivity(activity, begin, ImmutableList.of(list.get(0), list.get(1), list.get(3)));
			} else {
				super.addActivity(activity, begin, list);
			}
		}

		@Override
		public void addActivityAndRemoveMemoryWhenStopped(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super PiglinBrute>> tasks, @NotNull MemoryModuleType<?> memoryType) {
			if (activity.equals(Activity.FIGHT) && piglinBrute.getTags().contains("boss_no_pathfinding")) {
				// remove SetWalkTargetFromAttackTargetIfTargetOutOfReach if pathfinding is disabled
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin, ImmutableList.of(tasks.get(0), tasks.get(2)), memoryType);
			} else if (activity.equals(Activity.FIGHT)) {
				// changed: configurable attack speed
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin, ImmutableList.of(tasks.get(0), tasks.get(1), MeleeAttack.create(attackSpeed)), memoryType);
			} else {
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin, tasks, memoryType);
			}
		}

		// Piglin Brutes should ignore wither skeletons/withers
		@Override
		public <U> @NotNull Optional<U> getMemory(@NotNull MemoryModuleType<U> type) {
			if (type == MemoryModuleType.NEAREST_VISIBLE_NEMESIS) {
				return (Optional<U>) super.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
			}

			return super.getMemory(type);
		}

		@Override
		public <U> Optional<U> getMemoryInternal(@NotNull MemoryModuleType<U> type) {
			if (type == MemoryModuleType.NEAREST_VISIBLE_NEMESIS) {
				return (Optional<U>) super.getMemoryInternal(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
			}
			return super.getMemoryInternal(type);
		}

	}

	@SuppressWarnings("deprecation")
	public static class CustomHoglinBrain extends Brain<Hoglin> {
		private final Hoglin hoglin;

		public CustomHoglinBrain(Hoglin hoglin, Collection<? extends MemoryModuleType<?>> memories, Collection<? extends SensorType<? extends Sensor<? super Hoglin>>> sensors,
								 ImmutableList memoryEntries, Supplier<Codec<Brain<Hoglin>>> codecSupplier) {
			super(memories, sensors, memoryEntries, codecSupplier);
			this.hoglin = hoglin;
		}

		@Override
		public void addActivity(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super Hoglin>> list) {
			if (activity.equals(Activity.CORE) && hoglin.getTags().contains("boss_no_pathfinding")) {
				// remove MoveToTargetSink if pathfinding is disabled
				super.addActivity(activity, begin, ImmutableList.of(list.getFirst()));
			} else if (activity.equals(Activity.IDLE)) {
				super.addActivity(activity, begin, ImmutableList.of(
					// removed BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200),
					// removed new AnimalMakeLove(EntityType.HOGLIN, 0.6F),
					// removed SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, true),
					list.get(3), // StartAttacking.create(HoglinAi::findNearestValidAttackTarget),
					// removed BehaviorBuilder.triggerIf(Hoglin::isAdult, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4F, 8, false)),
					SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
					// removed BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 0.6F),
					list.get(list.size() - 1)// createIdleMovementBehaviors()
				));
			} else {
				super.addActivity(activity, begin, list);
			}
		}

		@Override
		public void addActivityAndRemoveMemoryWhenStopped(@NotNull Activity activity, int begin, @NotNull ImmutableList<? extends BehaviorControl<? super Hoglin>> tasks, @NotNull MemoryModuleType<?> memoryType) {
			if (activity.equals(Activity.FIGHT)) {
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin,
					ImmutableList.copyOf(Stream.<BehaviorControl<? super Hoglin>>of(
						// removed BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200),
						// removed new AnimalMakeLove(EntityType.HOGLIN, 0.6F),
						hoglin.getTags().contains("boss_no_pathfinding") ? null : SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
						BehaviorBuilder.triggerIf(Hoglin::isAdult, MeleeAttack.create(40)),
						BehaviorBuilder.triggerIf(AgeableMob::isBaby, MeleeAttack.create(15)),
						StopAttackingIfTargetInvalid.create()
						// remove EraseMemoryIf.create(HoglinAi::isBreeding, MemoryModuleType.ATTACK_TARGET)
					).filter(Objects::nonNull).toList()), memoryType);
			} else {
				super.addActivityAndRemoveMemoryWhenStopped(activity, begin, tasks, memoryType);
			}
		}

		@Override
		public <U> @NotNull Optional<U> getMemory(@NotNull MemoryModuleType<U> type) {
			if (type == MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT) {
				// prevent hoglins from fleeing from piglins by making them think there's no piglins around
				return (Optional<U>) Optional.of(0);
			}
			return super.getMemory(type);
		}
	}

	// copied from SetWalkTargetFromAttackTargetIfTargetOutOfReach
	public static BehaviorControl<Mob> createSetWalkTargetFromAttackTargetIfTargetOutOfReach(Function<LivingEntity, Float> speed, int extraRange) {
		return BehaviorBuilder.create((context) -> context.group(context.registered(MemoryModuleType.WALK_TARGET), context.registered(MemoryModuleType.LOOK_TARGET), context.present(MemoryModuleType.ATTACK_TARGET), context.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply(context, (walkTarget, lookTarget, attackTarget, visibleMobs) -> (world, entity, time) -> {
			LivingEntity livingEntity = context.get(attackTarget);
			Optional<NearestVisibleLivingEntities> optional = context.tryGet(visibleMobs);
			if (optional.isPresent() && optional.get().contains(livingEntity) && BehaviorUtils.isWithinAttackRange(entity, livingEntity, 1 - extraRange)) {
				walkTarget.erase();
			} else {
				lookTarget.set(new EntityTracker(livingEntity, true));
				walkTarget.set(new WalkTarget(new EntityTracker(livingEntity, false), speed.apply(entity), 0));
			}

			return true;
		}));
	}

	private static class CustomCrossbowAttack<E extends Mob & CrossbowAttackMob> extends CrossbowAttack<E, LivingEntity> {

		private final int extraRange;
		private final int minShotDelay;
		private final int maxShotDelay;

		CustomCrossbowAttack(int extraRange, int minShotDelay, int maxShotDelay) {
			this.extraRange = extraRange;
			this.minShotDelay = minShotDelay;
			this.maxShotDelay = maxShotDelay;
		}

		@Override
		protected boolean checkExtraStartConditions(@NotNull ServerLevel world, @NotNull E entity) {
			LivingEntity livingEntity = getAttackTarget(entity);
			if (livingEntity == null) {
				return false;
			}
			// change: extend range
			return entity.isHolding(Items.CROSSBOW) && BehaviorUtils.canSee(entity, livingEntity) && BehaviorUtils.isWithinAttackRange(entity, livingEntity, -extraRange);
		}

		@Nullable
		private static LivingEntity getAttackTarget(LivingEntity entity) {
			if (entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
				return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
			}
			return null;
		}

		@Override
		protected void tick(@NotNull ServerLevel serverLevel, @NotNull E mob, long l) {
			int oldAttackDelay = attackDelay;
			super.tick(serverLevel, mob, l);
			int newAttackDelay = attackDelay;
			// change: configurable shot delay (default is 20 to 40 ticks)
			if (newAttackDelay > oldAttackDelay) {
				this.attackDelay = minShotDelay == maxShotDelay ? minShotDelay : minShotDelay + mob.getRandom().nextInt(maxShotDelay - minShotDelay + 1);
			}
		}
	}

}
