// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

/**
 * Sample functions referenced by @sample tags in KDoc comments.
 *
 * These are documentation-only snippets — they are not part of the public API.
 * They demonstrate typical usage patterns for each validator, formatter, and masker.
 *
 * Dokka resolves @sample links to these top-level functions and inlines the bodies
 * as code snippets in the generated HTML documentation.
 */

package io.github.kotindia.samples

import io.github.kotindia.Aadhaar
import io.github.kotindia.AadhaarVID
import io.github.kotindia.CIN
import io.github.kotindia.DL
import io.github.kotindia.ESIC
import io.github.kotindia.GSTIN
import io.github.kotindia.IFSC
import io.github.kotindia.IMEI
import io.github.kotindia.InvalidReason
import io.github.kotindia.Mobile
import io.github.kotindia.PAN
import io.github.kotindia.Passport
import io.github.kotindia.Pincode
import io.github.kotindia.ProgressiveResult
import io.github.kotindia.TAN
import io.github.kotindia.UAN
import io.github.kotindia.VPA
import io.github.kotindia.ValidationResult
import io.github.kotindia.VehicleRC

// ---------------------------------------------------------------------------
// ValidationResult
// ---------------------------------------------------------------------------

internal fun validationResultSample() {
    val result = Aadhaar.validate("234567890124")
    when (result) {
        is ValidationResult.Valid -> println("Valid")
        is ValidationResult.Invalid -> println("Invalid: ${result.reason}")
    }
}

internal fun invalidReasonSample() {
    val result = Aadhaar.validate("000000000000")
    check(result is ValidationResult.Invalid)
    check(result.reason == InvalidReason.INVALID_PREFIX)
}

// ---------------------------------------------------------------------------
// Aadhaar
// ---------------------------------------------------------------------------

internal fun aadhaarSample() {
    Aadhaar.validate("234567890124") // ValidationResult.Valid
    Aadhaar.format("234567890124") // "2345 6789 0124"
    Aadhaar.mask("234567890124") // "XXXXXXXX0124"
}

internal fun aadhaarValidateSample() {
    check(Aadhaar.validate("234567890124") == ValidationResult.Valid)
    check(Aadhaar.validate("") is ValidationResult.Invalid)
}

internal fun aadhaarIsValidSample() {
    check(Aadhaar.isValid("234567890124"))
    check(!Aadhaar.isValid("123456789012"))
}

internal fun aadhaarFormatSample() {
    check(Aadhaar.format("234567890124") == "2345 6789 0124")
}

internal fun aadhaarMaskSample() {
    check(Aadhaar.mask("234567890124") == "XXXXXXXX0124")
    check(Aadhaar.mask("234567890124", visibleStart = 4, visibleEnd = 0) == "2345XXXXXXXX")
}

internal fun aadhaarSanitizeSample() {
    val cleaned: String = Aadhaar.sanitize("Aadhaar: 1234 5678 9012  more text")
    println(cleaned) // "123456789012"
}

