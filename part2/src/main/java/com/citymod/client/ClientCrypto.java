package com.citymod.client;

/** Данные телефона на клиенте. */
public final class ClientCrypto {
    public static boolean loaded = false;
    public static double[] prices = {42000, 2400, 95, 0.12};
    public static double[][] hist = new double[4][0];
    public static double[] hold = new double[4];
    public static int racks, target;
    public static int[] gpus = new int[3];
    public static int[] coolant = new int[3];
    public static double hash, mult, income;

    private ClientCrypto() {}
}
