package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellAnimaExpulsion extends Spell {

	private static final String ABILITY_NAME = "Anima Expulsion";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final float mVelocity;
	private final int mCooldown;
	private final int mCastTime;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellAnimaExpulsion(Plugin plugin, LivingEntity boss, int range, float velocity, int castTime, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mVelocity = velocity;
		mCooldown = cooldown;
		mCastTime = castTime;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.setTime(0);
		int mTempRadius = 7;


		BukkitRunnable runnable = new BukkitRunnable() {
			List<Player> mPlayers = HexfallUtils.getPlayersInRuten(mSpawnLoc);

			@Override
			public void run() {
				mPlayers = mPlayers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());
				float progress = (float) mChargeUp.getTime() / mCastTime;

				if (mChargeUp.getTime() % 2 == 0) {
					mPlayers.forEach(p -> {
						p.playSound(p.getLocation(), Sound.BLOCK_VINE_HIT, SoundCategory.HOSTILE, 0.25f, 0);
						p.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_STEP, SoundCategory.HOSTILE, 0.25f, 1);
						p.playSound(p.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 0.25f, 1);
					});

					new PPCircle(Particle.BLOCK_DUST, mBoss.getLocation(), (mTempRadius * (1d - ((double) mChargeUp.getTime() / (double) mCastTime))))
						.ringMode(true)
						.count(20)
						.data(Material.JUNGLE_LEAVES.createBlockData())
						.spawnAsBoss();
					new PPCircle(Particle.SLIME, mBoss.getLocation(), (mTempRadius * (1d - ((double) mChargeUp.getTime() / (double) mCastTime))))
						.ringMode(true)
						.count(5)
						.spawnAsBoss();
				}

				if (mChargeUp.getTime() % 5 == 0) {
					mPlayers.forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 0.8f + progress));
				}

				if (mChargeUp.nextTick()) {

					mPlayers.forEach(p -> {
						p.playSound(mBoss.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1f, 2f);
						p.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 2f);
						p.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 1.5f, 1f);
					});

					for (Player p : mPlayers) {
						MovementUtils.knockAway(mBoss.getLocation().add(0, 0, 0), p, mVelocity, false);
						p.setVelocity(p.getVelocity().add(new Vector(0, 0.1, 0)));
					}

					new PPSpiral(Particle.SLIME, mBoss.getLocation(), 7)
						.distanceFalloff(50)
						.count(120)
						.spawnAsBoss();

					mChargeUp.reset();
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
