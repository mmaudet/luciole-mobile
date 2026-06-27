package fr.openllm.luciole.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R

// Spectral (serif) — titres. IBM Plex Mono — chiffres / technique. Polices statiques.
val Spectral = FontFamily(
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_medium, FontWeight.Medium),
    Font(R.font.spectral_semibold, FontWeight.SemiBold),
    Font(R.font.spectral_bold, FontWeight.Bold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

// Public Sans (texte courant) — police variable : on décline la graisse via l'axe « wght ».
@OptIn(ExperimentalTextApi::class)
private fun publicSans(weight: FontWeight, axis: Int) = Font(
    R.font.public_sans_var,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
)

val PublicSans = FontFamily(
    publicSans(FontWeight.Normal, 400),
    publicSans(FontWeight.Medium, 500),
    publicSans(FontWeight.SemiBold, 600),
    publicSans(FontWeight.Bold, 700),
)

val LucioleTypography = Typography(
    displaySmall = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal, fontSize = 13.5f.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal, fontSize = 12.5f.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold, fontSize = 10.5f.sp, lineHeight = 14.sp),
)
