package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class EvolutionSeeds extends Spell {

	public static final String SPELL_NAME = "Evolution Seeds";
	public static final int COOLDOWN = 160;
	public static final int INTERNAL_COOLDOWN = 300;
	public static final int CAST_DELAY = 40;
	public static final int EVOLVE_COUNT_PER_PLAYER = 1;

	private final LivingEntity mBoss;
	private final PassiveGardenTwo mGarden;
	private final int mFinalCooldown;

	private boolean mOnCooldown = false;

	public EvolutionSeeds(LivingEntity boss, @Nullable DepthsParty party, PassiveGardenTwo garden) {
		mBoss = boss;
		mGarden = garden;
		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mOnCooldown = false, INTERNAL_COOLDOWN);

		BukkitRunnable evolveRunnable = new BukkitRunnable() {
			final ChargeUpManager mChargeUp = new ChargeUpManager(mBoss, CAST_DELAY,
				Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME, NamedTextColor.BLUE, TextDecoration.BOLD)),
				BossBar.Color.BLUE, BossBar.Overlay.PROGRESS, 100);

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}

				if (mChargeUp.nextTick()) {
					int playerCount = mBoss.getLocation().getNearbyPlayers(200).size();
					mGarden.evolveRandomFlowers(1 + playerCount * EVOLVE_COUNT_PER_PLAYER);
					this.cancel();
				}
			}
		};

		evolveRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	@Override
	public boolean canRun() {
		// Don't cast the spell if there are no Flowers alive in the Garden.
		long count = mBoss.getLocation().getNearbyEntities(200, 30, 200).stream().filter(e -> {
			Set<String> tags = e.getScoreboardTags();
			return tags.contains(Callicarpa.FLOWER_TAG) && !tags.contains(Callicarpa.FLOWER_EVOLVED_TAG);
		}).count();
		return !mOnCooldown && count != 0;
	}
}
