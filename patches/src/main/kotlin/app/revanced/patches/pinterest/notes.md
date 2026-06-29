Pinterest 14.23.0 (versionCode 14238020)

## Ad Architecture

### Google Mobile Ads (GMA)
- App ID: ca-app-pub-7687027632798059~1192996890
- SDK version: 0.24.0-beta01 (ads_mobile_sdk)
- GMA controls both banner ads and native ads (rendered as pins)

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

### Patch Strategy
Block ao0.a.j() -> return false always:
- Prevents GMA SDK from initializing (pz.j.c returns early)
- Prevents all GMA ad loading (pz.j.e and pz.j.f return early)
- Safe: callers already handle false return with early exit

### Not Blocked
- Promoted pins served directly by Pinterest API (first-party monetization)
- These go through the main pin feed rendering, not GMA pipeline
