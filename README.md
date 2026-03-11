# AutoBox

Custom ROM on top of LineageOS 23.2 (Android 16).

## Structure

```
autobox/
├── apps/        # Custom AutoBox applications (launcher, speed service, etc.)
├── overlays/    # Resource overlays (branding, colors, strings)
├── configs/     # Runtime configs, permissions, SELinux policies
├── docs/        # Design docs, references
└── build.sh     # Build script
```

## Devices
- OnePlus 8 (instantnoodle) — primary
- Samsung Galaxy S9 (starlte) — secondary

## Build

```bash
./autobox/build.sh instantnoodle userdebug
```

## Philosophy
Minimize changes to LineageOS source. Use:
- `vendor/autobox/` for product definitions and build config
- `autobox/overlays/` for resource overrides
- `autobox/apps/` for new apps added to the product
