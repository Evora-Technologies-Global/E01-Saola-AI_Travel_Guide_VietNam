# Store listing — Saola 1.0.0 (English)

Source of truth for the App Store and Google Play text. Every claim below is a feature that
ships in 1.0.0 — see [`../README-en.md`](../README-en.md) §2 and §7.

---

## 1. Google Play

### App name (30 chars max)

```
Saola: AI Travel Guide Vietnam
```

### Short description (80 chars max)

```
Point your camera at Vietnam. AI names it, tells its story, keeps your journey.
```

### Full description (4000 chars max)

```
Saola turns your phone camera into a knowledgeable local guide for Vietnam.

Just hold your phone up. Saola recognises the temple, the dish or the sign in front of you, explains what it is and why it matters, translates it, and answers your questions — on one screen, in your own language.

And everything you photograph is kept. Your pictures fill in a map of Vietnam, unlock a board of things worth finding, and turn each day into a page of a travel journal.


POINT AND KNOW

• Five camera modes — Auto, Heritage, Food, Museum and Translate
• Recognises landmarks, temples, dishes and artefacts
• Every result is structured, and says how confident the AI is
• Ask follow-up questions; the conversation is remembered
• Hear it read aloud in your language, hands free
• Or import a picture you already took

MENUS AND SIGNS, TRANSLATED IN PLACE

• Text recognised on your device, then translated
• The translation is drawn over each line of the original
• Built for menus, museum labels and street signs

TRAVEL PASSPORT

• Vietnam drawn as its 34 provinces, Hoàng Sa and Trường Sa included
• Each province fills in with the photograph you took there, clipped to its real outline
• No check-in button — photographing a pagoda is the check-in

CULTURE COLLECTION

• 61 things worth finding — phở, bánh chưng, a basket boat, a gong
• Each tile stays blank until you photograph the real thing
• Guide mode spells out what to look for before you go, not just a name

EXPLORE WHAT IS NEARBY

• A live map and ranked list of what is worth walking to within 5 km
• Built on OpenStreetMap and Wikipedia, ranked by how much people read about a place
• No invented star ratings — if the open data carries no number, we do not either
• Place names in your language, with the Vietnamese name kept underneath

TRAVEL JOURNAL

• Everything you photographed, grouped by day
• An AI-written summary of each day, and ideas for tomorrow
• Add your own notes to any discovery

EIGHT LANGUAGES

Vietnamese, English, Japanese, Korean, Chinese, French, Spanish and Thai — interface, answers and narration follow your device.

PHONES AND TABLETS

Phones get a tab bar; tablets get a rail and two panes — read on one side, ask the guide on the other.

YOUR TRIP STAYS YOURS

• No account and no sign-up — open the app and use it
• Photographs, journal, notes and conversations live on your device, not on a server of ours
• We do not sell, rent, trade or share your data with anyone, at any price
• Your photographs, journal and notes are never used to target anything at you
• A photo leaves your device only while it is being recognised; your location only finds what is nearby
• Erase everything permanently, in two taps, whenever you want
• Your journal reads offline; recognition and nearby places need a connection

WE NEVER SEE YOUR CARD

This version is free — it has no purchases and no subscriptions. Anything you buy in a later version goes through the store's own checkout, so your card details stay with Google or Apple. Saola connects to no bank, no e-wallet and no payment provider of its own, and will never ask you for a card number, a bank password, a PIN or an OTP. Any screen claiming to be Saola that asks for one is not us.

WRITTEN WITH RESPECT

• Vietnamese history treated with care — where the AI does not know, it says so instead of inventing
• War sites like Hỏa Lò, Côn Đảo and the Củ Chi tunnels are told as remembrance — never glorified, never graphic
• Nothing here promotes war, weapons, hatred or hostility between any countries, regions, peoples or faiths
• No adult content, no gambling, no messaging between users — no way for a stranger to reach anyone through this app

For visitors to Vietnam, for Vietnamese travelling their own country, and for anyone who ever wished someone would explain what they are looking at.

Requires Android 8.0 or later.

Data © OpenStreetMap (ODbL), Natural Earth, Wikipedia (CC BY-SA), Wikimedia Commons. Credits in Settings › About › Licences.
```

---

## 2. App Store

### App name (30 chars max)

```
Saola — Vietnam Travel Guide
```

### Subtitle (30 chars max)

```
Your AI guide to Vietnam
```

### Promotional text (170 chars max, editable without review)

```
Point your camera at a temple, a bowl of noodles or a menu. Saola names it, tells the story behind it, translates it — and keeps every photo in your travel journal.
```

### Keywords (100 chars max, comma separated, no spaces)

```
vietnam,travel,guide,translate,menu,landmark,camera,culture,food,heritage,museum,map,journal,scan
```

