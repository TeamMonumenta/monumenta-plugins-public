package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.FestiveTesseractSnowmanBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public class FestiveTesseractOverride extends BaseOverride implements Listener {
	private static final String TESSERACT_NAME = "Tesseract of Festivity";
	private static final String TESSERACT_UPGRADENAME = "Tesseract of Festivity (u)";
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final int COOLDOWN = 60 * 20 * 5;
	private static final HashMap<UUID, Integer> PLAYERS_ON_COOLDOWN = new HashMap<>();
	private static final List<String> STANDARD_SUMMONS = new ArrayList<String>(Arrays.asList("TurretSnowman", "SpeedySnowman", "HeavySnowman", "HoppingSnowman"));
	private static final List<String> UPGRADE_SUMMONS = new ArrayList<String>(Arrays.asList("SentrySnowman", "SneakySnowman", "TankSnowman", "AgileSnowman"));

	public FestiveTesseractOverride() {
		Bukkit.getPluginManager().registerEvents(this, Plugin.getInstance());
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (!InventoryUtils.testForItemWithName(item, TESSERACT_NAME, false)) {
			return true;
		}

		Location loc = player.getEyeLocation();
		loc.add(loc.getDirection().normalize().multiply(2));

		new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SNOWBALL, loc, 4, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
		if (checkTesseractName(item) == 2) {
			new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SNOWBALL, loc, 4, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
		}

		return false;
	}

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (checkTesseractName(item) == 0) {
			return true;
		}
		List<String> currentSummons = STANDARD_SUMMONS;

		if (checkTesseractName(item) == 2) {
			currentSummons = UPGRADE_SUMMONS;
		}

		Integer cooldownEnds = PLAYERS_ON_COOLDOWN.get(player.getUniqueId());
		if (cooldownEnds != null) {
			// On cooldown
			int secondsLeft = (cooldownEnds - Bukkit.getServer().getCurrentTick()) / 20;

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
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SNOWBALL, loc, 20, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
		loc.getWorld().playSound(loc, Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.1f);


		final @Nullable LivingEntity[] summons = new LivingEntity[6];
		int rnd;
		for (int i = 0; i < 6; i++) {
			rnd = FastUtils.RANDOM.nextInt(4);
			Location spawnLoc = loc.clone().add(-0.1 + 0.2 * Math.floor(i / 3.0), 0, -0.1 + 0.1 * (i % 3));
			summons[i] = ((LivingEntity) LibraryOfSoulsIntegration.summon(spawnLoc, currentSummons.get(rnd)));
		}

		for (LivingEntity summon : summons) {
			if (summon != null) {
				summon.getScoreboardTags().remove("boss_targetplayer");
				summon.getScoreboardTags().remove("boss_winter_snowman");
				if (summon instanceof Lootable) {
					((Lootable) summon).clearLootTable();
				}
				if (checkTesseractName(item) == 1) {
					BossManager.getInstance().unload(summon, false);
					try {
						BossManager.createBoss(null, summon, FestiveTesseractSnowmanBoss.identityTag);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		player.setCooldown(Material.ICE, COOLDOWN);
		PLAYERS_ON_COOLDOWN.put(player.getUniqueId(), Bukkit.getServer().getCurrentTick() + COOLDOWN);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			PLAYERS_ON_COOLDOWN.remove(player.getUniqueId());

			if (player.isOnline() && player.isValid()) {
				MessagingUtils.sendActionBarMessage(player, "Your Tesseract of Festivity is off cooldown");
			}
		}, COOLDOWN);

		return false;
	}

	private int checkTesseractName(ItemStack item) {
		if (ItemUtils.getPlainName(item).equals(TESSERACT_UPGRADENAME)) {
			return 2;
		} else if (ItemUtils.getPlainName(item).equals(TESSERACT_NAME)) {
			return 1;
		}
		return 0;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event) {
		Integer cooldownUntil = PLAYERS_ON_COOLDOWN.get(event.getPlayer().getUniqueId());
		if (cooldownUntil != null && cooldownUntil > Bukkit.getServer().getCurrentTick()) {
			event.getPlayer().setCooldown(Material.ICE, cooldownUntil - Bukkit.getServer().getCurrentTick());
		}
	}

}

