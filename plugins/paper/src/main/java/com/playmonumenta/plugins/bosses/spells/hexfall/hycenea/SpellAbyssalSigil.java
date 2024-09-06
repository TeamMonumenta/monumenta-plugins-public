package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellAbyssalSigil extends Spell {

	private static final String ABILITY_NAME = "Abyssal Sigil";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mDamage;
	private final double mRadius;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellAbyssalSigil(Plugin plugin, LivingEntity boss, int range, int damage, double radius, int castTime, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDamage = damage;
		mRadius = radius;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		World world = mBoss.getWorld();
		List<Location> locs = new ArrayList<>();
		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			locs.add(LocationUtils.fallToGround(player.getLocation().add(0, 0.25, 0), mSpawnLoc.getY()));
			for (Location loc : locs) {
				player.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.5f);
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 5 == 0) {
					for (Location loc : locs) {
						new PPCircle(Particle.REDSTONE, loc, mRadius).ringMode(true)
							.count(40)
							.data(new Particle.DustOptions(Color.fromRGB(127, 0, 255), 1.65f))
							.spawnAsBoss();
						for (int i = 0; i < mRadius; i += 1) {
							if (mRadius < 1) {
								break;
							}
							new PPCircle(Particle.REDSTONE, loc, i - 1)
								.count(40)
								.data(new Particle.DustOptions(Color.fromRGB(204, 153, 255), 1.65f))
								.spawnAsBoss();
						}
					}
				}
				if (mChargeUp.getTime() % 10 == 0) {
					for (Location loc : locs) {
						world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.5f);
					}
				}

				if (mChargeUp.nextTick()) {
					for (Location loc : locs) {
						new PPSpiral(Particle.SOUL_FIRE_FLAME, loc, mRadius)
							.distancePerParticle(0.1)
							.spawnAsBoss();

						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 2);
						world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 1.5f, 0);

						for (Player p : HexfallUtils.playersInBossInXZRange(loc, mRadius, true)) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
							MovementUtils.knockAway(loc, p, 0f, 0.25f, false);
							world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1f, 1f);
						}
					}

					mChargeUp.remove();
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
