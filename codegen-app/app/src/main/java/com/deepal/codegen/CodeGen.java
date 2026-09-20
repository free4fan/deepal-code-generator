package com.deepal.codegen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CodeGen {

    private static final Map<Character, Integer> B2D = new HashMap<>();

    static {
        String keys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String vals = "01234567899876543210012345";
        for (int i = 0; i < keys.length(); i++) {
            B2D.put(keys.charAt(i), vals.charAt(i) - '0');
        }
    }

    private static final int[] SHUFFLE = {2, 5, 1, 4, 7, 0, 3, 6};

    private CodeGen() {
    }

    public static int indexOfUnacceptable(String vin) {
        for (int i = 0; i < vin.length(); i++) {
            if (!isVinChar(vin.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public static String genCode(String vin) {
        if (vin == null) {
            throw new IllegalArgumentException("VIN is null");
        }
        if (vin.length() < 8) {
            throw new IllegalArgumentException("VIN must be at least 8 characters long");
        }
        String last8 = vin.substring(vin.length() - 8).toUpperCase(Locale.ROOT);
        int[] c = new int[8];
        for (int i = 0; i < 8; i++) {
            char ch = last8.charAt(i);
            if (ch >= '0' && ch <= '9') {
                c[i] = ch - '0';
            } else {
                Integer v = B2D.get(ch);
                if (v == null) {
                    throw new IllegalArgumentException("unexpected character in VIN: " + ch);
                }
                c[i] = v;
            }
        }
        StringBuilder sb = new StringBuilder(12);
        sb.append("*#");
        for (int i = 0; i < 8; i++) {
            sb.append(c[SHUFFLE[i]]);
        }
        return sb.append("#*").toString();
    }

    private static boolean isVinChar(char ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'A' && ch <= 'Z' && ch != 'I' && ch != 'O' && ch != 'Q')
                || (ch >= 'a' && ch <= 'z' && ch != 'i' && ch != 'o' && ch != 'q');
    }
}
