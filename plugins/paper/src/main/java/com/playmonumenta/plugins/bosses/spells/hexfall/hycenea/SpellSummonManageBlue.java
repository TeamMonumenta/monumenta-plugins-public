package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HarrakfarGodOfLife;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellSummonManageBlue extends Spell {
	private final Plugin mMonumentaPlugin;
	private @Nullable LivingEntity mBlue;
	private int mT = 0;
	private int mAdvancementTicks = 0;
	private final Location mSpawnLoc;
	private final HyceneaRageOfTheWolf mHyceneaRageOfTheWolf;

	public SpellSummonManageBlue(Plugin monumentaPlugin, Location spawnLoc, HyceneaRageOfTheWolf hyceneaRageOfTheWolf) {
		mMonumentaPlugin = monumentaPlugin;
		mSpawnLoc = spawnLoc;
		mHyceneaRageOfTheWolf = hyceneaRageOfTheWolf;
	}

	@Override
	public void run() {
		if (mT++ == 0) {
			Entity blue = LibraryOfSoulsIntegration.summon(mSpawnLoc, "HarrakfarGodOfLife");
			if (blue instanceof LivingEntity livingBlue) {
				mBlue = livingBlue;
				mBlue.addScoreboardTag("boss_harrakfar[first=" + (mHyceneaRageOfTheWolf.getPhase() == 2 ? "true" : "false") + "]");
				mMonumentaPlugin.mBossManager.manuallyRegisterBoss(mBlue, new HarrakfarGodOfLife(mMonumentaPlugin, mBlue));
			}
		} else {
			if (mBlue != null && mBlue.getHealth() != HarrakfarGodOfLife.mHealth) {
				mAdvancementTicks++;
			}

			if (mBlue != null && (mBlue.getHealth() / EntityUtils.getAttributeBaseOrDefault(mBlue, Attribute.GENERIC_MAX_HEALTH, HarrakfarGodOfLife.mHealth) <= 0.5 || !mBlue.isValid())) {
				if (mAdvancementTicks <= 60 && mBlue.isValid()) {
					for (Player p : mHyceneaRageOfTheWolf.mPlayersStartingFight) {
						AdvancementUtils.grantAdvancement(p, "monumenta:dungeons/hexfall/false_god_of_life");
					}
				}

				new SpellSetHyceneaPhase(mHyceneaRageOfTheWolf, mHyceneaRageOfTheWolf.getPhase() == 2 ? 6 : 7, 0).run();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
