/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

/* Inertial Measurement Unit  */
const val ServiceUuid = "0000180f-0000-1000-8000-00805f9b34fb"
const val CharUuid0 = "00002a19-0000-1000-8000-00805f9b34fb"
const val CharUuid1 = "00002a20-0000-1000-8000-00805f9b34fb"
const val CharUuid2 = "00002a21-0000-1000-8000-00805f9b34fb"

/* LineGraphSeries */
var mSeriesXaccel: LineGraphSeries<DataPoint> = LineGraphSeries()
var mSeriesYaccel: LineGraphSeries<DataPoint> = LineGraphSeries()
var mSeriesZaccel: LineGraphSeries<DataPoint> = LineGraphSeries()