// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kotindia.CIN
import io.github.kotindia.GSTIN
import io.github.kotindia.IFSC
import io.github.kotindia.Pincode
import io.github.kotindia.TAN
import io.github.kotindia.VPA
import io.github.kotindia.VehicleRC
import io.github.kotindia.demo.components.ValidatorCard
import io.github.kotindia.demo.components.ValidatorCardSpec

@Composable
internal fun PublicTab(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val specs = remember {
        listOf(
            ValidatorCardSpec(
                title = "Pincode",
                formatHint = "6 digits, first digit 1-9 (India Post Postal Index Number)",
                sampleInputs = listOf("560001", "110001"),
                validate = Pincode::validate,
                format = { Pincode.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "IFSC",
                formatHint = "[A-Z]{4}0[A-Z0-9]{6} — RBI bank branch code",
                sampleInputs = listOf("HDFC0000001", "SBIN0000001"),
                validate = IFSC::validate,
                format = { IFSC.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "GSTIN",
                formatHint = "15 chars: state(2)+PAN(10)+entity+Z+checksum",
                sampleInputs = listOf("27AAPFU0939F1ZV", "38AAPFU0939F1ZV"),
                validate = GSTIN::validate,
                format = { GSTIN.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "CIN",
                formatHint = "[LU]+industry(5)+state(2)+year(4)+class(3)+reg(6) — 21 chars",
                sampleInputs = listOf("U72200KA2013PTC097389"),
                validate = CIN::validate,
                format = { CIN.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "VPA (UPI ID)",
                formatHint = "username@psp — max 50 chars (NPCI Virtual Payment Address)",
                sampleInputs = listOf("user@oksbi", "9876543210@paytm"),
                validate = VPA::validate,
                format = { VPA.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "Vehicle RC",
                formatHint = "[A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{4} — MoRTH registration plate",
                sampleInputs = listOf("MH12AB1234", "KA05CD5678"),
                validate = VehicleRC::validate,
                format = { VehicleRC.format(it) },
                mask = null,
            ),
            ValidatorCardSpec(
                title = "TAN",
                formatHint = "[A-Z]{4}[0-9]{5}[A-Z] — 10 chars (Tax Deduction Account Number)",
                sampleInputs = listOf("MUMD12345A"),
                validate = TAN::validate,
                format = { TAN.format(it) },
                mask = null,
            ),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(specs, key = { it.title }) { spec ->
            ValidatorCard(spec = spec)
        }
    }
}
