package nodagumi.ananPJ.network;

import java.nio.ByteOrder;

/** Byte Operations
 */
public class objectToByte {

    /** Arguments checker for charToByte method */
    public static boolean checkArguments(byte[] bytes, int offset, int size) {
        if (bytes == null) {
            System.err.println("byte is null");
            return false;
        } else if (bytes.length < size) {
            System.err.println("byte size is too small");
            return false;
        } else if (offset < 0) {
            System.err.println("invalid offset");
            return false;
        } else if (bytes.length - size < offset) {
            System.err.println("byte does not have enough size");
            return false;
        }
        return true;
    }

    /** Store the object to the byte array.
     * @param bytes byte array to store the object.
     * @param offset the offset of the byte array.
     * @param val the object to store.
     * @return whether succeed or not.
     */
    public static boolean charToByte(byte[] bytes, int offset, char val) {
        int size = Character.SIZE / Byte.SIZE;
        if (!checkArguments(bytes, offset, size))
            return false;
        for (int i = 0; i < size; i++) {
            if (((ByteOrder) ByteOrder.nativeOrder()).toString().equals(
                    ByteOrder.BIG_ENDIAN)) {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * (size - 1 - i))).byteValue();
            } else {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * i)).byteValue();
            }
        }
        return true;
    }

    public static boolean shortToByte(byte[] bytes, int offset, short val) {
        int size = Short.SIZE / Byte.SIZE;
        if (!checkArguments(bytes, offset, size))
            return false;
        for (int i = 0; i < size; i++) {
            if (((ByteOrder) ByteOrder.nativeOrder()).toString().equals(
                    ByteOrder.BIG_ENDIAN)) {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * (size - 1 - i))).byteValue();
            } else {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * i)).byteValue();
            }
        }
        return true;
    }

    public static boolean intToByte(byte[] bytes, int offset, int val) {
        int size = Integer.SIZE / Byte.SIZE;
        if (!checkArguments(bytes, offset, size))
            return false;
        for (int i = 0; i < size; i++) {
            if (((ByteOrder) ByteOrder.nativeOrder()).toString().equals(
                    ByteOrder.BIG_ENDIAN)) {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * (size - 1 - i))).byteValue();
            } else {
                bytes[offset + i] = Integer.valueOf(
                        val >> (Byte.SIZE * i)).byteValue();
            }
        }
        return true;
    }

    public static boolean floatToByte(byte[] bytes, int offset, float val) {
        return intToByte(bytes, offset, Float.floatToIntBits(val));
    }

    /** Simple hex dump
     * @param bytes dumped byte objects.
     * @param len dumped length from the head of bytes object.
     */
    public static void hexDump(byte[] bytes, int len) {
        int i, j;

        System.out.printf("--- hex dump --- size: %d\n", len);
        for (i = 0; i < len; i+= 16) {
            System.out.printf("%08x", i);
            for (j = i; j < i + 16; j++) {
                if (j < len)
                    System.out.printf(" %02x", bytes[j]);
                else
                    System.out.printf(" --");
            }
            System.out.printf(" |");
            for (j = i; j < i + 16; j++) {
                if (j < len) {
                    if ((bytes[j] > 0x1f) && (bytes[j] < 0x7f))
                        System.out.printf("%1c", (char) bytes[j]);
                    else
                        System.out.printf(".");
                } else
                    System.out.printf(" ");
            }
            System.out.printf("|\n");
        }
        System.out.printf("----------------\n");
    }
}
