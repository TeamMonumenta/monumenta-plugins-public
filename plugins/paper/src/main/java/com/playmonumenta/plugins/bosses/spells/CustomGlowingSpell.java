package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.managers.GlowingManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class CustomGlowingSpell extends Spell {
	private final NamedTextColor mOtherGlowingColor;
	private final NamedTextColor mSelfGlowingColor;
	private final LivingEntity mBoss;
	private final EntityTargets mEntityTargets;
	private final int mPriority;
	private final ArrayList<LivingEntity> mSelf;
	private final boolean mGlowSelf;

	public CustomGlowingSpell(LivingEntity boss, String otherGlowColor, String selfGlowColor, EntityTargets targetingType, int priority, boolean passengerSelf, boolean glowSelf) {
		mBoss = boss;
		mOtherGlowingColor = NamedTextColor.NAMES.valueOr(otherGlowColor, NamedTextColor.BLACK);
		mSelfGlowingColor = NamedTextColor.NAMES.valueOr(selfGlowColor, NamedTextColor.WHITE);
		mPriority = priority;
		mEntityTargets = targetingType;
		mGlowSelf = glowSelf;

		mSelf = new ArrayList<>();
		mSelf.add(boss);

		if (passengerSelf) {
			getAllPassengers(boss, mSelf);
		}
	}

	@Override
	public void run() {
		for (LivingEntity e : mEntityTargets.getTargetsList(mBoss)) {
			if (mSelf.contains(e)) {
				continue;
			}
			GlowingManager.startGlowing(e, mOtherGlowingColor, 6, mPriority);
		}
		if (mGlowSelf) {
			for (LivingEntity e : mSelf) {
				GlowingManager.startGlowing(e, mSelfGlowingColor, 6, mPriority);
			}
		}
	}

	private void getAllPassengers(LivingEntity e, List<LivingEntity> list) {
		for (Entity entity : e.getPassengers()) {
			if (entity instanceof LivingEntity add) {
				list.add(add);
				getAllPassengers(add, list);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 5;
	}
}
