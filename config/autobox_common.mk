# AutoBox common product configuration
# Included by all AutoBox product makefiles

$(call inherit-product, vendor/autobox/config/version.mk)

# Branding
PRODUCT_BRAND := AutoBox
PRODUCT_MANUFACTURER_OVERRIDE :=

# Compute version independently here (inherit-product variable scoping can be unreliable).
# AUTOBOX_VERSION_MAJOR/MINOR are plain strings; BUILD_DATE uses $(shell).
_AUTOBOX_BUILD_DATE := $(shell date -u +%Y%m%d)
ifndef AUTOBOX_BUILDTYPE
    AUTOBOX_BUILDTYPE := UNOFFICIAL
endif
_AUTOBOX_VER := 1.0-$(_AUTOBOX_BUILD_DATE)-$(AUTOBOX_BUILDTYPE)

# Override Lineage version variables so ro.lineage.* properties show AutoBox branding.
# PRODUCT_PRODUCT_PROPERTIES keeps $(LINEAGE_VERSION) as a lazy reference — overriding
# the variable here changes the final property value without adding a duplicate key.
LINEAGE_VERSION := AutoBox-$(_AUTOBOX_VER)
LINEAGE_DISPLAY_VERSION := AutoBox-$(_AUTOBOX_VER)

PRODUCT_PRODUCT_PROPERTIES += \
    ro.build.display.id=AutoBox-$(_AUTOBOX_VER) \
    ro.autobox.enabled=false

# AutoBox overlays
$(call inherit-product, vendor/autobox/overlay/autobox_overlays.mk)

# AutoBox apps — populated as apps are added
# $(call inherit-product-if-exists, autobox/apps/autobox_apps.mk)
