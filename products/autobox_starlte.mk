#
# AutoBox product definition for Samsung Galaxy S9 (starlte)
#

# Inherit everything from lineage_starlte
$(call inherit-product, device/samsung/starlte/lineage_starlte.mk)

# AutoBox customizations (branding, version, overlays)
$(call inherit-product, vendor/autobox/config/autobox_common.mk)

# Override product identity
PRODUCT_NAME := autobox_starlte
