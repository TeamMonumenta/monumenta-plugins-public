package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import java.util.NavigableSet;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Void Corruption: Effect given in Vesperidys DD2 Boss 3.
 * <p>
 * While in the Boss Fight, receives less healing dependent on the amount of Corruption,
 * taking damage results in more corruption.
 * <p>
 * Reaching 100% Corruption causes an explosion.
 */
public class VoidCorruption extends Effect {
	public static final double RANGE = 8;
	public static final int EXPLODE_TICK = 20;

	public static final String GENERIC_NAME = "Corruption";
	public static final String effectID = "VoidCorruption";

	public int mCorruption;
	private final Vesperidys mVesperidys;
	private final Plugin mPlugin;

	private final LivingEntity mBoss;
	private final BossBar mBossBar;
	private int mT;
	private int mHitTick = -1;
	private int mExplosionTick = -1;

	private final PartialParticle mPCursed;
	private final PartialParticle mPSmoke;
	private final PartialParticle mPExplode;

	public VoidCorruption(int duration, Plugin plugin, Vesperidys vesperidys, LivingEntity boss, int corruption) {
		super(duration, effectID);
		mPlugin = plugin;
		mCorruption = corruption;
		mVesperidys = vesperidys;
		mBoss = boss;
		Component title = Component.text("Corruption Meter", NamedTextColor.LIGHT_PURPLE)
			.append(Component.text(": ", NamedTextColor.YELLOW))
			.append(Component.text((int) Math.floor(mCorruption) + "%", NamedTextColor.LIGHT_PURPLE))
			.append(Component.text(" - ", NamedTextColor.YELLOW))
			.append(Component.text(getHealingReduction() + "% Heal Reduc. ", NamedTextColor.DARK_RED));
		mBossBar = BossBar.bossBar(title, (float) (corruption/100.0), BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);

		mPCursed = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 2, 0.2, 0.3, 0.2, 0.1);
		mPSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 2, 0.25, 0.25, 0.25, 0.1);
		mPExplode = new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (mBoss.isDead() || !mBoss.isValid() || mVesperidys.mDefeated) {
			mDuration = 0;
			return;
		}

		if (entity instanceof Player player) {
			DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
			if (dp != null && dp.mGraveRunnable != null) {
				mDuration = 0;
				return;
			}

			player.showBossBar(mBossBar);

			if (oneHertz) {
				mT++;
			}

			if (mT >= 3
				&& (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
				&& mExplosionTick < 0
				&& mCorruption < 100) {
				mT = 0;

				double equilibrium = mVesperidys.corruptionEqulibrium();

				if (equilibrium > mCorruption + 1) {
					addCorruption(1);
				} else if (equilibrium <= mCorruption - 1) {
					int decAmount = Math.max(1, Math.min(5, (int) Math.floor((mCorruption - 1 - equilibrium) / 5)));
					addCorruption(-decAmount);
				}

				List<Player> players = PlayerUtils.playersInRange(entity.getLocation(), RANGE, true);
				for (Player p2 : players) {
					NavigableSet<VoidCorruption> corruptionSet = mPlugin.mEffectManager.getEffects(p2, VoidCorruption.class);
					if (corruptionSet.size() > 0) {
						double pCorruption = corruptionSet.last().getCorruption();
						if (pCorruption > mCorruption + 5) {
							addCorruption(1);
						} else if (pCorruption <= mCorruption - 5) {
							addCorruption(-1);
						}
					}
				}
			}

			if (fourHertz) {
				updateBossBar();
				if (mHitTick >= 0) {
					mHitTick += 1;
					if (mHitTick > 1) {
						mHitTick = -1;
					}
				}

				if (mCorruption >= 100 && mExplosionTick < 0) {
					mExplosionTick = 0;
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 0);
				} else if (mExplosionTick >= 0) {
					mExplosionTick += 1;

					mPCursed.location(player.getLocation().add(0, 0.3, 0)).spawnAsBoss();
					new PPCircle(Particle.SPELL_WITCH, player.getLocation().add(0, 0.25, 0), RANGE).ringMode(true).count(30).delta(0.1, 0.05, 0.1).spawnAsBoss();
					new PPCircle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.25, 0), RANGE).ringMode(true).count(30).delta(0.1, 0.05, 0.1).spawnAsBoss();

					mPSmoke.count(5).delta(RANGE, RANGE, RANGE).extra(0.5f).spawnAsBoss();

					if (mExplosionTick >= EXPLODE_TICK) {
						player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 1);
						mPExplode.count(10).delta(0.25, 0.25, 0.25).location(player.getLocation().add(0, 0.3, 0)).spawnAsBoss();

						List<Player> playersInRange = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
						for (Player p : playersInRange) {
							mVesperidys.dealPercentageAndCorruptionDamage(p, 0.5, "Corruption Explosion");

							mPlugin.mEffectManager.addEffect(player, "Vesperidys Antiheal", new PercentHeal(10 * 20, -1.00));
							mPlugin.mEffectManager.addEffect(player, "Vesperidys Antiabsroption", new PercentAbsorption(10 * 20, -1.00));
							player.sendActionBar(Component.text("You cannot heal for 10s", NamedTextColor.RED));
							PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.BAD_OMEN, 10 * 20, 1));
						}

						mExplosionTick = -1;
						mCorruption = 50;
					}
				}
			}
		}
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity damager) {
		if (entity instanceof Player player && mExplosionTick < 0) {
			double damage = event.getFinalDamage(false);
			double maxHealth = EntityUtils.getMaxHealth(player);

			double percentageHealth = (damage / maxHealth) * 100;
			addCorruption((int) Math.round(percentageHealth * 0.5));

			updateBossBar();
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (mBoss.isDead() || !mBoss.isValid() || mVesperidys.mDefeated) {
			MMLog.severe("Vesperidys attempted entityGainEvent while dead/dying. Method & Line number: " + new Throwable().getStackTrace()[1]);
			return;
		}
		if (entity instanceof Player player) {
			player.playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 20, 1);
			player.sendMessage(
				Component.text("The void's corruption seeps into your body. The more", NamedTextColor.LIGHT_PURPLE)
					.append(Component.text(" corrupted ", NamedTextColor.DARK_PURPLE))
					.append(Component.text("you become, the longer your ", NamedTextColor.LIGHT_PURPLE))
					.append(Component.text("wounds", NamedTextColor.RED))
					.append(Component.text(" linger.", NamedTextColor.LIGHT_PURPLE)));
			player.showBossBar(mBossBar);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player player) {
			player.hideBossBar(mBossBar);
		}
		mDuration = 0;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.hideBossBar(mBossBar);
		}
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * (1 - getHealingReduction() * 0.01));
		return -getHealingReduction() * 0.01 > -1;
	}

	@Override
	public void entityGainAbsorptionEvent(EntityGainAbsorptionEvent event) {
		event.setAmount(event.getAmount() * (1 - getHealingReduction() * 0.01));
		event.setMaxAmount(event.getMaxAmount() * (1 - getHealingReduction() * 0.01));
	}

	public void updateBossBar() {
		if (mCorruption < 100 && mExplosionTick < 0) {
			mBossBar.name(Component.text("Corruption Meter", NamedTextColor.LIGHT_PURPLE)
				.append(Component.text(": ", NamedTextColor.YELLOW))
				.append(Component.text((int) Math.floor(mCorruption) + "%", NamedTextColor.LIGHT_PURPLE))
				.append(Component.text(" - ", NamedTextColor.YELLOW))
				.append(Component.text(getHealingReduction() + "% Heal Reduc. ", NamedTextColor.DARK_RED)));
			mBossBar.progress((float) (mCorruption / 100.0));

			if (mHitTick >= 0) {
				mBossBar.color(BossBar.Color.YELLOW);
			} else {
				mBossBar.color(BossBar.Color.PURPLE);
			}
		} else if (mExplosionTick >= 0 && mExplosionTick <= EXPLODE_TICK) {
			mBossBar.name(Component.text("Corruption Meter", NamedTextColor.LIGHT_PURPLE)
				.append(Component.text(": ", NamedTextColor.YELLOW))
				.append(Component.text((int) Math.floor(mCorruption) + "%", NamedTextColor.LIGHT_PURPLE))
				.append(Component.text(" - ", NamedTextColor.YELLOW))
				.append(Component.text("EXPLOSION IMMINENT!", NamedTextColor.DARK_RED, TextDecoration.BOLD)));
			mBossBar.progress(((float) (EXPLODE_TICK - mExplosionTick) / EXPLODE_TICK));
			if (mBossBar.color() == BossBar.Color.RED) {
				mBossBar.color(BossBar.Color.WHITE);
			} else {
				mBossBar.color(BossBar.Color.RED);
			}
		}
	}

	public int getCorruption() {
		return mCorruption;
	}

	public void addCorruption(int corruption) {
		mCorruption += corruption;
		if (mCorruption > 100) {
			mCorruption = 100;
		} else if (mCorruption < 0) {
			mCorruption = 0;
		}

		mHitTick = 0;
	}

	public int getHealingReduction() {
		return (int) (100* (1 - 100.0 / (100.0 + mCorruption)));
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d, Corruption: %d", this.getDuration(), this.mCorruption);
	}
}
