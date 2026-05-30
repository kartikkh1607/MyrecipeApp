# MealTime — Design System

**Direction A · "Linen"** · Editorial serif + humanist sans on warm paper, with thin rules and generous whitespace. The whole app was rebuilt to this spec in commit `b911b75` (May 30 2026); this document is the canonical text reference for the tokens.

Tokens live in `app/src/main/java/com/kartik/mealtime/ui/theme/` — `Color.kt`, `Type.kt`, `Theme.kt` (shapes + color schemes). When in doubt, read those files; this doc explains the *intent* behind the values.

---

## 1. Brand & philosophy

- **Aesthetic**: warm, editorial, premium. Paper-like backgrounds, forest-green primary, terracotta accent, hair-thin rules.
- **Principles**: tight radii, restrained elevation, deliberate whitespace, no gradients unless they earn their place (only on the gradient AI hero, the gradient chat avatar, and the shopping-list progress hatch).
- **Dark mode**: "aged linen in shadow" — same warm tone, just unlit. No cold blue.

---

## 2. Color tokens

### Light — warm paper

| Token | Hex | Role |
|---|---|---|
| `Linen` | `#F4EEE5` | Background — warm paper |
| `White` | `#FFFFFF` | Surface — clean white |
| `LinenSurface2` | `#FBF7F0` | Subtle raised / inset surface |
| `Graphite` | `#23201B` | Text primary — warm near-black |
| `Stone` | `#7C756A` | Text secondary — warm muted gray |
| `LinenFaint` | `#A79E91` | Faintest text (placeholders, hints) |
| `ForestGreen` | `#2D5A4E` | **Primary** — elegant deep green |
| `OnForest` | `#FBF8F2` | On-primary text (warm white) |
| `ForestQuiet` | `#E6EEE9` | Primary container / quiet green |
| `ForestDeep` | `#1F4136` | Deepest green (emphasis, gradients) |
| `Terracotta` | `#BC6B30` | Accent — warm terracotta |
| `TerracottaQuiet` | `#F4E7D9` | Accent container |
| `StarGold` | `#C8861C` | Ratings / stars |
| `Heart` | `#C0492F` | Saved / favorite / destructive |
| `LightGray` | `#E8E0D3` | Outline / thin rules |

### Dark — aged linen in shadow

| Token | Hex | Role |
|---|---|---|
| `Midnight` | `#17140D` | Background |
| `DarkSurface` | `#251F16` | Surface — raised warm charcoal |
| `DarkSurface2` | `#322A1E` | Subtle raised / inset surface |
| `Cream` | `#F3EEE4` | Text primary — warm off-white |
| `MutedGray` | `#A89E8B` | Text secondary — warm gray |
| `DarkFaint` | `#7C7361` | Faintest text |
| `Teal` (sage) | `#8FCBAE` | **Primary** — luminous sage |
| `OnSageDark` | `#0D1A13` | On-primary text |
| `SageQuietDark` | `8FCBAE @ 16%` | Primary container |
| `SageDeepDark` | `#B4DEC8` | Deepest sage (emphasis) |
| `TerracottaDark` | `#E9AE66` | Accent — warm glowing amber |
| `TerracottaQuietDark` | `E9AE66 @ 18%` | Accent container |
| `StarGoldDark` | `#E7BB5E` | Ratings / stars |
| `HeartDark` | `#E78C70` | Saved / favorite |
| `LineDark` | `F4E9D2 @ 13%` | Outline — warm paper-tinted hairline |

### Semantic

| Token | Light | Dark |
|---|---|---|
| Success | `#3C7A4E` | `#85C89B` |
| Error | `#C0492F` | `Heart` (shared) |

Color schemes are wired in `Theme.kt` as `LinenLightColorScheme` / `LinenDarkColorScheme` — they replace the prior `PremiumLight/Dark` schemes wholesale.

---

## 3. Typography

Two Google Fonts loaded via `GoogleFont.Provider`:

- **Newsreader** — editorial serif. Display + headline only (screen titles, hero copy). Weights: 400 / 500 / 600 / 700. Display tracking is gently negative (≈ −0.01em) per the Linen direction; serif weights use editorial **Medium (500)** rather than Bold for an editorial feel.
- **Hanken Grotesk** — clean humanist sans. Titles, body, labels, all UI chrome. Weights: 400 / 500 / 600 / 700 / 800.

