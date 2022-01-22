package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

public class SpellEndlessAgony extends Spell {
	public static final Particle.DustOptions ENDLESS_AGONY_COLOR = new Particle.DustOptions(Color.fromRGB(214, 58, 166), 1.65f);
	public static final double RADIUS = 3;
	private static final int MOVEMENT_TIME = 4 * 20;
	private static final int WAIT_UNTIL_DAMAGE_TIME = (int) (1.5 * 20);
	private static final int MAX_COUNT = 25;

	private Plugin mPlugin;
	private RKitxet mRKitxet;
	private double mRange;
	private Location mCenter;
	private int mCount;
	private ChargeUpManager mChargeUp;
	private int mCooldown;

	public SpellEndlessAgony(Plugin plugin, RKitxet rKitxet, Location center, double range, int cooldown) {
		mPlugin = plugin;
		mRKitxet = rKitxet;
		mRange = range;
		mCenter = center;
		mCount = 0;
		mCooldown = cooldown;

		mChargeUp = new ChargeUpManager(mRKitxet.getEntity(), MOVEMENT_TIME + WAIT_UNTIL_DAMAGE_TIME, ChatColor.DARK_PURPLE + "Forming Endless Agony...",
				BarColor.PURPLE, BarStyle.SEGMENTED_10, RKitxet.detectionRange);
	}

	@Override
	public void run() {
		mRKitxet.useSpell("Endless Agony");

		World world = mCenter.getWorld();

		List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, false);
		if (players.size() == 0) {
			return;
		}
		if (players.size() > 1 && mRKitxet.getFuryTarget() != null) {
			players.remove(mRKitxet.getFuryTarget());
		}
		Collections.shuffle(players);
		Player target = players.get(0);
		mRKitxet.setAgonyTarget(target);

		mCount++;

		world.playSound(target.getLocation(), Sound.BLOCK_BASALT_STEP, SoundCategory.HOSTILE, 3, 0.8f);
		world.playSound(target.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2, 1);
		target.playSound(target.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 3, 1);
		target.sendMessage(ChatColor.LIGHT_PURPLE + "Pain and suffering haunt you with every step you take.");

		PPGroundCircle indicator = new PPGroundCircle(Particle.REDSTONE, target.getLocation(), 30, 0.1, 0.05, 0.1, 0, ENDLESS_AGONY_COLOR).init(0, true);

		mChargeUp.setTitle(ChatColor.DARK_PURPLE + "Forming Endless Agony...");
		mChargeUp.setColor(BarColor.PURPLE);

		BukkitRunnable movementRunnable = new BukkitRunnable() {
			Location mLoc = target.getLocation();
			@Override
			public void run() {
				mChargeUp.nextTick(5);

				Location targetLoc = target.getLocation();
				//If they die, stop
				if (targetLoc.distance(mRKitxet.getBossLocation()) > RKitxet.detectionRange) {
					mChargeUp.reset();
					this.cancel();
					return;
				}

				if (mChargeUp.getTime() <= MOVEMENT_TIME) {
					mLoc = targetLoc;
					mLoc.setY((int) mLoc.getY());
				}

				indicator.radius(RADIUS).location(mLoc).spawnAsBoss();

				if (mChargeUp.getTime() == MOVEMENT_TIME) {
					world.playSound(targetLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2f, 0.3f);
					mChargeUp.setTitle(ChatColor.RED + "Channeling Endless Agony...");
					mChargeUp.setColor(BarColor.RED);
				}

				if (mChargeUp.getTime() >= MOVEMENT_TIME + WAIT_UNTIL_DAMAGE_TIME) {
					world.playSound(targetLoc, Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 4, 0.8f);
					mRKitxet.mAgonyLocations.add(mLoc);
					mRKitxet.setAgonyTarget(null);
					mChargeUp.reset();
					this.cancel();
					return;
				}
			}

		};
		movementRunnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(movementRunnable);
	}

	@Override
	public boolean canRun() {
		return mCount < MAX_COUNT && PlayerUtils.playersInRange(mCenter, mRange, false).size() > 0 && mRKitxet.canUseSpell("Endless Agony");
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
