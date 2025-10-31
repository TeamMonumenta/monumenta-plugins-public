package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EntityTargets implements Cloneable {

	/**
	 * targetsParam=[   TARGETS
	 * where: TARGETS ∈ {PLAYER, MOB, ENTITY}
	 * range
	 * where: range is a double that let you choose the range in where to search for targets
	 * limit=?
	 * where: ? is in {(number,sorted), (enum,sorted)} &&
	 * where: sorted ∈ {RANDOM, CLOSER, FARTHER, LOWER_HP, HIGHER_HP, NONE(?)} &&
	 * where: enum ∈ {ALL, HALF, ONE_QUARTER, ONE_EIGHTH} &&
	 * where: number ∈ {1,..,TARGERS.size}
	 * <p>
	 * the limit will limit the output of entity this param will give, execute after all the filters.
	 * BY DEFAULT: (ALL,RANDOM)
	 * <p>
	 * filters=?
	 * where: ? is a list of enumFilters &&
	 * where: {
	 * if TARGETS == PLAYER :
	 * enumFilters ∈ {isStealthed, isMage, isAlch, isRogue, .......}
	 * if TARGETS == MOB :
	 * enumFilters ∈ {isElite, isBoss, isZombie, notSelf, .....}
	 * if TARGETS == ENTITY :
	 * enumFilters ∈ {isArmorStand, isMob, isVillager,  ...........}
	 * }
	 * the filters will filter the result of TARGETS so that:
	 * {for each entity in TARGETS.getTargets(), enitity ∈ filteredList if and only if
	 * ∀(entity, filter(entity) == true) for all filters ∈ allMatchFilters, allMatchFilters = {filter : filter.anyMatchMode() == false}} AND
	 * (∃(entity, filter(entity) == true) for all filters ∈ anyMatchFilters, anyMatchFilters = {filter : filter.anyMatchMode() == true} OR anyMatchFilters = Ø)
	 * (Basically if all the non-mutually exclusive filters pass AND any the mutually exclusive filters pass OR there are no mutually exclusive filters)
	 * tags=?
	 * where: ? is a list of TagString && ----- work like filters, is considered a mutually exclusive filter BUT entities are only filtered AFTER filtering from `filters=[...], checks for lack of tag if tag starts with "!"
	 * <p>
	 * targetsParam=[TARGETS,range]
	 * targetsParam=[TARGETS,range,opt]
	 * targetsParam=[TARGETS,range,opt,limit=(..),filters=[..],tags=[..]]
	 * TARGETS: must be always the first element
	 * range: must be always the second element
	 * opt: when there are 3 or more element must always be the third
	 * limit=(), filters=[], tags=[]: can be any element 4th or later
	 * <p>
	 * DEFAULT TYPE:
	 * targetsParam=[TARGETS,range,TRUE,limit=(ALL,RANDOM),filters=[],tags=[]]
	 *
	 */

	public interface EntityFinder {
		List<? extends LivingEntity> getTargets(LivingEntity launcher, Location loc, double range);
	}

	//enum don't works with generics
	public enum TARGETS implements EntityFinder {
		PLAYER {
			@Override
			public List<Player> getTargets(LivingEntity launcher, Location loc, double range) {
				return PlayerUtils.playersInRange(loc, range, true);
			}
		},
		MOB {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location loc, double range) {
				return EntityUtils.getNearbyMobs(loc, range);
			}
		},
		ENTITY {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location loc, double range) {
				Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, range, range, range);
				entities.removeIf(entity -> !(entity instanceof LivingEntity));
				List<LivingEntity> entitiesList = new ArrayList<>(entities.size());
				for (Entity entity : entities) {
					entitiesList.add((LivingEntity) entity);
				}
				return entitiesList;
			}
		},
		SELF {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location notUsed, double notUsed2) {
				return List.of(launcher);
			}
		}
	}

	public interface EntityFilter {
		<V extends Entity> boolean filter(LivingEntity launcher, V entity);

		default boolean anyMatchType() {
			return false;
		}
	}

	//-----------------------------------------------------MOB FILTERS------------------------------------------------------------------------------------
	public enum MOBFILTER implements EntityFilter {
		IS_BOSS {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isBoss(entity);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_ELITE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isElite(entity);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		NOT_SELF {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return !launcher.equals(entity);
			}
		},
		IS_TARGET {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher instanceof Mob mob && entity.equals(mob.getTarget());
			}
		},
		HAS_LINEOFSIGHT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher.hasLineOfSight(entity);
			}
		}
	}

	//-----------------------------------------------------PLAYER FILTERS------------------------------------------------------------------------------------
	public enum PLAYERFILTER implements EntityFilter {
		IS_CLASSLESS {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && AbilityUtils.getClassNum(player) == 0;
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_MAGE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isMage(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_WARRIOR {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isWarrior(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_CLERIC {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isCleric(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_ROGUE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isRogue(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_ALCHEMIST {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isAlchemist(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_SCOUT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isScout(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_WARLOCK {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player player && PlayerUtils.isWarlock(player);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		HAS_LINEOFSIGHT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher.hasLineOfSight(entity);
			}
		},
		NOT_STEALTHED {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return !(entity instanceof Player p && AbilityUtils.isStealthed(p));
			}
		},
		IS_TARGET {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher instanceof Mob mob && entity.equals(mob.getTarget());
			}
		}

	}

	//-----------------------------------------------------ENTITY FILTERS------------------------------------------------------------------------------------
	//TODO-implements different filters.
	public enum ENTITYFILTERENUM implements EntityFilter {
		IS_ARMORSTAND {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity.getType() == EntityType.ARMOR_STAND;
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_HOSTILE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isHostileMob(entity);
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		IS_PLAYER {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player;
			}

			@Override
			public boolean anyMatchType() {
				return true;
			}
		},
		HAS_LINEOFSIGHT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher.hasLineOfSight(entity);
			}
		},
		NOT_SELF {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return !launcher.equals(entity);
			}
		},
		IS_TARGET {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher instanceof Mob mob && entity.equals(mob.getTarget());
			}
		}

	}


	public static class TagsListFiter implements EntityFilter {
		private final Set<String> mTags;

		public TagsListFiter(Set<String> tags) {
			mTags = tags;
		}

		@Override
		public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
			for (String tag : mTags) {
				if (tag.startsWith("!") && !entity.getScoreboardTags().contains(tag.substring(1))) {
					return true;
				}
				if (entity.getScoreboardTags().contains(tag)) {
					return true;
				}
			}
			return false;
		}


		@Override
		public String toString() {
			boolean first = true;
			String string = "[";
			for (String tag : mTags) {
				string += (first ? "" : ",") + tag;
				first = false;
			}

			return string + "]";
		}

		public static final TagsListFiter DEFAULT = new TagsListFiter(new LinkedHashSet<>());
	}

	public static class Limit {

		private interface LimitToNum {
			int getNum(List<?> list);
		}

		private interface SortingInterface {
			<V extends Entity> void sort(Location loc, List<V> list);
		}

		public enum LIMITSENUM implements LimitToNum {
			ALL {
				@Override
				public int getNum(List<?> list) {
					return list.size();
				}

			}, HALF {
				@Override
				public int getNum(List<?> list) {
					int num = list.size() / 2;
					if (!list.isEmpty()) {
						return num > 0 ? num : 1;
					}
					return 0;
				}

			}, ONE_QUARTER {
				@Override
				public int getNum(List<?> list) {
					int num = list.size() / 4;
					if (!list.isEmpty()) {
						return num > 0 ? num : 1;
					}
					return 0;
				}

			}, ONE_EIGHTH {
				@Override
				public int getNum(List<?> list) {
					int num = list.size() / 8;
					if (!list.isEmpty()) {
						return num > 0 ? num : 1;
					}
					return 0;
				}
			}
		}

		public enum SORTING implements SortingInterface {
			RANDOM {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					Collections.shuffle(list);
				}
			},
			CLOSER {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					list.sort(Comparator.comparingDouble(e -> loc.distance(e.getLocation())));
				}
			},
			FARTHER {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					list.sort((e1, e2) -> Double.compare(
						loc.distance(e2.getLocation()),
						loc.distance(e1.getLocation())
					));
				}
			},
			LOWER_HP {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					list.sort((e1, e2) -> {
						// Entities with no health are counted as 0 health,
						// for satisfying comparator contract.
						double e1Health = 0;
						double e2Health = 0;
						if (e1 instanceof LivingEntity livingE1) {
							e1Health = livingE1.getHealth();
						}
						if (e2 instanceof LivingEntity livingE2) {
							e2Health = livingE2.getHealth();
						}
						return Double.compare(e1Health, e2Health);
					});
				}
			},
			HIGHER_HP {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					Collections.sort(list, (e1, e2) -> {
						// Entities with no health are counted as 0 health,
						// for satisfying comparator contract.
						double e1Health = 0;
						double e2Health = 0;
						if (e1 instanceof LivingEntity livingE1) {
							e1Health = livingE1.getHealth();
						}
						if (e2 instanceof LivingEntity livingE2) {
							e2Health = livingE2.getHealth();
						}
						return Double.compare(e2Health, e1Health);
					});
				}
			}
		}

		private final @Nullable LIMITSENUM mLimitEnum;
		private final SORTING mSorting;
		private final int mNumb;

		public Limit(LIMITSENUM limit) {
			this(limit, SORTING.RANDOM);
		}

		public Limit(LIMITSENUM limit, SORTING sorting) {
			mLimitEnum = limit;
			mSorting = sorting;
			mNumb = 0;
		}

		public Limit(int num) {
			this(num, SORTING.RANDOM);
		}

		public Limit(int num, SORTING sorting) {
			mLimitEnum = null;
			mSorting = sorting;
			mNumb = num;
		}


		public <V extends Entity> List<V> sort(Location loc, List<V> list) {
			if (list == null) {
				return new ArrayList<>(0);
			}

			int num = mNumb;
			if (mLimitEnum != null) {
				num = mLimitEnum.getNum(list);
			}

			if (num > list.size()) {
				num = list.size();
			}
			mSorting.sort(loc, list);
			return list.subList(0, num);

		}

		@Override
		public String toString() {
			if (mLimitEnum == null) {
				return "(" + mNumb + "," + mSorting.name() + ")";
			} else {
				return "(" + mLimitEnum.name() + "," + mSorting.name() + ")";
			}
		}


		public static final Limit DEFAULT = new Limit(LIMITSENUM.ALL, SORTING.RANDOM);
		public static final Limit DEFAULT_ONE = new Limit(1, SORTING.RANDOM);
		public static final Limit CLOSER_ONE = new Limit(1, SORTING.CLOSER);
		public static final Limit DEFAULT_CLOSER = new Limit(LIMITSENUM.ALL, SORTING.CLOSER);
	}


	public static final EntityTargets GENERIC_PLAYER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, Limit.DEFAULT, List.of(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_PLAYER_TARGET_LINE_OF_SIGHT = new EntityTargets(TARGETS.PLAYER, 30, Limit.DEFAULT, List.of(PLAYERFILTER.HAS_LINEOFSIGHT), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_MOB_TARGET = new EntityTargets(TARGETS.MOB, 30, Limit.DEFAULT, List.of(MOBFILTER.NOT_SELF), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_SELF_TARGET = new EntityTargets(TARGETS.SELF, 0, Limit.DEFAULT, List.of(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_ONE_PLAYER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, Limit.DEFAULT_ONE, List.of(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_ONE_PLAYER_CLOSER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, Limit.CLOSER_ONE, List.of(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_ONE_PLAYER_CLOSER_TARGET_LINE_OF_SIGHT = new EntityTargets(TARGETS.PLAYER, 30, Limit.CLOSER_ONE, List.of(PLAYERFILTER.HAS_LINEOFSIGHT), TagsListFiter.DEFAULT);

	private static final String LIMIT_STRING = "limit=";
	private static final String FILTERS_STRING = "filters=";
	private static final String TAGS_FILTER_STRING = "tags=";

	private final TARGETS mTargets;
	private double mRange;
	private Limit mLimit;
	private Collection<EntityFilter> mFilters;
	private final TagsListFiter mTagsFilter;

	public EntityTargets(TARGETS targets, double range) {
		this(targets, range, Limit.DEFAULT);
	}

	public EntityTargets(TARGETS targets, double range, Limit limit) {
		this(targets, range, limit, List.of());
	}

	public EntityTargets(TARGETS targets, double range, Limit limit, List<EntityFilter> filters) {
		this(targets, range, limit, filters, TagsListFiter.DEFAULT);
	}

	public EntityTargets(TARGETS targets, double range, Limit.LIMITSENUM limitsenum, Limit.SORTING sorting, @Nullable List<EntityFilter> filters, List<String> tagsListFilter) {
		this(targets, range, new Limit(limitsenum, sorting), filters == null ? List.of() : filters, new TagsListFiter(new HashSet<>(tagsListFilter)));
	}

	public EntityTargets(TARGETS targets, double range, Limit limit, Collection<EntityFilter> filters, TagsListFiter tagsfilter) {
		mTargets = targets;
		mRange = range;
		mLimit = limit;
		mFilters = filters;
		mTagsFilter = tagsfilter;
	}

	@Override
	public String toString() {
		return "[" + mTargets.name() + "," + mRange + "," + LIMIT_STRING + mLimit.toString() + "," + FILTERS_STRING + mFilters + "," + TAGS_FILTER_STRING + mTagsFilter.toString() + "]";
	}

	public List<? extends LivingEntity> getTargetsList(LivingEntity boss) {
		return getTargetsListByLocation(boss, boss.getLocation());
	}

	public List<? extends LivingEntity> getTargetsListByLocation(LivingEntity boss, Location loc) {
		// Mutable List
		List<? extends LivingEntity> list = mTargets.getTargets(boss, loc, mRange);

		// pass = !(any modeAll fail) && (any modeAny pass || modeAny = {})
		if (!mFilters.isEmpty()) {
			list.removeIf(entity -> {
				boolean modeAllOneFailed = false;
				boolean modeAnyOnePassed = false;
				boolean hasModeAny = false;
				for (EntityFilter filter : mFilters) {
					if (filter.anyMatchType()) {
						hasModeAny = true;
						if (filter.filter(boss, entity)) {
							modeAnyOnePassed = true;
						}
					} else {
						if (!filter.filter(boss, entity)) {
							modeAllOneFailed = true;
						}
					}
				}

				return modeAllOneFailed || (hasModeAny && !modeAnyOnePassed);
			});
		}
		if (!mTagsFilter.mTags.isEmpty()) {
			list.removeIf(entity -> !mTagsFilter.filter(boss, entity));
		}

		return mLimit.sort(loc, list);
	}

	public double getRange() {
		return mRange;
	}

	public EntityTargets setRange(double range) {
		mRange = range;
		return this;
	}

	public EntityTargets setLimit(Limit limit) {
		mLimit = limit;
		return this;
	}

	public EntityTargets setFilters(List<EntityFilter> filters) {
		mFilters = filters;
		return this;
	}

	@Override
	public EntityTargets clone() {
		return new EntityTargets(mTargets, mRange, mLimit, mFilters, mTagsFilter);
	}

	public List<Location> getTargetsLocationList(LivingEntity boss) {
		List<? extends LivingEntity> entityList = getTargetsList(boss);
		List<Location> locations = new ArrayList<>(entityList.size());
		for (LivingEntity entity : entityList) {
			locations.add(entity.getLocation());
		}
		return locations;
	}

	public static EntityTargets fromString(String string) {
		return Parser.parseOrDefault(Parser.getParserMethod(EntityTargets.class), string, new EntityTargets(TARGETS.MOB, 25));
	}
}
