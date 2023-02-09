package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

/*
 * Putrid Plague (Holds one of four colored wools reflecting a pillar):
 * Ground around the arena starts smoking, except the selected color area.
 * After a delay, all players in the arena not within the area are inflicted with poison,
 * wither and slowness for 2m.

 */
public class SpellPutridPlague extends Spell {
	private enum Pillar {
		RED("KaulPutridPlagueRed", NamedTextColor.RED, NamedTextColor.DARK_RED, ChatColor.RED, BarColor.RED, "Your blood begins to shiver slightly..."),
		BLUE("KaulPutridPlagueBlue", NamedTextColor.BLUE, NamedTextColor.DARK_BLUE, ChatColor.BLUE, BarColor.BLUE, "The water begins to ripple..."),
		YELLOW("KaulPutridPlagueYellow", NamedTextColor.YELLOW, NamedTextColor.GOLD, ChatColor.YELLOW, BarColor.YELLOW, "You feel the temperature rise significantly..."),
		GREEN("KaulPutridPlagueGreen", NamedTextColor.GREEN, NamedTextColor.DARK_GREEN, ChatColor.DARK_GREEN, BarColor.GREEN, "The ground begins to vibrate...");

		final String mTag;
		final NamedTextColor mTextColor;
		final NamedTextColor mDarkTextColor;
		final ChatColor mChatColor;
		final BarColor mBarColor;
		final String mMessage;

		Pillar(String tag, NamedTextColor textColor, NamedTextColor darkTextColor, ChatColor chatColor, BarColor barColor, String message) {
			mTag = tag;
			mTextColor = textColor;
			mDarkTextColor = darkTextColor;
			mChatColor = chatColor;
			mBarColor = barColor;
			mMessage = message;
		}
	}


	private static final int DAMAGE = 30;

	private static boolean mPlagueActive;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Kaul mKaul;
	private final boolean mPhase3;
	private final int mTime;
	private final ChargeUpManager mChargeUp;

	public static boolean getPlagueActive() {
		return mPlagueActive;
	}

	public SpellPutridPlague(Plugin plugin, LivingEntity boss, Kaul kaul, boolean phase3) {
		mPlugin = plugin;
		mBoss = boss;
		mKaul = kaul;
		mPhase3 = phase3;
		mTime = (int) (mPhase3 ? 20 * 7.5 : 20 * 9);

		mChargeUp = new ChargeUpManager(mBoss, mTime, ChatColor.GREEN + "Charging " + ChatColor.DARK_GREEN + "Putrid Plague...",
			BarColor.GREEN, BarStyle.SEGMENTED_10, 50);
	}

	@Override
	public void run() {
		mPlagueActive = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlagueActive = false, mTime);

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 10, 0.8f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 10, 0f);
		List<ArmorStand> points = new ArrayList<>();
		for (Entity e : mBoss.getNearbyEntities(Kaul.detectionRange, Kaul.detectionRange, Kaul.detectionRange)) {
			if (e instanceof ArmorStand as && Stream.of(Pillar.values()).anyMatch(pillar -> ScoreboardUtils.checkTag(as, pillar.mTag))) {
				points.add(as);
			}
		}
		if (!points.isEmpty()) {
			ArmorStand point = points.get(FastUtils.RANDOM.nextInt(points.size()));
			Pillar pillar = Stream.of(Pillar.values()).filter(p -> ScoreboardUtils.checkTag(point, p.mTag)).findAny().orElse(null);
			if (pillar == null) {
				MMLog.warning("[Kaul] Could not find a pillar for Putrid Plague");
				return;
			}

			Team team = ScoreboardUtils.getEntityTeam(mBoss);
			if (team == null) {
				MMLog.warning("[Kaul] Could not find Kaul's team");
				return;
			}

			team.color(pillar.mTextColor);

			mChargeUp.setTitle(ChatColor.GREEN + "Charging " + pillar.mChatColor + "Putrid Plague...");
			mChargeUp.setColor(pillar.mBarColor);

			Collection<Player> players = mKaul.getArenaParticipants();
			Component message = Component.text(pillar.mMessage, mPhase3 ? pillar.mDarkTextColor : pillar.mTextColor);
			players.forEach(p -> p.sendMessage(message));

			new BukkitRunnable() {
				final Location mPoint1 = point.getLocation().add(4, 6, 4);
				final Location mPoint2 = point.getLocation().add(-4, 6, -4);
				final Location mPoint3 = point.getLocation().add(4, 6, -4);
				final Location mPoint4 = point.getLocation().add(-4, 6, 4);

				@Override
				public void run() {

					for (Player player : players) {
						// Spawn the particles for players so that way there
						// isn't as much particle lag
						new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation(), 60, 15, 0, 15, 0)
							.spawnForPlayer(ParticleCategory.BOSS, player);
					}

					new PartialParticle(Particle.SPELL_INSTANT, mPoint1, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_INSTANT, mPoint2, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_INSTANT, mPoint3, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_INSTANT, mPoint4, 30, 0.45, 6, 0.45).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_INSTANT, point.getLocation(), 65, 7, 3, 7).spawnAsEntityActive(mBoss);

					new PPCircle(Particle.SPELL_INSTANT, point.getLocation(), 7).count(60).ringMode(true).spawnAsEntityActive(mBoss);

					if (mChargeUp.nextTick(2)) {
						this.cancel();
						mChargeUp.reset();
						team.color(NamedTextColor.WHITE);
						Location base = point.getLocation();
						base.setY(0);
						List<Player> safe = new Hitbox.UprightCylinderHitbox(base, Kaul.ARENA_MAX_Y, 4).getHitPlayers(true);
						Collection<Player> ps = mKaul.getArenaParticipants();
						for (Player player : ps) {
							if (!safe.contains(player)) {
								PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
								if ((resistance == null || resistance.getAmplifier() < 4)
									    && !StasisListener.isInStasis(player)) {
									player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1, 2);
									new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1, 0), 50, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 30, 0.3, 0.45, 0.3, 0,
										Material.LIME_CONCRETE.createBlockData()).spawnAsEntityActive(mBoss);
									player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 30, 1));
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 1));
									player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 1));
									DamageUtils.damage(mBoss, player, DamageType.MAGIC, DAMAGE, null, false, true, "Putrid Plague");
								}
							} else {
								new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 25, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 35, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
								player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1);
								if (!mPhase3) {
									player.removePotionEffect(PotionEffectType.WITHER);
									player.removePotionEffect(PotionEffectType.SLOW);
									player.removePotionEffect(PotionEffectType.POISON);
									player.removePotionEffect(PotionEffectType.WEAKNESS);
								} else {
									for (PotionEffect effect : player.getActivePotionEffects()) {
										if (effect.getType().equals(PotionEffectType.WITHER)
											    || effect.getType().equals(PotionEffectType.SLOW)
											    || effect.getType().equals(PotionEffectType.POISON)
											    || effect.getType().equals(PotionEffectType.WEAKNESS)) {
											int duration = effect.getDuration() - (20 * 80);
											if (duration <= 0) {
												continue;
											}
											int amp = effect.getAmplifier() - 1;
											if (amp <= 0) {
												continue;
											}
											player.removePotionEffect(effect.getType());
											player.addPotionEffect(new PotionEffect(effect.getType(), duration, amp));
										}
									}
								}
							}
						}
					}
				}

			}.runTaskTimer(mPlugin, 0, 2);
		}
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
