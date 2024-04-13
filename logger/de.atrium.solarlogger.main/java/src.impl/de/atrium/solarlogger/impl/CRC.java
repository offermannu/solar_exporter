/*
 * Copyright (c) Papierfabrik Louisenthal GmbH
 *
 * All rights reserved.
 */
package de.atrium.solarlogger.impl;

import java.io.UnsupportedEncodingException;
public class CRC {

//    private static final int POLYNOMIAL = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) https://introcs.cs.princeton.edu/java/61data/CRC16CCITT.java

        private static final int POLYNOMIAL = 0x8408;   // 0001 0000 0010 0001  (16, 12, 5, 1) Axel

    // initial value
    private              int crc        = 0xFFFF;

    public CRC _data(String data) {
        byte[] bytes = data.getBytes();
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= POLYNOMIAL;
                }
            }
        }
        return this;
    }

    public CRC data(String data) {
        byte[] bytes = data.getBytes();
        for (byte b : bytes) {
            if ( ((crc ^ 0x0001) ^ (b & 0x0001)) == 1) {
                crc = (crc >> 1) ^ POLYNOMIAL;
            } else {
                crc = crc >> 1;
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return Integer.toHexString(crc & 0xffff);
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(new CRC().data("123456789").toString());
        System.out.println(new CRC().data("*049 180TL ").toString());
        System.out.println(new CRC().data("*059 100TL ").toString());
        System.out.println(new CRC().data("*02n 20 100TL 75  419.1  0.03    13  492.0  0.00     0  233.8  0.00  234.9  0.00  233.7  0.00    13     0 1.000  34.3      0 ").toString());
        System.out.println(new CRC().data("*04n 20 180TL 4  685.4  4.93  3382  595.1  2.08  1243  238.7 ")
                                    .data(" 6.49  238.1  6.49  238.8  6.46  4626  4661 1.000  45.9  29947")
                                    .data(" ")
                                    .toString());

//        Checksum checksum = Checksum.getInstance(ALG_ISO3309_CRC16, false);
//        checksum.init(new byte[]{(byte)0xff,(byte)0xff}, (short)0, (short)2);
//        String msg1 = "*04n 20 180TL 4  685.4  4.93  3382  595.1  2.08  1243  238.7 ";
//        String msg2 = " 6.49  238.1  6.49  238.8  6.46  4626  4661 1.000  45.9  29947";
//        String msg3 = " ";
//        byte[] out=new byte[4];
//        checksum.update(msg1.getBytes(StandardCharsets.US_ASCII), (short) 0, (short) msg1.length());
//        checksum.update(msg2.getBytes(StandardCharsets.US_ASCII), (short) 0, (short) msg2.length());
//        checksum.doFinal(msg3.getBytes(StandardCharsets.US_ASCII), (short) 0, (short) msg3.length(), out, (short) 0);
//        System.out.println(Arrays.toString(out));

    }
}
