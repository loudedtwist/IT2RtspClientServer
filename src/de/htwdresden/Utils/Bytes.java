package de.htwdresden.Utils;

import com.sun.istack.internal.NotNull;

public class Bytes {
    public static byte[] xor(@NotNull byte[] arr1, @NotNull byte[] arr2) {
        int maxLength = Math.max(arr1.length, arr2.length);

        byte[] newArray = new byte[maxLength];

        for (int i = 0; i < arr1.length; i++) {
            newArray[i] = (byte) (newArray[i] ^ arr1[i]);
        }
        for (int i = 0; i < arr2.length; i++) {
            newArray[i] = (byte) (newArray[i] ^ arr2[i]);
        }

        return newArray;
    }

    public static void print(@NotNull byte[] a) {
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i]);
            if (i != a.length - 1) System.out.print(", ");
        }
        System.out.println();
    }
}
