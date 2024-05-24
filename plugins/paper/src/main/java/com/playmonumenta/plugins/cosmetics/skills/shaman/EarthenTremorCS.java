package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.utils.*;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

public class EarthenTremorCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.EARTHEN_TREMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIRT;
	}

	public void earthenTremorEffect(Player player, double radius) {
		World world = player.getWorld();
		Location loc = player.getLocation().add(0, 0.1, 0);
		DisplayEntityUtils.groundBlockQuake(loc, radius, List.of(Material.PODZOL, Material.DIRT, Material.MUD), new Display.Brightness(12, 12));
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2, 0.6f);
		world.playSound(loc, Sound.ITEM_AXE_WAX_OFF, SoundCategory.PLAYERS, 0.4f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.25f, 0.5f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.4f, 2.0f);
	}

	public void earthenTremorEnhancement(Player player, Location shockwaveLoc, double enhancementRadius) {
		DisplayEntityUtils.groundBlockQuake(shockwaveLoc.clone().add(0, 0.2, 0), enhancementRadius,
			List.of(Material.DIORITE, Material.GRANITE, Material.IRON_ORE),
			new Display.Brightness(8, 8));
	}
}
