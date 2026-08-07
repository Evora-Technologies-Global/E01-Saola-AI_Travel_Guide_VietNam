package com.evora.technologies.saola.feature.settings

/**
 * The two documents the app publishes about itself, and the whole of their configuration.
 *
 * They are constants rather than a screen, because neither is content this app owns: the
 * privacy policy and the terms are Evora Technologies Global's, they change on a schedule the
 * app knows nothing about, and a copy bundled into the binary would be the version that
 * shipped rather than the version in force. The rows open them in the browser.
 *
 * **Deliberately one place, and one line each.** A URL typed at the call site would be typed
 * twice — the phone's settings page and the large window's both draw these rows — and the
 * second copy is the one that keeps pointing at last year's document. The same argument
 * `feature/discovery/ReportMail.kt` makes for its single address, and the same one
 * `mobile/feature/licenses/LicensesScreen.kt` makes for its four licence URLs.
 *
 * Both point at the published site. Re-check them when the site moves: a URL that has stopped
 * answering is worse than a dead button, because the traveller has left the app to find out.
 *
 * **Moved off GitHub Pages on 07.08.2026, and the reason is the status code rather than the
 * page.** Pages serves a single-page site by handing every unknown path to `404.html`, so
 * `…github.io/…/privacy` rendered correctly in a browser and answered **404** to everything
 * else. A human reviewer would not have noticed; the Play Console's check on the privacy-policy
 * URL reads the status line, and a 404 there blocks the submission. The Vercel deployment
 * answers 200 on both paths.
 */
internal const val PRIVACY_POLICY_URL = "https://evoratech.vercel.app/privacy"

/** @see PRIVACY_POLICY_URL */
internal const val TERMS_OF_SERVICE_URL = "https://evoratech.vercel.app/terms"
