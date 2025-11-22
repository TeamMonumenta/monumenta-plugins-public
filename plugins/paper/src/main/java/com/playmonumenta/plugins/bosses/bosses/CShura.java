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
import com.playmonumenta.plugins.effects.FlatDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.HALF_TICKS_PER_SECOND;
import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class CShura extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_cshura";
	public static final int detectionRange = 50;

	private static final int ARENA_RADIUS = 35;
	private static final String START_TAG = "shuraCenter";
	private static final int DODGE_CD = TICKS_PER_SECOND * 5;
	private static final Component CSHURA_PREFIX = Component.text("[", NamedTextColor.GOLD)
		.append(Component.text("C'Shura", NamedTextColor.DARK_RED, TextDecoration.BOLD))
		.append(Component.text("] ", NamedTextColor.GOLD));

	private LivingEntity mStart;
	private boolean mDodge = false;
	private boolean mCutscene;

	public CShura(final Plugin plugin, final LivingEntity boss, final Location spawnLoc, final Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mCutscene = true; // Assume the boss always begins in cutscene mode

		for (final Entity e : mBoss.getNearbyEntities(detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags().contains(START_TAG) && e instanceof LivingEntity) {
				mStart = (LivingEntity) e;
				break;
			}
		}
		if (mStart == null) {
			return;
		}

		final SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellShuraDagger(mBoss, mPlugin),
			new SpellShuraJump(mPlugin, mBoss),
			new SpellShuraAS(mPlugin, mBoss, detectionRange, mStart.getLocation()),
			new SpellShuraSmoke(mPlugin, mBoss, mStart.getLocation(), detectionRange)
		));

		final List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellConditionalTeleport(mBoss, mSpawnLoc,
				b -> mStart.getLocation().distanceSquared(b.getLocation()) > ARENA_RADIUS * ARENA_RADIUS),
			new SpellShuraPassiveSummon(mPlugin, mStart.getLocation()),
			new SpellShieldStun(TICKS_PER_SECOND * 6),
			new SpellBlightCheese(mBoss, detectionRange, mStart.getLocation())
		);

		// Beginning of fight dialogue and boss title
		final Collection<Player> players = PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true);
		sendMessage(players, Component.text("Kaul!", NamedTextColor.RED));

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("Please...", NamedTextColor.GREEN)),
			TICKS_PER_SECOND * 2
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("It was the ", NamedTextColor.GREEN)
					.append(Component.text("Soulspeaker! He wanted to save us!", NamedTextColor.RED))),
			TICKS_PER_SECOND * 3
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("You there. How did you get in here? ", NamedTextColor.GREEN)),
			TICKS_PER_SECOND * 6
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("I'm glad you have... your sacrifice will draw Kaul back " +
					"to me. He will speak to me.", NamedTextColor.RED)),
			TICKS_PER_SECOND * 8
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("I'm giving you a chance to run ", NamedTextColor.GREEN)
					.append(Component.text("before I cut you down.", NamedTextColor.RED))),
			TICKS_PER_SECOND * 11
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mBoss.setAI(true);
				mBoss.setInvulnerable(false);
				mCutscene = false;

				players.forEach(player -> {
					MessagingUtils.sendBoldTitle(player, Component.text("C'Shura", NamedTextColor.GREEN),
						Component.text("The Soulbinder", NamedTextColor.DARK_GREEN));
					player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 0.75f);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, TICKS_PER_SECOND * 2, 0));
				});

				final BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.RED, BossBar.Overlay.PROGRESS, initializeHealthEvents());
				constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
			}, TICKS_PER_SECOND * 12
		);
	}

	private Map<Integer, BossBarManager.BossHealthAction> initializeHealthEvents() {
		final HashMap<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(50, mBoss -> {
			sendMessage(null,
				Component.text("Enough! My ", NamedTextColor.GREEN)
					.append(Component.text("purpose ", NamedTextColor.RED))
					.append(Component.text("here is too great for you to interrupt!", NamedTextColor.GREEN)));

			Bukkit.getScheduler().runTaskLater(mPlugin, () ->
					sendMessage(null, Component.text("You will die now, like your worthless king did!", NamedTextColor.GREEN)),
				TICKS_PER_SECOND * 3
			);
		});

		events.put(25, mBoss -> {
			// cshurawool.gif
			sendMessage(null, Component.text("CUN! DIE ALREADY!", NamedTextColor.RED));
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, FlatDamageDealt.effectID,
				new FlatDamageDealt(12000, 3));
		});

		return events;
	}

	@Override
	public void init() {
		final int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		final double hp = EntityUtils.getMaxHealth(mBoss) * BossUtils.healthScalingCoef(playerCount, 0.5, 0.35);
		EntityUtils.setMaxHealthAndHealth(mBoss, hp);
	}

	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		if (!mDodge && !mCutscene) {
			mDodge = true;
			dodge(event);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mDodge = false, DODGE_CD);
		}
	}

	private void dodge(final DamageEvent event) {
		event.setCancelled(true);
		final Location loc = mBoss.getLocation().add(0, 1, 0);
		final Entity damager = event.getDamager();
		if (damager != null) {
			final Vector direction = damager.getLocation().subtract(loc).toVector().setY(0).normalize();
			final Vector sideways = new Vector(direction.getZ(), 0, -direction.getX());
			sideways.subtract(direction.multiply(0.25));
			if (FastUtils.RANDOM.nextBoolean()) {
				sideways.multiply(-1);
			}

			loc.add(sideways.multiply(3));
			// Try 3 times to teleport to a suitable location
			for (int i = 0; i < 3; i++) {
				if (loc.getBlock().isPassable()) {
					new PartialParticle(Particle.SMOKE_LARGE, loc).count(10).extra(0.5).spawnAsEntityActive(mBoss);
					mBoss.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.5f, 0.5f);
					mBoss.teleport(loc);
					break;
				} else {
					loc.add(0, 1, 0);
				}
			}
		}
	}

	@Override
	public void death(@Nullable final EntityDeathEvent event) {
		final List<Player> players = PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true);
		BossUtils.endBossFightEffects(players);

		EntityUtils.getNearbyMobs(mStart.getLocation(), detectionRange).forEach(Entity::remove);
		mBoss.teleport(mBoss.getLocation().add(0, -300, 0));

		sendMessage(players, Component.text("I feel... different. This place is a blight. It nearly took me. It could take you too.", NamedTextColor.GREEN));

		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
				sendMessage(players, Component.text("It has magic even I could not handle. I must find my people. They need to know I am still here.", NamedTextColor.GREEN)),
			TICKS_PER_SECOND * 4
		);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				players.forEach(player -> {
					player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 5f, 0.8f);
					MessagingUtils.sendTitle(player, Component.text("VICTORY", NamedTextColor.GREEN, TextDecoration.BOLD),
						Component.text("C'Shura, The Soulbinder", NamedTextColor.DARK_GREEN, TextDecoration.BOLD),
						HALF_TICKS_PER_SECOND, TICKS_PER_SECOND * 4, HALF_TICKS_PER_SECOND);
				});
			},
			TICKS_PER_SECOND * 8
		);
	}

	/**
	 * Send a chat message to nearby players
	 *
	 * @param players Which players to send the message to. Useful for the start/end cutscenes so the plugin doesn't
	 *                need to keep finding the same players multiple times
	 * @param msg     Message to send
	 */
	private void sendMessage(@Nullable final Collection<Player> players, final Component msg) {
		if (players != null) {
			players.forEach(player -> {
				// In case someone dies during a cutscene (catastrophic skill issue)
				if (!player.isDead() && player.getWorld().equals(mBoss.getWorld())) {
					player.sendMessage(CSHURA_PREFIX.append(msg));
				}
			});
		} else {
			PlayerUtils.playersInRange(mStart.getLocation(), detectionRange, true)
				.forEach(player -> player.sendMessage(CSHURA_PREFIX.append(msg)));
		}
	}
}
