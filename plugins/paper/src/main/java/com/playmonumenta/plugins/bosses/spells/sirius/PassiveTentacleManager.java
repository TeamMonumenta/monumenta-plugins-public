package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class PassiveTentacleManager extends Spell {
	public boolean mCancelMovements;
	private final Sirius mSirius;

	private final Plugin mPlugin;
	private final List<Tentacle> mTentacles;
	private static final int TELEGRAPHDURATION = 20;
	private static final int DAMAGE = 60;
	private static final int SPEED = 2; //knockback

	public PassiveTentacleManager(Sirius sirius, Plugin plugin) {
		mSirius = sirius;
		mPlugin = plugin;
		mTentacles = new ArrayList<>();
		mCancelMovements = false;
		//matrix addition is weird.
		//1st
		mTentacles.add(new Tentacle(List.of(//Start
			List.of(new Matrix4f(0.5389f, -0.2174f, -0.1496f, 0.0000f, 0.2639f, 0.4427f, 0.3072f, 0.0000f, -0.0009f, -0.3417f, 0.4932f, 0.0000f, -2.3750f, 3.6875f, 1.1316f, 1.0000f),
				new Matrix4f(-0.3534f, -0.0415f, -0.3512f, 0.0000f, -0.4130f, -0.7579f, 0.5051f, 0.0000f, -0.2872f, 0.3236f, 0.2507f, 0.0000f, -1.8125f, 3.6875f, 1.4375f, 1.0000f),
				new Matrix4f(-0.2694f, -0.0764f, -0.2856f, 0.0000f, -0.2027f, -0.8813f, 0.4269f, 0.0000f, -0.2843f, 0.1729f, 0.2220f, 0.0000f, -2.3125f, 3.1250f, 2.0000f, 1.0000f),
				new Matrix4f(-0.1988f, -0.1352f, -0.1795f, 0.0000f, 0.1115f, -0.8491f, 0.5163f, 0.0000f, -0.2222f, 0.0826f, 0.1839f, 0.0000f, -2.5625f, 2.3125f, 2.3125f, 1.0000f),
				new Matrix4f(-0.2776f, 0.0370f, 0.1077f, 0.0000f, 0.1866f, -0.6755f, 0.7134f, 0.0000f, 0.0991f, 0.2181f, 0.1806f, 0.0000f, -2.5625f, 1.3125f, 2.6250f, 1.0000f),
				new Matrix4f(-0.2199f, 0.0076f, -0.0670f, 0.0000f, -0.2724f, -0.4675f, 0.8410f, 0.0000f, -0.0249f, 0.2032f, 0.1049f, 0.0000f, -2.3125f, 0.6250f, 3.4375f, 1.0000f),
				new Matrix4f(-0.0350f, -0.1909f, -0.0482f, 0.0000f, -0.7107f, -0.0469f, 0.7020f, 0.0000f, -0.1363f, 0.0588f, -0.1340f, 0.0000f, -2.6250f, 0.3125f, 4.3750f, 1.0000f),
				new Matrix4f(-0.0217f, -0.1454f, -0.0298f, 0.0000f, -0.8219f, 0.0056f, 0.5697f, 0.0000f, -0.0827f, 0.0369f, -0.1196f, 0.0000f, -3.3750f, 0.2500f, 5.0625f, 1.0000f)
			),
			//End
			List.of(new Matrix4f(0.5389f, -0.2174f, -0.1496f, 0.0000f, 0.2639f, 0.4427f, 0.3072f, 0.0000f, -0.0009f, -0.3417f, 0.4932f, 0.0000f, -2.3512f, 3.6875f, 1.1316f, 1.0000f),
				new Matrix4f(0.3512f, -0.0415f, -0.3534f, 0.0000f, -0.5051f, -0.7579f, -0.4130f, 0.0000f, -0.2507f, 0.3236f, -0.2872f, 0.0000f, -2.1012f, 3.6875f, 1.9375f, 1.0000f),
				new Matrix4f(0.3456f, -0.0764f, -0.1863f, 0.0000f, -0.3599f, -0.8813f, -0.3063f, 0.0000f, -0.1408f, 0.1729f, -0.3321f, 0.0000f, -2.6012f, 3.1250f, 1.5000f, 1.0000f),
				new Matrix4f(0.0606f, -0.0991f, 0.2766f, 0.0000f, -0.0052f, -0.9418f, -0.3361f, 0.0000f, 0.2938f, 0.0189f, -0.0576f, 0.0000f, -3.0000f, 2.4375f, 0.8750f, 1.0000f),
				new Matrix4f(0.2754f, 0.0565f, -0.1048f, 0.0000f, -0.1349f, -0.6799f, -0.7208f, 0.0000f, -0.1120f, 0.2126f, -0.1796f, 0.0000f, -2.9375f, 1.4375f, 0.8750f, 1.0000f),
				new Matrix4f(0.2026f, -0.0377f, -0.1021f, 0.0000f, -0.4673f, -0.4466f, -0.7630f, 0.0000f, -0.0168f, 0.2023f, -0.1081f, 0.0000f, -3.1250f, 0.8750f, 0.1250f, 1.0000f),
				new Matrix4f(0.0463f, -0.1909f, 0.0375f, 0.0000f, 0.5048f, -0.0469f, -0.8620f, 0.0000f, 0.1663f, 0.0588f, 0.0942f, 0.0000f, -3.6250f, 0.5625f, -0.6875f, 1.0000f),
				new Matrix4f(0.0132f, -0.1454f, 0.0344f, 0.0000f, 0.9413f, 0.0056f, -0.3375f, 0.0000f, 0.0489f, 0.0369f, 0.1369f, 0.0000f, -3.1250f, 0.5000f, -1.5625f, 1.0000f))),
			0,
			new Vector(-5, -2, 4),
			new Vector(0, -1, -4),
			List.of(//option 1
				List.of(new Matrix4f(-0.0237f, 0.3062f, -0.3946f, 0.0000f, -0.7500f, 0.5000f, 0.4330f, 0.0000f, 0.3299f, 0.3062f, 0.2178f, 0.0000f, -2.3323f, 3.3793f, 1.5870f, 1.0000f),
					new Matrix4f(0.0402f, 0.2202f, -0.3315f, 0.0000f, -0.5975f, 0.6995f, 0.3921f, 0.0000f, 0.3182f, 0.1823f, 0.1597f, 0.0000f, -3.0487f, 3.9544f, 2.0528f, 1.0000f),
					new Matrix4f(-0.0617f, 0.1898f, -0.2240f, 0.0000f, -0.8475f, 0.2665f, 0.4591f, 0.0000f, 0.1468f, 0.2182f, 0.1444f, 0.0000f, -3.4415f, 4.6447f, 2.3464f, 1.0000f),
					new Matrix4f(-0.1909f, 0.1330f, -0.1894f, 0.0000f, -0.7643f, -0.4744f, 0.4368f, 0.0000f, -0.0318f, 0.2281f, 0.1922f, 0.0000f, -4.0485f, 4.9509f, 2.7236f, 1.0000f),
					new Matrix4f(-0.1822f, 0.0365f, -0.1356f, 0.0000f, -0.4250f, -0.8365f, 0.3459f, 0.0000f, -0.1008f, 0.1206f, 0.1679f, 0.0000f, -4.7093f, 4.7293f, 3.1306f, 1.0000f),
					new Matrix4f(-0.1591f, -0.0286f, -0.1177f, 0.0000f, 0.0039f, -0.9729f, 0.2310f, 0.0000f, -0.1211f, 0.0363f, 0.1549f, 0.0000f, -5.1269f, 4.0000f, 3.4729f, 1.0000f),
					new Matrix4f(-0.1003f, -0.0380f, -0.1048f, 0.0000f, 0.3365f, -0.9415f, 0.0188f, 0.0000f, -0.0994f, -0.0334f, 0.1072f, 0.0000f, -5.1768f, 3.0990f, 3.7006f, 1.0000f)),
				//option 2
				List.of(new Matrix4f(0.1768f, 0.3062f, -0.3536f, 0.0000f, -0.8660f, 0.5000f, -0.0000f, 0.0000f, 0.1768f, 0.3062f, 0.3536f, 0.0000f, -2.3215f, 3.3793f, 1.5361f, 1.0000f),
					new Matrix4f(0.2006f, 0.2202f, -0.2670f, 0.0000f, -0.7135f, 0.6995f, 0.0409f, 0.0000f, 0.1958f, 0.1823f, 0.2974f, 0.0000f, -3.1748f, 3.9544f, 1.5813f, 1.0000f),
					new Matrix4f(0.0586f, 0.1898f, -0.2249f, 0.0000f, -0.9635f, 0.2665f, -0.0261f, 0.0000f, 0.0550f, 0.2182f, 0.1984f, 0.0000f, -3.6617f, 4.6447f, 1.6391f, 1.0000f),
					new Matrix4f(-0.0706f, 0.1330f, -0.2595f, 0.0000f, -0.8803f, -0.4744f, -0.0038f, 0.0000f, -0.1236f, 0.2281f, 0.1506f, 0.0000f, -4.3760f, 4.9509f, 1.6623f, 1.0000f),
					new Matrix4f(-0.0899f, 0.0365f, -0.2085f, 0.0000f, -0.5410f, -0.8365f, 0.0871f, 0.0000f, -0.1713f, 0.1206f, 0.0950f, 0.0000f, -5.1517f, 4.7293f, 1.6844f, 1.0000f),
					new Matrix4f(-0.0789f, -0.0286f, -0.1815f, 0.0000f, -0.1122f, -0.9729f, 0.2020f, 0.0000f, -0.1824f, 0.0363f, 0.0736f, 0.0000f, -5.6846f, 4.0000f, 1.7720f, 1.0000f),
					new Matrix4f(-0.0345f, -0.0380f, -0.1410f, 0.0000f, 0.2820f, -0.9415f, 0.1845f, 0.0000f, -0.1397f, -0.0334f, 0.0432f, 0.0000f, -5.8416f, 3.0990f, 1.9442f, 1.0000f))
			)));

		//2nd
		mTentacles.add(new Tentacle(List.of(
			List.of(new Matrix4f(0.1496f, -0.2174f, 0.5389f, 0.0000f, -0.3072f, 0.4427f, 0.2639f, 0.0000f, -0.4932f, -0.3417f, -0.0009f, 0.0000f, -0.0871f, 2.6014f, -2.2887f, 1.0000f),
				new Matrix4f(0.3512f, -0.0415f, -0.3534f, 0.0000f, -0.5051f, -0.7579f, -0.4130f, 0.0000f, -0.2507f, 0.3236f, -0.2872f, 0.0000f, -0.3930f, 2.6014f, -1.7262f, 1.0000f),
				new Matrix4f(0.2856f, -0.0764f, -0.2694f, 0.0000f, -0.4269f, -0.8813f, -0.2027f, 0.0000f, -0.2220f, 0.1729f, -0.2843f, 0.0000f, -0.9555f, 2.0389f, -2.2262f, 1.0000f),
				new Matrix4f(0.0878f, -0.2068f, -0.1988f, 0.0000f, -0.8717f, -0.4772f, 0.1115f, 0.0000f, -0.1179f, 0.1635f, -0.2222f, 0.0000f, -1.2680f, 1.2264f, -2.4762f, 1.0000f),
				new Matrix4f(-0.0747f, 0.0859f, -0.2776f, 0.0000f, -0.9555f, -0.2283f, 0.1866f, 0.0000f, -0.0473f, 0.2792f, 0.0991f, 0.0000f, -2.0247f, 0.5625f, -2.5000f, 1.0000f),
				new Matrix4f(0.0667f, -0.0100f, -0.2199f, 0.0000f, -0.9333f, -0.2340f, -0.2724f, 0.0000f, -0.0487f, 0.2234f, -0.0249f, 0.0000f, -3.0000f, 0.4375f, -2.2748f, 1.0000f),
				new Matrix4f(0.0482f, -0.1909f, -0.0350f, 0.0000f, -0.7020f, -0.0469f, -0.7107f, 0.0000f, 0.1340f, 0.0588f, -0.1363f, 0.0000f, -3.9716f, 0.3750f, -2.5625f, 1.0000f),
				new Matrix4f(0.0298f, -0.1454f, -0.0217f, 0.0000f, -0.5697f, 0.0056f, -0.8219f, 0.0000f, 0.1196f, 0.0369f, -0.0827f, 0.0000f, -4.6595f, 0.3125f, -3.2500f, 1.0000f)
			),
			List.of(new Matrix4f(0.1496f, -0.2174f, 0.5389f, 0.0000f, -0.3072f, 0.4427f, 0.2639f, 0.0000f, -0.4932f, -0.3417f, -0.0009f, 0.0000f, 0.0379f, 2.4764f, -2.0387f, 1.0000f),
				new Matrix4f(0.4362f, -0.1310f, 0.2064f, 0.0000f, 0.3132f, -0.3486f, -0.8834f, 0.0000f, 0.1876f, 0.4500f, -0.1110f, 0.0000f, -0.7680f, 2.4764f, -1.7887f, 1.0000f),
				new Matrix4f(0.1035f, -0.0255f, 0.3855f, 0.0000f, 0.5992f, -0.7720f, -0.2120f, 0.0000f, 0.3030f, 0.2530f, -0.0646f, 0.0000f, -0.4329f, 2.2500f, -2.7500f, 1.0000f),
				new Matrix4f(-0.1255f, -0.2656f, 0.0606f, 0.0000f, 0.9036f, -0.4283f, -0.0052f, 0.0000f, 0.0273f, 0.0541f, 0.2938f, 0.0000f, 0.3690f, 1.8125f, -3.0000f, 1.0000f),
				new Matrix4f(-0.0469f, 0.0565f, 0.2909f, 0.0000f, 0.6916f, -0.6799f, 0.2436f, 0.0000f, 0.2115f, 0.2126f, -0.0072f, 0.0000f, 1.0264f, 1.1875f, -2.9375f, 1.0000f),
				new Matrix4f(-0.1245f, -0.0377f, 0.1897f, 0.0000f, 0.7862f, -0.4466f, 0.4271f, 0.0000f, 0.0686f, 0.2023f, 0.0852f, 0.0000f, 1.7522f, 0.5625f, -2.7500f, 1.0000f),
				new Matrix4f(-0.0375f, -0.1909f, 0.0463f, 0.0000f, 0.8620f, -0.0469f, 0.5048f, 0.0000f, -0.0942f, 0.0588f, 0.1663f, 0.0000f, 2.5000f, 0.3125f, -2.3584f, 1.0000f),
				new Matrix4f(-0.0344f, -0.1454f, 0.0132f, 0.0000f, 0.3375f, 0.0056f, 0.9413f, 0.0000f, -0.1369f, 0.0369f, 0.0489f, 0.0000f, 3.3125f, 0.2500f, -1.8252f, 1.0000f)
			)),
			1,
			new Vector(-5, -2, -2),
			new Vector(3, -1, -6),
			List.of(//option 1
				List.of(new Matrix4f(0.3944f, 0.3062f, -0.0270f, 0.0000f, -0.4394f, 0.5000f, -0.7463f, 0.0000f, -0.2150f, 0.3062f, 0.3317f, 0.0000f, -0.4038f, 2.1057f, -1.8659f, 1.0000f),
					new Matrix4f(0.3318f, 0.2202f, 0.0374f, 0.0000f, -0.3972f, 0.6995f, -0.5941f, 0.0000f, -0.1570f, 0.1823f, 0.3196f, 0.0000f, -0.8756f, 2.6807f, -2.5782f, 1.0000f),
					new Matrix4f(0.2235f, 0.1898f, -0.0636f, 0.0000f, -0.4663f, 0.2665f, -0.8435f, 0.0000f, -0.1431f, 0.2182f, 0.1480f, 0.0000f, -1.1725f, 3.3711f, -2.9685f, 1.0000f),
					new Matrix4f(0.1878f, 0.1330f, -0.1925f, 0.0000f, -0.4433f, -0.4744f, -0.7605f, 0.0000f, -0.1925f, 0.2281f, -0.0301f, 0.0000f, -1.5549f, 3.6773f, -3.5723f, 1.0000f),
					new Matrix4f(0.1341f, 0.0365f, -0.1833f, 0.0000f, -0.3495f, -0.8365f, -0.4220f, 0.0000f, -0.1687f, 0.1206f, -0.0994f, 0.0000f, -1.9675f, 3.4556f, -4.2295f, 1.0000f),
					new Matrix4f(0.1164f, -0.0286f, -0.1601f, 0.0000f, -0.2310f, -0.9729f, 0.0058f, 0.0000f, -0.1560f, 0.0363f, -0.1198f, 0.0000f, -2.3134f, 2.7264f, -4.6442f, 1.0000f),
					new Matrix4f(0.1040f, -0.0380f, -0.1012f, 0.0000f, -0.0159f, -0.9415f, 0.3367f, 0.0000f, -0.1081f, -0.0334f, -0.0985f, 0.0000f, -2.5415f, 1.8253f, -4.6922f, 1.0000f)),
				//option 2
				List.of(new Matrix4f(0.3550f, 0.3062f, 0.1738f, 0.0000f, -0.0074f, 0.5000f, -0.8660f, 0.0000f, -0.3520f, 0.3062f, 0.1798f, 0.0000f, -0.3189f, 2.1057f, -1.8868f, 1.0000f),
					new Matrix4f(0.2687f, 0.2202f, 0.1983f, 0.0000f, -0.0469f, 0.6995f, -0.7131f, 0.0000f, -0.2957f, 0.1823f, 0.1983f, 0.0000f, -0.3714f, 2.6807f, -2.7397f, 1.0000f),
					new Matrix4f(0.2253f, 0.1898f, 0.0567f, 0.0000f, 0.0179f, 0.2665f, -0.9637f, 0.0000f, -0.1980f, 0.2182f, 0.0566f, 0.0000f, -0.4333f, 3.3711f, -3.2261f, 1.0000f),
					new Matrix4f(0.2589f, 0.1330f, -0.0728f, 0.0000f, -0.0037f, -0.4744f, -0.8803f, 0.0000f, -0.1516f, 0.2281f, -0.1223f, 0.0000f, -0.4626f, 3.6773f, -3.9402f, 1.0000f),
					new Matrix4f(0.2077f, 0.0365f, -0.0917f, 0.0000f, -0.0917f, -0.8365f, -0.5402f, 0.0000f, -0.0964f, 0.1206f, -0.1704f, 0.0000f, -0.4913f, 3.4556f, -4.7157f, 1.0000f),
					new Matrix4f(0.1808f, -0.0286f, -0.0805f, 0.0000f, -0.2029f, -0.9729f, -0.1104f, 0.0000f, -0.0752f, 0.0363f, -0.1818f, 0.0000f, -0.5835f, 2.7264f, -5.2478f, 1.0000f),
					new Matrix4f(0.1407f, -0.0380f, -0.0357f, 0.0000f, -0.1821f, -0.9415f, 0.2836f, 0.0000f, -0.0443f, -0.0334f, -0.1393f, 0.0000f, -0.7570f, 1.8253f, -5.4033f, 1.0000f))
			)));

		//3rd
		mTentacles.add(new Tentacle(List.of(List.of(new Matrix4f(-0.5389f, -0.2174f, 0.1496f, 0.0000f, -0.2639f, 0.4427f, -0.3072f, 0.0000f, 0.0009f, -0.3417f, -0.4932f, 0.0000f, 2.9137f, 2.4764f, -0.0246f, 1.0000f),
				new Matrix4f(0.3534f, -0.0415f, 0.3512f, 0.0000f, 0.4130f, -0.7579f, -0.5051f, 0.0000f, 0.2872f, 0.3236f, -0.2507f, 0.0000f, 2.5000f, 2.4764f, -0.5000f, 1.0000f),
				new Matrix4f(0.2694f, -0.0764f, 0.2856f, 0.0000f, 0.2027f, -0.8813f, -0.4269f, 0.0000f, 0.2843f, 0.1729f, -0.2220f, 0.0000f, 2.8512f, 1.9139f, -0.8930f, 1.0000f),
				new Matrix4f(0.1988f, -0.2068f, 0.0878f, 0.0000f, -0.1115f, -0.4772f, -0.8717f, 0.0000f, 0.2222f, 0.1635f, -0.1179f, 0.0000f, 3.1012f, 1.1014f, -1.2055f, 1.0000f),
				new Matrix4f(0.2776f, 0.0859f, -0.0747f, 0.0000f, -0.1866f, -0.2283f, -0.9555f, 0.0000f, -0.0991f, 0.2792f, -0.0473f, 0.0000f, 3.1250f, 0.4375f, -1.9622f, 1.0000f),
				new Matrix4f(0.2199f, -0.0100f, 0.0667f, 0.0000f, 0.2724f, -0.2340f, -0.9333f, 0.0000f, 0.0249f, 0.2234f, -0.0487f, 0.0000f, 2.8998f, 0.3125f, -2.9375f, 1.0000f),
				new Matrix4f(0.0350f, -0.1909f, 0.0482f, 0.0000f, 0.7107f, -0.0469f, -0.7020f, 0.0000f, 0.1363f, 0.0588f, 0.1340f, 0.0000f, 3.1875f, 0.2500f, -3.9091f, 1.0000f),
				new Matrix4f(0.0217f, -0.1454f, 0.0298f, 0.0000f, 0.8219f, 0.0056f, -0.5697f, 0.0000f, 0.0827f, 0.0369f, 0.1196f, 0.0000f, 3.8750f, 0.1875f, -4.5970f, 1.0000f)
			),
			List.of(new Matrix4f(-0.5389f, -0.2174f, 0.1496f, 0.0000f, -0.2639f, 0.4427f, -0.3072f, 0.0000f, 0.0009f, -0.3417f, -0.4932f, 0.0000f, 2.8512f, 2.4764f, -0.0246f, 1.0000f),
				new Matrix4f(-0.2064f, -0.1310f, 0.4362f, 0.0000f, 0.8834f, -0.3486f, 0.3132f, 0.0000f, 0.1110f, 0.4500f, 0.1876f, 0.0000f, 2.6012f, 2.4764f, -0.8305f, 1.0000f),
				new Matrix4f(-0.3855f, -0.0255f, 0.1035f, 0.0000f, 0.2120f, -0.7720f, 0.5992f, 0.0000f, 0.0646f, 0.2530f, 0.3030f, 0.0000f, 3.5625f, 2.2500f, -0.4954f, 1.0000f),
				new Matrix4f(-0.0606f, -0.2656f, -0.1255f, 0.0000f, 0.0052f, -0.4283f, 0.9036f, 0.0000f, -0.2938f, 0.0541f, 0.0273f, 0.0000f, 3.8125f, 1.8125f, 0.3065f, 1.0000f),
				new Matrix4f(-0.2909f, 0.0565f, -0.0469f, 0.0000f, -0.2436f, -0.6799f, 0.6916f, 0.0000f, 0.0072f, 0.2126f, 0.2115f, 0.0000f, 3.7500f, 1.1875f, 0.9639f, 1.0000f),
				new Matrix4f(-0.1897f, -0.0377f, -0.1245f, 0.0000f, -0.4271f, -0.4466f, 0.7862f, 0.0000f, -0.0852f, 0.2023f, 0.0686f, 0.0000f, 3.5625f, 0.5625f, 1.6897f, 1.0000f),
				new Matrix4f(-0.0463f, -0.1909f, -0.0375f, 0.0000f, -0.5048f, -0.0469f, 0.8620f, 0.0000f, -0.1663f, 0.0588f, -0.0942f, 0.0000f, 3.1709f, 0.3125f, 2.4375f, 1.0000f),
				new Matrix4f(-0.0132f, -0.1454f, -0.0344f, 0.0000f, -0.9413f, 0.0056f, 0.3375f, 0.0000f, -0.0489f, 0.0369f, -0.1369f, 0.0000f, 2.6377f, 0.2500f, 3.2500f, 1.0000f)
			)),
			2,
			new Vector(6, -2, 4),
			new Vector(1, -1, -4), List.of(//option 1
			List.of(new Matrix4f(0.0237f, 0.3062f, 0.3946f, 0.0000f, 0.7500f, 0.5000f, -0.4330f, 0.0000f, -0.3299f, 0.3062f, -0.2178f, 0.0000f, 2.7475f, 2.2611f, -0.5059f, 1.0000f),
				new Matrix4f(-0.0402f, 0.2202f, 0.3315f, 0.0000f, 0.5975f, 0.6995f, -0.3921f, 0.0000f, -0.3182f, 0.1823f, -0.1597f, 0.0000f, 3.4639f, 2.8362f, -0.9717f, 1.0000f),
				new Matrix4f(0.0617f, 0.1898f, 0.2240f, 0.0000f, 0.8475f, 0.2665f, -0.4591f, 0.0000f, -0.1468f, 0.2182f, -0.1444f, 0.0000f, 3.8567f, 3.5265f, -1.2653f, 1.0000f),
				new Matrix4f(0.1909f, 0.1330f, 0.1894f, 0.0000f, 0.7643f, -0.4744f, -0.4368f, 0.0000f, 0.0318f, 0.2281f, -0.1922f, 0.0000f, 4.4637f, 3.8327f, -1.6425f, 1.0000f),
				new Matrix4f(0.1822f, 0.0365f, 0.1356f, 0.0000f, 0.4250f, -0.8365f, -0.3459f, 0.0000f, 0.1008f, 0.1206f, -0.1679f, 0.0000f, 5.1244f, 3.6111f, -2.0495f, 1.0000f),
				new Matrix4f(0.1591f, -0.0286f, 0.1177f, 0.0000f, -0.0039f, -0.9729f, -0.2310f, 0.0000f, 0.1211f, 0.0363f, -0.1549f, 0.0000f, 5.5420f, 2.8818f, -2.3918f, 1.0000f),
				new Matrix4f(0.1003f, -0.0380f, 0.1048f, 0.0000f, -0.3365f, -0.9415f, -0.0188f, 0.0000f, 0.0994f, -0.0334f, -0.1072f, 0.0000f, 5.5919f, 1.9808f, -2.6195f, 1.0000f)),
			//option 2
			List.of(new Matrix4f(-0.1768f, 0.3062f, 0.3536f, 0.0000f, 0.8660f, 0.5000f, -0.0000f, 0.0000f, -0.1768f, 0.3062f, -0.3536f, 0.0000f, 2.7620f, 2.2611f, -0.5175f, 1.0000f),
				new Matrix4f(-0.2006f, 0.2202f, 0.2670f, 0.0000f, 0.7135f, 0.6995f, -0.0409f, 0.0000f, -0.1958f, 0.1823f, -0.2974f, 0.0000f, 3.6152f, 2.8362f, -0.5627f, 1.0000f),
				new Matrix4f(-0.0586f, 0.1898f, 0.2249f, 0.0000f, 0.9635f, 0.2665f, 0.0261f, 0.0000f, -0.0550f, 0.2182f, -0.1984f, 0.0000f, 4.1022f, 3.5265f, -0.6205f, 1.0000f),
				new Matrix4f(0.0706f, 0.1330f, 0.2595f, 0.0000f, 0.8803f, -0.4744f, 0.0038f, 0.0000f, 0.1236f, 0.2281f, -0.1506f, 0.0000f, 4.8165f, 3.8327f, -0.6437f, 1.0000f),
				new Matrix4f(0.0899f, 0.0365f, 0.2085f, 0.0000f, 0.5410f, -0.8365f, -0.0871f, 0.0000f, 0.1713f, 0.1206f, -0.0950f, 0.0000f, 5.5922f, 3.6111f, -0.6658f, 1.0000f),
				new Matrix4f(0.0789f, -0.0286f, 0.1815f, 0.0000f, 0.1122f, -0.9729f, -0.2020f, 0.0000f, 0.1824f, 0.0363f, -0.0736f, 0.0000f, 6.1250f, 2.8818f, -0.7534f, 1.0000f),
				new Matrix4f(0.0345f, -0.0380f, 0.1410f, 0.0000f, -0.2820f, -0.9415f, -0.1845f, 0.0000f, 0.1397f, -0.0334f, -0.0432f, 0.0000f, 6.2821f, 1.9808f, -0.9256f, 1.0000f))
		)));

		//4th
		mTentacles.add(new Tentacle(List.of(
			List.of(new Matrix4f(-0.1496f, -0.2174f, -0.5389f, 0.0000f, 0.3072f, 0.4427f, -0.2639f, 0.0000f, 0.4932f, -0.3417f, 0.0009f, 0.0000f, 1.1496f, 2.6014f, 3.1637f, 1.0000f),
				new Matrix4f(-0.3512f, -0.0415f, 0.3534f, 0.0000f, 0.5051f, -0.7579f, 0.4130f, 0.0000f, 0.2507f, 0.3236f, 0.2872f, 0.0000f, 1.4555f, 2.6014f, 2.6012f, 1.0000f),
				new Matrix4f(-0.2856f, -0.0764f, 0.2694f, 0.0000f, 0.4269f, -0.8813f, 0.2027f, 0.0000f, 0.2220f, 0.1729f, 0.2843f, 0.0000f, 2.0180f, 2.0389f, 3.1012f, 1.0000f),
				new Matrix4f(-0.0878f, -0.2068f, 0.1988f, 0.0000f, 0.8717f, -0.4772f, -0.1115f, 0.0000f, 0.1179f, 0.1635f, 0.2222f, 0.0000f, 2.3305f, 1.2264f, 3.3512f, 1.0000f),
				new Matrix4f(0.0747f, 0.0859f, 0.2776f, 0.0000f, 0.9555f, -0.2283f, -0.1866f, 0.0000f, 0.0473f, 0.2792f, -0.0991f, 0.0000f, 3.0872f, 0.5625f, 3.3750f, 1.0000f),
				new Matrix4f(-0.0667f, -0.0100f, 0.2199f, 0.0000f, 0.9333f, -0.2340f, 0.2724f, 0.0000f, 0.0487f, 0.2234f, 0.0249f, 0.0000f, 4.0625f, 0.4375f, 3.1498f, 1.0000f),
				new Matrix4f(-0.0482f, -0.1909f, 0.0350f, 0.0000f, 0.7020f, -0.0469f, 0.7107f, 0.0000f, -0.1340f, 0.0588f, 0.1363f, 0.0000f, 5.0341f, 0.3750f, 3.4375f, 1.0000f),
				new Matrix4f(-0.0298f, -0.1454f, 0.0217f, 0.0000f, 0.5697f, 0.0056f, 0.8219f, 0.0000f, -0.1196f, 0.0369f, 0.0827f, 0.0000f, 5.7220f, 0.3125f, 4.1250f, 1.0000f)
			),
			List.of(new Matrix4f(-0.1496f, -0.2174f, -0.5389f, 0.0000f, 0.3072f, 0.4427f, -0.2639f, 0.0000f, 0.4932f, -0.3417f, 0.0009f, 0.0000f, 1.0871f, 2.5389f, 3.1637f, 1.0000f),
				new Matrix4f(-0.4362f, -0.1310f, -0.2064f, 0.0000f, -0.3132f, -0.3486f, 0.8834f, 0.0000f, -0.1876f, 0.4500f, 0.1110f, 0.0000f, 1.8930f, 2.5389f, 2.9137f, 1.0000f),
				new Matrix4f(-0.1035f, -0.0255f, -0.3855f, 0.0000f, -0.5992f, -0.7720f, 0.2120f, 0.0000f, -0.3030f, 0.2530f, 0.0646f, 0.0000f, 1.5579f, 2.3125f, 3.8750f, 1.0000f),
				new Matrix4f(0.1255f, -0.2656f, -0.0606f, 0.0000f, -0.9036f, -0.4283f, 0.0052f, 0.0000f, -0.0273f, 0.0541f, -0.2938f, 0.0000f, 0.7560f, 1.8750f, 4.1250f, 1.0000f),
				new Matrix4f(0.0469f, 0.0565f, -0.2909f, 0.0000f, -0.6916f, -0.6799f, -0.2436f, 0.0000f, -0.2115f, 0.2126f, 0.0072f, 0.0000f, 0.0986f, 1.2500f, 4.0625f, 1.0000f),
				new Matrix4f(0.1245f, -0.0377f, -0.1897f, 0.0000f, -0.7862f, -0.4466f, -0.4271f, 0.0000f, -0.0686f, 0.2023f, -0.0852f, 0.0000f, -0.6272f, 0.6250f, 3.8750f, 1.0000f),
				new Matrix4f(0.0375f, -0.1909f, -0.0463f, 0.0000f, -0.8620f, -0.0469f, -0.5048f, 0.0000f, 0.0942f, 0.0588f, -0.1663f, 0.0000f, -1.3750f, 0.3750f, 3.4834f, 1.0000f),
				new Matrix4f(0.0344f, -0.1454f, -0.0132f, 0.0000f, -0.3375f, 0.0056f, -0.9413f, 0.0000f, 0.1369f, 0.0369f, -0.0489f, 0.0000f, -2.1875f, 0.3125f, 2.9502f, 1.0000f)
			)),
			3,
			new Vector(-3, -2, 2),
			new Vector(5, -1, 6),
			List.of(//option 1
				List.of(new Matrix4f(-0.3946f, 0.3062f, 0.0237f, 0.0000f, 0.4330f, 0.5000f, 0.7500f, 0.0000f, 0.2178f, 0.3062f, -0.3299f, 0.0000f, 1.4787f, 2.2767f, 3.0585f, 1.0000f),
					new Matrix4f(-0.3315f, 0.2202f, -0.0402f, 0.0000f, 0.3921f, 0.6995f, 0.5975f, 0.0000f, 0.1597f, 0.1823f, -0.3182f, 0.0000f, 1.9445f, 2.8518f, 3.7749f, 1.0000f),
					new Matrix4f(-0.2240f, 0.1898f, 0.0617f, 0.0000f, 0.4591f, 0.2665f, 0.8475f, 0.0000f, 0.1444f, 0.2182f, -0.1468f, 0.0000f, 2.2380f, 3.5422f, 4.1677f, 1.0000f),
					new Matrix4f(-0.1894f, 0.1330f, 0.1909f, 0.0000f, 0.4368f, -0.4744f, 0.7643f, 0.0000f, 0.1922f, 0.2281f, 0.0318f, 0.0000f, 2.6153f, 3.8484f, 4.7747f, 1.0000f),
					new Matrix4f(-0.1356f, 0.0365f, 0.1822f, 0.0000f, 0.3459f, -0.8365f, 0.4250f, 0.0000f, 0.1679f, 0.1206f, 0.1008f, 0.0000f, 3.0223f, 3.6267f, 5.4354f, 1.0000f),
					new Matrix4f(-0.1177f, -0.0286f, 0.1591f, 0.0000f, 0.2310f, -0.9729f, -0.0039f, 0.0000f, 0.1549f, 0.0363f, 0.1211f, 0.0000f, 3.3646f, 2.8975f, 5.8531f, 1.0000f),
					new Matrix4f(-0.1048f, -0.0380f, 0.1003f, 0.0000f, 0.0188f, -0.9415f, -0.3365f, 0.0000f, 0.1072f, -0.0334f, 0.0994f, 0.0000f, 3.5922f, 1.9964f, 5.9029f, 1.0000f)),
				//option 2
				List.of(new Matrix4f(-0.3536f, 0.3062f, -0.1768f, 0.0000f, 0.0000f, 0.5000f, 0.8660f, 0.0000f, 0.3536f, 0.3062f, -0.1768f, 0.0000f, 1.4150f, 2.2767f, 3.0715f, 1.0000f),
					new Matrix4f(-0.2670f, 0.2202f, -0.2006f, 0.0000f, 0.0409f, 0.6995f, 0.7135f, 0.0000f, 0.2974f, 0.1823f, -0.1958f, 0.0000f, 1.4602f, 2.8518f, 3.9248f, 1.0000f),
					new Matrix4f(-0.2249f, 0.1898f, -0.0586f, 0.0000f, -0.0261f, 0.2665f, 0.9635f, 0.0000f, 0.1984f, 0.2182f, -0.0550f, 0.0000f, 1.5180f, 3.5422f, 4.4117f, 1.0000f),
					new Matrix4f(-0.2595f, 0.1330f, 0.0706f, 0.0000f, -0.0038f, -0.4744f, 0.8803f, 0.0000f, 0.1506f, 0.2281f, 0.1236f, 0.0000f, 1.5412f, 3.8484f, 5.1260f, 1.0000f),
					new Matrix4f(-0.2085f, 0.0365f, 0.0899f, 0.0000f, 0.0871f, -0.8365f, 0.5410f, 0.0000f, 0.0950f, 0.1206f, 0.1713f, 0.0000f, 1.5633f, 3.6267f, 5.9017f, 1.0000f),
					new Matrix4f(-0.1815f, -0.0286f, 0.0789f, 0.0000f, 0.2020f, -0.9729f, 0.1122f, 0.0000f, 0.0736f, 0.0363f, 0.1824f, 0.0000f, 1.6509f, 2.8975f, 6.4346f, 1.0000f),
					new Matrix4f(-0.1410f, -0.0380f, 0.0345f, 0.0000f, 0.1845f, -0.9415f, -0.2820f, 0.0000f, 0.0432f, -0.0334f, 0.1397f, 0.0000f, 1.8231f, 1.9964f, 6.5916f, 1.0000f))

			)));
	}


	@Override
	public void run() {
		if (!mCancelMovements && !mSirius.mCheeseLock) {
			for (Tentacle tent : mTentacles) {
				tent.updateBoundingBox();
			}
			for (Player p : PlayerUtils.playersInRange(mSirius.getPlayers(), mSirius.mBoss.getLocation(), 15, true, true)) {
				for (Tentacle tent : mTentacles) {
					if (tent.mBox.overlaps(p.getBoundingBox())) {
						if (tent.canRun()) {
							tent.run();
						}
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private class Tentacle extends Spell {
		private final List<List<Matrix4f>> mMatrix;
		private static final int COOLDOWN = 10 * 20;
		private boolean mOnCooldown;
		private final int mListPos;
		private final Vector mVec1;
		private final Vector mVec2;
		private BoundingBox mBox;
		private boolean mWiggleLock;
		private final List<List<Matrix4f>> mWiggleMatrix;

		public Tentacle(List<List<Matrix4f>> matrix, int listPos, Vector vec1, Vector vec2, List<List<Matrix4f>> wiggleMatrix) {
			mMatrix = matrix;
			mOnCooldown = false;
			mListPos = listPos;
			mVec1 = vec1;
			mVec2 = vec2;
			updateBoundingBox();
			mWiggleLock = false;
			mWiggleMatrix = wiggleMatrix;
			wiggle();
		}

		private void updateBoundingBox() {
			mBox = BoundingBox.of(mSirius.mBoss.getLocation().add(mVec1), mSirius.mBoss.getLocation().add(mVec2));
		}

		@Override
		public void run() {
			mOnCooldown = true;
			mWiggleLock = true;
			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
			new BukkitRunnable() {
				int mTicks = 0;
				int mTentalceFrame = 1;

				@Override
				public void run() {
					if (mTicks == 0) {
						World world = mSirius.mTentacles.get(mListPos).get(0).getWorld();
						Location loc = mSirius.mTentacles.get(mListPos).get(0).getLocation();
						world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.4f, 0.8f);
						world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 2f, 0.6f);
						world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 0.8f, 1.2f);

						for (int i = 0; i < mSirius.mTentacles.get(mListPos).size(); i++) {
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDuration(5);
							mSirius.mTentacles.get(mListPos).get(i).setTransformationMatrix(mMatrix.get(0).get(i));
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDelay(-1);
						}
						mSirius.mBoss.getWorld().playSound(mSirius.mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 1, 0.5f);
					}
					if (mTicks % 10 == 0) {
						World world = mSirius.mTentacles.get(mListPos).get(0).getWorld();
						Location loc = mSirius.mTentacles.get(mListPos).get(0).getLocation();
						world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
						world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.4f, 0.9f);
						world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 0.7f, 0.2f);
						world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.6f, 1.4f);


						//probably swap to particlueurils.drawrectangle
						double mX = (mVec1.getX() - mVec2.getX());
						double mZ = (mVec1.getZ() - mVec2.getZ());
						if (mListPos % 2 == 0) {
							ParticleUtils.drawRectangleTelegraph(mSirius.mBoss.getLocation().add(mVec2.getX(), mVec1.getY(), -mVec1.getZ()), mX, mZ, 10, 1, TELEGRAPHDURATION, 0.2, Particle.END_ROD, mPlugin, mSirius.mBoss);
						} else {
							ParticleUtils.drawRectangleTelegraph(mSirius.mBoss.getLocation().add(mVec2.getX(), mVec1.getY(), mVec2.getZ()), mX, mZ, 10, 1, TELEGRAPHDURATION, 0.2, Particle.END_ROD, mPlugin, mSirius.mBoss);
						}
					}
					if ((mTicks > TELEGRAPHDURATION && mTicks < TELEGRAPHDURATION + 5) && mTentalceFrame < mMatrix.size()) {
						World world = mSirius.mBoss.getWorld();
						world.playSound(mSirius.mTuulenLocation, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.NEUTRAL, 1f, 0.5f);
						for (int i = 0; i < mSirius.mTentacles.get(mListPos).size(); i++) {
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDuration(3);
							mSirius.mTentacles.get(mListPos).get(i).setTransformationMatrix(mMatrix.get(mTentalceFrame).get(i));
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDelay(-1);
						}
						hurt(true);
						mTentalceFrame++;
					}
					if (mTicks > TELEGRAPHDURATION + TELEGRAPHDURATION && mTentalceFrame > 0) {
						mTentalceFrame -= 2;
						World world = mSirius.mBoss.getWorld();
						world.playSound(mSirius.mTuulenLocation, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.NEUTRAL, 1f, 0.5f);
						for (int i = 0; i < mSirius.mTentacles.get(mListPos).size(); i++) {
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDuration(3);
							mSirius.mTentacles.get(mListPos).get(i).setTransformationMatrix(mMatrix.get(mTentalceFrame).get(i));
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDelay(-1);
						}
						hurt(false);
					}
					if (mTicks == TELEGRAPHDURATION + TELEGRAPHDURATION + 5 + 5 || mCancelMovements) {
						//reset
						for (int i = 0; i < mSirius.mTentacleRestTransforms.get(mListPos).size(); i++) {
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDuration(5);
							mSirius.mTentacles.get(mListPos).get(i).setInterpolationDelay(-1);
							mSirius.mTentacles.get(mListPos).get(i).setTransformation(mSirius.mTentacleRestTransforms.get(mListPos).get(i));

						}
						mWiggleLock = false;
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		@Override
		public boolean canRun() {
			return !mOnCooldown;
		}

		@Override
		public int cooldownTicks() {
			return COOLDOWN;
		}

		private void hurt(boolean forward) {
			for (Player p : PlayerUtils.playersInRange(mSirius.getPlayers(), mSirius.mBoss.getLocation(), 10, true, true)) {
				if (mBox.overlaps(p.getBoundingBox())) {
					DamageUtils.damage(null, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "Tentacle Swipe");
					if (forward) {
						if (mListPos == 0) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec1).add(-(mVec1.getX() - mVec2.getX()) / 2.0, 0, 0), p, SPEED, 1, true);
						} else if (mListPos == 1) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec1).add(0, 0, -(mVec1.getZ() - mVec2.getZ()) / 2.0), p, SPEED, 1, true);
						} else if (mListPos == 2) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec2).add((mVec1.getX() - mVec2.getX()) / 2.0, 0, 0), p, SPEED, 1, true);
						} else {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec2).add(0, 0, (mVec1.getZ() - mVec2.getZ()) / 2.0), p, SPEED, 1, true);
						}
					} else {
						if (mListPos == 0) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec2).add((mVec1.getX() - mVec2.getX()) / 2.0, 0, 0), p, SPEED, 1, true);
						} else if (mListPos == 1) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec2).add(0, 0, (mVec1.getZ() - mVec2.getZ()) / 2.0), p, SPEED, 1, true);
						} else if (mListPos == 2) {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec1).add(-(mVec1.getX() - mVec2.getX()) / 2.0, 0, 0), p, SPEED, 1, true);
						} else {
							MovementUtils.knockAwayRealistic(mSirius.mBoss.getLocation().add(mVec1).add(0, 0, -(mVec1.getZ() - mVec2.getZ()) / 2.0), p, SPEED, 1, true);
						}
					}
				}
			}
		}

		private void wiggle() {
			new BukkitRunnable() {

				@Override
				public void run() {
					if (!mWiggleLock && !mCancelMovements && !mSirius.mCheeseLock) {
						//dont always trigger
						if (FastUtils.randomFloatInRange(0, 1) > 0.75) {
							//wiggle left or right
							int pos = FastUtils.randomIntInRange(0, mWiggleMatrix.size() - 1);
							for (int i = 1; i < mSirius.mTentacles.get(mListPos).size(); i++) {
								Display dis = mSirius.mTentacles.get(mListPos).get(i);
								dis.setInterpolationDuration(10);
								dis.setTransformationMatrix(mWiggleMatrix.get(pos).get(i - 1));
								//dis.setTransformationMatrix(DisplayEntityUtils.transformationToMatrix4(dis.getTransformation()).sub(mWiggleMatrix.get(0).get(i - 1)));
								dis.setInterpolationDelay(-1);
							}
							Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
								for (int i = 1; i < mSirius.mTentacleRestTransforms.get(mListPos).size(); i++) {
									Display dis = mSirius.mTentacles.get(mListPos).get(i);
									dis.setInterpolationDuration(9);
									dis.setInterpolationDelay(-1);
									dis.setTransformation(mSirius.mTentacleRestTransforms.get(mListPos).get(i));
									//dis.setTransformationMatrix(DisplayEntityUtils.transformationToMatrix4(dis.getTransformation()).add(mWiggleMatrix.get(0).get(i - 1)));
								}
							}, 11);
						}
					}
					if (mSirius.mBoss.isDead()) {
						this.cancel();
					}
				}
				//creates a stagger.
			}.runTaskTimer(mPlugin, 100, 6 * 20 + FastUtils.randomIntInRange(0, 3 * 20));
		}
	}
}
