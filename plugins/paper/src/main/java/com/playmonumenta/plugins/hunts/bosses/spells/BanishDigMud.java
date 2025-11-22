package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.Muddied;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BanishDigMud extends Spell {
	public static final int DURATION = 8 * 20;
	public static final int MUD_BLOCKS_TO_BREAK = 3;
	private static final int RANGE = 30;
	public static final String MUDDIED_EFFECT_SOURCE = "HuntsBossExperimentSeventyOneMuddied";

	// Used to run only once
	public boolean mOnCooldown = false;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Plugin mPlugin;
	private final ExperimentSeventyOne mQuarry;

	public BanishDigMud(Plugin plugin, LivingEntity boss, ExperimentSeventyOne quarry) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mQuarry = quarry;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE, true, true);
		for (Player player : players) {
			player.sendMessage(Component.text("The mud begins to creep further along the ground...", ExperimentSeventyOne.TEXT_COLOR));
			// Give the players Muddied effect
			EffectManager.getInstance().addEffect(player, MUDDIED_EFFECT_SOURCE, new Muddied(DURATION + 5, MUD_BLOCKS_TO_BREAK));
		}
		List<Player> toRemove = new ArrayList<>();
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, DURATION, Component.text(String.format("Spreading Mud (%s)", Quarry.BANISH_CHARACTER), TextColor.color(179, 132, 48)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, RANGE);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 5f, 1.2f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 5f, 0.7f);

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mOnCooldown = true;
				// Check if the player no longer has the Muddied effect
				for (Player player : players) {
					if (!toRemove.contains(player) && !EffectManager.getInstance().hasEffect(player, Muddied.class)) {
						player.sendMessage(Component.text("You manage to get rid of the mud.", ExperimentSeventyOne.TEXT_COLOR));
						toRemove.add(player);
						chargeUp.excludePlayer(player);
					}
				}
				if (mTicks >= DURATION) {
					players.removeAll(toRemove);
					for (Player player : players) {
						player.sendMessage(Component.text("Before you sink too far in the mud, you retreat to the lodge.", ExperimentSeventyOne.TEXT_COLOR));
						player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1, 0.5f);
						mQuarry.banish(player);
					}
					this.cancel();
					return;
				}

				if (mTicks % 10 == 0) {
					int neededMud = (int) ((players.size() - toRemove.size()) * MUD_BLOCKS_TO_BREAK * Math.min((double) (2 * mTicks) / DURATION, 1));
					double radius = 0.5 + Math.sqrt(neededMud / Math.PI);
					for (Block block : BlockUtils.getBlocksInCylinder(mBoss.getLocation(), radius, 3)) {
						if (block.getType().isSolid() && !mQuarry.isWormSpawner(block)) {
							mQuarry.placeMudBlock(block, DURATION - mTicks);
						}
					}
				}

				chargeUp.nextTick();
				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	@Override
	public int cooldownTicks() {
		return 10 * 20;
	}

	@Override
	public boolean persistOnPhaseChange() {
		return true;
	}
}
