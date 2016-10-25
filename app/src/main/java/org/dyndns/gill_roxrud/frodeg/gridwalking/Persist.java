package org.dyndns.gill_roxrud.frodeg.gridwalking;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.InputMismatchException;

public class Persist {

    private static final long PERSIST_INTERVAL = 30*1000;
    private long mostRecentPersist = 0;
    private boolean isModified = false;

    private static final String FILENAME = "filename";

    private static final byte[] SALT = new byte[] {94, -19, 78, -35, -34, -9, 16, 10}; // http://www.random.org

    private static final byte[] buffer = new byte[8];


    public void setIsModified() {
        isModified = true;
    }

    public void Load() {
        Grid grid = GameState.getInstance().getGrid();
        Bonus bonus = GameState.getInstance().getBonus();

        InputStream stream = null;
        try {
            stream = new FileInputStream(FILENAME);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);

            int i, count;
            //Read Grid
            synchronized (grid.gridsLock) {
                for (i = 0; Grid.LEVEL_COUNT>i; i++) {
                    grid.grids[i].clear();

                    count = Read32Bit(stream, digest);
                    while (0 < count) {
                        grid.grids[i].add(Read64Bit(stream, digest));
                    }

                    byte crc = Read8Bit(stream, digest);
                    if (0 != crc) {
                        throw new InputMismatchException("Load: CRC error");
                    }
                }
                grid.mru_list.clear();
            }

            //Read Bonus
            bonus.bonuses.clear();
            count = Read32Bit(stream, digest);
            while (0 < count) {
                bonus.bonuses.add(Read32Bit(stream, digest));
            }

            byte crc = Read8Bit(stream, digest);
            if (0 != crc) {
                throw new InputMismatchException("Load: CRC error");
            }

            byte[] expected_digest = digest.digest();
            byte[] actual_digest = new byte[32];
            for (i = 0; 32 > i; i++) {
                actual_digest[i] = Read8Bit(stream, digest);
            }
            if (!MessageDigest.isEqual(expected_digest, actual_digest)) {
                throw new InputMismatchException("Load: Checksum error");
            }
        }
        catch (Exception e) {
            synchronized (grid.gridsLock) {
                for (int i = 0; Grid.LEVEL_COUNT>i; i++) {
                    grid.grids[i].clear();
                }
                grid.mru_list.clear();
            }
            bonus.bonuses.clear();
        } finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }

            mostRecentPersist = System.currentTimeMillis();
            isModified = false;
        }
    }

    public void saveIfModified() {
        if (!isModified) {
            return;
        }

        long now = System.currentTimeMillis();
        if (PERSIST_INTERVAL>(now-mostRecentPersist)) {
            isModified = true;
            return;
        }

        Grid grid = GameState.getInstance().getGrid();
        Bonus bonus = GameState.getInstance().getBonus();
        if (false) {
            OutputStream stream = null;
            try {
                stream = new FileOutputStream(FILENAME);

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(SALT);

                int i;
                //Write Grid
                synchronized (grid.gridsLock) {
                    for (i = 0; Grid.LEVEL_COUNT > i; i++) {
                        Write32Bit(grid.grids[i].size(), stream, digest);
                        for (Long value : grid.grids[i]) {
                            Write64Bit(value, stream, digest);
                        }
                        Write8Bit((byte) 0, stream, digest);
                    }
                }

                //Write Bonus
                Write32Bit(bonus.bonuses.size(), stream, digest);
                for (Integer value : bonus.bonuses) {
                    Write32Bit(value, stream, digest);
                }

                Write8Bit((byte) 0, stream, digest);

                byte[] actual_digest = digest.digest();
                for (i = 0; 32 > i; i++) {
                    Write8Bit(actual_digest[i], stream, digest);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }

                mostRecentPersist = System.currentTimeMillis();
                isModified = false;
            }
        }
    }

    private static byte Read8Bit(InputStream s, MessageDigest digest) throws IOException, DigestException {
        s.read(buffer, 0, 1);
        digest.update(buffer, 0, 1);
        return buffer[0];
    }

    private static int Read32Bit(InputStream s, MessageDigest digest) throws IOException, DigestException {
        s.read(buffer, 0, 4);
        digest.update(buffer, 0, 4);
        return buffer[0]<<24 | buffer[1]<<16 | buffer[2]<<8 | buffer[3];
    }

    private static long Read64Bit(InputStream s, MessageDigest digest) throws IOException, DigestException {
        s.read(buffer, 0, 8);
        digest.update(buffer, 0, 8);
        return buffer[0]<<56 | buffer[1]<<48 | buffer[2]<<40 | buffer[3]<<32 | buffer[4]<<24 | buffer[5]<<16 | buffer[6]<<8 | buffer[7];
    }

    private static void Write8Bit(byte value, OutputStream s, MessageDigest digest) throws IOException, DigestException {
        buffer[0] = value;
        s.write(buffer, 0, 1);
        digest.update(buffer, 0, 1);
    }

    private static void Write32Bit(int value, OutputStream s, MessageDigest digest) throws IOException, DigestException {
        buffer[0] = (byte)((value>>24)&0xFF);
        buffer[1] = (byte)((value>>16)&0xFF);
        buffer[2] = (byte)((value>>8)&0xFF);
        buffer[3] = (byte)(value&0xFF);
        s.write(buffer, 0, 4);
        digest.update(buffer, 0, 4);
    }

    private static void Write64Bit(long value, OutputStream s, MessageDigest digest) throws IOException, DigestException {
        buffer[0] = (byte)((value>>56)&0xFF);
        buffer[1] = (byte)((value>>48)&0xFF);
        buffer[2] = (byte)((value>>40)&0xFF);
        buffer[3] = (byte)((value>>32)&0xFF);
        buffer[4] = (byte)((value>>24)&0xFF);
        buffer[5] = (byte)((value>>16)&0xFF);
        buffer[6] = (byte)((value>>8)&0xFF);
        buffer[7] = (byte)(value&0xFF);
        s.write(buffer, 0, 8);
        digest.update(buffer, 0, 8);
    }
}
