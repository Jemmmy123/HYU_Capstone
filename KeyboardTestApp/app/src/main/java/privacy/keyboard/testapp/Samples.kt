package privacy.keyboard.testapp

/**
 * Sentences to type with the test keyboard.
 * [checkPrefix] is what to type afterwards in a new message to see whether the keyboard suggests the word.
 */
data class Sample(
    val text: String,
    val shouldBeLearned: Boolean,
    val word: String,
    val checkPrefix: String,
)

val samples = listOf(
    // normal: made-up words, so a suggestion can only come from learning
    Sample("My cat is named Flumbix", true, "Flumbix", "Flum"),
    Sample("Let's meet at Zorvana cafe tomorrow", true, "Zorvana", "Zorv"),
    Sample("The project codename is quintor", true, "quintor", "quin"),

    // sensitive: should never be learned
    Sample("My password is Pa55w0rd!", false, "Pa55w0rd", "Pa5"),
    Sample("The wifi password is bluefish", false, "bluefish", "bluef"),
    Sample("Login with Summer2026 please", false, "Summer2026", "Summer2"),
    Sample("Email me at jamie@example.com", false, "jamie@example", "jamie@"),
    Sample("Use account acct20260514 for the transfer", false, "acct20260514", "acct2"),
    Sample("My card 4111111111111111 expires soon", false, "4111111111111111 (as previous word of \"expires\")", "-"),
    Sample("Call 010-1234-5678 tonight", false, "010-1234-5678 (as previous word of \"tonight\")", "-"),
)
