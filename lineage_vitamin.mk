#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the custom device configuration.
$(call inherit-product, device/oneplus/vitamin/device.mk)

# Inherit from the LineageOS configuration.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_BRAND := OnePlus
PRODUCT_DEVICE := vitamin
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_MODEL := PHP110
PRODUCT_NAME := lineage_vitamin
PERF_ANIM_OVERRIDE := true

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="sys_mssi_64_cn_armv82-user 15 AP3A.240617.008 1761016969111 release-keys" \
    BuildFingerprint=OnePlus/PHP110/OP5927:15/AP3A.240617.008/T.207f82b-1_1:user/release-keys \
    DeviceName=OP5927 \
    DeviceProduct=PHP110 \
    SystemDevice=OP5927 \
    SystemName=PHP110
