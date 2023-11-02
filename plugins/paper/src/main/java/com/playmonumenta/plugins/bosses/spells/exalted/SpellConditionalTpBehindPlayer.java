package com.playmonumenta.plugins.bosses.spells.exalted;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import java.util.function.Predicate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellConditionalTpBehindPlayer extends Spell {

	private LivingEntity mLauncher;
	private Plugin mPlugin;
	private Predicate<Entity> mPredicate;
	private boolean mCooldown = false;

	public SpellConditionalTpBehindPlayer(Plugin plugin, LivingEntity launcher, Predicate<Entity> predicate) {
		mPlugin = plugin;
		mLauncher = launcher;
		mPredicate = predicate;
	}

	@Override
	public void run() {
		if (mPredicate.test(mLauncher) && !mCooldown) {
			mCooldown = true;
			new BukkitRunnable() {
				@Override public void run() {
					mCooldown = false;
				}
			}.runTaskLater(mPlugin, 20 * 10);
			/* TODO: This needs to be able to load the destination chunk if needed */
			Spell spell = new SpellTpBehindPlayer(mPlugin, mLauncher, 99999, 110, 50, 10, true);
			spell.run();
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
