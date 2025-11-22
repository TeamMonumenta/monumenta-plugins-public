package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import java.util.function.Supplier;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

/**
 * <li>Intended to allow spells to manage their own cooldowns</Li>
 */
public class SpellCooldownManager {
	private final int mCooldown;
	private int mCurrentCooldown;
	@Nullable
	private final Supplier<Boolean> mCanTickDown;
	private final Supplier<Boolean> mValid;
	private final BukkitTask mTickDownTask;
	public static final int PERIOD = 2;

	/**
	 * @param cooldown                    cooldown in ticks
	 * @param validRequirement            if this is false, the runnable cancels
	 * @param cooldownTickDownRequirement if this is true, the cooldown decreases
	 */
	public SpellCooldownManager(int cooldown, Supplier<Boolean> validRequirement, @Nullable Supplier<Boolean> cooldownTickDownRequirement) {
		this(cooldown, 0, validRequirement, cooldownTickDownRequirement);
	}

	/**
	 * @param cooldown                    cooldown in ticks
	 * @param startingCooldown            initial cooldown on initialisation
	 * @param validRequirement            if this is false, the runnable cancels
	 * @param cooldownTickDownRequirement if this is true, the cooldown decreases
	 */
	public SpellCooldownManager(int cooldown, int startingCooldown, Supplier<Boolean> validRequirement, @Nullable Supplier<Boolean> cooldownTickDownRequirement) {
		mCooldown = cooldown;
		mCurrentCooldown = startingCooldown;
		mValid = validRequirement;
		mCanTickDown = cooldownTickDownRequirement;
		mTickDownTask = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, PERIOD);
	}

	public BukkitTask getTickDownTask() {
		return mTickDownTask;
	}

	private void tick() {
		if (!mValid.get()) {
			mTickDownTask.cancel();
		}
		if (mCurrentCooldown > 0 && (mCanTickDown == null || mCanTickDown.get())) {
			mCurrentCooldown -= PERIOD;
		}
	}

	public boolean onCooldown() {
		return mCurrentCooldown > 0;
	}

	public void setOnCooldown() {
		mCurrentCooldown = mCooldown;
	}

	// Sets a custom cooldown
	public void setOnCooldown(int cooldown) {
		mCurrentCooldown = cooldown;
	}
}
