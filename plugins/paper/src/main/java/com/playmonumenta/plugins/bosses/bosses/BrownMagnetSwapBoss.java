package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

public class BrownMagnetSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_brown_magnetswap";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Number of Ticks between each swap (default 200 [10s])")
		public int SWAP_TICKS = 200;
		@BossParam(help = "Initial Charge of Magnet Swap Boss (Either 'PLUS' or 'MINUS')")
		public String INITIAL_CHARGE = "PLUS";
		@BossParam(help = "If player is of opposite charge, Boss' damage is multiplied by this (default 0.8)")
		public double PLAYER_DAMAGE_RESIST = 0.8;
		@BossParam(help = "If player is of opposite charge, Player's damage is multiplied by this (default 1.2)")
		public double ENEMY_DAMAGE_VULN = 1.2;
	}

	private static final Set<Material> LEATHER_ARMOR_TYPES = Set.of(
		Material.LEATHER_HELMET,
		Material.LEATHER_CHESTPLATE,
		Material.LEATHER_LEGGINGS,
		Material.LEATHER_BOOTS
	);

	public static final String playerTagPositive = "brown-positive";
	public static final String playerTagNegative = "brown-negative";

	private boolean mIsPositive;
	private int mTicks;
	private final double mBossVuln;
	private final double mPlayerResist;
	private double mLastDamageTick;

	private Team mRedTeam;
	private Team mBlueTeam;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BrownMagnetSwapBoss(plugin, boss);
	}

	public BrownMagnetSwapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		BrownMagnetSwapBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new BrownMagnetSwapBoss.Parameters());
		mTicks = p.SWAP_TICKS;
		mPlayerResist = p.PLAYER_DAMAGE_RESIST;
		mBossVuln = p.ENEMY_DAMAGE_VULN;
		mLastDamageTick = mBoss.getTicksLived();
		createTeams(); // TODO: Replace this with ScoreboardUtils.getExistingTeamOrCreate() when #1859 is pushed

		mBoss.setGlowing(true);

		if (p.INITIAL_CHARGE.equalsIgnoreCase("minus")) {
			mIsPositive = false;
			mBlueTeam.addEntry(mBoss.getUniqueId().toString());
		} else {
			mIsPositive = true;
			mRedTeam.addEntry(mBoss.getUniqueId().toString());
		}

		BossBarManager bossBar = new BossBarManager(plugin, boss, 40, mIsPositive ? BarColor.RED : BarColor.BLUE, BarStyle.SEGMENTED_20, null);

		List<Spell> passives = Arrays.asList(
			new SpellRunAction(() -> {
				// Every SWAP TICKS duration, swap charges.
				if (mTicks <= 0) {
					mIsPositive = !mIsPositive;
					mTicks = p.SWAP_TICKS;

					// If boss is wearing leather anything, change color of armor.
					if (mBoss.getEquipment() != null) {
						for (EquipmentSlot slot : EquipmentSlot.values()) {
							ItemStack item = mBoss.getEquipment().getItem(slot);
							if (LEATHER_ARMOR_TYPES.contains(item.getType())) {
								LeatherArmorMeta armorMeta = (LeatherArmorMeta) item.getItemMeta();
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
					int length = name.length();

					if (name.charAt(0) == '+' && !mIsPositive) {
						name = "-" + name.substring(1, length - 1) + "-";
					} else if (name.charAt(0) == '-' && mIsPositive) {
						name = "+" + name.substring(1, length - 1) + "+";
					}

					if (mIsPositive) {
						mRedTeam.addEntry(mBoss.getUniqueId().toString());
						bossBar.setColor(BarColor.RED);
					} else {
						mBlueTeam.addEntry(mBoss.getUniqueId().toString());
						bossBar.setColor(BarColor.BLUE);
					}

					mBoss.customName(Component.text(name));

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 2);
					new PartialParticle(Particle.VILLAGER_HAPPY, mBoss.getLocation().add(0, 1, 0), 10, 0.5, 1).spawnAsEnemy();
				}

				mTicks -= BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passives, 100, bossBar);
	}

	@Override public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (mIsPositive) {
				// Positively charged, does less damage when player negative
				if (ScoreboardUtils.checkTag(player, playerTagNegative)) {
					event.setDamage(event.getDamage() * mPlayerResist);
				}
			} else {
				// Negatively charged, does less damage when player positive
				if (ScoreboardUtils.checkTag(player, playerTagPositive)) {
					event.setDamage(event.getDamage() * mPlayerResist);
				}
			}
		}
	}

	@Override public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player player) {
			if (mIsPositive) {
				// Positively charged, dealt more damage when player negative
				if (ScoreboardUtils.checkTag(player, playerTagNegative)) {
					event.setDamage(event.getDamage() * mBossVuln);
					playAesthetic();
				}
			} else {
				// Negatively charged, dealt more damage when player positive
				if (ScoreboardUtils.checkTag(player, playerTagPositive)) {
					event.setDamage(event.getDamage() * mBossVuln);
					playAesthetic();
				}
			}
		}
	}

	private void playAesthetic() {
		if (mLastDamageTick < mBoss.getTicksLived() - 10) {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 0.5f);
			new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 1, 0), 10, 0.5, 1).spawnAsEnemy();
			mLastDamageTick = mBoss.getTicksLived();
		}
	}

	private void createTeams() {
		mRedTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("Red");

		if (mRedTeam == null) {
			mRedTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Red");
			mRedTeam.color(NamedTextColor.RED);
		}

		mBlueTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("Blue");

		if (mBlueTeam == null) {
			mBlueTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Blue");
			mBlueTeam.color(NamedTextColor.BLUE);
		}
	}
}
