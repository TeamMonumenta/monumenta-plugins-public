package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 * Putrid Plague (Holds one of four colored wools reflecting a pillar):
 * Ground around the arena starts smoking, except the selected color area.
 * After a delay, all players in the arena not within the area are inflicted with poison,
 * wither and slowness for 2m.

 */
public class SpellPutridPlague extends Spell {
	public enum Pillar {
		RED(Kaul.RED_OFFSET, NamedTextColor.RED, NamedTextColor.DARK_RED, BossBar.Color.RED, "Your blood begins to shiver slightly...", "FireVassal"),
		BLUE(Kaul.BLUE_OFFSET, NamedTextColor.BLUE, NamedTextColor.DARK_BLUE, BossBar.Color.BLUE, "The water begins to ripple...", "WaterVassal"),
		YELLOW(Kaul.YELLOW_OFFSET, NamedTextColor.YELLOW, NamedTextColor.GOLD, BossBar.Color.YELLOW, "You feel the temperature rise significantly...", "AirVassal"),
		GREEN(Kaul.GREEN_OFFSET, NamedTextColor.GREEN, NamedTextColor.DARK_GREEN, BossBar.Color.GREEN, "The ground begins to vibrate...", "EarthVassal");

		public final Vector mOffset;
		final NamedTextColor mTextColor;
		final TextColor mDarkTextColor;
		final BossBar.Color mBarColor;
		final String mMessage;
		public final String mSpiderLos;

		Pillar(Vector offset, NamedTextColor textColor, TextColor darkTextColor, BossBar.Color barColor, String message, String spiderLos) {
			mOffset = offset;
			mTextColor = textColor;
			mDarkTextColor = darkTextColor;
			mBarColor = barColor;
			mMessage = message;
			mSpiderLos = spiderLos;
		}

		public Vector getSpiderOffset() {
			return mOffset.clone().multiply(15.0 / Kaul.PILLAR_OFFSET);
		}

		public static Pillar getRandom() {
			Pillar[] values = values();
			return values[FastUtils.randomIntInRange(0, values.length)];
		}
	}

	private static final String SPELL_NAME = "Putrid Plague";
	private static final int DAMAGE = 30;
	private static final String SLOWNESS_SRC = "PutridPlagueSlowness";
	private static final int DEBUFF_DURATION = 20 * 30;

	private static boolean mPlagueActive;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final boolean mPhase3;
	private final Location mCenter;
	private final int mTime;
	private final ChargeUpManager mChargeUp;

	public static boolean getPlagueActive() {
		return mPlagueActive;
	}

	public SpellPutridPlague(Plugin plugin, LivingEntity boss, boolean phase3, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mPhase3 = phase3;
		mCenter = center;
		mTime = (int) (mPhase3 ? 20 * 7.5 : 20 * 9);

		mChargeUp = Kaul.defaultChargeUp(mBoss, mTime, SPELL_NAME);
	}

	@Override
	public void run() {
		mPlagueActive = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlagueActive = false, mTime);

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 10, 0.8f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 10, 0f);

		Pillar pillar = Pillar.getRandom();
		Location pillarLoc = mCenter.clone().add(pillar.mOffset);

		GlowingManager.startGlowing(mBoss, pillar.mTextColor, mTime, GlowingManager.BOSS_SPELL_PRIORITY);

		mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", pillar.mDarkTextColor)));
		mChargeUp.setColor(pillar.mBarColor);

		Collection<Player> players = Kaul.getArenaParticipants(mCenter);
		Component message = Component.text(pillar.mMessage, mPhase3 ? pillar.mDarkTextColor : pillar.mTextColor);
		players.forEach(p -> p.sendMessage(message));

		BukkitRunnable runnable = new BukkitRunnable() {
			final Location mPoint1 = pillarLoc.clone().add(4, 6, 4);
			final Location mPoint2 = pillarLoc.clone().add(-4, 6, -4);
			final Location mPoint3 = pillarLoc.clone().add(4, 6, -4);
			final Location mPoint4 = pillarLoc.clone().add(-4, 6, 4);

			@Override
			public void run() {
				for (Player player : players) {
					// Spawn the particles for players so that way there isn't as much particle lag
					new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation(), 60, 15, 0, 15, 0)
						.spawnForPlayer(ParticleCategory.BOSS, player);
				}

				new PartialParticle(Particle.SPELL_INSTANT, mPoint1, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPELL_INSTANT, mPoint2, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPELL_INSTANT, mPoint3, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPELL_INSTANT, mPoint4, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPELL_INSTANT, pillarLoc, 65, 7, 3, 7).spawnAsEntityActive(mBoss);
				new PPCircle(Particle.SPELL_INSTANT, pillarLoc, 7).count(60).spawnAsEntityActive(mBoss);

				if (mChargeUp.nextTick(2)) {
					this.cancel();
					mChargeUp.reset();
					Location base = pillarLoc.clone();
					base.setY(0);
					List<Player> safe = new Hitbox.UprightCylinderHitbox(base, Kaul.ARENA_MAX_Y, 8).getHitPlayers(true);
					Collection<Player> ps = Kaul.getArenaParticipants(mCenter);
					for (Player player : ps) {
						if (!safe.contains(player)) {
							if (!EffectManager.getInstance().hasEffect(player, SpellKaulsJudgement.IMMUNITY_SOURCE) && !StasisListener.isInStasis(player)) {
								player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1, 2);

								new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 50, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0,
									Material.LIME_CONCRETE.createBlockData()).spawnAsEntityActive(mBoss);

								player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, DEBUFF_DURATION, 1));
								player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, DEBUFF_DURATION, 1));
								com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player,
									SLOWNESS_SRC, new PercentSpeed(DEBUFF_DURATION, -0.3, SLOWNESS_SRC));

								DamageUtils.damage(mBoss, player, DamageType.MAGIC, DAMAGE, null, false, true, SPELL_NAME);
							}
							return;
						}

						player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1);

						new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 25, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 35, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);

						if (!mPhase3) {
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(player, SLOWNESS_SRC);
							player.removePotionEffect(PotionEffectType.WITHER);
							player.removePotionEffect(PotionEffectType.POISON);
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mTime + (20 * 12);
	}

	@Override
	public int castTicks() {
		return mTime;
	}
}
