package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BanishFallDistance extends Spell {
	//Used to run only once
	public boolean mOnCooldown = false;
	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final Quarry mQuarry;
	//Distance it targets players. Needs to be large as its 20-25 blocks up in the air generally.
	private static final int RANGE = 75;
	//How long till the banish ends.

	private static final int DURATION = 10 * 20;
	//Approximately how far they have to fall. Sometimes it needs to be built up a bit more (~1 block)
	private static final float FALL_DISTANCE = 5.5f;

	public BanishFallDistance(Plugin plugin, LivingEntity boss, Quarry quarry) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE, true, true);
		for (Player p : players) {
			p.sendMessage(Component.text("Your eyelids begin to feel heavy...", SteelWingHawk.COLOR));
			p.playSound(p.getLocation(), Sound.ENTITY_FOX_SCREECH, SoundCategory.HOSTILE, 2.0f, 1.2f);
			p.playSound(p.getLocation(), Sound.ENTITY_FOX_SCREECH, SoundCategory.HOSTILE, 2.0f, 1.5f);
			p.playSound(p.getLocation(), Sound.ENTITY_PARROT_IMITATE_PHANTOM, SoundCategory.HOSTILE, 2.0f, 1.2f);
		}
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, DURATION, Component.text(String.format("Soporific Shriek (%s)", Quarry.BANISH_CHARACTER), NamedTextColor.RED), BossBar.Color.RED, BossBar.Overlay.PROGRESS, RANGE);
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mOnCooldown = true;
				List<Player> toRemove = new ArrayList<>();
				for (Player p : players) {
					if (p.getFallDistance() >= FALL_DISTANCE) {
						p.sendMessage(Component.text("The fall jolts you back awake!", SteelWingHawk.COLOR));
						toRemove.add(p);
						chargeUp.excludePlayer(p);
					}
				}
				players.removeAll(toRemove);
				if (mTicks == DURATION / 2) {
					for (Player p : players) {
						p.sendMessage(Component.text("Your mind is starting to slip away...", SteelWingHawk.COLOR));
					}
				}
				if (mTicks >= DURATION) {
					for (Player p : players) {
						p.sendMessage(Component.text("The shrill noise fully works itself into your mind. Sweet slumber takes you...", SteelWingHawk.COLOR));
						p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1, 0.5f);
						mQuarry.banish(p);
					}
					this.cancel();
				}
				chargeUp.nextTick();
				mTicks++;
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
