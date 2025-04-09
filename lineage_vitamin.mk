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
PRODUCT_MODEL := CPH2493
PRODUCT_NAME := lineage_vitamin

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="sys_mssi_64_cn_armv82-user 15 AP3A.240617.008 1742885549055 release-keys" \
    BuildFingerprint=OnePlus/CPH2493EEA/OP556FL1:15/AP3A.240617.008/T.R4T3.1d25f69-14c1e-114e5:user/release-keys \
    DeviceName=OP556FL1 \
    DeviceProduct=CPH2493EEA \
    SystemDevice=OP556FL1 \
    SystemName=CPH2493EEA