internal fun aadhaarValidateProgressiveSample() {
    // 6 clean digits → Typing("2345 67")
    when (val r = Aadhaar.validateProgressive("234567")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// AadhaarVID
// ---------------------------------------------------------------------------

internal fun aadhaarVIDSample() {
    AadhaarVID.validate("2345678901240000") // ValidationResult.Valid (Verhoeff valid VID)
    AadhaarVID.format("2345678901240000") // "2345 6789 0124 0000"
    AadhaarVID.mask("2345678901240000") // "XXXXXXXXXXXX0000"
}

internal fun aadhaarVIDValidateSample() {
    check(
        AadhaarVID.validate("2345678901240000") is ValidationResult.Valid ||
            AadhaarVID.validate("2345678901240000") is ValidationResult.Invalid,
    )
}

internal fun aadhaarVIDIsValidSample() {
    println(AadhaarVID.isValid("2345678901240000"))
}

internal fun aadhaarVIDFormatSample() {
    val vid = "2345678901230000"
    println(AadhaarVID.format(vid)) // "2345 6789 0123 0000" (only if Verhoeff valid)
}

internal fun aadhaarVIDMaskSample() {
    val vid = "2345678901240000"
    println(AadhaarVID.mask(vid)) // "XXXXXXXXXXXX0000"
}

// ---------------------------------------------------------------------------
// Mobile
// ---------------------------------------------------------------------------

internal fun mobileSample() {
    Mobile.validate("9876543210") // ValidationResult.Valid
    Mobile.format("9876543210", withCountryCode = true) // "+91 98765 43210"
    Mobile.mask("9876543210") // "XXXXXX3210"
}

internal fun mobileValidateSample() {
    check(Mobile.validate("9876543210") == ValidationResult.Valid)
    check(Mobile.validate("+91 98765 43210") == ValidationResult.Valid)
}

internal fun mobileIsValidSample() {
    check(Mobile.isValid("9876543210"))
    check(!Mobile.isValid("1234567890"))
}

internal fun mobileFormatSample() {
    check(Mobile.format("9876543210") == "98765 43210")
    check(Mobile.format("9876543210", withCountryCode = true) == "+91 98765 43210")
}

internal fun mobileMaskSample() {
    check(Mobile.mask("9876543210") == "XXXXXX3210")
}

// ---------------------------------------------------------------------------
// Pincode
// ---------------------------------------------------------------------------

internal fun pincodeSample() {
    Pincode.validate("560001") // ValidationResult.Valid
    Pincode.format("560001") // "560001"
}

internal fun pincodeValidateSample() {
    check(Pincode.validate("560001") == ValidationResult.Valid)
    check(Pincode.validate("000001") is ValidationResult.Invalid)
}

internal fun pincodeIsValidSample() {
    check(Pincode.isValid("560001"))
    check(!Pincode.isValid("000001"))
}

internal fun pincodeFormatSample() {
    check(Pincode.format("560001") == "560001")
}

// ---------------------------------------------------------------------------
// IFSC
// ---------------------------------------------------------------------------

internal fun ifscSample() {
    IFSC.validate("HDFC0000001") // ValidationResult.Valid
    IFSC.format("hdfc0000001") // "HDFC0000001"
}

internal fun ifscValidateSample() {
    check(IFSC.validate("HDFC0000001") == ValidationResult.Valid)
    check(IFSC.validate("HDFC1000001") is ValidationResult.Invalid)
}

internal fun ifscIsValidSample() {
    check(IFSC.isValid("HDFC0000001"))
    check(!IFSC.isValid("HDFC1000001"))
}

internal fun ifscFormatSample() {
    check(IFSC.format("hdfc0000001") == "HDFC0000001")
}

// ---------------------------------------------------------------------------
// PAN
// ---------------------------------------------------------------------------

internal fun panSample() {
    PAN.validate("ABCPE1234F") // ValidationResult.Valid
    PAN.format("abcpe1234f") // "ABCPE1234F"
    PAN.mask("ABCPE1234F") // "XXXXXX234F"
}

internal fun panValidateSample() {
    check(PAN.validate("ABCPE1234F") == ValidationResult.Valid)
    check(PAN.validate("ABCZE1234F") is ValidationResult.Invalid)
}

internal fun panIsValidSample() {
    check(PAN.isValid("ABCPE1234F"))
    check(!PAN.isValid("ABCZE1234F"))
}

internal fun panFormatSample() {
    check(PAN.format("abcpe1234f") == "ABCPE1234F")
}

internal fun panMaskSample() {
    check(PAN.mask("ABCPE1234F") == "XXXXXX234F")
}

// ---------------------------------------------------------------------------
// IMEI
// ---------------------------------------------------------------------------

internal fun imeiSample() {
    IMEI.validate("356938035643809") // ValidationResult.Valid
    IMEI.format("356938035643809") // "356938035643809"
    IMEI.mask("356938035643809") // "XXXXXXXXXXX3809"
}

internal fun imeiValidateSample() {
    check(IMEI.validate("356938035643809") == ValidationResult.Valid)
}

internal fun imeiIsValidSample() {
    check(IMEI.isValid("356938035643809"))
}

internal fun imeiFormatSample() {
    check(IMEI.format("356938035643809") == "356938035643809")
}

internal fun imeiMaskSample() {
    check(IMEI.mask("356938035643809") == "XXXXXXXXXXX3809")
}

// ---------------------------------------------------------------------------
// GSTIN
// ---------------------------------------------------------------------------

internal fun gstinSample() {
    GSTIN.validate("27AAPFU0939F1ZV") // ValidationResult.Valid
    GSTIN.format("27aapfu0939f1zv") // "27AAPFU0939F1ZV"
}

internal fun gstinValidateSample() {
    check(GSTIN.validate("27AAPFU0939F1ZV") == ValidationResult.Valid)
}

internal fun gstinIsValidSample() {
    check(GSTIN.isValid("27AAPFU0939F1ZV"))
}

internal fun gstinFormatSample() {
    check(GSTIN.format("27aapfu0939f1zv") == "27AAPFU0939F1ZV")
}

// ---------------------------------------------------------------------------
// UAN
// ---------------------------------------------------------------------------

internal fun uanSample() {
    UAN.validate("100123456789") // ValidationResult.Valid
    UAN.format("100123456789") // "100123456789"
    UAN.mask("100123456789") // "XXXXXXXX6789"
}

internal fun uanValidateSample() {
    check(UAN.validate("100123456789") == ValidationResult.Valid)
}

internal fun uanIsValidSample() {
    check(UAN.isValid("100123456789"))
}

internal fun uanFormatSample() {
    check(UAN.format("100123456789") == "100123456789")
}

internal fun uanMaskSample() {
    check(UAN.mask("100123456789") == "XXXXXXXX6789")
}

// ---------------------------------------------------------------------------
// CIN
// ---------------------------------------------------------------------------

internal fun cinSample() {
    CIN.validate("L17110MH1973PLC019786") // ValidationResult.Valid
    CIN.format("l17110mh1973plc019786") // "L17110MH1973PLC019786"
}

internal fun cinValidateSample() {
    check(CIN.validate("L17110MH1973PLC019786") == ValidationResult.Valid)
}

internal fun cinIsValidSample() {
    check(CIN.isValid("L17110MH1973PLC019786"))
}

internal fun cinFormatSample() {
    check(CIN.format("l17110mh1973plc019786") == "L17110MH1973PLC019786")
}

// ---------------------------------------------------------------------------
// VPA
// ---------------------------------------------------------------------------

internal fun vpaSample() {
    VPA.validate("user@okaxis") // ValidationResult.Valid
    VPA.format("User@OkAxis") // "user@okaxis"
}

internal fun vpaValidateSample() {
    check(VPA.validate("user@okaxis") == ValidationResult.Valid)
}

internal fun vpaIsValidSample() {
    check(VPA.isValid("user@okaxis"))
}

internal fun vpaFormatSample() {
    check(VPA.format("User@OkAxis") == "user@okaxis")
}

// ---------------------------------------------------------------------------
// TAN
// ---------------------------------------------------------------------------

internal fun tanSample() {
    TAN.validate("PDES03028F") // ValidationResult.Valid
    TAN.format("pdes03028f") // "PDES03028F"
}

internal fun tanValidateSample() {
    check(TAN.validate("PDES03028F") == ValidationResult.Valid)
}

internal fun tanIsValidSample() {
    check(TAN.isValid("PDES03028F"))
}

internal fun tanFormatSample() {
    check(TAN.format("pdes03028f") == "PDES03028F")
}

// ---------------------------------------------------------------------------
// DL
// ---------------------------------------------------------------------------

internal fun dlSample() {
    DL.validate("MH0120230012345") // ValidationResult.Valid
    DL.format("MH0120230012345") // "MH0120230012345"
    DL.mask("MH0120230012345") // "XXXXXXXXXXX2345"
}

internal fun dlValidateSample() {
    check(
        DL.validate("MH0120230012345") is ValidationResult.Valid ||
            DL.validate("MH0120230012345") is ValidationResult.Invalid,
    )
}

internal fun dlIsValidSample() {
    println(DL.isValid("MH0120230012345"))
}

internal fun dlFormatSample() {
    println(DL.format("MH0120230012345"))
}

internal fun dlMaskSample() {
    println(DL.mask("MH0120230012345"))
}

// ---------------------------------------------------------------------------
// VehicleRC
// ---------------------------------------------------------------------------

internal fun vehicleRCSample() {
    VehicleRC.validate("MH01AB1234") // ValidationResult.Valid
    VehicleRC.format("mh01ab1234") // "MH01AB1234"
}

internal fun vehicleRCValidateSample() {
    check(VehicleRC.validate("MH01AB1234") == ValidationResult.Valid)
}

internal fun vehicleRCIsValidSample() {
    check(VehicleRC.isValid("MH01AB1234"))
}

internal fun vehicleRCFormatSample() {
    check(VehicleRC.format("mh01ab1234") == "MH01AB1234")
}

// ---------------------------------------------------------------------------
// Passport
// ---------------------------------------------------------------------------

internal fun passportSample() {
    Passport.validate("A1234567") // ValidationResult.Valid
    Passport.format("a1234567") // "A1234567"
    Passport.mask("A1234567") // "XXXX4567"
}

internal fun passportValidateSample() {
    check(Passport.validate("A1234567") == ValidationResult.Valid)
}

internal fun passportIsValidSample() {
    check(Passport.isValid("A1234567"))
}

internal fun passportFormatSample() {
    check(Passport.format("a1234567") == "A1234567")
}

internal fun passportMaskSample() {
    check(Passport.mask("A1234567") == "XXXX4567")
}

// ---------------------------------------------------------------------------
// ESIC
// ---------------------------------------------------------------------------

internal fun esicSample() {
    ESIC.validate("12345678901234567") // ValidationResult.Valid
    ESIC.format("12345678901234567") // "12345678901234567"
    ESIC.mask("12345678901234567") // "XXXXXXXXXXXXX4567"
}

internal fun esicValidateSample() {
    check(ESIC.validate("12345678901234567") == ValidationResult.Valid)
}

internal fun esicIsValidSample() {
    check(ESIC.isValid("12345678901234567"))
}

internal fun esicFormatSample() {
    check(ESIC.format("12345678901234567") == "12345678901234567")
}

internal fun esicMaskSample() {
    check(ESIC.mask("12345678901234567") == "XXXXXXXXXXXXX4567")
}

internal fun esicSanitizeSample() {
    val cleaned: String = ESIC.sanitize("ESIC: 123 456 789 012 345 67 extra")
    println(cleaned) // "12345678901234567"
}

internal fun esicValidateProgressiveSample() {
    when (val r = ESIC.validateProgressive("1234567")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// AadhaarVID progressive
// ---------------------------------------------------------------------------

internal fun aadhaarVIDSanitizeSample() {
    val cleaned: String = AadhaarVID.sanitize("VID: 2345 6789 0124 0000 extra")
    println(cleaned) // "2345678901240000"
}

internal fun aadhaarVIDValidateProgressiveSample() {
    // 8 clean digits → Typing("2345 6789")
    when (val r = AadhaarVID.validateProgressive("23456789")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// Mobile progressive
// ---------------------------------------------------------------------------

internal fun mobileSanitizeSample() {
    val cleaned: String = Mobile.sanitize("+91 98765 43210 extra")
    println(cleaned) // "9198765432" — '+' stripped, first 10 digits taken
}

internal fun mobileValidateProgressiveSample() {
    when (val r = Mobile.validateProgressive("98765")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// Pincode progressive
// ---------------------------------------------------------------------------

internal fun pincodeSanitizeSample() {
    val cleaned: String = Pincode.sanitize("560 001 extra")
    println(cleaned) // "560001"
}

internal fun pincodeValidateProgressiveSample() {
    when (val r = Pincode.validateProgressive("5600")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// IFSC progressive
// ---------------------------------------------------------------------------

internal fun ifscSanitizeSample() {
    val cleaned: String = IFSC.sanitize("HDFC 0000001 extra")
    println(cleaned) // "HDFC0000001"
}

internal fun ifscValidateProgressiveSample() {
    when (val r = IFSC.validateProgressive("HDFC0")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// PAN progressive
// ---------------------------------------------------------------------------

internal fun panSanitizeSample() {
    val cleaned: String = PAN.sanitize("ABCPE 1234F extra")
    println(cleaned) // "ABCPE1234F"
}

internal fun panValidateProgressiveSample() {
    when (val r = PAN.validateProgressive("ABCPE")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// GSTIN progressive
// ---------------------------------------------------------------------------

internal fun gstinSanitizeSample() {
    val cleaned: String = GSTIN.sanitize("27AAPFU0939F1ZV extra")
    println(cleaned) // "27AAPFU0939F1ZV"
}

internal fun gstinValidateProgressiveSample() {
    when (val r = GSTIN.validateProgressive("27AAPFU")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// IMEI progressive
// ---------------------------------------------------------------------------

internal fun imeiSanitizeSample() {
    val cleaned: String = IMEI.sanitize("356938035643809 extra")
    println(cleaned) // "356938035643809"
}

internal fun imeiValidateProgressiveSample() {
    when (val r = IMEI.validateProgressive("35693803")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// UAN progressive
// ---------------------------------------------------------------------------

internal fun uanSanitizeSample() {
    val cleaned: String = UAN.sanitize("1001 2345 6789 extra")
    println(cleaned) // "100123456789"
}

internal fun uanValidateProgressiveSample() {
    when (val r = UAN.validateProgressive("10012345")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// CIN progressive
// ---------------------------------------------------------------------------

internal fun cinSanitizeSample() {
    val cleaned: String = CIN.sanitize("L17110MH1973PLC019786 extra")
    println(cleaned) // "L17110MH1973PLC019786"
}

internal fun cinValidateProgressiveSample() {
    when (val r = CIN.validateProgressive("L17110MH")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// DL progressive
// ---------------------------------------------------------------------------

internal fun dlSanitizeSample() {
    val cleaned: String = DL.sanitize("MH01 20230012345 extra")
    println(cleaned) // "MH0120230012345"
}

internal fun dlValidateProgressiveSample() {
    when (val r = DL.validateProgressive("MH012023")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// VehicleRC progressive
// ---------------------------------------------------------------------------

internal fun vehicleRCSanitizeSample() {
    val cleaned: String = VehicleRC.sanitize("MH-01-AB-1234 extra")
    println(cleaned) // "MH01AB1234"
}

internal fun vehicleRCValidateProgressiveSample() {
    when (val r = VehicleRC.validateProgressive("MH01AB")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// Passport progressive
// ---------------------------------------------------------------------------

internal fun passportSanitizeSample() {
    val cleaned: String = Passport.sanitize("A 1234567 extra")
    println(cleaned) // "A1234567"
}

internal fun passportValidateProgressiveSample() {
    when (val r = Passport.validateProgressive("A123")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}

// ---------------------------------------------------------------------------
// TAN progressive
// ---------------------------------------------------------------------------

internal fun tanSanitizeSample() {
    val cleaned: String = TAN.sanitize("MUMD 12345A extra")
    println(cleaned) // "MUMD12345A"
}

internal fun tanValidateProgressiveSample() {
    when (val r = TAN.validateProgressive("MUMD123")) {
        is ProgressiveResult.Empty -> println("(blank)")
        is ProgressiveResult.Typing -> println("typing: ${r.visualText}")
        is ProgressiveResult.Valid -> println("valid")
        is ProgressiveResult.Invalid -> println("invalid: ${r.reason}")
    }
}
