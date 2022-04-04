package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

public class Hope implements Infusion {

	/* How much longer an item lasts per level */
	private static final int EXTRA_MINUTES_PER_LEVEL = 5;
	private static final int TICK_PERIOD = 6;
	public static final String NO_AGE_CHANGE_TAG = "NoHopeAgeChange";

	@Override
	public String getName() {
		return "Hope";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.HOPE;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		// Do not set the age if DeathSort has set the age to despawn next tick
		if (!item.getScoreboardTags().contains(NO_AGE_CHANGE_TAG)) {
			NBTEntity nbt = new NBTEntity(item);
			nbt.setShort("Age", (short) (-1 * EXTRA_MINUTES_PER_LEVEL * Constants.TICKS_PER_MINUTE * value));
		}

		new BukkitRunnable() {
			int mNumTicks = 0;

			@Override
			public void run() {
				Location loc = item.getLocation();

				if (!loc.isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				item.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 3, 0.2, 0.2, 0.2, 0);

				// Very infrequently check if the item is still actually there
				mNumTicks++;
				if (mNumTicks > 100) {
					mNumTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, TICK_PERIOD);
	}
}
