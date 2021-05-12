package com.example.smsotp.server.dto;

import android.app.Activity;
import android.telephony.SmsManager;

import androidx.annotation.Nullable;

public enum SmsResultType {
    OK(Activity.RESULT_OK),
    ERROR_GENERIC_FAILURE(SmsManager.RESULT_ERROR_GENERIC_FAILURE),
    ERROR_NO_SERVICE(SmsManager.RESULT_ERROR_NO_SERVICE),
    ERROR_NULL_PDU(SmsManager.RESULT_ERROR_NULL_PDU),
    ERROR_RADIO_OFF(SmsManager.RESULT_ERROR_RADIO_OFF);

    private final int code;

    SmsResultType(int code) {
        this.code = code;
    }

    @Nullable
    public static SmsResultType lookup(int code) {
        for (SmsResultType type : SmsResultType.values()) {
            if (type.getCode() == code)
                return type;
        }
        return null;
    }

    public int getCode() {
        return code;
    }
}
