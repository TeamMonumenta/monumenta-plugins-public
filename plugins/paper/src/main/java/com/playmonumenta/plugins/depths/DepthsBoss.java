package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public enum DepthsBoss {
	HEDERA("HederaVenomoftheWaves", Hedera.identityTag),
	DAVEY("LieutenantDaveyVoidHerald", Davey.identityTag),
	NUCLEUS("Gyrhaeddant", Nucleus.identityTag),
	CALLICARPA("CallicarpaBlightoftheBeyond", Callicarpa.identityTag),
	BROODMOTHER("TheBroodmother", Broodmother.identityTag),
	VESPERIDYS("TheVesperidys", Vesperidys.identityTag);

	private final String mLos;
	private final String mTag;

	DepthsBoss(String los, String tag) {
		mLos = los;
		mTag = tag;
	}

	public void summon(Location loc) {
		try {
			Entity entity = LibraryOfSoulsIntegration.summon(loc, mLos);
			if (entity instanceof LivingEntity boss) {
				BossManager.createBoss(null, boss, mTag, loc.clone().add(0, -2, 0));
			} else {
				MMLog.severe("Failed to summon depths boss " + mTag);
			}
		} catch (Exception e) {
			MMLog.severe("Failed to set up depths boss '" + mTag + "': " + e.getMessage());
			e.printStackTrace();
		}
	}
}
