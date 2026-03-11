# vendor/autobox

AutoBox ROM — product overlay on top of LineageOS 23.2 (Android 16).

## Structure

```
vendor/autobox/
├── AndroidProducts.mk          # Registers autobox products for lunch
├── build.sh                    # Build entry point
├── build/tasks/bacon.mk        # Produces autobox-*.zip alongside the OTA package
├── config/
│   ├── autobox_common.mk       # Shared product config (branding, flags, overlays)
│   └── version.mk              # Version variables (AUTOBOX_VERSION)
├── products/
│   └── autobox_instantnoodle.mk  # Device product definition (inherits lineage_instantnoodle)
└── apps/
    └── AutoBoxLauncher/        # Home launcher (Compose, TomTom Navigation SDK 2.1.2)
```

## Devices

| Device | Codename | SoC |
|--------|----------|-----|
| OnePlus 8 | instantnoodle | Snapdragon 865 (SM8250) |

## Build

```bash
source build/envsetup.sh
vendor/autobox/build.sh                          # defaults: instantnoodle userdebug bp4a
vendor/autobox/build.sh instantnoodle userdebug  # explicit
```

Or manually:

```bash
source build/envsetup.sh
lunch autobox_instantnoodle-bp4a-userdebug
export LINEAGE_BUILD=instantnoodle
mka autobox
```

> `LINEAGE_BUILD` must be set explicitly — Lineage's lunch only sets it for `lineage_*` products.
> Without it, `vendor/lineage/config/BoardConfigLineage.mk` is not included and the kernel config is missing.

Output: `out/target/product/instantnoodle/autobox-instantnoodle-<version>.zip`

## TomTom API Key

AutoBoxLauncher requires a TomTom Navigation SDK API key at build time.
Place the key (single line, no newline) in:

```
vendor/autobox/apps/AutoBoxLauncher/tomtom.key
```

## Philosophy

Do not modify LineageOS / AOSP / Qualcomm sources. All AutoBox-specific changes live in `vendor/autobox/`.
