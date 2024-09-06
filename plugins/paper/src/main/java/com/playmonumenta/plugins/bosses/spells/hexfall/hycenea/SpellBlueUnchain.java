package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HarrakfarGodOfLife;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellBlueUnchain extends Spell {

	private static final String ABILITY_NAME = "Unchain (☠)";
	private final Plugin mPlugin;
	private final LivingEntity mBlue;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellBlueUnchain(Plugin mPlugin, LivingEntity mBlue, int mRange, int mCastTime, int mCooldown, Location mSpawnLoc) {
		this.mPlugin = mPlugin;
		this.mBlue = mBlue;
		this.mCooldown = mCooldown;
		this.mSpawnLoc = mSpawnLoc;
		mChargeUp = new ChargeUpManager(mBlue, mCastTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange * 2);
	}

	@Override
	public void run() {

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mBlue.getHealth() / EntityUtils.getAttributeBaseOrDefault(mBlue, Attribute.GENERIC_MAX_HEALTH, HarrakfarGodOfLife.mHealth) <= 0.5) {
					mChargeUp.remove();
					this.cancel();
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						PlayerUtils.killPlayer(player, mBlue, ABILITY_NAME, true, true, true);
					}
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
