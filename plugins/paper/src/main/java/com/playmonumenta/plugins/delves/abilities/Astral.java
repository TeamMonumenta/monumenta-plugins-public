package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.ChestLockBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class Astral {

	public static final String DESCRIPTION = "Sometimes, the stars gaze back.";
	private static final List<String> MOB_POOL;
	private static final List<String> SPECIAL_MOB_POOL;

	private static final NamespacedKey CHEST_CHECKED_PERSISTENT_DATA_KEY = NamespacedKeyUtils.fromString("epic:astral_checked");

	private static final List<Component> POSSIBLE_DESCRIPTIONS = Arrays.asList(
		Component.text("Po").append(Component.text("tesn").decorate(TextDecoration.OBFUSCATED)).append(Component.text("e co")).append(Component.text("nsp").decorate(TextDecoration.OBFUSCATED)).append(Component.text("icere ira")).append(Component.text("m c").decorate(TextDecoration.OBFUSCATED)).append(Component.text("aeli?")),
		Component.text("Perc").append(Component.text("ipi").decorate(TextDecoration.OBFUSCATED)).append(Component.text("sne co")).append(Component.text("nple").decorate(TextDecoration.OBFUSCATED)).append(Component.text("xu")).append(Component.text("m as").decorate(TextDecoration.OBFUSCATED)).append(Component.text("trorum?")),
		Component.text("Astra ").append(Component.text("consumu").decorate(TextDecoration.OBFUSCATED)).append(Component.text("nt omni")).append(Component.text("a").decorate(TextDecoration.OBFUSCATED)),
		Component.text("Pe").append(Component.text("r as").decorate(TextDecoration.OBFUSCATED)).append(Component.text("pera ")).append(Component.text("ad a").decorate(TextDecoration.OBFUSCATED)).append(Component.text("stra"))
	);


	public static String[] rankDescription(int level) {
		return new String[]{
			MessagingUtils.legacyFromComponent(POSSIBLE_DESCRIPTIONS.get(FastUtils.RANDOM.nextInt(POSSIBLE_DESCRIPTIONS.size())))
		};
	}


	static {
		MOB_POOL = Arrays.asList("PillarAlpha", "PillarBeta", "PillarGamma", "PillarDelta", "PillarEpsilon", "PillarNu", "PillarEta", "PillarXi", "PillarLambda");
		SPECIAL_MOB_POOL = Arrays.asList("PillarMutatedAlpha", "PillarMutatedBeta", "PillarMutatedGamma", "PillarMutatedDelta", "PillarMutatedEpsilon", "PillarMutatedNu", "PillarMutatedEta", "PillarMutatedXi", "PillarMutatedLambda");
	}

	private static void summonAstral(Block block) {
		List<Location> validSpawnLocs = new ArrayList<>();
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				@Nullable Location loc = getNearestBlockUnder(block.getLocation().clone().add(i, 0, j), 5);
				if (!(i == 0 && j == 0) && loc != null) {
					validSpawnLocs.add(loc.clone().add(0.5, 1, 0.5));
				}
			}
		}
		Location currentBlockLocation = block.getLocation().clone().add(0.5, 1, 0.5);
		if (isLocationValidSpawnLoc(currentBlockLocation)) {
			validSpawnLocs.add(currentBlockLocation);
		}
		if (validSpawnLocs.size() > 0) {
			Location loc = validSpawnLocs.get(FastUtils.RANDOM.nextInt(validSpawnLocs.size()));
			List<String> mobPool = MOB_POOL;
			if (FastUtils.RANDOM.nextDouble() < 0.0133) {
				mobPool = SPECIAL_MOB_POOL;
			}
			LivingEntity boss = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, mobPool.get(FastUtils.RANDOM.nextInt(mobPool.size())));
			if (boss != null) {
				boss.addScoreboardTag(ChestLockBoss.identityTag + String.format("[x=%s,y=%s,z=%s]", block.getX(), block.getY(), block.getZ()));
				boss.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 10, 3f);
				try {
					BossManager.createBoss(null, boss, ChestLockBoss.identityTag);
				} catch (Exception e) {
					Plugin.getInstance().getLogger().warning("Failed to create boss ChestLockBoss: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private static @Nullable Location getNearestBlockUnder(Location location, int distance) {
		for (int i = 0; i < distance; i++) {
			Location locationUnder = location.clone().add(0, -i, 0);
			if (isLocationValidSpawnLoc(locationUnder)) {
				return locationUnder;
			}
		}
		return null;
	}

	private static boolean isLocationValidSpawnLoc(Location testLocation) {
		Block blockCurrent = testLocation.clone().add(0, 0, 0).getBlock();
		Block block1Above = testLocation.clone().add(0, 1, 0).getBlock();
		Block block2Above = testLocation.clone().add(0, 2, 0).getBlock();
		Block block3Above = testLocation.clone().add(0, 3, 0).getBlock();
		if (blockCurrent.isSolid() && !block1Above.isSolid() && !block2Above.isSolid() && !block3Above.isSolid()
			    && !block1Above.getType().equals(Material.LAVA) && !block2Above.getType().equals(Material.LAVA)
			    && !block3Above.getType().equals(Material.LAVA)) {
			return true;
		}
		return false;
	}

	public static void applyModifiers(Chest chest, int level) {
		if (level == 0 || ZoneUtils.hasZoneProperty(chest.getLocation(), ZoneUtils.ZoneProperty.LOOTROOM)) {
			return;
		}
		if (!chest.getPersistentDataContainer().has(CHEST_CHECKED_PERSISTENT_DATA_KEY)) {
			chest.getPersistentDataContainer().set(CHEST_CHECKED_PERSISTENT_DATA_KEY, PersistentDataType.BYTE, (byte) 1);
			chest.update();
			if (FastUtils.RANDOM.nextDouble() < 0.33) {
				summonAstral(chest.getBlock());
			}
		}
	}
}
