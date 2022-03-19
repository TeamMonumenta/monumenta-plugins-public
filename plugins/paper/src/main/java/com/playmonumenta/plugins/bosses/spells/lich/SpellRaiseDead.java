package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellRaiseDead extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private int mCeiling;
	private boolean mCanRun = true;
	private int mDelay = 20 * 40;
	private ChargeUpManager mChargeUp;

	public SpellRaiseDead(Plugin plugin, LivingEntity boss, Location loc, double detectRange, int ceil) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = detectRange;
		mCeiling = ceil;
		mChargeUp = new ChargeUpManager(mBoss, 100, ChatColor.YELLOW + "Channeling Raise Dead...", BarColor.RED, BarStyle.SOLID, 200);
	}

	@Override
	public boolean canRun() {
		return mCanRun;
	}

	@Override
	public void run() {
		//prevents recasts within 40 seconds
		mCanRun = false;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCanRun = true;
			}

		}.runTaskLater(mPlugin, mDelay);

		World world = mBoss.getWorld();
		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				List<Player> players = Lich.playersInRange(mCenter, mRange, true);
				players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p) || p.getLocation().getY() >= mCenter.getY() + mCeiling);
				players.removeIf(p -> p.getLocation().getY() >= mCenter.clone().getY() + 33);

				double amount = 10;
				if (players.size() <= 10) {
					amount = Math.log10(players.size()) * 15 + 5;
				} else {
					amount = 10 + players.size() * 1;
				}
				double a = amount;
				world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 10, 0.75f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 10, 1f);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 50, 0.5, 0.25, 0.5, 0).spawnAsBoss();
				BukkitRunnable runB = new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						mT++;
						double progress = Math.min(1.0, mT / a);
						mChargeUp.setProgress(progress);
						riseUndead(getRandomCenteration(mCenter, 39), mPlugin);
						if (mT >= a || Lich.phase3over()) {
							this.cancel();
							mChargeUp.reset();
						}
					}

				};
				runB.runTaskTimer(mPlugin, 0, 2);
				mActiveRunnables.add(runB);
			}

		};
		runA.runTaskLater(mPlugin, 30);
		mActiveRunnables.add(runA);
	}

	public static void riseUndead(Location loc, Plugin plugin) {
		// mob stats are rebalanced versions of gray/fred mobs. With NEW NAMES!
		String summon = null;
		int x = FastUtils.RANDOM.nextInt(8);
		if (x == 0) {
			summon = "FireImpTroop";
		} else if (x == 1) {
			summon = "InfestedUndead";
		} else if (x == 2) {
			summon = "SoppingUndead";
		} else if (x == 3) {
			summon = "DesiccatedHorror";
		} else if (x == 4) {
			summon = "DesiccatedArcher";
		} else if (x == 5) {
			summon = "DesiccatedSavage";
		} else if (x == 6) {
			summon = "StifleLancer";
		} else {
			summon = "DrownedDraugr";
		}

		String s = summon;
		PartialParticle dust1 = new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.4, 0.1, 0.4, 0.25, Material.DIRT.createBlockData());
		PartialParticle dust2 = new PartialParticle(Particle.BLOCK_DUST, loc, 16, 0.25, 0.1, 0.25, 0.25, Material.DIRT.createBlockData());
		new BukkitRunnable() {
			int mINC = 0;
			@Override
			public void run() {
				mINC += 2;
				dust1.spawnAsBoss();
				if (mINC >= 20) {
					loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1f);
					dust2.spawnAsBoss();
					LibraryOfSoulsIntegration.summon(loc, s);
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 2);
	}

	public static Location getRandomCenteration(Location origin, double range) {
		Location loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
		//reroll condition
		int mINC = 0;
		while ((loc.getBlock().getType().isSolid() || loc.distance(origin) > range) && mINC < 5) {
			loc = origin.clone().add(FastUtils.randomDoubleInRange(-range, range), 0, FastUtils.randomDoubleInRange(-range, range));
			mINC++;
		}
		return loc;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 24;
	}
}
