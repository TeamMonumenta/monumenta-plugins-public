package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class EntityTargets implements Cloneable {

	/**
	 *          targetsParam=[   TARGETS
	 *                                         where: TARGETS ∈ {PLAYER, MOB, ENTITY}
	 *                             range
	 *                                         where: range is a double that let you choose the range in where to search for targets
	 *                               opt
	 *                                         where: opt is a boolean used for targets function {includeNonTargetable, notIncludeLauncher} by default true
	 *                                limit=?
	 *                                         where: ? is in {(number,sorted), (enum,sorted)} &&
	 *                                             where: sorted ∈ {RANDOM, CLOSER, FARTHER, LOWER_HP, HIGHER_HP, NONE(?)} &&
	 *                                             where: enum ∈ {ALL, HALF, ONE_QUARTER, ONE_EIGHTH} &&
	 *                                             where: number ∈ {1,..,TARGERS.size}
	 *
	 *                                         the limit will limit the output of entity this param will give, execute after all the filters.
	 *                                         BY DEFAULT: (ALL,RANDOM)
	 *
	 *                                filters=?
	 *                                         where: ? is a list of enumFilters &&
	 *                                            where: {
	 *                                                        if TARGETS == PLAYER :
	 *                                                               enumFilters ∈ {isStealthed, isMage, isAlch, isRogue, .......}
	 *                                                        if TARGETS == MOB :
	 *                                                               enumFilters ∈ {isElite, isBoss, isZombie, isScheleton, .....}
	 *                                                        if TARGETS == ENTITY :
	 *                                                               enumFilters ∈ {isArmorStand, isMob, isVillager,  ...........}
	 *                                                   }
	 *                                        the filters will filter the result of TARGETS so that:
	 *                                            {for each entity in TARGETS.getTargets(), enitity ∈ filteredList if and only if ∃ enumFilter in enumFiltersList | enumFilter(entity) == true}
	 *                                tags=?
	 *                                         where: ? is a list of TagString && ----- work like filters
	 *
	 *
	 *         targetsParam=[TARGETS,range]
	 *         targetsParam=[TARGETS,range,opt]
	 *         targetsParam=[TARGETS,range,opt,limit=(..)]
	 *         targetsParam=[TARGETS,range,opt,filters=[..]]
	 *         targetsParam=[TARGETS,range,opt,xxx,yyy,tags=[..]]
	 *           TARGETS: must be always the first element
	 *           range: must be always the second element
	 *           opt: when there are 3 or more element must always be the third
	 *           limit=(): can be both 4th or 5th element when present
	 *           filters=[]: can be both 4th or 5th element when present
	 *           tags=[]: must be the 5th element when present
	 *
	 * DEFAULT TYPE:
	 *         targetsParam=[TARGETS,range,TRUE,limit=(ALL,RANDOM),filters=[],tags=[]]
	 *
	 */

	public interface EntityFinder {
		List<? extends LivingEntity> getTargets(LivingEntity launcher, Location loc, double range, boolean optional);
	}

	//enum don't works with generics
	public enum TARGETS implements EntityFinder {
		PLAYER {
			@Override
			public List<Player> getTargets(LivingEntity launcher, Location loc, double range, boolean includeNonTargetable) {
				return PlayerUtils.playersInRange(loc, range, includeNonTargetable);
			}
		},
		MOB {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location loc, double range, boolean notIncludeLauncher) {
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, range, launcher);
				if (!notIncludeLauncher) {
					mobs.add(launcher);
				}
				return mobs;
			}
		},
		ENTITY {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location loc, double range, boolean notIncludeLauncher) {
				Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, range, range, range);
				if (notIncludeLauncher) {
					entities.remove(launcher);
				}
				entities.removeIf(entity -> !(entity instanceof LivingEntity));
				List<LivingEntity> entitiesList = new ArrayList<>(entities.size());
				for (Entity entity : entities) {
					entitiesList.add((LivingEntity)entity);
				}
				return entitiesList;
			}
		},
		SELF {
			@Override
			public List<LivingEntity> getTargets(LivingEntity launcher, Location notUsed, double notUsed2, boolean notUsed3) {
				return List.of(launcher);
			}
		};
	}

	public interface EntityFilter {
		<V extends Entity> boolean filter(LivingEntity launcher, V entity);

	}

	//-----------------------------------------------------MOB FILTERS------------------------------------------------------------------------------------
	public enum MOBFILTER implements EntityFilter {
		IS_BOSS {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isBoss(entity);
			}
		},
		IS_ELITE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isElite(entity);
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
		};
	}

	//-----------------------------------------------------PLAYER FILTERS------------------------------------------------------------------------------------
	public enum PLAYERFILTER implements EntityFilter {
		IS_CLASSLESS {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return ScoreboardUtils.getScoreboardValue((Player) entity, "Class").get() == 0;
			}
		},
		IS_MAGE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isMage((Player) entity);
			}
		},
		IS_WARRIOR {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isWarrior((Player) entity);
			}
		},
		IS_CLERIC {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isCleric((Player) entity);
			}
		},
		IS_ROGUE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isRogue((Player) entity);
			}
		},
		IS_ALCHEMIST {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isAlchemist((Player) entity);
			}
		},
		IS_SCOUT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isScout((Player) entity);
			}
		},
		IS_WARLOCK {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return PlayerUtils.isWarlock((Player) entity);
			}
		},
		HAS_LINEOFSIGHT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher.hasLineOfSight(entity);
			}
		};

	}

	//-----------------------------------------------------ENTITY FILTERS------------------------------------------------------------------------------------
	//TODO-implements different filters.
	public enum ENTITYFILTERENUM implements EntityFilter {
		IS_ARMORSTAND {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity.getType() == EntityType.ARMOR_STAND;
			}
		},
		IS_HOSTILE {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return EntityUtils.isHostileMob(entity);
			}
		},
		IS_PLAYER {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return entity instanceof Player;
			}
		},
		HAS_LINEOFSIGHT {
			@Override
			public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
				return launcher.hasLineOfSight(entity);
			}
		};

	}


	public static class TagsListFiter implements EntityFilter {
		private Set<String> mTags;

		public TagsListFiter(Set<String> tags) {
			mTags = tags;
		}

		@Override
		public <V extends Entity> boolean filter(LivingEntity launcher, V entity) {
			for (String tag : mTags) {
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

		public static ParseResult<TagsListFiter> parseResult(StringReader reader, String hoverDescription) {
			if (!reader.advance("[")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
			}

			Set<String> tags = new LinkedHashSet<>();
			boolean atLeastOneTagsIter = false;

			while (true) {
				if (reader.advance("]")) {
					break;
				}

				if (atLeastOneTagsIter) {
					if (!reader.advance(",")) {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", hoverDescription),
							Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
					}
				}

				atLeastOneTagsIter = true;


				String tag = reader.readString();

				if (tag == null) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "\"tagggg\"", "write tag")));
				}

				tags.add(tag);
			}

			return ParseResult.of(new TagsListFiter(tags));
		}
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
					if (list.size() > 0) {
						return num > 0 ? num : 1;
					}
					return 0;
				}

			}, ONE_QUARTER {
				@Override
				public int getNum(List<?> list) {
					int num = list.size() / 4;
					if (list.size() > 0) {
						return num > 0 ? num : 1;
					}
					return 0;
				}

			}, ONE_EIGHTH {
				@Override
				public int getNum(List<?> list) {
					int num = list.size() / 8;
					if (list.size() > 0) {
						return num > 0 ? num : 1;
					}
					return 0;
				}
			};
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
					Collections.sort(list, (e1, e2) -> {
						return (int) (loc.distance(((Entity)e1).getLocation()) - loc.distance(((Entity)e2).getLocation()));
					});
				}
			},
			FARTHER {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					Collections.sort(list, (e1, e2) -> {
						return (int) ((loc.distance(((Entity)e1).getLocation()) - loc.distance(((Entity)e2).getLocation())) * -1);
					});
				}
			},
			LOWER_HP {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					Collections.sort(list, (e1, e2) -> {
						if (e1 instanceof LivingEntity && e2 instanceof LivingEntity) {
							return (int) (((LivingEntity)e1).getHealth() - ((LivingEntity)e2).getHealth());
						}
						return 0;
					});
				}
			},
			HIGHER_HP {
				@Override
				public <V extends Entity> void sort(Location loc, List<V> list) {
					Collections.sort(list, (e1, e2) -> {
						if (e1 instanceof LivingEntity && e2 instanceof LivingEntity) {
							return (int) ((((LivingEntity)e1).getHealth() - ((LivingEntity)e2).getHealth()) * -1);
						}
						return 0;
					});
				}
			};
		}

		private LIMITSENUM mLimitEnum;
		private SORTING mSorting;
		private int mNumb;

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

		//format (num,sortingEnum) || (limitEnum,sortingEnum)
		public static ParseResult<Limit> fromReader(StringReader reader, String hoverDescription) {
			if (!reader.advance("(")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", hoverDescription)));
			}

			LIMITSENUM limit = reader.readEnum(LIMITSENUM.values());
			Long number = null;
			if (limit == null) {
				number = reader.readLong();
				if (number == null) {
					// Entry not valid, offer all entries as completions
					List<Tooltip<String>> suggArgs = new ArrayList<>(LIMITSENUM.values().length + 1);
					String soFar = reader.readSoFar();
					for (LIMITSENUM valid : LIMITSENUM.values()) {
						suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
					}
					suggArgs.add(Tooltip.of(soFar + "1", "Specify the numbers"));

					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}
			}

			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
			}

			SORTING sort = reader.readEnum(SORTING.values());

			if (sort == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(SORTING.values().length);
				String soFar = reader.readSoFar();
				for (SORTING valid : SORTING.values()) {
					suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
			}

			if (limit == null) {
				return ParseResult.of(new Limit(number.intValue(), sort));
			} else {
				return ParseResult.of(new Limit(limit, sort));
			}
		}


	}


	public static final EntityTargets GENERIC_PLAYER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, true, Limit.DEFAULT, new ArrayList<>(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_PLAYER_TARGET_LINE_OF_SIGHT = new EntityTargets(TARGETS.PLAYER, 30, true, Limit.DEFAULT, List.of(PLAYERFILTER.HAS_LINEOFSIGHT), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_MOB_TARGET = new EntityTargets(TARGETS.MOB, 30, true, Limit.DEFAULT, new ArrayList<>(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_SELF_TARGET = new EntityTargets(TARGETS.SELF, 0, false, Limit.DEFAULT, new ArrayList<>(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_ONE_PLAYER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, true, Limit.DEFAULT_ONE, new ArrayList<>(), TagsListFiter.DEFAULT);
	public static final EntityTargets GENERIC_ONE_PLAYER_CLOSER_TARGET = new EntityTargets(TARGETS.PLAYER, 30, true, Limit.CLOSER_ONE, new ArrayList<>(), TagsListFiter.DEFAULT);

	private static final String LIMIT_STRING = "limit=";
	private static final String FILTERS_STRING = "filters=";
	private static final String TAGS_FILTER_STRING = "tags=";

	private TARGETS mTargets;
	private double mRange;
	private Boolean mOptional = true;
	private Limit mLimit;
	private List<EntityFilter> mFilters;
	private TagsListFiter mTagsFilter;


	public EntityTargets(TARGETS targets, double range) {
		this(targets, range, true);
	}

	public EntityTargets(TARGETS targets, double range, boolean optional) {
		this(targets, range, optional, Limit.DEFAULT);
	}

	public EntityTargets(TARGETS targets, double range, boolean optional, Limit limit) {
		this(targets, range, optional, limit, new ArrayList<>());
	}

	public EntityTargets(TARGETS targets, double range, boolean optional, Limit limit, List<EntityFilter> filters) {
		this(targets, range, optional, limit, filters, TagsListFiter.DEFAULT);
	}

	public EntityTargets(TARGETS targets, double range, boolean optional, Limit limit, List<EntityFilter> filters, TagsListFiter tagsfilter) {
		mTargets = targets;
		mRange = range;
		mOptional = optional;
		mLimit = limit;
		mFilters = filters;
		mTagsFilter = tagsfilter;
	}

	@Override
	public String toString() {
		String string = "[" + mTargets.name() + "," + mRange + "," + mOptional.toString() + "," + LIMIT_STRING + mLimit.toString() + "," + FILTERS_STRING + "[";
		boolean first = true;
		for (EntityFilter filter : mFilters) {
			if (filter instanceof Enum) {
				string += (first ? "" : ",") + ((Enum<?>) filter).name();
				first = false;
			}
		}
		string += "],";
		string += TAGS_FILTER_STRING + mTagsFilter.toString() + "]";
		return string;
	}

	public List<? extends LivingEntity> getTargetsList(LivingEntity boss) {
		return getTargetsListByLocation(boss, boss.getLocation());
	}

	public List<? extends LivingEntity> getTargetsListByLocation(LivingEntity boss, Location loc) {
		List<? extends LivingEntity> list = mTargets.getTargets(boss, loc, mRange, mOptional);

		if (!mTagsFilter.mTags.isEmpty() || !mFilters.isEmpty()) {
			for (LivingEntity entity : new ArrayList<>(list)) {

				if (mTagsFilter.filter(boss, entity)) {
					continue;
				}

				boolean dontRemove = false;
				for (EntityFilter filter : mFilters) {
					if (filter.filter(boss, entity)) {
						dontRemove = true;
						break;
					}
				}
				if (!dontRemove) {
					list.remove(entity);
				}
			}
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

	@Override
	public EntityTargets clone() {
		return new EntityTargets(mTargets, mRange, mOptional, mLimit, mFilters, mTagsFilter);
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
		ParseResult<EntityTargets> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as EntityTargets");
			Thread.dumpStack();
			return new EntityTargets(TARGETS.MOB, 25);
		}

		return result.getResult();
	}


	public static ParseResult<EntityTargets> fromReader(StringReader reader, String hoverDescription) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
		}

		TARGETS target = reader.readEnum(TARGETS.values());
		if (target == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(TARGETS.values().length);
				String soFar = reader.readSoFar();
				for (TARGETS valid : TARGETS.values()) {
					suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
		}

		Double range = reader.readDouble();
		if (range == null) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "10.0", hoverDescription)));
		}

		if (!reader.advance(",")) {
			if (!reader.advance("]")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + ",", hoverDescription),
					Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
			}
			return ParseResult.of(new EntityTargets(target, range));
		}

		Boolean optional = reader.readBoolean();
		if (optional == null) {
			switch (target) {
				case PLAYER -> {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "true", "true -> target player in stealth"),
						Tooltip.of(reader.readSoFar() + "false", "false -> DON'T target player in stealth")));
				}
				case MOB, ENTITY -> {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "true", "true -> DON'T include the launcher"),
						Tooltip.of(reader.readSoFar() + "false", "false -> include the launcher")));
				}
				default -> {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "true", "not used >.<"),
						Tooltip.of(reader.readSoFar() + "false", "not used >.>")));
				}
			}
		}


		if (!reader.advance(",")) {
			if (!reader.advance("]")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + ",", hoverDescription),
					Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
			}
			return ParseResult.of(new EntityTargets(target, range, optional));
		}

		//2 options
		//reading limits
		//reading filters
		boolean foundLimits = reader.advance(LIMIT_STRING);
		boolean foundFilters = reader.advance(FILTERS_STRING);

		if (!foundLimits && !foundFilters) {
			//no match but we want to write them
			return ParseResult.of(Tooltip.arrayOf(
				Tooltip.of(reader.readSoFar() + LIMIT_STRING, hoverDescription),
				Tooltip.of(reader.readSoFar() + FILTERS_STRING, hoverDescription)));
		}

		Limit limit = null;
		List<EntityFilter> filters = new ArrayList<>(4);

		if (foundLimits) {
			ParseResult<Limit> parseLimit = Limit.fromReader(reader, hoverDescription);
			if (parseLimit.getResult() == null) {
				return ParseResult.of(parseLimit.getTooltip());
			}

			if (!reader.advance(",")) {
				if (!reader.advance("]")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
				}
				return ParseResult.of(new EntityTargets(target, range, optional, parseLimit.getResult()));
			}

			limit = parseLimit.getResult();

			foundFilters = reader.advance(FILTERS_STRING);
			if (!foundFilters) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + FILTERS_STRING, hoverDescription)));
			}

			if (!reader.advance("[")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
			}

			boolean atLeastOneFilterIter = false;

			while (true) {

				if (reader.advance("]")) {
					break;
				}

				if (atLeastOneFilterIter) {
					if (!reader.advance(",")) {
						if (!reader.advance("]")) {
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.of(reader.readSoFar() + ",", hoverDescription),
								Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
						}
						break;
					}
				}

				atLeastOneFilterIter = true;

				if (target == TARGETS.PLAYER) {
					//read the playerfilter
					PLAYERFILTER filter = reader.readEnum(PLAYERFILTER.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(PLAYERFILTER.values().length);
						String soFar = reader.readSoFar();
						for (PLAYERFILTER valid : PLAYERFILTER.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished

				} else if (target == TARGETS.MOB) {
					//read the mobs
					MOBFILTER filter = reader.readEnum(MOBFILTER.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(MOBFILTER.values().length);
						String soFar = reader.readSoFar();
						for (MOBFILTER valid : MOBFILTER.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished
				} else if (target == TARGETS.ENTITY) {
					//read for entity
					ENTITYFILTERENUM filter = reader.readEnum(ENTITYFILTERENUM.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(ENTITYFILTERENUM.values().length);
						String soFar = reader.readSoFar();
						for (ENTITYFILTERENUM valid : ENTITYFILTERENUM.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished
				} /* else if (target == TARGETS.SELF) {
					//im' not sure if we need some condictions for this since it would be a "fake" Statefull boss
				}*/

			}
		} else {
			//----------------------------------------------------------------------------------------------------------------------
			//found a filter


			if (!reader.advance("[")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
			}
			boolean atLeastOneFilterIter = false;

			while (true) {
				if (reader.advance("]")) {
					break;
				}

				if (atLeastOneFilterIter) {
					if (!reader.advance(",")) {
						if (!reader.advance("]")) {
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.of(reader.readSoFar() + ",", hoverDescription),
								Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
						}
						break;
					}
				}

				atLeastOneFilterIter = true;

				if (target == TARGETS.PLAYER) {
					//read the playerfilter
					PLAYERFILTER filter = reader.readEnum(PLAYERFILTER.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(PLAYERFILTER.values().length);
						String soFar = reader.readSoFar();
						for (PLAYERFILTER valid : PLAYERFILTER.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished

				} else if (target == TARGETS.MOB) {
					//read the mobs
					MOBFILTER filter = reader.readEnum(MOBFILTER.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(MOBFILTER.values().length);
						String soFar = reader.readSoFar();
						for (MOBFILTER valid : MOBFILTER.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished
				} else if (target == TARGETS.ENTITY) {
					//read for entity
					ENTITYFILTERENUM filter = reader.readEnum(ENTITYFILTERENUM.values());
					if (filter == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(ENTITYFILTERENUM.values().length);
						String soFar = reader.readSoFar();
						for (ENTITYFILTERENUM valid : ENTITYFILTERENUM.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}
					filters.add(filter);
					continue;
					//not finished
				} /* else if (target == TARGETS.SELF) {
					//im' not sure if we need some condictions for this since it would be a "fake" Statefull boss
				}*/

			}

			if (!reader.advance(",")) {
				if (!reader.advance("]")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
				}
				return ParseResult.of(new EntityTargets(target, range, optional, Limit.DEFAULT, filters));
			}

			//we want read the limit
			if (!reader.advance(LIMIT_STRING)) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + LIMIT_STRING, hoverDescription)));
			}

			ParseResult<Limit> parseLimit = Limit.fromReader(reader, hoverDescription);
			limit = parseLimit.getResult();
		}



		if (!reader.advance(",")) {
			if (!reader.advance("]")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + ",", hoverDescription),
					Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
			}
			return ParseResult.of(new EntityTargets(target, range, optional, limit, filters));
		}


		//TAGS

		if (!reader.advance(TAGS_FILTER_STRING)) {
			return ParseResult.of(Tooltip.arrayOf(
				Tooltip.of(reader.readSoFar() + TAGS_FILTER_STRING, hoverDescription)));
		}

		ParseResult<TagsListFiter> parseTags = TagsListFiter.parseResult(reader, hoverDescription);
		if (parseTags.getResult() == null) {
			return ParseResult.of(parseTags.getTooltip());
		}

		TagsListFiter tags = parseTags.getResult();

		if (!reader.advance("]")) {
			return ParseResult.of(Tooltip.arrayOf(
					Tooltip.of(reader.readSoFar() + "]", hoverDescription)));
		}

		return ParseResult.of(new EntityTargets(target, range, optional, limit, filters, tags));

	}

}
