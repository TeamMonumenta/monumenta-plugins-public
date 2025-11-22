package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class TerrestrialShield extends Spell {
	private static final int COOLDOWN = 5 * 20;
	private static final int SELF_COOLDOWN = 30 * 20;
	private static final int WINDUP = (int) (1.5 * 20);

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final Uamiel mUamiel;
	private final List<Player> mShieldedPlayers = new ArrayList<>();

	private boolean mOnCooldown = false;

	private double mAngle = 0;
	private int mGrowthTicks = 0;
	private final List<BlockDisplay> mDisplays = new ArrayList<>();
	private final List<BlockDisplay> mGrowingDisplays = new ArrayList<>();

	public TerrestrialShield(Plugin plugin, LivingEntity boss, Uamiel uamiel) {
		mBoss = boss;
		mPlugin = plugin;
		mUamiel = uamiel;

		for (int i = 0; i < 3; i++) {
			mDisplays.add(createDisplay());
			mGrowingDisplays.add(createDisplay());
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid()) {
					for (BlockDisplay display : mDisplays) {
						if (display.isValid()) {
							display.remove();
						}
					}
					this.cancel();
					return;
				}
				for (int i = 0; i < 3; i++) {
					double angle = mAngle + i * 120;
					moveDisplay(mDisplays.get(i), angle, 1);
					moveDisplay(mGrowingDisplays.get(i), angle, Math.max(((float) mGrowthTicks) / WINDUP, 0.1f));
				}
				mAngle += 3;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private BlockDisplay createDisplay() {
		return mBoss.getWorld().spawn(mBoss.getLocation(), BlockDisplay.class, display -> {
			display.setBlock(Material.CHISELED_TUFF.createBlockData());
			display.setVisibleByDefault(false);
			EntityUtils.setRemoveEntityOnUnload(display);
		});
	}

	private void moveDisplay(BlockDisplay display, double degrees, float scale) {
		display.teleport(mBoss.getLocation().setDirection(new Vector(1, 0, 0)));

		double radians = Math.toRadians(degrees);
		float cos = (float) FastUtils.cos(radians);
		float sin = (float) FastUtils.sin(radians);
		display.setTransformation(new Transformation(
			new Vector3f(1.8f * cos + 0.5f * sin * scale, 1 - 0.75f * scale, 1.8f * sin - 0.5f * cos * scale),
			new AxisAngle4f((float) -radians, 0, 1, 0),
			new Vector3f(0.3f * scale, 1.5f * scale, scale),
			new AxisAngle4f()
		));
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, SELF_COOLDOWN);

		List<Player> newPlayers = new ArrayList<>(mUamiel.getPlayers());
		newPlayers.removeAll(mShieldedPlayers);

		mGrowthTicks = 0;
		if (!newPlayers.isEmpty()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mGrowthTicks >= WINDUP || !mBoss.isValid()) {
						this.cancel();
					}
					// keep counting one more time
					mGrowthTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		for (Player player : newPlayers) {
			player.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.HOSTILE, 1.3f, 0.53f);
			player.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.0f, 0.7f);

			BossBar bar = BossBar.bossBar(Component.text("Terrestrial Shield", Uamiel.TEXT_COLOR), 0, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
			player.showBossBar(bar);

			for (BlockDisplay display : mGrowingDisplays) {
				player.showEntity(mPlugin, display);
			}

			new BukkitRunnable() {
				@Override
				public void run() {
					if (!mBoss.isValid() || !player.isValid() || !mUamiel.hasPlayer(player)) {
						this.cancel();
						return;
					}
					if (mGrowthTicks <= WINDUP) {
						if (mGrowthTicks == WINDUP) {
							mShieldedPlayers.add(player);
							for (BlockDisplay display : mDisplays) {
								player.showEntity(mPlugin, display);
							}
							for (BlockDisplay display : mGrowingDisplays) {
								player.hideEntity(mPlugin, display);
							}
							player.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.3f, 1.2f);
							player.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.5f, 0.8f);
						}

						bar.progress(((float) mGrowthTicks) / WINDUP);
					} else if (!mShieldedPlayers.contains(player)) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					player.hideBossBar(bar);
					mShieldedPlayers.remove(player);
					for (BlockDisplay display : mDisplays) {
						player.hideEntity(mPlugin, display);
					}
					for (BlockDisplay display : mGrowingDisplays) {
						player.hideEntity(mPlugin, display);
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (!(source instanceof Player player) || !mShieldedPlayers.contains(player)) {
			return;
		}
		if (ItemUtils.isPickaxe(player.getInventory().getItemInMainHand())) {
			if (event.getType() == DamageEvent.DamageType.MELEE) {
				mShieldedPlayers.remove(player);
				player.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 2f, 0.6f);
				new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 25, 1.5f, 0.75f).spawnForPlayer(ParticleCategory.BOSS, player);
			} else {
				// Catch weird cases where they deal non-melee damage with the pickaxe and don't punish or do damage
				event.setCancelled(true);
			}
		} else {
			if (!AbilityUtils.isIndirectDamage(event) && mUamiel.spoil(player)) {
				player.sendMessage(Component.text("Your attack damages the shield without destroying it, ruining the sinew and spoiling your loot.", Uamiel.TEXT_COLOR));
			}
			event.setCancelled(true);
		}
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mShieldedPlayers.containsAll(mUamiel.getPlayers());
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}
}
