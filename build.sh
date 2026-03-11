#!/usr/bin/env bash
#
# AutoBox build script
# Usage: vendor/autobox/build.sh [DEVICE] [VARIANT] [RELEASE]
#   DEVICE  - device codename (default: instantnoodle)
#   VARIANT - build variant: user | userdebug | eng (default: userdebug)
#   RELEASE - release config (default: bp4a)
#
# Example:
#   vendor/autobox/build.sh
#   vendor/autobox/build.sh instantnoodle userdebug bp4a
#

set -e

DEVICE="${1:-instantnoodle}"
VARIANT="${2:-userdebug}"
RELEASE="${3:-bp4a}"
TARGET="autobox_${DEVICE}"

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "========================================="
echo " AutoBox Build"
echo " Target  : ${TARGET}-${RELEASE}-${VARIANT}"
echo " Root    : ${REPO_ROOT}"
echo "========================================="

cd "$REPO_ROOT"

# Load build environment
# shellcheck source=/dev/null
source build/envsetup.sh

# Select target
lunch "${TARGET}-${RELEASE}-${VARIANT}"

# BoardConfigLineage.mk (which exports kernel vars to Soong) is only included
# when LINEAGE_BUILD is non-empty. Lineage's lunch sets it for lineage_* products
# but clears it for others. Set it explicitly so kati includes the board config.
export LINEAGE_BUILD="${DEVICE}"

# Build the ROM zip (autobox target produces autobox-*.zip)
mka autobox

echo "========================================="
echo " Build finished!"
echo " Output: out/target/product/${DEVICE}/"
echo "========================================="
