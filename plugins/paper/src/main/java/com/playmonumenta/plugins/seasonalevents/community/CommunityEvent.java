package com.playmonumenta.plugins.seasonalevents.community;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class CommunityEvent {
	private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	// the loot paths for default rewards
	private static final String ITEM_SKIN_KEY = "epic:pass/metamorphosis_token";
	private static final String TREASURE_WHEEL_KEY = "epic:pass/treasure_wheel_token";
	private static final String RELIC_WHEEL_KEY = "epic:pass/relic_wheel_token";

	public final LocalDateTime mStart;
	public final LocalDateTime mEnd;
	public final String mEventId;
	public final List<CommunityMissionDefinition> mMissions;

	// optional, for missions that unlock new content / something
	public final @Nullable String mCompletionFunctionCommand;
	public final @Nullable String mCompletionDescription;


	public static class TieredRewardSchema {
		public final String mLootT1;
		public final String mLootT2;
		public final String mLootT3;
		public final int mAmountT1;
		public final int mAmountT2;
		public final int mAmountT3;

		public TieredRewardSchema(String l1, int a1, String l2, int a2, String l3, int a3) {
			this.mLootT1 = l1;
			this.mAmountT1 = a1;
			this.mLootT2 = l2;
			this.mAmountT2 = a2;
			this.mLootT3 = l3;
			this.mAmountT3 = a3;
		}
	}

	private static final TieredRewardSchema STANDARD_PRESET = new TieredRewardSchema(
		ITEM_SKIN_KEY, 2,
		TREASURE_WHEEL_KEY, 6,
		RELIC_WHEEL_KEY, 2
	);

	// eventually can fill out with more preset reward pools
	private static final List<TieredRewardSchema> DEFAULT_REWARD_POOL = Arrays.asList(
		STANDARD_PRESET,
		STANDARD_PRESET,
		STANDARD_PRESET,
		STANDARD_PRESET,
		STANDARD_PRESET
	);

	// constructor for events themselves, random missions, default rewards
	public CommunityEvent(LocalDateTime start, LocalDateTime end) {
		this(start, end, null, null, null, null, null);
	}

	// constructor for optional setting missions, optional mission unlock as well
	public CommunityEvent(LocalDateTime start, LocalDateTime end,
						  @Nullable CommunityMissionDefinition m1,
						  @Nullable CommunityMissionDefinition m2,
						  @Nullable CommunityMissionDefinition m3,
						  @Nullable String completionFunctionCommand,
						  @Nullable String completionDescription) {
		this.mStart = start;
		this.mEnd = end;
		this.mEventId = start.format(ID_FORMAT);
		this.mCompletionFunctionCommand = completionFunctionCommand;
		this.mCompletionDescription = completionDescription;

		List<CommunityMissionDefinition> rawMissions;
		long seed = start.toLocalDate().toEpochDay();
		Random random = new Random(seed);

		if (m1 != null && m2 != null && m3 != null) {
			rawMissions = new ArrayList<>();
			rawMissions.add(m1);
			rawMissions.add(m2);
			rawMissions.add(m3);
		} else {
			rawMissions = generateRandomMissions(random);
		}

		this.mMissions = new ArrayList<>();
		for (CommunityMissionDefinition def : rawMissions) {
			// picks random reward scheme from list
			TieredRewardSchema defaultOption = DEFAULT_REWARD_POOL.get(random.nextInt(DEFAULT_REWARD_POOL.size()));
			this.mMissions.add(def.withRewards(defaultOption));
		}
	}

	// constructor for optional setting missions, no mission unlock
	public CommunityEvent(LocalDateTime start, LocalDateTime end,
						  CommunityMissionDefinition m1, CommunityMissionDefinition m2, CommunityMissionDefinition m3) {
		this(start, end, m1, m2, m3, null, null);
	}

	private List<CommunityMissionDefinition> generateRandomMissions(Random random) {
		List<CommunityMissionDefinition> result = new ArrayList<>();
		List<CommunityMissionType> allTypes = new ArrayList<>(CommunityMissionType.getAll());

		Collections.shuffle(allTypes, random);
		Set<CommunityMissionType.Category> usedCategories = new HashSet<>();

		for (CommunityMissionType type : allTypes) {
			if (result.size() >= 3) {
				break;
			}
			if (!usedCategories.contains(type.mCategory)) {
				result.add(new CommunityMissionDefinition(type));
				usedCategories.add(type.mCategory);
			}
		}
		return result;
	}

	public boolean isActive(LocalDateTime now) {
		return (now.isEqual(mStart) || now.isAfter(mStart)) && now.isBefore(mEnd);
	}

	public boolean isFinished(LocalDateTime now) {
		return now.isAfter(mEnd);
	}
}
