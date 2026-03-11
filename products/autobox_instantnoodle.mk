#
# AutoBox product definition for OnePlus 8 (instantnoodle)
#
# Strategy: inherit from lineage_instantnoodle to get all device tree,
# vendor blobs, hardware namespaces, and lineage configs intact.
# Then apply AutoBox customizations on top.
#

# Inherit everything from lineage_instantnoodle
$(call inherit-product, device/oneplus/instantnoodle/lineage_instantnoodle.mk)

# AutoBox customizations (branding, version, font, overlays)
$(call inherit-product, vendor/autobox/config/autobox_common.mk)

# Override product identity
PRODUCT_NAME := autobox_instantnoodle

# Ensure all hardware/vendor namespaces from SM8250 platform are present.
# These are normally loaded via roomservice dependency chain for lineage_*
# products but need to be explicit for custom product names.
PRODUCT_SOONG_NAMESPACES += \
    hardware/qcom-caf/sm8250 \
    hardware/qcom-caf/bootctrl \
    hardware/qcom-caf/thermal-legacy-um \
    hardware/qcom-caf/wlan \
    hardware/qcom-caf/wlan/qcwcn \
    vendor/qcom/opensource/commonsys/display \
    vendor/qcom/opensource/commonsys-intf/display \
    vendor/qcom/opensource/display \
    vendor/qcom/opensource/data-ipa-cfg-mgr-legacy-um \
    vendor/qcom/opensource/dataservices
