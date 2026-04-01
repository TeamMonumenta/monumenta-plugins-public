package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.OBFUSCATED;

public class CreeperMistletoe extends Ability {
	private static final String SCOREBOARD = "CreeperMistletoe";
	private static final String ADVANCEMENT_REQ = "monumenta:challenges/r1/coalrupted/fastcoal";
	private static final int POINT_COST = 4;
	private static final double RANGE = 7;
	private static final int EXPLOSION_COAL = 2;
	private static final NamespacedKey LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r1/dungeons/koal/incoalporeal_coal");

	public static final AbilityInfo<CreeperMistletoe> INFO =
		new SnowPerkGui.SnowPerkInfo<>(CreeperMistletoe.class, "Creeper Mistletoe", CreeperMistletoe::new)
			.advancementReq(ADVANCEMENT_REQ)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.SWEET_BERRIES)
			.description(getDescription());

	public CreeperMistletoe(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Creeper> creepers = new Hitbox.SphereHitbox(mPlayer.getLocation(), RANGE).getHitEntitiesByClass(Creeper.class);
		creepers.removeIf(creeper -> ScoreboardUtils.checkTag(creeper, AbilityUtils.IGNORE_TAG));
		for (Creeper creeper : creepers) {
			if (!creeper.isIgnited()) {
				Location loc = creeper.getLocation();
				World world = loc.getWorld();
				BukkitScheduler scheduler = Bukkit.getScheduler();
				new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 2, 0)).count(12).delta(0.1, 0.25, 0.1).spawnAsPlayerPassive(mPlayer);
				scheduler.runTaskLater(mPlugin, () -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, SoundCategory.PLAYERS, 1f, 1.25f * 1.25f), 0);
				scheduler.runTaskLater(mPlugin, () -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, SoundCategory.PLAYERS, 1f, 1.25f * 1.33f), 2);
				scheduler.runTaskLater(mPlugin, () -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, SoundCategory.PLAYERS, 1f, 1.25f * 1.5f), 4);

				creeper.addScoreboardTag(AbilityUtils.IGNORE_TAG);
				creeper.addScoreboardTag(CrowdControlImmunityBoss.identityTag);
				ScoreboardUtils.addEntityToTeam(creeper, "unpushable_dark_green");
				creeper.setGlowing(true);
				creeper.setIgnited(true);
				creeper.setMaxFuseTicks(30);

				new BukkitRunnable() {
					final Location mLocation = LocationUtils.getEntityCenter(creeper);
					@Override
					public void run() {
						ItemStack coal = InventoryUtils.getItemFromLootTable(mLocation, LOOT_TABLE);
						if (coal != null) {
							coal.setAmount(EXPLOSION_COAL);
							mLocation.getWorld().dropItem(mLocation, coal);
						}

						new PartialParticle(Particle.BLOCK_CRACK, mLocation).count(100).extra(5).delta(1).data(Material.COAL_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.ITEM_CRACK, mLocation).count(50).extra(0.4).delta(0.8).data(new ItemStack(Material.COAL)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.ITEM_CRACK, mLocation).count(50).extra(0.5).delta(0.8).data(new ItemStack(Material.COAL)).spawnAsPlayerActive(mPlayer);
					}
				}.runTaskLater(mPlugin, 30);
			}
		}
	}

	public static Description<CreeperMistletoe> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.ACHIEVEMENT_ARROW_COLOR)
			.addLine("*Unlock:* *Pile up 100 Coal within 30s of starting.*").styles(DescriptionUtils.REQUIREMENT_LABEL, DescriptionUtils.REQUIREMENT_TEXT)
			.addDashedLine()
			.addIfElse((a, p) -> !AdvancementUtils.checkAdvancement(p, ADVANCEMENT_REQ),
				desc -> desc.addLine("*Creepers are automatically ignited when*").styles(OBFUSCATED)
					.addLine("*they come within X blocks of you, and*").styles(OBFUSCATED)
					.addLine("*their explosions drop X Coal.*").styles(OBFUSCATED),
				desc -> desc.addLine("Creepers are automatically ignited when")
					.addLine("they come within %d blocks of you, and").statValues(stat(RANGE))
					.addLine("their explosions drop +%d *Coal*.").styles(SnowPerkGui.COAL_COLOR).statValues(stat(EXPLOSION_COAL)))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
