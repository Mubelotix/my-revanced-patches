Pinterest 14.23.0 (versionCode 14238020)

## Ad Architecture

### Google Mobile Ads (GMA)
- App ID: ca-app-pub-7687027632798059~1192996890
- SDK version: 0.24.0-beta01 (ads_mobile_sdk, ~3172 obfuscated smali files)
- GMA controls both banner ads and native ads (rendered as pins)
- Uses Open Measurement (IAB OM SDK), Privacy Sandbox AdServices API
- Google Play Services ads-identifier dependency

### Gatekeeper Flow
```
pz.j.d()                        # Master gate: rz.g.f123390e && ao0.a.j()
  ├── rz.g.f123390e             # Set true after GMA SDK init completes
  └── ao0.a.j()                 # Experiment check "android_ad_gma_new"
       └── ao0.i0.n("android_ad_gma_new", "enabled", ...)

pz.j.e()  # Banner ad load -> calls d() first, returns if false
pz.j.f()  # Native ad load -> calls d() first, returns if false
pz.j.c()  # GMA SDK init -> calls ao0.a.j() first, returns if false
```

### GMA SDK Init Chain
- `pz.j.c()` -> checks `ao0.a.j()` && consent -> crash backoff -> coroutine launch
- `rz.f.a()` -> sets `rz.g.f123390e = true` on init complete callback
- `t00.c` (adsGmaConfigManager) -> fetches `ThirdPartyAdConfig` from PinService
- `v00.b` (adsConfigRepository) -> caches config on disk with key "THIRD_PARTY_AD_CONFIG"
- Config model: `io` { gma: `jo` { load: Boolean, ad_unit_ids: [...], ttl_seconds, ... } }
- `jo.u()` -> returns `load` field (defaults false) — remote kill switch

### Promoted Pin Detection
Two API fields determine if a pin is promoted:
- `me.I5()` -> field `w1` -> `@bp.b("is_promoted")` -> JSON key "is_promoted"
- `me.q5()` -> field `Q0` -> `@bp.b("is_downstream_promotion")` -> JSON key "is_downstream_promotion"

Both are nullable Boolean getters returning FALSE on null. 

Key check in `iv/c.U(me)`:
```java
return meVar.I5().booleanValue() || meVar.q5().booleanValue();
```

Used in 100+ locations for showing "Shop now" labels, sponsored badges, 
promoted-by attributions, and ad-specific analytics.

`ht/n.java:81` — chooses "Shop now" CTA vs regular variant based on I5():
```java
int i16 = meVar.I5().booleanValue() ? fb0.t0.pin_action_shop_now : fb0.t0.pin_action_variant;
```

### Shop the Pin / Shop the Look
- NOT an ad — Pinterest's organic product matching feature
- `ue.J0(me)` -> checks `me.Z3()` (aggregated pin data) -> `h0.L()` (is_shop_the_look)
- Shows in closeup view via `shop_the_look_module` story type
- Layouts: `shop_the_look_focus_carousel_layout`, `shop_the_look_focus_scene_pin_item`
- Module classes: `n41/d.java` (carousel container), `q41/p.java` (product container)
- String resource "Shop the Pin" — dynamically set by module system

### Feed Cell Types (ro0/c.java)
- `PROMOTED_PIN` = 41 (value 15 from findByValue)
- `DARK_PROMOTED_PIN` = 111 (value 71)
- Various SHOPPING_* types (216, 266-275)
- `VIEW_TYPE_PROMOTED_PIN_VIDEO_AD_EXCLUDE` = 259 (used by video ad creation)

### Third-Party Ad SDKs Present
- `com.google.android.gms.ads` — Google Play Services ads (94 smali files)
- `com.google.android.libraries.ads` — Google internal ads library (11 files)
- `ads_mobile_sdk` — obfuscated GMA internal (3172 files)
- `ad/` package — another obfuscated ad SDK (36 files)
- `adsGmaLibrary`, `adsOpenMeasurement`, `adsWebViewPin` — Pinterest ad wrappers

## Patch Strategy

### Block 1: GMA experiment gate (ao0.a.j)
Fingerprint: strings "android_ad_gma_new", "enabled", returns Z
Effect: return false -> no GMA SDK init, no GMA ad loading
Safe: callers pz.j.c/e/f already handle false with early exit

### Block 2: Promoted pin flags (me.I5, me.q5)
Approach: class-direct lookup (fingerprint with custom{} failed on field ref matching)
Inject: sget-object FALSE; return-object at method start
Effect: all pins appear organic — no "Shop now" CTA, no promoted labels
Note: Must return Boolean.FALSE (not null) — callers do .booleanValue()
      returnEarly(false) returns null for L type, so use addInstructions instead

### Block 3: Shop the Pin module (ue.J0)
Approach: class-direct lookup
Effect: return false -> is_shop_the_look always reports false
Safe: callers already handle false gracefully

## Gotchas

### returnEarly(false) on Boolean getters returns null
`returnEarly()` for `L` return type inserts `const/4 v0, 0; return-object v0` — null.
Callers do `.booleanValue()` on the result -> NPE.
Fix: use `addInstructions` with `sget-object v0, Boolean.FALSE; return-object v0`.

### fingerprint with custom{} field ref matching
Attempted `custom{}` with `FieldReference` matching failed to resolve correctly.
Instead use `classes.find { type == "Ldescriptor;" }` + `proxy().mutableClass.methods.find { name == "..." }`.
This is cleaner and works for any method in any class.

### me.smali is in smali_classes5/classes3.dex
The pin model class switches dex files between original (smali_classes5) and 
patched (classes3.dex). The descriptor `Lcom/pinterest/api/model/me;` works regardless.

### adb install version downgrade
Device may have newer version. Use `adb uninstall` first, then `adb install`.
Data loss unavoidable. Alternatively update compatibleWith version.

## Future Work
- Remove shopping product carousel entirely (organic, not ad)
- Block analytics ad events (extensive logging: 15+ payload types)
- Remove promoted pin cell types from feed adapters
- Block "Why am I seeing this ad" feature (waista/AdsReasonView)
- Remove ad preview feature (android_promoted_pin_preview experiment)
- Block lead generation ads (leadgen module)
- More recent Pinterest versions may have different obfuscation
