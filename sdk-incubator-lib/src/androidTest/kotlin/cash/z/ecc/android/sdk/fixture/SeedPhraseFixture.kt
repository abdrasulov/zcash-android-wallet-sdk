package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.SeedPhrase

object SeedPhraseFixture {
    @Suppress("MaxLineLength")
    val SEED_PHRASE = "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"

    fun new(seedPhrase: String = SEED_PHRASE) = SeedPhrase.new(seedPhrase)
}