### Description (4000 chars max)

Same body as the Google Play full description (3992 chars), with the one requirement line
replaced — keep it short, the body has only a few characters of headroom and App Store Connect
counts iPad support from the build's metadata anyway:

```
Requires iOS 16.0 or later.
```

---

## 3. What's New — 1.0.0

```
The first release of Saola.

• Point your camera at a landmark, a dish or an artefact and get its story
• Five modes: Auto, Heritage, Food, Museum and Translate
• Menus and signs translated in place, over the original text
• Ask follow-up questions and hear the answers read aloud
• A travel passport across Vietnam's 34 provinces, filled in with your own photographs
• A culture collection of 61 things worth finding
• Explore what is worth walking to within 5 km
• A travel journal with an AI summary of each day
• Eight languages, following your device
• Phone and tablet layouts
```

---

## 4. Notes for whoever fills the store forms

**The privacy policy and terms live on the company website**, and the app links to them from
Settings › About. The two URLs are constants in
`shared/src/commonMain/kotlin/com/evora/technologies/saola/feature/settings/LegalLinks.kt`;
whatever goes into the store forms must be those same two URLs.

```
https://evoratech.vercel.app/privacy
https://evoratech.vercel.app/terms
```

> **The GitHub Pages copies answered 404** — Pages hands every unknown path to `404.html`, so the
> page rendered in a browser while the status line said "not found", which the Play Console's URL
> check reads and rejects. Moved to the Vercel deployment on 07.08.2026; **both paths verified
> 200**, and `LegalLinks.kt` updated to match. If the site moves again, that file and both store
> forms move with it.
>
> Two things still to eyeball in a browser, because the pages are client-rendered and a command
> line cannot see the result: that the policy text actually appears (the site loads its legal
> content at runtime and has a `legal.loadError` state), and that it **describes Saola's own data
> practices** — camera and photos going to Google Gemini, coarse location going to OpenStreetMap
> and Wikipedia, on-device storage, no account, in-app deletion. A generic company policy that
> does not cover those is a Play policy violation even though the URL answers 200.

**What the store forms want the policy to cover** — the description below already states each of
these to the traveller, so the policy page must not contradict any of them: no data is sold; the
app's own content is never used to target anything; there is no account and no server of ours;
photos go to Google Gemini
only at the moment of recognition; coarse location goes to OpenStreetMap and Wikipedia; nothing
touches a bank or a payment provider; everything can be deleted in-app, instantly.

**Data safety (Play) / App Privacy (App Store)** — declare against what the app actually does:

| Type | Collected | Shared | Why |
|---|---|---|---|
| Photos | No¹ | Yes — Google (Gemini) | Sent only at the moment of recognition, not stored by us |
| Location (approximate + precise) | No¹ | Yes — Overpass / Wikipedia / Gemini prompt | Nearby places, and stamping a photo onto the right province |
| App activity, personal info, contacts, identifiers | No | No | There is no account and no identifier |
| Analytics, crash logs | No | No | No such SDK is in the dependency list |
| Advertising, device IDs | No | No | True of the 1.0.0 build only. **An ads SDK flips both to Yes** — see §5 |
| Financial info | No | No | 1.0.0 has no payment code, no billing library and no bank integration. **Re-answer this row the moment billing ships** — see §5 |

¹ "Collected" in store terms means transmitted off the device **and retained**. Saola retains
nothing on a server — there is no server. Photos, journal, notes and conversations live in the
app's own storage on the device. Both forms also want: **data is encrypted in transit** (yes,
HTTPS everywhere) and **users can request deletion** (yes — Settings › Clear all discoveries,
instant, no request needed).

**Permissions to explain in the listing / usage strings:** Camera (to recognise what you point
at), Photo library (to import a picture you already took), Location (to place a photo on the
passport and to find what is nearby). All three are asked for at the moment they are needed.
The iOS usage strings already in `Info.plist` say the same thing and must not drift from this.

**Content rating questionnaire** — the answers, all of which the app can back up:

| Question | Answer |
|---|---|
| Violence, sexual content, nudity, profanity | None. AI output is constrained to travel, culture and history; adult content is refused |
| Horror, gambling, drugs, alcohol, tobacco | None |
| Users can interact / share content with each other | **No** — no accounts, no profiles, no messaging, no comments |
| Shares user location with other users | **No** |
| Unrestricted web access | **No** — no in-app browser. Outbound links are the fixed licence and policy URLs, plus a *Directions* handoff to the device's maps app |
| Purchases, ads | **None in 1.0.0.** Both stores re-ask this when a build first carries ads or billing — see §5 |
| War / conflict content | War memorials and history sites appear, treated as remembrance — factual, non-graphic, non-glorifying. The rule is in the code: `GeminiGuardrails.TONE` |

