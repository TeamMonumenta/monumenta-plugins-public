package com.playmonumenta.plugins.overrides;

import java.util.HashMap;
import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.FestiveTesseractSnowmanBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public class FestiveTesseractOverride extends BaseOverride {
	private static final String TESSERACT_NAME = "Tesseract of Festivity";
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final int COOLDOWN = 60 * 20 * 5;
	private static HashMap<UUID, Integer> PLAYERS_ON_COOLDOWN = null;

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME)) {
			return true;
		}

		Location loc = player.getEyeLocation();
		loc.add(loc.getDirection().normalize().multiply(2));

		loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
		loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
		loc.getWorld().spawnParticle(Particle.SNOWBALL, loc, 4, 0.2, 0.2, 0.2, 0);

		return false;
	}

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME)) {
			return true;
		}

		if (PLAYERS_ON_COOLDOWN == null) {
			PLAYERS_ON_COOLDOWN = new HashMap<>();
		}

		Integer cooldownEnds = PLAYERS_ON_COOLDOWN.get(player.getUniqueId());
		if (cooldownEnds != null) {
			// On cooldown
			int secondsLeft = (cooldownEnds - Bukkit.getServer().getCurrentTick())/20;

			String timespec;
			if (secondsLeft < 60) {
				timespec = ChatColor.RED + "" + ChatColor.BOLD + secondsLeft + ChatColor.RESET + ChatColor.AQUA + " second";
				if (secondsLeft > 1) {
					timespec += "s";
				}
			} else {
				int minutes = secondsLeft / 60;
				timespec = ChatColor.RED + "" + ChatColor.BOLD + minutes + ChatColor.RESET + ChatColor.AQUA + " minute";
				if (minutes > 1) {
					timespec += "s";
				}
			}

			player.sendMessage(ChatColor.AQUA + "Your tesseract is on cooldown! You can use it in " + timespec);

			return false;
		}

		// Off cooldown, available to cast
		Location loc = player.getLocation();
		loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
		loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
		loc.getWorld().spawnParticle(Particle.SNOWBALL, loc, 20, 0.2, 0.2, 0.2, 0);
		loc.getWorld().playSound(loc, Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.1f);

		final LivingEntity[] summons = new LivingEntity[6];
		float rnd = FastUtils.RANDOM.nextFloat();
		if (rnd < 1.0/5) {
			summons[0] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
			summons[1] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
			summons[2] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
			summons[3] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
			summons[4] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
			summons[5] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "TurretSnowman");
		} else if (rnd >= 1.0/5 && rnd < 2.0/5) {
			summons[0] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[1] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[2] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[3] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[4] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[5] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
		} else if (rnd >= 2.0/5 && rnd < 3.0/5) {
			summons[0] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[1] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[2] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[3] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[4] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[5] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
		} else if (rnd >= 3.0/5 && rnd < 4.0/5) {
			summons[0] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[1] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[2] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[3] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[4] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[5] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
		} else {
			summons[0] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[1] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SpeedySnowman");
			summons[2] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[3] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HeavySnowman");
			summons[4] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
			summons[5] = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "HoppingSnowman");
		}

		for (int i = 0; i < summons.length; i++) {
			LivingEntity summon = summons[i];
			if (summon != null) {
				summon.getScoreboardTags().remove("boss_targetplayer");
				summon.getScoreboardTags().remove("boss_winter_snowman");
				if (summon instanceof Lootable) {
					((Lootable)summon).clearLootTable();
				}
				BossManager.getInstance().unload(summon, false);
				try {
					BossManager.getInstance().createBoss(null, summon, FestiveTesseractSnowmanBoss.identityTag);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		PLAYERS_ON_COOLDOWN.put(player.getUniqueId(), Bukkit.getServer().getCurrentTick() + COOLDOWN);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			PLAYERS_ON_COOLDOWN.remove(player.getUniqueId());

			if (player.isOnline() && player.isValid()) {
				MessagingUtils.sendActionBarMessage(plugin, player, "Your Tesseract of Festivity is off cooldown");
			}
		}, COOLDOWN);

		return false;
	}
}

