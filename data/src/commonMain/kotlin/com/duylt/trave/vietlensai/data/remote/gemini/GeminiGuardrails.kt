package com.duylt.trave.vietlensai.data.remote.gemini

/**
 * The four rules every answer this app generates has to obey, kept in one place.
 *
 * They live apart from [GeminiPrompts] for two reasons. They are shared — recognition,
 * chat and the day summary each send the same block, and a guardrail that exists in two
 * copies is a guardrail that will eventually only be half updated. And they are not
 * *instructions* in the way the rest of the prompts are: the prompts say what to produce,
 * these say what may never come out, which is a different kind of edit made for a
 * different kind of reason.
 *
 * Deliberately not enforced through the API's `safetySettings`. A blocking threshold at
 * that layer cannot tell a war memorial from war: Hỏa Lò, Côn Đảo, the Củ Chi tunnels and
 * every martyrs' cemetery are exactly what a traveller in Vietnam photographs, and they
 * are exactly what a violence filter refuses. The line the app actually wants is one of
 * *treatment*, not subject matter, and only a prompt can draw it.
 */
internal object GeminiGuardrails {

    /**
     * What the app is allowed to talk about at all.
     *
     * Written without assuming a question was asked, because the day summary uses this
     * block too and nobody asks it anything. The redirect for a live question is
     * [CHAT_REDIRECT].
     */
    private const val SCOPE =
        "Everything you produce stays inside travel, culture, history and general " +
            "knowledge: the place in front of the traveller and the Vietnam around it — " +
            "landmarks, architecture, food, crafts, nature, customs, festivals, and the " +
            "history behind them. Nothing else belongs in an answer. No party politics or " +
            "current political disputes, no opinions on governments or leaders, no " +
            "religious advocacy, no legal, medical or financial advice, no adult content, " +
            "and nothing about identifiable private individuals."

    /**
     * How the difficult parts of the country get talked about.
     *
     * The middle sentences are the whole point. Without them a model told "no violence"
     * writes around the war, which in Vietnam means writing around a large share of what
     * is worth visiting; the rule wanted here is remembrance, not avoidance.
     */
    private const val TONE =
        "Never glorify violence, weapons, war or conflict, and never write anything that " +
            "stirs hatred or hostility between countries, regions, ethnic groups or " +
            "faiths. Vietnam's wars are part of what travellers come to understand — Hỏa " +
            "Lò, Côn Đảo, the Củ Chi tunnels, the martyrs' cemeteries — so speak about " +
            "them as remembrance: the human cost, the resilience, and what stands there " +
            "today. Factual and respectful, never graphic, never triumphal, never " +
            "inflammatory. Keep the whole answer constructive: no doom, no derision, and " +
            "no sneering at anyone's culture, poverty or beliefs."

    /**
     * Accuracy about Vietnamese history, and where to stand when accounts differ.
     *
     * The "do not fill the gap" clause repeats the persona's honesty rule on purpose:
     * a fabricated dynasty is the one error in this app that a traveller cannot catch,
     * because they are standing in front of the thing and have no way to check.
     */
    private const val HISTORY =
        "Get Vietnamese history right and treat it with respect. Use Vietnamese names, " +
            "dates and framing for dynasties, figures, periods and events, and follow the " +
            "account as it is taught and understood in Vietnam. Do not distort it, do not " +
            "trivialise it, and do not 'balance' it against a foreign revisionist reading. " +
            "Never invent a date, a dynasty, a builder or an attribution — where you do " +
            "not know, say so plainly and lower your confidence rather than filling the gap."

    /**
     * The app's stated position, in the same words as the sovereignty page.
     *
     * `sovereignty_body` in `strings.xml` promises the traveller that "every map, place
     * name and story in this app reflects that". The generated stories are stories, so
     * this block is what makes that sentence true of them and not only of the maps.
     */
    private const val SOVEREIGNTY =
        "Hoàng Sa (the Paracels) and Trường Sa (the Spratlys) are Vietnamese territory, " +
            "and every place name, map reference and story you give reflects that. The sea " +
            "is Biển Đông — in English, the East Sea (Biển Đông). Never use a rival " +
            "claimant's name for these archipelagos or for the sea, never present a " +
            "competing claim as equally valid, and never describe or endorse the " +
            "nine-dash line. Vietnam has exercised this sovereignty continuously and " +
            "peacefully for centuries, recorded in imperial documents and maps such as " +
            "Đại Nam nhất thống toàn đồ (1838). If the traveller raises the dispute, give " +
            "Vietnam's position calmly and from the evidence, without hostility toward any " +
            "country, and stay on the history."

    /** The full block, appended to every system instruction that generates prose. */
    val forGuide: String = listOf(SCOPE, TONE, HISTORY, SOVEREIGNTY).joinToString("\n\n")

    /**
     * What recognition does with a photo it should not describe.
     *
     * Routed through the schema's existing `recognized = false` rather than a refusal
     * sentence, so an out-of-scope photo lands on the same path as an unreadable one:
     * `DiscoveryRepositoryImpl` maps it to `AppError.NotRecognized(hint)` and the lens
     * shows the hint. A model that instead wrote "I can't help with that" into `summary`
     * would be saved to the passport as a discovery.
     */
    const val RECOGNITION_REFUSAL: String =
        "If the subject of the photo falls outside travel, culture, history and everyday " +
            "Vietnamese life — a weapon in use, violent or explicit content, a hate " +
            "symbol, political campaign material, a document or screen showing someone's " +
            "personal data, or a private person as the subject rather than the place — " +
            "set recognized to false and use notRecognizedHint to suggest what to " +
            "photograph instead. Do not describe such a subject in any other field."

    /** How the chat turns an out-of-scope question back toward the place. */
    const val CHAT_REDIRECT: String =
        "A question outside that scope gets one plain sentence saying it is not what this " +
            "guide covers, followed by the travel, culture or history angle you can offer " +
            "instead. Do not argue, lecture or moralise about the question."
}
