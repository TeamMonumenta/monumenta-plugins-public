package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.PartingShot;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDrawAggro;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class PartingShotDummyBoss extends BossAbilityGroup {
	public static final String identityTag = "PartingShotDummyBoss";
	public static final int detectionRange = 30;

	@Nullable
	private PartingShot mPartingShot = null;

	public PartingShotDummyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(PartingShot partingShot, double radius, int duration) {
		mPartingShot = partingShot;

		List<Spell> passives = List.of(new SpellDrawAggro(mBoss, radius));
		super.changePhase(SpellManager.EMPTY, passives, null);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mBoss.isValid() && !mBoss.isDead()) {
				DamageUtils.damage(null, mBoss, DamageEvent.DamageType.OTHER, 10000);
			}
		}, duration);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (mPartingShot == null) {
			return;
		}

		Location loc = LocationUtils.getHalfHeightLocation(mBoss);
		mPartingShot.revealMobs(loc);
	}
}
