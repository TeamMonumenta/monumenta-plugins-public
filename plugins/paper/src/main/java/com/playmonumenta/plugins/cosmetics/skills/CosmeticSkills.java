package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeEchoesCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.RitualRingCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.BloodyRetaliationCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.TouchOfEntropyCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.DarkLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.HallowedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.SanguineAegisCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.TwistedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DecapitationCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.TranscCombosCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.EverseeingEyeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.TwistedCompanionCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.InfernalFlamesCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.VampiricDrainCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ColossalBruteCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.GloryExecutionCS;
import com.playmonumenta.plugins.utils.MMLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CosmeticSkills {

	private static final ImmutableMap<String, CosmeticSkill> COSMETIC_SKILLS =
		ImmutableMap.<String, CosmeticSkill>builder()
			//Alchemist
			.put(SunriseBrewCS.NAME, new SunriseBrewCS())
			.put(GruesomeEchoesCS.NAME, new GruesomeEchoesCS())
			.put(RitualRingCS.NAME, new RitualRingCS())
			//Cleric
			.put(DarkPunishmentCS.NAME, new DarkPunishmentCS())
			.put(TouchOfEntropyCS.NAME, new TouchOfEntropyCS())
			.put(BloodyRetaliationCS.NAME, new BloodyRetaliationCS())
			//Mage
			.put(VolcanicBurstCS.NAME, new VolcanicBurstCS())
			.put(TwistedLanceCS.NAME, new TwistedLanceCS())
			.put(SanguineAegisCS.NAME, new SanguineAegisCS())
			//Rogue
			.put(WindStepCS.NAME, new WindStepCS())
			.put(TranscCombosCS.NAME, new TranscCombosCS())
			.put(DecapitationCS.NAME, new DecapitationCS())
			//Scout
			.put(FireworkStrikeCS.NAME, new FireworkStrikeCS())
			.put(TwistedCompanionCS.NAME, new TwistedCompanionCS())
			.put(EverseeingEyeCS.NAME, new EverseeingEyeCS())
			//Warlock
			.put(AvalanchexCS.NAME, new AvalanchexCS())
			.put(InfernalFlamesCS.NAME, new InfernalFlamesCS())
			.put(VampiricDrainCS.NAME, new VampiricDrainCS())
			//Warrior
			.put(BrambleShellCS.NAME, new BrambleShellCS())
			.put(ColossalBruteCS.NAME, new ColossalBruteCS())
			.put(GloryExecutionCS.NAME, new GloryExecutionCS())
			//Deprecated or test only
			.put(DarkLanceCS.NAME, new DarkLanceCS())
			.put(HallowedLanceCS.NAME, new HallowedLanceCS())
			.build();

	/* Notes for implementing more Cosmetic Skills
	 * 0. If the ability is not skinned, add a default CS class!
	 * 0.a A default CS should have an ImmutableList to store all skins of this ability.
	 * 0.b A default CS should store all effects to be skinned.
	 * 0.c Add skin checker and mCosmetic field in the ability
	 * 1. Create new class in correct package, extending the base CS.
	 * 2. Override effect methods for new skin.
	 * 3. Override getCosmetic() and getDisplayItem() for correct cosmetic display.
	 * 4. Put a new entry here.
	 */

	private static final ImmutableList<DepthsCS> DEPTHS_CS;
	private static final ImmutableList<GalleryCS> GALLERY_CS;

	static {
		CosmeticSkill cs;

		ImmutableList.Builder<DepthsCS> depthsBuilder = ImmutableList.<DepthsCS>builder();
		for (String skin : getDepthsNames()) {
			cs = getCosmeticSkill(skin);
			if (cs instanceof DepthsCS) {
				depthsBuilder.add((DepthsCS) cs);
			} else {
				//Why here
				MMLog.warning("Depths cosmetic skill list building exception at: " + skin + ". Please check name list!");
			}
		}
		DEPTHS_CS = depthsBuilder.build();

		ImmutableList.Builder<GalleryCS> galleryBuilder = ImmutableList.<GalleryCS>builder();
		for (String skin : getGalleryNames()) {
			cs = getCosmeticSkill(skin);
			if (cs instanceof GalleryCS) {
				galleryBuilder.add((GalleryCS) cs);
			} else {
				//Why here
				MMLog.warning("Gallery cosmetic skill list building exception at: " + skin + ". Please check name list!");
			}
		}
		GALLERY_CS = galleryBuilder.build();
	}

	public static Cosmetic getCosmeticByName(String name) {
		CosmeticSkill cs = COSMETIC_SKILLS.getOrDefault(name, null);
		return cs != null ? cs.getCosmetic() : null;
	}

	public static CosmeticSkill getCosmeticSkill(String name) {
		return COSMETIC_SKILLS.getOrDefault(name, null);
	}

	public static <T extends CosmeticSkill> T getPlayerCosmeticSkill(@Nullable Player player, T baseCosmetic, ImmutableMap<String, T> skinList) {
		String name = CosmeticsManager.getInstance().getSkillCosmeticName(player, baseCosmetic.getAbilityName());
		return skinList.getOrDefault(name, baseCosmetic);
	}

	public static Material getDisplayItem(String cosmeticSkillName) {
		CosmeticSkill cosmeticSkill = COSMETIC_SKILLS.get(cosmeticSkillName);
		if (cosmeticSkill != null) {
			return cosmeticSkill.getDisplayItem();
		} else {
			return Material.BLAZE_POWDER;
		}
	}

	public static String[] getNames() {
		return COSMETIC_SKILLS.keySet().toArray(new String[0]);
	}

	public static ImmutableList<String> getDepthsNames() {
		return ImmutableList.<String>builder()
			.add(SunriseBrewCS.NAME)
			.add(DarkPunishmentCS.NAME)
			.add(VolcanicBurstCS.NAME)
			.add(WindStepCS.NAME)
			.add(FireworkStrikeCS.NAME)
			.add(AvalanchexCS.NAME)
			.add(BrambleShellCS.NAME)
			.build();
	}

	public static ImmutableList<DepthsCS> getDepthsSkins() {
		return DEPTHS_CS;
	}

	public static ImmutableList<String> getDelvesNames() {
		return ImmutableList.<String>builder()
			.add(GruesomeEchoesCS.NAME)
			.add(TouchOfEntropyCS.NAME)
			.add(TwistedLanceCS.NAME)
			.add(TranscCombosCS.NAME)
			.add(TwistedCompanionCS.NAME)
			.add(InfernalFlamesCS.NAME)
			.add(ColossalBruteCS.NAME)
			.build();
	}

	public static ImmutableList<String> getGalleryNames() {
		return ImmutableList.<String>builder()
			.add(RitualRingCS.NAME)
			.add(BloodyRetaliationCS.NAME)
			.add(SanguineAegisCS.NAME)
			.add(DecapitationCS.NAME)
			.add(EverseeingEyeCS.NAME)
			.add(VampiricDrainCS.NAME)
			.add(GloryExecutionCS.NAME)
			.build();
	}

	public static ImmutableList<GalleryCS> getGallerySkins() {
		return GALLERY_CS;
	}
}
