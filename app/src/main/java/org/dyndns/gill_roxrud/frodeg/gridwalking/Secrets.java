package org.dyndns.gill_roxrud.frodeg.gridwalking;


public class Secrets {

    private static final int CRC_SEED1 = 0;
    private static final int CRC_SEED2 = 0;
    private static final int CRC_SEED3 = 0;
    private static final int CRC_SEED4 = 0;

    private int[] crc = new int[4];

    
    public Secrets() {
        crc[0] = CRC_SEED1;
        crc[1] = CRC_SEED2;
        crc[2] = CRC_SEED3;
        crc[3] = CRC_SEED4;
    }

    public void Append(final byte b) {
        /* https://en.wikipedia.org/wiki/Fletcher's_checksum */
        crc[0] = (crc[0] + (b&0xFF)) % 255;
        crc[1] = (crc[1] + crc[0]) % 255;
        crc[2] = (crc[2] + crc[1]) % 255;
        crc[3] = (crc[3] + crc[2]) % 255;
    }

    public void Append(final byte[] s) {
        int i;
        for (i = 0; i < s.length; i++) {
            Append(s[i]);
        }
    }

    public int Crc16() {
        return ((crc[1]&0xFF) << 8) | crc[0];
    }

    public long Crc32() {
        return ((crc[3]<<24) | (crc[2]<<16) | (crc[1]<<8) | crc[0]) & 0x00000000FFFFFFFFL;
    }

}
