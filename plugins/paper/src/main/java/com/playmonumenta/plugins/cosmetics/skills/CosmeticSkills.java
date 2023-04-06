package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcaneAmalgamCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcaneBezoarCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcaneTinctureCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArtilleryBombCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeEchoesCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.ArcanePanaceaCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.ArcaneRemedyCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.ArcaneTransmutationCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.PrestigiousRemedyCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.RitualRingCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ArcaneScorchedEarthCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.PrestigiousEsotericCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.BloodyRetaliationCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.TouchOfEntropyCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.PrestigiousBeamCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.PrestigiousInfusionCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.DarkLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.HallowedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.SanguineAegisCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.TwistedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.PrestigiousMoonbladeCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.PrestigiousStarfallCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DaggerOfNothingCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DecapitationCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.TranscCombosCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.PrestigiousBlitzCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.PrestigiousRondeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.EverseeingEyeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.TwistedCompanionCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PrestigiousPinningShotCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.PrestigiousManeuverCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.InfernalFlamesCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.VampiricDrainCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.PrestigiousBondsCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.PrestigiousShadesCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ColossalBruteCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.GloryExecutionCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.PrestigiousSlamCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.PrestigiousShieldCS;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CosmeticSkills {

	private static final ImmutableList<Supplier<CosmeticSkill>> COSMETIC_SKILLS = ImmutableList.of(
		//Alchemist
		SunriseBrewCS::new,
		GruesomeEchoesCS::new,
		RitualRingCS::new,
		PrestigiousEsotericCS::new,
		PrestigiousRemedyCS::new,
		ArcanePotionsCS::new,
		ArcanePanaceaCS::new,
		ArcaneTinctureCS::new,
		ArcaneScorchedEarthCS::new,
		ArcaneRemedyCS::new,
		ArcaneTransmutationCS::new,
		ArcaneBezoarCS::new,
		ArcaneAmalgamCS::new,
		ArtilleryBombCS::new,

		//Cleric
		DarkPunishmentCS::new,
		TouchOfEntropyCS::new,
		BloodyRetaliationCS::new,
		PrestigiousInfusionCS::new,
		PrestigiousBeamCS::new,

		//Mage
		VolcanicBurstCS::new,
		TwistedLanceCS::new,
		SanguineAegisCS::new,
		PrestigiousMoonbladeCS::new,
		PrestigiousStarfallCS::new,

		//Rogue
		WindStepCS::new,
		TranscCombosCS::new,
		DecapitationCS::new,
		PrestigiousRondeCS::new,
		PrestigiousBlitzCS::new,

		//Scout
		FireworkStrikeCS::new,
		TwistedCompanionCS::new,
		EverseeingEyeCS::new,
		PrestigiousManeuverCS::new,
		PrestigiousPinningShotCS::new,

		//Warlock
		AvalanchexCS::new,
		InfernalFlamesCS::new,
		VampiricDrainCS::new,
		PrestigiousBondsCS::new,
		PrestigiousShadesCS::new,

		//Warrior
		BrambleShellCS::new,
		ColossalBruteCS::new,
		GloryExecutionCS::new,
		PrestigiousSlamCS::new,
		PrestigiousShieldCS::new,

		//Deprecated or test only
		DarkLanceCS::new,
		HallowedLanceCS::new,
		DaggerOfNothingCS::new
	);

	private static final ImmutableMap<String, Supplier<CosmeticSkill>> COSMETIC_SKILLS_BY_NAME = COSMETIC_SKILLS.stream()
		                                                                                             .collect(ImmutableMap.toImmutableMap(skill -> skill.get().getName(), skill -> skill));

	/* Notes for implementing more Cosmetic Skills
	 * 0. 基础 If the ability is not skinned, add a default CS class!
	 * 0.b 效果 A default CS should store all effects to be skinned.
	 * 0.c 变量 Add skin checker and mCosmetic field in the ability
	 * 1. 新类 Create new class in correct package, extending the base CS.
	 * 2. 重载 Override effect methods for new skin.
	 * 3. 属性 Override getCosmetic() and getDisplayItem() for correct cosmetic display.
	 * 4. 注册 Put a new entry here.
	 */

	private static final ImmutableList<DepthsCS> DEPTHS_CS;
	private static final ImmutableList<GalleryCS> GALLERY_CS;
	private static final ImmutableList<PrestigeCS> PRESTIGE_CS;

	static {
		ImmutableList.Builder<DepthsCS> depthsBuilder = ImmutableList.<DepthsCS>builder();
		ImmutableList.Builder<GalleryCS> galleryBuilder = ImmutableList.<GalleryCS>builder();
		for (Supplier<CosmeticSkill> supplier : COSMETIC_SKILLS_BY_NAME.values()) {
			CosmeticSkill cs = supplier.get();
			if (cs instanceof DepthsCS) {
				depthsBuilder.add((DepthsCS) cs);
			} else if (cs instanceof GalleryCS) {
				galleryBuilder.add((GalleryCS) cs);
			}
		}
		DEPTHS_CS = depthsBuilder.build();
		GALLERY_CS = galleryBuilder.build();
		// Prestige skins need a special order in shop GUI
		PRESTIGE_CS = ImmutableList.<PrestigeCS>builder()
			.add(new PrestigiousEsotericCS())
			.add(new PrestigiousInfusionCS())
			.add(new PrestigiousMoonbladeCS())
			.add(new PrestigiousRondeCS())
			.add(new PrestigiousManeuverCS())
			.add(new PrestigiousBondsCS())
			.add(new PrestigiousSlamCS())
			.add(new PrestigiousRemedyCS())
			.add(new PrestigiousBeamCS())
			.add(new PrestigiousStarfallCS())
			.add(new PrestigiousBlitzCS())
			.add(new PrestigiousPinningShotCS())
			.add(new PrestigiousShadesCS())
			.add(new PrestigiousShieldCS())
			.build();
	}

	public static @Nullable Cosmetic getCosmeticByName(String name) {
		CosmeticSkill cs = getCosmeticSkill(name);
		return cs != null ? cs.getCosmetic() : null;
	}

	public static @Nullable CosmeticSkill getCosmeticSkill(String name) {
		Supplier<CosmeticSkill> cosmeticSkill = COSMETIC_SKILLS_BY_NAME.get(name);
		return cosmeticSkill == null ? null : cosmeticSkill.get();
	}

	@SuppressWarnings("unchecked") // the cast is actually checked with baseCosmetic.getClass().isAssignableFrom
	public static <T extends CosmeticSkill> T getPlayerCosmeticSkill(@Nullable Player player, T baseCosmetic) {
		String name = CosmeticsManager.getInstance().getSkillCosmeticName(player, baseCosmetic.getAbility());
		CosmeticSkill cosmeticSkill = getCosmeticSkill(name);
		return cosmeticSkill != null && baseCosmetic.getClass().isAssignableFrom(cosmeticSkill.getClass()) ? (T) cosmeticSkill : baseCosmetic;
	}

	public static Material getDisplayItem(String cosmeticSkillName) {
		CosmeticSkill cosmeticSkill = getCosmeticSkill(cosmeticSkillName);
		if (cosmeticSkill != null) {
			return cosmeticSkill.getDisplayItem();
		} else {
			return Material.BLAZE_POWDER;
		}
	}

	public static String[] getNames() {
		return COSMETIC_SKILLS_BY_NAME.keySet().toArray(new String[0]);
	}

	public static ImmutableList<DepthsCS> getDepthsSkins() {
		return DEPTHS_CS;
	}

	public static ImmutableList<String> getDepthsNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (DepthsCS cs : getDepthsSkins()) {
			builder.add(Objects.requireNonNull(((CosmeticSkill) cs).getName()));
		}
		return builder.build();
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

	public static ImmutableList<GalleryCS> getGallerySkins() {
		return GALLERY_CS;
	}

	public static ImmutableList<String> getGalleryNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (GalleryCS cs : getGallerySkins()) {
			builder.add(Objects.requireNonNull(((CosmeticSkill) cs).getName()));
		}
		return builder.build();
	}

	public static ImmutableList<PrestigeCS> getPrestigeSkins() {
		return PRESTIGE_CS;
	}

	public static ImmutableList<String> getPrestigeNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (PrestigeCS cs : getPrestigeSkins()) {
			builder.add(Objects.requireNonNull(((CosmeticSkill) cs).getName()));
		}
		return builder.build();
	}
}
