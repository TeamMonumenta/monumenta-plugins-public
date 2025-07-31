package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCerebralOnslaught extends Spell {
	private static final String SPELL_NAME = "Cerebral Overcharge";
	public static final String SPAWN_TAG = "CerebralOnslaught";
	protected final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	private final ChargeUpManager mChargeUpManager;
	private final SpellCooldownManager mSpellCooldownManager;

	public static final int OVERCHARGE_TIME = 3 * 20;
	public static final int SPAWN_COOLDOWN = 2 * 20;
	public static final int COOLDOWN_TICKS = SPAWN_COOLDOWN * 4 + OVERCHARGE_TIME + 20;

	public SpellCerebralOnslaught(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mChargeUpManager = new ChargeUpManager(boss, OVERCHARGE_TIME, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
		mSpellCooldownManager = new SpellCooldownManager(40 * 20, 20 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		mSpellCooldownManager.setOnCooldown();
		mActiveTasks.add(new BukkitRunnable() {
			int mSpawned = 0;
			int mTicks = 0;

			private final List<Location> mSpawnedLocations = new ArrayList<>();

			@Override
			public void run() {
				if (mSpawned < 3) {
					if (mTicks >= SPAWN_COOLDOWN) {
						mTicks = 0;
						for (int i = 0; i < 3; i++) {
							Entity summon = LibraryOfSoulsIntegration.summon(LocationUtils.randomSafeLocationInCircle(mCenter, 25, location ->
								mSpawnedLocations.stream().allMatch(loc -> loc.distance(location) > 3)), "CerebralOnslaught");
							if (summon != null) {
								summon.addScoreboardTag(SPAWN_TAG);
								mSpawnedLocations.add(summon.getLocation());
							}
						}
						mSpawned++;
						mChargeUpManager.setTime(0);
					}
					mTicks++;
				} else {
					if (mTicks >= SPAWN_COOLDOWN) {
						Location location = mBoss.getLocation();
						location.setY(mCenter.getY());
						Entity summon = LibraryOfSoulsIntegration.summon(location, "CerebralOvercharge");
						if (summon != null) {
							summon.addScoreboardTag(SPAWN_TAG);
						}

						EntityUtils.selfRoot(mBoss, OVERCHARGE_TIME);
						new BukkitRunnable() {
							@Override
							public void run() {
								if (mChargeUpManager.nextTick()) {
									mChargeUpManager.remove();
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
						this.cancel();
					}
					mTicks++;
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	public void cancel() {
		EntityUtils.getNearbyMobs(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE, EnumSet.of(EntityType.ARMOR_STAND))
			.stream().filter(entity -> entity.getScoreboardTags().contains("CerebralOnslaught"))
			.forEach(Entity::remove);
		super.cancel();
	}
}
