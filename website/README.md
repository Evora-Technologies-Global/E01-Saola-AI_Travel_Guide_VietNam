# `website` — the legal pages Saola links to

One self-contained HTML file holding **both** the privacy policy and the terms of service, in
**Vietnamese and English**. It is the page a traveller reaches from *Settings › About › Privacy
policy* and *› Terms of service*, and the URL both store submission forms must carry.

```
website/
├── index.html     the page. No build step, no dependency, no external request
├── logo.png       the app icon at 128 px, from app/src/main/assets/logo_app.png
├── vercel.json    maps /privacy and /terms onto it — all three answer 200
└── README.md      this file
```

The page wears **the app's own palette** — lacquer red, temple gold, warm sand neutrals — read
straight off `shared/.../core/designsystem/theme/Color.kt` and `Theme.kt`, light scheme and dark
scheme both. Each custom property in `index.html` carries the Kotlin name it came from, so a
change to the app's theme can be mirrored here without guessing which red was which.

`logo.png` is `sips -Z 128` of the 1254 px original — 27 KB against 1.4 MB, and the header draws
it at 34 px. Re-run that when the icon changes:

```bash
sips -s format png -Z 128 app/src/main/assets/logo_app.png --out website/logo.png
```

---

## 1. Why one file and not two

The page carries two documents behind two tabs, and **picks the tab from the URL it was opened
on**: land on `…/terms` and it opens Terms, anywhere else it opens Privacy. So one file answers
both paths, and a fix to a shared paragraph is made once.

With JavaScript disabled — which is how a policy crawler often reads a page — the tabs and the
language switch stop hiding anything, and **both documents in both languages are in the HTML**.
That is deliberate: the Play Console needs to find the text, not run a script to see it.

---

## 2. Deploying it

### Option A — its own Vercel project (what `vercel.json` is for)

```bash
cd website
vercel deploy --prod
```

The page is `index.html`, so `/` serves it with no configuration at all; `vercel.json` only adds
`/privacy` and `/terms` on top. All three answer **200**. Point `LegalLinks.kt` at whatever domain
the project gets.

**Framework Preset must be `Other`.** Picking a framework by hand makes Vercel run that
framework's build — `react-scripts build` for Create React App — against a folder with no
`package.json`, and it fails with `command not found` and exit 127. There is nothing to build
here; Vercel only has to copy the files up. `vercel.json` now pins `"framework": null` and
`"outputDirectory": "."`, and those override the dashboard, so the preset cannot break a
deployment again.

**If a fresh deployment 404s, it is almost always the Root Directory.** Deploying the repository
without setting it to `website` gives Vercel an Android project: no `index.html` at the root, no
`vercel.json`, and therefore a 404 on every path. Either set *Project Settings › Build & Development
Settings › Root Directory* to `website`, or run `vercel deploy --prod` from inside this folder so
the folder itself is the deployment root.

The destination of a rewrite must never be a `.html` path while `cleanUrls` is on — that option
makes `/x.html` redirect to `/x`, so a rewrite pointing at `/x.html` lands on a redirect instead of
a file. That is why both rewrites here target `/`.

### Option B — dropped into the existing company site

Copy `index.html` into that project's `public/` directory twice:

```
public/privacy/index.html
public/terms/index.html
```

Two identical copies is the cost of not touching that site's routing. If the site is on Vercel,
prefer adding the same two rewrites to its own `vercel.json` and keeping one copy.

**Do not host it on GitHub Pages at a path with no file behind it.** Pages answers every unknown
path with `404.html` *and a 404 status*. The page renders, a human sees it, and the Play Console's
URL check rejects it — that is exactly what happened to the previous `…github.io/…/privacy`.

---

## 3. The URLs are compiled into the app

`shared/src/commonMain/kotlin/com/evora/technologies/saola/feature/settings/LegalLinks.kt` holds
`PRIVACY_POLICY_URL` and `TERMS_OF_SERVICE_URL` as constants. They ship inside the binary.

**Changing where this page lives means changing that file and shipping a new build.** Every copy
of the app already installed keeps opening the old URL forever. So decide the final domain before
the release build, and once an app is published, keep that URL alive even if the site moves —
redirect it rather than delete it.

Both store forms carry the same two URLs. When they change, three places change together: this
folder, `LegalLinks.kt`, and both store listings.

---

## 4. What the text is allowed to say

Every claim in `index.html` was written against what the code actually does, and a few of them
are load-bearing:

| The page says | What has to stay true |
|---|---|
| Photos and journal never reach the ad network | The ads SDK must never be handed a capture, a discovery or a note |
| We never ask for a card number, PIN or OTP | Any paid feature goes through Play Billing / StoreKit, never a card form in a WebView |
| The app connects to no bank or payment provider | No direct MoMo / ZaloPay / VNPay / card-gateway integration |
| Recognition refuses out-of-scope subjects | `GeminiGuardrails.RECOGNITION_REFUSAL` in `:data` |
| War sites are told as remembrance, never glorified | `GeminiGuardrails.TONE` |
| The app never invents a date or a dynasty | `GeminiGuardrails.HISTORY` |
| No account, no messaging, no user-to-user contact | Stays true only while the app has no social feature |

The page deliberately carries **no version numbers and no "in this release" wording**, so shipping
advertising or in-app purchases needs no edit here. It does need edits in
[`../docs/store-listing.md`](../docs/store-listing.md) §5.

---

## 5. Editing it

Plain HTML, inline CSS and about 40 lines of JavaScript. No framework, no build.

- Every translatable string is a pair: `<span data-l="vi">…</span><span data-l="en">…</span>`.
  **Both must exist**, or the page goes blank in one language. A quick check:

  ```bash
  grep -c 'data-l="vi"' website/index.html
  grep -c 'data-l="en"' website/index.html   # the two numbers must match
  ```

- Section numbers are written by hand in the headings. Renumbering one means fixing the
  cross-references in the prose (`mục 2.5`, `section 9`, …).
- Colours, spacing and the dark theme are CSS custom properties at the top of the file.
- Update the effective date in three places when the text changes materially: both `.meta` lines
  and the footer.
