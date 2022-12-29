package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

/*
 * Putrid Plague (Holds one of four colored wools reflecting a pillar):
 * Ground around the arena starts smoking, with the exception of the selected color area.
 * After a delay, all players in the arena not within the area are inflicted with poison,
 * wither and slowness for 2m.

 */
public class SpellPutridPlague extends Spell {
	private static final int DAMAGE = 30;
	private static final String PUTRID_PLAGUE_TAG_RED = "KaulPutridPlagueRed";
	private static final String PUTRID_PLAGUE_TAG_BLUE = "KaulPutridPlagueBlue";
	private static final String PUTRID_PLAGUE_TAG_YELLOW = "KaulPutridPlagueYellow";
	private static final String PUTRID_PLAGUE_TAG_GREEN = "KaulPutridPlagueGreen";

	private static boolean mPlagueActive;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRange;
	private final boolean mPhase3;
	private final int mTime;
	private final Location mCenter;
	private final ChargeUpManager mChargeUp;

	public static boolean getPlagueActive() {
		return mPlagueActive;
	}

	public SpellPutridPlague(Plugin plugin, LivingEntity boss, double range, boolean phase3, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mPhase3 = phase3;
		mCenter = center;
		mTime = (int) (mPhase3 ? 20 * 7.5 : 20 * 9);

		mChargeUp = new ChargeUpManager(mBoss, mTime, ChatColor.GREEN + "Charging " + ChatColor.DARK_GREEN + "Putrid Plague...",
			BarColor.GREEN, BarStyle.SEGMENTED_10, 50);
	}

	@Override
	public void run() {
		mPlagueActive = true;

		new BukkitRunnable() {
			@Override
			public void run() {
				mPlagueActive = false;
			}
		}.runTaskLater(mPlugin, mTime);

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 10, 0.8f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 10, 0f);
		List<ArmorStand> points = new ArrayList<ArmorStand>();
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if ((e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)
				     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)
				     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)
				     || e.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) && e instanceof ArmorStand) {
				points.add((ArmorStand) e);
			}
		}
		if (!points.isEmpty()) {
			Location loc = mCenter;
			ArmorStand point = points.get(FastUtils.RANDOM.nextInt(points.size()));
			if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_BLUE)) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color blue");
				mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.BLUE + "Putrid Plague...");
				mChargeUp.setColor(BarColor.BLUE);
				for (Player player : PlayerUtils.playersInRange(loc, mRange, true)) {
					if (!mPhase3) {
						player.sendMessage(ChatColor.BLUE + "The water begins to ripple...");
					} else {
						player.sendMessage(ChatColor.DARK_BLUE + "The water begins to ripple...");
					}
				}
			} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_RED)) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color red");
				mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.RED + "Putrid Plague...");
				mChargeUp.setColor(BarColor.RED);
				for (Player player : PlayerUtils.playersInRange(loc, mRange, true)) {
					if (!mPhase3) {
						player.sendMessage(ChatColor.RED + "Your blood begins to shiver slightly...");
					} else {
						player.sendMessage(ChatColor.DARK_RED + "Your blood begins to shiver slightly...");
					}
				}
			} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_YELLOW)) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color yellow");
				mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.YELLOW + "Putrid Plague...");
				mChargeUp.setColor(BarColor.YELLOW);
				for (Player player : PlayerUtils.playersInRange(loc, mRange, true)) {
					if (!mPhase3) {
						player.sendMessage(ChatColor.YELLOW + "You feel the temperature rise significantly...");
					} else {
						player.sendMessage(ChatColor.GOLD + "You feel the temperature rise significantly...");
					}
				}
			} else if (point.getScoreboardTags().contains(PUTRID_PLAGUE_TAG_GREEN)) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color green");
				mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.DARK_GREEN + "Putrid Plague...");
				mChargeUp.setColor(BarColor.GREEN);
				for (Player player : PlayerUtils.playersInRange(loc, mRange, true)) {
					if (!mPhase3) {
						player.sendMessage(ChatColor.GREEN + "The ground begins to vibrate...");
					} else {
						player.sendMessage(ChatColor.DARK_GREEN + "The ground begins to vibrate...");
					}
				}
			}
			List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, true);
			players.removeIf(p -> p.getLocation().getY() >= 61);
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
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "team modify kaul color white");
						List<Player> safe = PlayerUtils.playersInRange(point.getLocation(), 8, true);
						List<Player> ps = PlayerUtils.playersInRange(mCenter, mRange, true);
						ps.removeIf(p -> p.getLocation().getY() >= 61);
						for (Player player : ps) {
							if (!safe.contains(player)) {
								PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
								if ((resistance == null || resistance.getAmplifier() < 4)
									    && !StasisListener.isInStasis(player)) {
									player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1, 2);
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
								player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
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
