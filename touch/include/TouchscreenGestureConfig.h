/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include "TouchscreenGesture.h"

namespace vendor {
namespace lineage {
namespace touch {
namespace V1_0 {
namespace implementation {

const int TouchscreenGesture::kSupportedGestures = makeBitField(
        kGestureUpVee, kGestureDownVee, kGestureLeftVee, kGestureRightVee,
        kGestureCircle, kGestureDoubleSwipe,
        kGestureLeftToRight, kGestureRightToLeft, kGestureUpToDown, kGestureDownToUp,
        kGestureM, kGestureW, kGestureSingleTap);

}  // namespace implementation
}  // namespace V1_0
}  // namespace touch
}  // namespace lineage
}  // namespace vendor
