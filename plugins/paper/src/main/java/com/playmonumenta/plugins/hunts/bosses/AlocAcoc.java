package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveCryogenesis;
import com.playmonumenta.plugins.hunts.bosses.spells.PassivePolarAura;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellBorealAwakening;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellFrigidBeam;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellPolarizingSlash;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellSnowStorm;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellSnowyBolts;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AlocAcoc extends Quarry {
	public static final String identityTag = "boss_alocacoc";
	public static final TextColor COLOR = TextColor.color(137, 207, 240);
	public static final int INNER_RADIUS = 40;
	public static final int OUTER_RADIUS = 60;
	private static final int HEALTH = 11000;

	private static final List<Material> WARMING_ITEMS = List.of(
		Material.LANTERN,
		Material.SOUL_LANTERN,
		Material.TORCH,
		Material.SOUL_TORCH,
		Material.CAMPFIRE,
		Material.SOUL_CAMPFIRE
	);

	private final World mWorld;

	private final List<Entity> mSummons = new ArrayList<>();

	private @Nullable Spell mLastCastSpell = null;

	public final PassivePolarAura mAura;

	public AlocAcoc(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.ALOC_ACOC);

		mWorld = boss.getWorld();

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.customName(Component.text("Aloc Acoc", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		mBoss.setCustomNameVisible(false);
		EntityUtils.setMaxHealthAndHealth(mBoss, HEALTH);

		spawnAnimation();

		mAura = new PassivePolarAura(boss, this);

		List<Spell> passives = List.of(
			mAura,
			new PassiveCryogenesis(mBoss),
			new SpellBlockBreak(mBoss, true, true),
			new SpellShieldStun(6 * 20)
		);

		SpellManager phase1 = new SpellManager(List.of(
			new SpellSnowyBolts(plugin, boss, this, mAura),
			new SpellPolarizingSlash(boss, plugin, this, mAura),
			new SpellSnowStorm(boss, plugin, this, mAura),
			new SpellFrigidBeam(plugin, boss, this, 1)
		));
		SpellManager phase2 = new SpellManager(List.of(
			new SpellSnowyBolts(plugin, boss, this, mAura),
			new SpellPolarizingSlash(boss, plugin, this, mAura),
			new SpellSnowStorm(boss, plugin, this, mAura),
			new SpellBorealAwakening(plugin, boss, this),
			new SpellFrigidBeam(plugin, boss, this, 2)
		));
		SpellManager phase3 = new SpellManager(List.of(
			new SpellSnowyBolts(plugin, boss, this, mAura),
			new SpellPolarizingSlash(boss, plugin, this, mAura),
			new SpellSnowStorm(boss, plugin, this, mAura),
			new SpellBorealAwakening(plugin, boss, this),
			new SpellFrigidBeam(plugin, boss, this, 3)
		));

		Map<Integer, BossBarManager.BossHealthAction> healthEvents = new HashMap<>();
		healthEvents.put(70, eventBoss -> changePhase(phase2, passives, null));
		healthEvents.put(30, eventBoss -> changePhase(phase3, passives, null));

		BossBarManager bossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10, healthEvents, true, true, mSpawnLoc);
		super.constructBoss(phase1, passives, OUTER_RADIUS, bossBar, 30, 1);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);

		// Cleave AOE
		if (event.getType() == DamageEvent.DamageType.MELEE && event.getBossSpellName() == null) {
			UUID uuid = damagee.getUniqueId();
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
				if (!player.getUniqueId().equals(uuid)) {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, event.getDamage(), "Frozen Cleave", null);
				}
				mAura.addFrostbite(player, 0.1f);
			}

			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 0.5f);
			new PartialParticle(Particle.SWEEP_ATTACK, mBoss.getLocation())
				.count(10)
				.delta(2, 0, 2)
				.extra(0.1)
				.spawnAsBoss();
		}
	}

	public void addSummon(Entity entity) {
		mSummons.add(entity);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		mAura.clearFrostbiteBars();
		mSummons.forEach(Entity::remove);
	}

	@Override
	public void onDespawn() {
		super.onDespawn();

		mAura.clearFrostbiteBars();
	}

	@Override
	public void unload() {
		super.unload();
		mAura.clearFrostbiteBars();
	}

	public boolean canRunSpell(Spell spell) {
		if (mLastCastSpell == null || mLastCastSpell != spell) {
			mLastCastSpell = spell;

			return true;
		}
		return false;
	}

	public boolean isWarmingItem(ItemStack item) {
		return WARMING_ITEMS.contains(item.getType()) || (item.getType().equals(Material.LIGHT_GRAY_STAINED_GLASS) && MessagingUtils.plainText(item.displayName()).contains("Tesseract of Light"));
	}

	private void spawnAnimation() {
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);

		mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 5f, 0.7f);
		mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 5f, 1.2f);

		for (int i = 0; i < 8; i++) {
			new BukkitRunnable() {
				int mTicks = 0;
				final int mDuration = 8;
				final double mTheta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

				@Override
				public void run() {
					new PPParametric(Particle.SNOWFLAKE, LocationUtils.getHalfHeightLocation(mBoss), (parameter, builder) -> {
						double adjustedParameter = (parameter + mTicks) / mDuration;
						double extraAngle = adjustedParameter * Math.PI / 1.4;
						Vector direction = new Vector(FastUtils.cos(mTheta + extraAngle) * 3, 1.2 * adjustedParameter - 0.6, FastUtils.sin(mTheta + extraAngle) * 3);
						builder.location(LocationUtils.getHalfHeightLocation(mBoss).clone().add(direction));
						direction = direction.normalize();
						builder.offset(direction.getX(), direction.getY(), direction.getZ());
					})
						.directionalMode(true)
						.count(10)
						.extra(0.15)
						.spawnAsBoss();

					new PPParametric(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(mBoss), (parameter, builder) -> {
						double adjustedParameter = (parameter + mTicks) / mDuration;
						double extraAngle = adjustedParameter * Math.PI / 1.4;
						Vector direction = new Vector(FastUtils.cos(mTheta + extraAngle) * 3, 1.2 * adjustedParameter - 0.6, FastUtils.sin(mTheta + extraAngle) * 3);
						builder.location(LocationUtils.getHalfHeightLocation(mBoss).clone().add(direction));
						direction = direction.normalize();
						builder.offset(direction.getX(), direction.getY(), direction.getZ());
					})
						.data(new Particle.DustOptions(Color.fromRGB(50, 88, 148), 0.8f))
						.directionalMode(true)
						.count(10)
						.extra(0.15)
						.spawnAsBoss();

					mTicks++;
					if (mTicks >= mDuration) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.setAI(true);
			mBoss.setInvulnerable(false);

			new PPCircle(Particle.CLOUD, mBoss.getLocation().clone().add(0, 0.25, 0), 3)
				.directionalMode(true)
				.rotateDelta(true)
				.delta(1, 0.5, 0)
				.extra(0.2)
				.countPerMeter(4)
				.spawnAsBoss();
		}, 10);
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/aloc_acoc_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/aloc_acoc_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/aloc_acoc";
	}

	@Override
	public String getQuestTag() {
		return "HuntBear";
	}
}
