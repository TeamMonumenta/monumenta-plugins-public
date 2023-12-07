package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.rooms.DarkestDepthsRoomRepository;
import com.playmonumenta.plugins.depths.rooms.RoomRepository;
import com.playmonumenta.plugins.depths.rooms.ZenithRoomRepository;
import java.util.function.Supplier;

public enum DepthsContent {
	DARKEST_DEPTHS("[Depths Party]", DarkestDepthsRoomRepository::new, DepthsBoss.HEDERA, DepthsBoss.DAVEY, DepthsBoss.NUCLEUS, 1),
	CELESTIAL_ZENITH("[Zenith Party]", ZenithRoomRepository::new, DepthsBoss.CALLICARPA, DepthsBoss.BROODMOTHER, DepthsBoss.VESPERIDYS, 1.5);

	private final String mPrefix;
	private final Supplier<RoomRepository> mRoomRepositorySupplier;
	private final DepthsBoss[] mBosses;
	private final double mDamageMultiplier;

	DepthsContent(String prefix, Supplier<RoomRepository> supplier, DepthsBoss f1boss, DepthsBoss f2boss, DepthsBoss f3boss, double damageMultiplier) {
		mPrefix = prefix;
		mRoomRepositorySupplier = supplier;
		mBosses = new DepthsBoss[]{f1boss, f2boss, f3boss};
		mDamageMultiplier = damageMultiplier;
	}

	public String getPrefix() {
		return mPrefix;
	}

	public DepthsBoss getBoss(int floor) {
		return mBosses[(floor - 1) % 3];
	}

	// Should only be called once to initialize
	public RoomRepository getRoomRepository() {
		return mRoomRepositorySupplier.get();
	}

	public double getDamageMultiplier() {
		return mDamageMultiplier;
	}
}
