package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.shura.SpellBlightCheese;
import com.playmonumenta.plugins.bosses.spells.shura.SpellShuraAS;
import com.playmonumenta.plugins.bosses.spells.shura.SpellShuraDagger;
import com.playmonumenta.plugins.bosses.spells.shura.SpellShuraJump;
import com.playmonumenta.plugins.bosses.spells.shura.SpellShuraPassiveSummon;
import com.playmonumenta.plugins.bosses.spells.shura.SpellShuraSmoke;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CShura extends BossAbilityGroup {
	public static final String identityTag = "boss_cshura";
	public static final int detectionRange = 50;
	private static final String START_TAG = "shuraCenter";
	private static final int DODGE_CD = 5;

	private LivingEntity mStart;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private boolean mDodge = false;
	private boolean mCutscene = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CShura(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public CShura(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		for (Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags().contains(START_TAG) && e instanceof LivingEntity) {
				mStart = (LivingEntity) e;
				break;
			}
		}
		if (mStart == null) {
			return;
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellShuraDagger(mBoss, mPlugin),
			new SpellShuraJump(mPlugin, mBoss, detectionRange),
			new SpellShuraAS(mPlugin, mBoss, detectionRange, mStart.getLocation()),
			new SpellShuraSmoke(mPlugin, mBoss, mStart.getLocation(), detectionRange)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> mStart.getLocation().distance(b.getLocation()) > 35),
			new SpellShuraPassiveSummon(mPlugin, mStart.getLocation()),
			new SpellShieldStun(6 * 20),
			new SpellBlightCheese(mBoss, detectionRange, mStart.getLocation())
		);
		String suffix = ChatColor.GOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "C'Shura" + ChatColor.GOLD + "] ";

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<Integer, BossBarManager.BossHealthAction>();
		events.put(50, mBoss -> {
			String[] dio1 = new String[] {
				ChatColor.GREEN + "Enough! My " + ChatColor.RED + "purpose " + ChatColor.GREEN + "here is too great for you to interrupt!",
				ChatColor.GREEN + "You will die now, like your worthless king did!"
			};
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT < dio1.length) {
						for (Player p : PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)) {
							p.sendMessage(suffix + dio1[mT]);
						}
						mT++;
					} else {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 20 * 4);

		});

		events.put(25, mBoss -> {
			String dio2 = ChatColor.RED + "CUN! DIE ALREADY!";
			for (Player p : PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)) {
				p.sendMessage(suffix + dio2);
			}
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 12000, 0));

		});

		String[] dio = new String[] {
			ChatColor.RED + "Kaul!",
			ChatColor.GREEN + "Please...",
			ChatColor.GREEN + "It was the " + ChatColor.RED + "Soulspeaker! He wanted to save us!",
			ChatColor.GREEN + "You there. How did you get in here? " + ChatColor.RED + "I'm glad you have... your sacrifice will draw Kaul back to me. He will speak to me.",
			ChatColor.GREEN + "I'm giving you a chance to run " + ChatColor.RED + "before I cut you down.",
			};
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mCutscene = true;
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT < dio.length) {
					for (Player p : PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendMessage(suffix + dio[mT]);
					}
					mT++;
				} else {
					this.cancel();
					for (Player p : PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)) {
						p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "C'Shura",
							ChatColor.DARK_GREEN + "The Soulbinder", 10, 70, 20);
						p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 0.75f);
						p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 2));
					}
					mBoss.setAI(true);
					mBoss.setInvulnerable(false);
					mCutscene = false;
					BossBarManager bossBar = new BossBarManager(plugin, mBoss, detectionRange, BarColor.RED, BarStyle.SOLID, events);
					constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 4);

	}

	@Override
	public void init() {
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		double hpDelta = mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		double hp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.5, 0.35);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
		mBoss.setHealth(hp);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (!mDodge && !mCutscene) {
			mDodge = true;
			dodge(event);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mDodge = false;
			}, DODGE_CD * 20);
		}
	}

	private void dodge(DamageEvent event) {
		event.setCancelled(true);
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation().add(0, 1, 0);
		Entity damager = event.getDamager();
		if (damager != null) {
			Vector direction = damager.getLocation().subtract(loc).toVector().setY(0).normalize();
			Vector sideways = new Vector(direction.getZ(), 0, -direction.getX());
			sideways.subtract(direction.multiply(0.25));
			if (FastUtils.RANDOM.nextBoolean()) {
				sideways.multiply(-1);
			}

			loc.add(sideways.multiply(3));
			for (int i = 0; i < 3; i++) {
				if (loc.getBlock().isPassable()) {
					new PartialParticle(Particle.SMOKE_LARGE, loc, 10, 0, 0, 0, 0.5).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);

					mBoss.teleport(loc);
					break;
				} else {
					loc.add(0, 1, 0);
				}
			}
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		// win resistance
		for (Player p : PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)) {
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.REGENERATION, 12 * 20, 2));
			PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 12 * 20, 10));
		}
		// win mob kill
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mStart.getLocation(), detectionRange)) {
			mob.setHealth(0);
		}

		mBoss.teleport(mBoss.getLocation().add(0, -300, 0));
		String suffix = ChatColor.GOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "C'Shura" + ChatColor.GOLD + "] ";
		String[] ded = new String[] {
			ChatColor.GREEN + "I feel... different. This place is a blight. it nearly took me. It could take you too.",
			ChatColor.GREEN + "This place has magic even I could not handle. I must find my people. They need to know I am still here."
		};
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true);
				if (mT < ded.length) {
					for (Player p : players) {
						p.sendMessage(suffix + ded[mT]);
					}
					mT++;
				} else {
					this.cancel();
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
					for (Player p : players) {
						p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100f, 0.8f);
						p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY",
							ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "C'Shura, The Soulbinder", 10, 80, 10);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 20 * 4);
	}
}
