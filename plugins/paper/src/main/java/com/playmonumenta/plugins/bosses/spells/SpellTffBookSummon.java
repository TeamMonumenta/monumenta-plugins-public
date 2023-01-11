package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellTffBookSummon extends Spell {
	private Plugin mPlugin;
	private static final int PLAYER_RANGE = 16;
	private static final int MAX_NEARBY_SUMMONS = 8;
	private final LivingEntity mBoss;
	private final int mCoolDown = 20 * 20;
	private final EnumSet<EntityType> mTypes = EnumSet.of(
		EntityType.VEX
	);
	private int mT = 5 * 20;

	public SpellTffBookSummon(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		mT -= 5;
		if (mT <= 0) {
			//check max mob count
			List<LivingEntity> nearbyEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), PLAYER_RANGE, mTypes);
			if (nearbyEntities.size() < MAX_NEARBY_SUMMONS) {
				//check target
				if (((mBoss instanceof Mob) && (((Mob) mBoss).getTarget() != null) && (((Mob) mBoss).getTarget() instanceof Player))) {
					//check line of sight
					if (LocationUtils.hasLineOfSight(mBoss, ((Mob) mBoss).getTarget())) {
						mT = mCoolDown;
						summon();
					}
				}
			}
		}
	}

	private void summon() {
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1f, 0.75f);
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1f, 1.5f);
				for (int i = 0; i < 4; i++) {
					LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "MostlyHarmlessTome");
				}
			}

		}.runTaskLater(mPlugin, 20 * 1);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