Mapping to Material 3 type scale lives in `Type.kt`. Rule of thumb:

| Slot | Family | Why |
|---|---|---|
| `displayLarge` / `displayMedium` / `displaySmall` | Newsreader | Hero headings |
| `headlineLarge` / `headlineMedium` / `headlineSmall` | Newsreader | Screen titles, section headers |
| `titleLarge` / `titleMedium` / `titleSmall` | Hanken Grotesk | Card titles, component labels |
| `bodyLarge` / `bodyMedium` / `bodySmall` | Hanken Grotesk | Body copy |
| `labelLarge` / `labelMedium` / `labelSmall` | Hanken Grotesk | Buttons, chips, captions |

---

## 4. Shape

A tighter Linen radius scale (in `Theme.kt`) replaces Material's default soft 12dp rounding:

| Token | Radius | Used on |
|---|---|---|
| `sm` | 9 dp | Chips, small pills, inset icon tiles |
| `md` | 13 dp | Cards, surfaces, text fields |
| `lg` | 18 dp | Hero cards, larger surfaces |
| `xl` | 26 dp | Sheets, full-screen modals |
| `pill` | 999 dp | Search pills, segmented controls, FAB-style buttons |

Borders are **0.5–1.0 dp** hairlines at ≈ 13–20% outline alpha. Cards default to **bordered Surface** rather than elevated Card — elevation is reserved for the sticky cooking bar, snackbars, and the gradient AI hero.

---

## 5. Motion

Spring-physics tokens (defined where they are used — there is no central `Motion.kt`):

- `dampingRatioMediumBouncy` for screen-level transitions
- `dampingRatioLowBouncy` for press / scale feedback
- `stiffnessMedium` / `stiffnessHigh` paired accordingly

Cooking-mode progress segments animate width per step; the recipe-of-the-week hero uses an auto-scroll pager with spring-snap. Featured image fades use a 250ms cubic ease.

---

## 6. Layout & spacing

- **Macro spacing** between sections: 20 / 24 / 32 dp
- **Micro spacing** inside components: 8 / 12 / 16 dp
- **Edge-to-edge** everywhere via `enableEdgeToEdge()`; screens consume `statusBarsPadding` / `navigationBarsPadding` themselves
- **Hairline dividers** at 0.5–1.0 dp with ≈10–15% outline alpha — never solid lines

---

## 7. Per-screen highlights

Each screen was rebuilt against the official "MealTime Redesign" prototype in `_design_handoff/`. Notable per-screen calls:

- **Splash** — forest-green background + diagonal hatch + white badge + serif wordmark
- **Auth** — photo hero with overlapping Linen card; Google button stacked above email/password
- **Home** — paper header, search pill, "Recipe of the week" editorial hero, carousel, quick actions, browse-by-mood chips, today's pick, recently viewed
- **Recipe Detail** — lean hero with paper title block below, 4-cell meta strip, nutrition grid, segmented tabs, ingredient check-off rows, method step rows, sticky bag + start-cooking bar
- **Cooking Mode** — equal-width per-step progress segments
- **Shopping List** — primary-green progress card with diagonal hatch + ring
- **Meal Planner** / **AI Creations** — gradient AI banner ("Auto-plan your week" / hero + Generate/Remix/Plan tool buttons row)
- **Chat** — header "AI Chef" + green-dot "Always ready"; gradient avatar
- **Settings** — muted uppercase section labels, bordered Linen group cards, 34dp radius-sm icon tiles, hairline row dividers; Theme picker as three cards
- **Profile** — muted section labels matching Settings, identity row at 22sp serif, bordered Surface group cards

---

## 8. Accessibility

- All text/background pairs target WCAG AA. Dark-mode StatTile / AboutRecipeRow specifically flipped to `primary / onPrimary` because `primary-on-primaryContainer` measured 1.4:1.
- Touch targets ≥ 48 dp everywhere interactive.
- Every interactive image / icon button supplies a `contentDescription`.
- Speech recognition (cooking mode) requires the OS mic permission and is announced via accessible status text.
