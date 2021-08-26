package euphoria.psycho.share;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class KeyShare {

    private static final long INITIALCRC = 0xFFFFFFFFFFFFFFFFL;
    private static final long POLY64REV = 0x95AC9329AC4BC9B5L;
    private static long[] sCrcTable = new long[256];

    static {
        // http://bioinf.cs.ucl.ac.uk/downloads/crc64/crc64.c
        long part;
        for (int i = 0; i < 256; i++) {
            part = i;
            for (int j = 0; j < 8; j++) {
                long x = ((int) part & 1) != 0 ? POLY64REV : 0;
                part = (part >> 1) ^ x;
            }
            sCrcTable[i] = part;
        }
    }

    public static final long crc64Long(byte[] buffer) {
        long crc = INITIALCRC;
        for (int k = 0, n = buffer.length; k < n; ++k) {
            crc = sCrcTable[(((int) crc) ^ buffer[k]) & 0xff] ^ (crc >> 8);
        }
        return crc;
    }

    public static final long crc64Long(String in) {
        if (in == null || in.length() == 0) {
            return 0;
        }
        return crc64Long(getBytes(in));
    }

    public static byte[] fromHex(String hexData) {
        if (null == hexData) {
            return new byte[0];
        }
        if ((hexData.length() & 1) != 0 || hexData.replaceAll("[a-fA-F0-9]", "").length() > 0) {
            throw new IllegalArgumentException("'" + hexData + "' is not a hex string");
        }
        byte[] result = new byte[(hexData.length() + 1) / 2];
        String hexNumber;
        int offset = 0;
        int byteIndex = 0;
        while (offset < hexData.length()) {
            hexNumber = hexData.substring(offset, offset + 2);
            offset += 2;
            result[byteIndex++] = (byte) Integer.parseInt(hexNumber, 16);
        }
        return result;
    }

    public static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        int output = 0;
        for (char ch : in.toCharArray()) {
            result[output++] = (byte) (ch & 0xFF);
            result[output++] = (byte) (ch >> 8);
        }
        return result;
    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                if (h.length() < 2)
                    hexString.append("0");
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] md5encode(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest;
        byte[] hash = null;
        // SHA-256
        digest = MessageDigest.getInstance("MD5");
        hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
        return hash;
    }

    public static String toHex(byte[] data) {
        if (null == data) {
            return null;
        }
        if (data.length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            String hv = Integer.toHexString(data[i]);
            if (hv.length() < 2) {
                sb.append("0");
            } else if (hv.length() == 8) {
                hv = hv.substring(6);
            }
            sb.append(hv);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }
}
