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
import io.github.kotindia.Aadhaar
import io.github.kotindia.AadhaarVID
import io.github.kotindia.DL
import io.github.kotindia.ESIC
import io.github.kotindia.IMEI
import io.github.kotindia.Mobile
import io.github.kotindia.PAN
import io.github.kotindia.Passport
import io.github.kotindia.UAN
import io.github.kotindia.demo.components.ValidatorCard
import io.github.kotindia.demo.components.ValidatorCardSpec

@Composable
internal fun PiiTab(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val specs = remember {
        listOf(
            ValidatorCardSpec(
                title = "Mobile",
                formatHint = "10 digits, prefix 6/7/8/9",
                sampleInputs = listOf("9876543210", "+91 98765 43210", "0 9876543210"),
                validate = Mobile::validate,
                format = { Mobile.format(it) },
                mask = { Mobile.mask(it) },
            ),
            ValidatorCardSpec(
                title = "PAN",
                formatHint = "[A-Z]{5}[0-9]{4}[A-Z], 4th-char category (P/C/H/A/B/G/J/L/F/T)",
                sampleInputs = listOf("ABCPE1234F", "ABCZE1234F"),
                validate = PAN::validate,
                format = { PAN.format(it) },
                mask = { PAN.mask(it) },
            ),
            ValidatorCardSpec(
                title = "Aadhaar",
                formatHint = "12 digits, prefix 2-9, Verhoeff checksum",
                sampleInputs = listOf("234567890124"),
                validate = Aadhaar::validate,
                format = { Aadhaar.format(it) },
                mask = { Aadhaar.mask(it) },
            ),
            ValidatorCardSpec(
                title = "AadhaarVID",
                formatHint = "16 digits, prefix 2-9, Verhoeff checksum",
                sampleInputs = listOf("2345678901234567"),
                validate = AadhaarVID::validate,
                format = { AadhaarVID.format(it) },
                mask = { AadhaarVID.mask(it) },
            ),
            ValidatorCardSpec(
                title = "IMEI",
                formatHint = "15 digits, Luhn checksum",
                sampleInputs = listOf("490154203237518"),
                validate = IMEI::validate,
                format = { IMEI.format(it) },
                mask = { IMEI.mask(it) },
            ),
            ValidatorCardSpec(
                title = "UAN",
                formatHint = "12 digits (EPFO Universal Account Number)",
                sampleInputs = listOf("123456789012"),
                validate = UAN::validate,
                format = { UAN.format(it) },
                mask = { UAN.mask(it) },
            ),
            ValidatorCardSpec(
                title = "Passport",
                formatHint = "[A-Z]\\d{7} — 1 letter + 7 digits (MEA format)",
                sampleInputs = listOf("M1234567"),
                validate = Passport::validate,
                format = { Passport.format(it) },
                mask = { Passport.mask(it) },
            ),
            ValidatorCardSpec(
                title = "ESIC",
                formatHint = "17 digits (Employee State Insurance Corporation)",
                sampleInputs = listOf("12345678901234567"),
                validate = ESIC::validate,
                format = { ESIC.format(it) },
                mask = { ESIC.mask(it) },
            ),
            ValidatorCardSpec(
                title = "Driving License",
                formatHint = "Post-2013 Sarathi: state+RTO+year+serial (14-15 chars)",
                sampleInputs = listOf("MH1220110012345", "KA0520150098765"),
                validate = DL::validate,
                format = { DL.format(it) },
                mask = { DL.mask(it) },
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
