package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class CarbonCapture extends Ability {
	private static final String SCOREBOARD = "CarbonCapture";
	private static final int POINT_COST = 3;
	private static final int BONUS_COAL = 3;
	private static final int MISSION_REQ = 10;
	private static final int MISSION_REWARD = 15;
	private static final String MISSION_COMPLETE_TAG = "CarbonCaptureComplete";
	private static final NamespacedKey LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r1/dungeons/koal/incoalporeal_coal");

	public static final AbilityInfo<CarbonCapture> INFO =
		new SnowPerkGui.SnowPerkInfo<>(CarbonCapture.class, "Carbon Capture", CarbonCapture::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.WITHER_SKELETON_SKULL)
			.description(getDescription());

	private int mKillCount;

	public CarbonCapture(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mKillCount = 0;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();
		if (EntityUtils.isElite(killedEntity)) {
			Location loc = LocationUtils.getEntityCenter(killedEntity);
			ItemStack coal = InventoryUtils.getItemFromLootTableOrThrow(loc, LOOT_TABLE);
			coal.setAmount(BONUS_COAL);
			loc.getWorld().dropItem(loc, coal);

			new PartialParticle(Particle.BLOCK_CRACK, loc).count(40).delta(0.3).extra(3).data(Material.COAL_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_NORMAL, loc).count(20).delta(0.1).extra(0.1).spawnAsPlayerActive(mPlayer);
			loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.75f, 0.75f);

			mKillCount++;
			if (mKillCount >= MISSION_REQ && !ScoreboardUtils.checkTag(mPlayer, MISSION_COMPLETE_TAG)) {
				mPlayer.getScoreboardTags().add(MISSION_COMPLETE_TAG);

				ItemStack reward = InventoryUtils.getItemFromLootTableOrThrow(loc, LOOT_TABLE);
				reward.setAmount(MISSION_REWARD);
				InventoryUtils.giveItem(mPlayer, reward);

				new PartialParticle(Particle.TOTEM, loc).count(50).delta(1).extra(0.25).spawnAsPlayerActive(mPlayer);
				loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.75f, 1.7f);
				mPlayer.sendMessage(MessagingUtils.fromMiniMessage("<dark_gray>[Fossilizer] <gray>%d Elites killed! Coal granted.".formatted(MISSION_REQ)));
			}
		}
	}

	public static Description<CarbonCapture> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Killing an Elite drops +%d *Coal*.").styles(SnowPerkGui.COAL_COLOR)
				.statValues(stat(BONUS_COAL))
			.addLine()
			.addLine("Once you've killed %d Elites,").statValues(stat(MISSION_REQ))
			.addLine("gain +%d *Coal*.").styles(SnowPerkGui.COAL_COLOR).statValues(stat(MISSION_REWARD))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
