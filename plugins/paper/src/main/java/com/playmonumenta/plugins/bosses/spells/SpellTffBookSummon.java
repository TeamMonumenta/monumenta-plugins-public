package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellTffBookSummon extends Spell {
	private Plugin mPlugin;
	private static final int PLAYER_RANGE = 16;
	private static final int MAX_NEARBY_SUMMONS = 8;
	private final LivingEntity mBoss;

	public SpellTffBookSummon(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.75f);
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VEX_CHARGE, 1f, 1.5f);
				for (int i = 0; i < 4; i++) {
					LibraryOfSoulsIntegration.summon(mBoss.getLocation(), "MostlyHarmlessTome");
				}
			}

		}.runTaskLater(mPlugin, 20 * 1);
	}

	@Override
	public boolean canRun() {
		List<Entity> nearbyEntities = mBoss.getNearbyEntities(PLAYER_RANGE, PLAYER_RANGE, PLAYER_RANGE);
		if (nearbyEntities.stream().filter(
				e -> e.getType().equals(EntityType.VEX)
			).count() > MAX_NEARBY_SUMMONS) {
			return false;
		}

		if (((Creature)mBoss).getTarget() == null) {
			return false;
		}

		if (((mBoss instanceof Mob) && (((Mob)mBoss).getTarget() instanceof Player))) {
			for (Player player : PlayerUtils.playersInRange(mBoss.getEyeLocation(), PLAYER_RANGE)) {
				if (LocationUtils.hasLineOfSight(mBoss, player)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 20; //20 seconds
	}
}
