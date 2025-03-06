package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.BrownPolarityDisplay;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class BrownMagnetSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_brown_magnetswap";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 100;

		@BossParam(help = "Time in ticks between each polarity swap")
		public int SWAP_TICKS = TICKS_PER_SECOND * 10;

		@BossParam(help = "Initial polarity of the launcher (either 'PLUS' or 'MINUS')")
		public String INITIAL_CHARGE = "PLUS";

		@BossParam(help = "If a player has opposite charge, multiply the launcher's damage against the player by this")
		public double PLAYER_DAMAGE_RESIST = 0.8;

		@BossParam(help = "If a player has opposite charge, multiply the player's damage against the launcher by this")
		public double ENEMY_DAMAGE_VULN = 1.2;
	}

	private static final Set<Material> LEATHER_ARMOR_TYPES = Set.of(
		Material.LEATHER_HELMET,
		Material.LEATHER_CHESTPLATE,
		Material.LEATHER_LEGGINGS,
		Material.LEATHER_BOOTS
	);

	private final World mWorld;
	private final double mBossVuln;
	private final double mPlayerResist;
	private boolean mIsPositive;
	private int mTicks;
	private double mLastDamageTick;

	public BrownMagnetSwapBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		mWorld = mBoss.getWorld();
		mTicks = p.SWAP_TICKS;
		mPlayerResist = p.PLAYER_DAMAGE_RESIST;
		mBossVuln = p.ENEMY_DAMAGE_VULN;
		mLastDamageTick = mBoss.getTicksLived();

		mIsPositive = !p.INITIAL_CHARGE.equalsIgnoreCase("minus");

		// glow with low priority (so that player abilities can override)
		GlowingManager.startGlowing(mBoss, mIsPositive ? NamedTextColor.RED : NamedTextColor.BLUE, -1, 0,
			null, "magnet");

		final BossBarManager bossBar = new BossBarManager(mBoss, p.DETECTION, mIsPositive ? BossBar.Color.RED :
			BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_20, null);

		final List<Spell> passives = List.of(
			new SpellRunAction(() -> {
				// Every SWAP TICKS duration, swap charges.
				if (mTicks <= 0) {
					mIsPositive = !mIsPositive;
					mTicks = p.SWAP_TICKS;

					// If boss is wearing leather anything, change color of armor.
					if (mBoss.getEquipment() != null) {
						for (final EquipmentSlot slot : EquipmentSlot.values()) {
							final ItemStack item = mBoss.getEquipment().getItem(slot);
							if (LEATHER_ARMOR_TYPES.contains(item.getType())) {
								final LeatherArmorMeta armorMeta = (LeatherArmorMeta) item.getItemMeta();
								if (mIsPositive) {
									armorMeta.setColor(Color.RED);
								} else {
									armorMeta.setColor(Color.BLUE);
								}
								item.setItemMeta(armorMeta);
								mBoss.getEquipment().setItem(slot, item);
							}
						}
					}

					String name = mBoss.getName();
					final int length = name.length();

					if (name.charAt(0) == '+' && !mIsPositive) {
						name = "-" + name.substring(1, length - 1) + "-";
					} else if (name.charAt(0) == '-' && mIsPositive) {
						name = "+" + name.substring(1, length - 1) + "+";
					}

					if (mIsPositive) {
						bossBar.setColor(BossBar.Color.RED);
					} else {
						bossBar.setColor(BossBar.Color.BLUE);
					}
					GlowingManager.startGlowing(mBoss, mIsPositive ? NamedTextColor.RED : NamedTextColor.BLUE,
						-1, 0, null, "magnet");

					mBoss.customName(Component.text(name));

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE,
						1, 2);
					new PartialParticle(Particle.VILLAGER_HAPPY, mBoss.getLocation().add(0, 1, 0)).count(10)
						.delta(0.5).extra(1).spawnAsEnemy();
				}

				mTicks -= PASSIVE_RUN_INTERVAL_DEFAULT;
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passives, p.DETECTION, bossBar);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (mIsPositive) {
				// Positively charged, does less damage when player negative
				if (ScoreboardUtils.checkTag(player, BrownPolarityDisplay.NEGATIVE_TAG)) {
					event.setFlatDamage(event.getDamage() * mPlayerResist);
				}
			} else {
				// Negatively charged, does less damage when player positive
				if (ScoreboardUtils.checkTag(player, BrownPolarityDisplay.POSITIVE_TAG)) {
					event.setFlatDamage(event.getDamage() * mPlayerResist);
				}
			}
		}
	}


	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		if (source instanceof final Player player) {
			if (mIsPositive) {
				// Positively charged, dealt more damage when player negative
				if (ScoreboardUtils.checkTag(player, BrownPolarityDisplay.NEGATIVE_TAG)) {
					event.setFlatDamage(event.getFlatDamage() * mBossVuln);
					playAesthetic();
				}
			} else {
				// Negatively charged, dealt more damage when player positive
				if (ScoreboardUtils.checkTag(player, BrownPolarityDisplay.POSITIVE_TAG)) {
					event.setFlatDamage(event.getFlatDamage() * mBossVuln);
					playAesthetic();
				}
			}
		}
	}

	private void playAesthetic() {
		if (mLastDamageTick < mBoss.getTicksLived() - 10) {
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 1, 0)).count(10).delta(0.5)
				.extra(1).spawnAsEnemy();
			mLastDamageTick = mBoss.getTicksLived();
		}
	}
}
