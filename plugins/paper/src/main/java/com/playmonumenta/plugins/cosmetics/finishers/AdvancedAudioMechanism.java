package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class AdvancedAudioMechanism implements EliteFinisher {

	public static final String NAME = "Advanced Audio Mechanism";
	private static final int DURATION = 28;
	private static final float DISPLAY_SIZE = 0.8f;

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		loc = LocationUtils.fallToGround(loc, 0);
		World world = p.getWorld();
		Vector dir = VectorUtils.rotateTargetDirection(LocationUtils.getDirectionTo(p.getLocation(), killedMob.getLocation()).setY(0).normalize(), 135 + 90 * Math.random(), 0).multiply(DISPLAY_SIZE);
		Location middleLoc = loc.clone().add(dir.clone().multiply(2.8));
		createDisplay(Material.LOOM, middleLoc, dir);
		ItemDisplay speakerLeft = createDisplay(Material.JUKEBOX, middleLoc.clone().add(VectorUtils.rotateTargetDirection(dir, -90, 0)), dir);
		ItemDisplay speakerRight = createDisplay(Material.JUKEBOX, middleLoc.clone().add(VectorUtils.rotateTargetDirection(dir, 90, 0)), dir);

		Skeleton DJ = (Skeleton) LibraryOfSoulsIntegration.summon(middleLoc.clone().add(dir).setDirection(dir.multiply(-1)), "SoundEngineer");
		if (DJ != null) {
			EliteFinishers.modifyFinisherMob(DJ, p, NamedTextColor.GRAY);
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 0.5f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, Constants.NotePitches.A15);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1f, 1f);
						break;
					case 4:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, Constants.NotePitches.A15);
						break;
					case 6:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 0.5f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.9f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 0.9f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.9f, Constants.NotePitches.A15);
						break;
					case 8:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, 0.8f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 0.8f, 1f);
						break;
					case 12:
						world.playSound(middleLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.3f, 1.8f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 0.5f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, Constants.NotePitches.A15);
						break;
					case 16:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1f, 1f);
						break;
					case 20:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 0.5f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D20);
						break;
					case 24:
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 0.5f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C18);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 0.8f, 1f);
						break;
					case 28:
						world.playSound(middleLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.3f, 0.8f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, 0.8f);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
						world.playSound(middleLoc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D20);
						break;
					default:
						break;
				}

				float expansion = 0.4f * (float) Math.pow(0.8, mTicks % 8);
				Transformation speakerLeftNew = speakerLeft.getTransformation();
				speakerLeftNew.getScale().set(4 * DISPLAY_SIZE + expansion, 4 * DISPLAY_SIZE + expansion, 4 * DISPLAY_SIZE + expansion);
				speakerLeft.setTransformation(speakerLeftNew);

				Transformation speakerRightNew = speakerRight.getTransformation();
				speakerRightNew.getScale().set(4 * DISPLAY_SIZE + expansion, 4 * DISPLAY_SIZE + expansion, 4 * DISPLAY_SIZE + expansion);
				speakerRight.setTransformation(speakerRightNew);

				if (DJ != null) {
					DJ.teleport(DJ.getLocation().setDirection(LocationUtils.getVectorTo(middleLoc.clone().add(0, 1.3 + 0.3 * Math.sin(22.5 * (mTicks % 8)), 0), DJ.getEyeLocation())));
				}

				if (mTicks % 8 == 0) {
					new PartialParticle(Particle.NOTE, speakerLeft.getLocation().add(0, 0.8, 0)).extra(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.NOTE, speakerRight.getLocation().add(0, 0.8, 0)).extra(1).spawnAsPlayerActive(p);
				}
				if (mTicks >= DURATION) {
					if (DJ != null) {
						DJ.remove();
					}
					new PartialParticle(Particle.EXPLOSION_LARGE, middleLoc).spawnAsPlayerActive(p);
					new PartialParticle(Particle.EXPLOSION_NORMAL, middleLoc, 10, 0, 0, 0, 0.4).spawnAsPlayerActive(p);
					this.cancel();
				}
				mTicks++;
			}
		};
		runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private ItemDisplay createDisplay(Material material, Location loc, Vector dir) {
		ItemDisplay display = loc.getWorld().spawn(loc.clone().add(0, 0.5 * DISPLAY_SIZE, 0).setDirection(dir), ItemDisplay.class);

		display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
		Transformation newTransform = display.getTransformation();
		newTransform.getScale().set(4 * DISPLAY_SIZE, 4 * DISPLAY_SIZE, 4 * DISPLAY_SIZE);
		newTransform.getTranslation().add(0, -0.65f, 0);
		display.setTransformation(newTransform);

		display.setItemStack(new ItemStack(material));
		EntityUtils.setRemoveEntityOnUnload(display);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), display::remove, DURATION);
		return display;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LOOM;
	}
}
