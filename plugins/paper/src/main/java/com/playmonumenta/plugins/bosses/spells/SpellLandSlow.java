package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class SpellLandSlow extends Spell {

	private LivingEntity mLauncher;
	private double mSlownessPercent;
	private Plugin mPlugin;

	public SpellLandSlow(Plugin plugin, LivingEntity launcher, double slownesspercent) {
		mLauncher = launcher;
		mSlownessPercent = slownesspercent;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();
		if (!loc.getBlock().isLiquid()) {
			if (EntityUtils.isSlowed(mPlugin, mLauncher)) {
				EntityUtils.setSlowTicks(mPlugin, mLauncher, 20);
			} else {
				EntityUtils.applySlow(mPlugin, 60, mSlownessPercent, mLauncher);
			}
		} else {
			EntityUtils.setSlowTicks(mPlugin, mLauncher, 0);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
