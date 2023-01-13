package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellFrostNova;
import com.playmonumenta.plugins.bosses.spells.SpellPushPlayersAway;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.masked.SpellShadowGlade;
import com.playmonumenta.plugins.bosses.spells.masked.SpellSummonBlazes;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Masked extends BossAbilityGroup {

	public static final String identityTag = "boss_masked";
	public static final int DETECTION_RANGE = 50;

	private static final double MOVEMENT_SPEED = 0.25;
	private static final int MAXIMUM_BASE_HEALTH = 1024;
	private static final int TIMER_INCREMENT = 20 * 1;
	private static final int TIME_SPAWN = 0;
	private static final int TIME_TITLE = 20 * 2;
	private static final int TIME_BEGIN = 20 * 8;

	private static final String SPAWN_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Beautiful, isn't it. A Black Shard, shorn from the Black Wool itself. I don't know how you survived me once, but I am impressed.\"}]";
	private static final String BEGIN_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" However, you have interfered with our plans. Nothing will stop us! Die!\"}]";
	private static final String PHASE_CHANGE_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Know that even with my death our plans will not stop. The Masked are unstoppable!\"}]";
	private static final String DEATH_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Hah... My death won't stop the shard... We will not fail... We will not fail Lord Calder...\"}]";

	private final World mWorld;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private final ItemStack mMeleeWeapon;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Masked(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Masked(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mWorld = boss.getWorld();
		mSpawnLoc = spawnLoc;
		mSpawnLoc.setY(mSpawnLoc.getBlockY());
		mEndLoc = endLoc;

		// Store the Arcane Gladius to a variable for phase 2
		mMeleeWeapon = mBoss.getEquipment().getItemInMainHand();

		mBoss.setRemoveWhenFarAway(false);

		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);

		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT == TIME_SPAWN) {
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, SPAWN_DIALOG_COMMAND);
					new PartialParticle(Particle.DRAGON_BREATH, mSpawnLoc, 50, 0.5, 0.5, 0.5, 0.02).spawnAsEntityActive(boss);
					mWorld.playSound(mSpawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2f, 1f);
				} else if (mT == TIME_TITLE) {
					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, DETECTION_RANGE, true)) {
						MessagingUtils.sendTitle(player, Component.text("The Masked Man", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
							Component.text("Harbinger of Shadow", NamedTextColor.LIGHT_PURPLE),
							15, 100, 15);
					}
				} else if (mT == TIME_BEGIN) {
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					// Swap weapon to bow for phase 1
					ItemStack item = new ItemStack(Material.BOW, 1);
					item.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
					mBoss.getEquipment().setItemInMainHand(item);
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, BEGIN_DIALOG_COMMAND);
					resumeBossFight();
					this.cancel();
				}

				mT += TIMER_INCREMENT;
			}
		}.runTaskTimer(mPlugin, 0, TIMER_INCREMENT);
	}

	@Override
	public void init() {
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, DETECTION_RANGE);
		int health = (int) ((1 - Math.pow(0.5, playerCount)) * MAXIMUM_BASE_HEALTH);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, health);
		mBoss.setHealth(health);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, DEATH_DIALOG_COMMAND);
	}

	private void resumeBossFight() {
		SpellManager activeSpells1 = new SpellManager(Arrays.asList(
			new SpellBaseLaser(mPlugin, mBoss, 40, 120, true, false, 160,
				// Tick action per player
				(LivingEntity target, int ticks, boolean blocked) -> {
					target.getWorld().playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);
					mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);
					if (!blocked && ticks > 0 && ticks % 20 == 0) {
						BossUtils.blockableDamage(mBoss, target, DamageType.MAGIC, 5);
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1).spawnAsEntityActive(mBoss);
				},
				null),
			new SpellShadowGlade(mPlugin, mBoss.getLocation(), 2),
			new SpellSummonBlazes(mPlugin, mBoss)
		));

		SpellManager activeSpells2 = new SpellManager(Arrays.asList(
			new SpellFrostNova(mPlugin, mBoss, 9, 6, 12),
			new SpellShadowGlade(mPlugin, mSpawnLoc, 2),
			new SpellSummonBlazes(mPlugin, mBoss)
		));


		List<Spell> passiveSpells1 = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellPushPlayersAway(mBoss, 7, 15),
			// Teleport the boss to spawn, preserving look direction
			new SpellRunAction(() -> {
				Location teleLoc = mSpawnLoc.clone();
				Location curLoc = mBoss.getLocation();
				teleLoc.setYaw(curLoc.getYaw());
				teleLoc.setPitch(curLoc.getPitch());
				mBoss.teleport(teleLoc);
			})
		);

		List<Spell> passiveSpells2 = Arrays.asList(
			new SpellBlockBreak(mBoss),
			// Teleport the boss to spawn preserving look direction whenever in water
			new SpellRunAction(() -> {
				Location curLoc = mBoss.getLocation();
				if (curLoc.getY() < mSpawnLoc.getY() - 5) {
					Location teleLoc = mSpawnLoc.clone();
					teleLoc.setYaw(curLoc.getYaw());
					teleLoc.setPitch(curLoc.getPitch());
					mBoss.teleport(teleLoc);
				}
			})
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(50, mBoss -> {
			changePhase(activeSpells2, passiveSpells2, null);
			EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
			EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
			// Put sword back in mainhand
			mBoss.getEquipment().setItemInMainHand(mMeleeWeapon);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, PHASE_CHANGE_DIALOG_COMMAND);
		});

		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, DETECTION_RANGE, BarColor.WHITE, BarStyle.SEGMENTED_10, events);
		super.constructBoss(activeSpells1, passiveSpells1, DETECTION_RANGE, bossBar);
	}
}