Expect **3+ (Play) / 4+ (App Store)**. Both stores also require a child-safety answer: the app is
not directed at children, has no social surface, and no way for a stranger to reach a user.

**Do not claim in the listing:** offline recognition (recognition needs a network), star ratings
for places (deliberately absent), or a photograph for every nearby place (most OSM places carry
none).

**Review risk to know about before submitting:** the app states Vietnam's sovereignty over Hoàng
Sa and Trường Sa, on the map and in a dedicated in-app page. That is deliberate and is argued
from the historical record without hostility to any country (`GeminiGuardrails.SOVEREIGNTY`, and
`sovereignty_body` in `strings.xml`) — but it is territorially sensitive, and a realistic cause of
rejection or takedown in some regional storefronts, China's in particular. Decide the storefront
list with that in mind rather than being surprised by it.

---

## 5. When ads and in-app purchases ship

Both are planned for a version after 1.0.0. **A store listing that still says "no ads" next to a
"Contains ads" badge is a metadata mismatch, and both stores reject on it** — so the listing
changes in the *same* release that carries the code, not after.

The legal page in [`../website/`](../website/) has already been written for this: it
describes advertising as something the app may show, names what an ad request sends, and keeps
no absolute "no ads" or "no purchases" promise anywhere. It needs no edit when either ships.

### 5.1 What is safe to promise forever, and what is not

| Claim in the description | Status |
|---|---|
| "We never see your card" / "never asks for a card number, bank password, PIN or OTP" | **Stays true**, provided every purchase goes through Google Play Billing or StoreKit. Those run the checkout inside the store's own UI; the app is handed a receipt, never a payment instrument. It stops being true the moment a card form appears in a WebView — do not build one |
| "Saola connects to no bank, e-wallet or payment provider of its own" | **Stays true** with Play Billing / StoreKit; false if you integrate MoMo, ZaloPay, VNPay or a card gateway directly |
| "We do not sell, rent, trade or share your data" | **Stays true** — selling a feature is not selling data. Keep it |
| "This version is free — it has no purchases and no subscriptions" | **Version-scoped on purpose.** Replace with what the paid version actually offers |
| "Your photographs, journal and notes are never used to target anything at you" | **Stays true** as long as no content from the app is passed to the ad network — which is the whole point of keeping the ads SDK away from the database. Do not weaken it |

The description was written so the first three survive billing untouched, and only the fourth
sentence has to be rewritten.

### 5.2 The checklist for that release

- **Use the store's own billing.** Play requires Google Play Billing for digital goods sold
  inside the app; Apple requires StoreKit. A direct payment gateway for in-app digital items is a
  removal-grade violation on both.
- **Play Data safety:** if you keep any purchase record, "Purchase history" becomes collected.
  Play Billing's own transaction data is Google's, not yours — declare what *your* app stores.
- **Content rating questionnaires:** both stores ask about digital purchases; re-answer. If items
  are randomised in any way, the loot-box questions apply — the honest answer for fixed-price
  feature unlocks is no.
- **App Store Connect:** each product needs its own metadata, screenshot and review note, and the
  reviewer needs a way to reach the paywall. Sandbox-test **restore purchases** — an app that
  cannot restore is rejected.
- **Privacy policy and terms on the company site** need a purchases section: what is sold, whether
  it is consumable or permanent, how refunds work (the store handles them, and Vietnamese consumer
  law still applies), and how to restore.
- **The in-app legal links** already point at those pages, so nothing in the app changes.
### 5.3 The ads-specific part

- **Play:** tick **"Contains ads"** in the Console — a build serving ads without that declaration
  is a policy violation on its own. Data safety gains **Device or other IDs**, collected *and*
  shared, purpose *Advertising or marketing*.
- **iOS:** if ads are personalised through the IDFA, the app must show the **App Tracking
  Transparency** prompt before requesting it, and App Privacy gains a *Data Used to Track You*
  entry. Declining the prompt must still leave a working app with non-personalised ads.
- **Keep the ads SDK away from the app's own data.** No photo, journal entry, note or discovery
  may reach it. That is what keeps the description's targeting line and the legal page's §2.5
  honest, and it is a code review item, not a promise.
- **Ad placement is a product decision with a review consequence:** an interstitial over the
  camera viewfinder, or an ad that covers the shutter, is both a poor experience and an Apple
  rejection under the interstitial guidelines. Keep ads out of the capture path.
- **The content rating questionnaires re-open** — answer that ads are shown, and keep the ad
  categories restricted so the 3+/4+ rating survives.

- **Do not paywall anything the 1.0.0 listing described as included** without saying so in the
  release notes. Users who installed on the strength of this description will read that as a
  bait-and-switch, and it is the single most common cause of a review-score collapse after a
  monetisation update.
