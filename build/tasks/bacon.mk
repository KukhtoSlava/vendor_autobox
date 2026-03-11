# AutoBox: produce autobox-*.zip alongside the lineage-*.zip built by
# vendor/lineage/build/tasks/bacon.mk (which sorts after this file).
#
# We cannot redefine 'bacon' here (lineage/bacon.mk runs after us and wins).
# Instead, hook into $(INTERNAL_OTA_PACKAGE_TARGET) directly.

ifeq ($(filter autobox_%,$(TARGET_PRODUCT)),$(TARGET_PRODUCT))

SHA256 := prebuilts/build-tools/path/$(HOST_PREBUILT_TAG)/sha256sum

AUTOBOX_TARGET_PACKAGE := $(PRODUCT_OUT)/autobox-$(LINEAGE_VERSION).zip

$(AUTOBOX_TARGET_PACKAGE): $(INTERNAL_OTA_PACKAGE_TARGET)
	$(hide) ln -f $(INTERNAL_OTA_PACKAGE_TARGET) $(AUTOBOX_TARGET_PACKAGE)
	$(hide) $(SHA256) $(AUTOBOX_TARGET_PACKAGE) | sed "s|$(PRODUCT_OUT)/||" > $(AUTOBOX_TARGET_PACKAGE).sha256sum
	@echo "AutoBox Package: $(AUTOBOX_TARGET_PACKAGE)" >&2

.PHONY: autobox
autobox: $(AUTOBOX_TARGET_PACKAGE) $(DEFAULT_GOAL)

endif
