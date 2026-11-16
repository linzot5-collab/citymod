package com.citymod.client;

/** Последние данные, полученные клиентом от сервера. */
public final class ClientData {
    public static long cash, bank, credit, earned, chips;
    public static String job = "";

    private ClientData() {}

    public static String jobTitle() {
        return switch (job) {
            case "lumberjack" -> "Дровосек";
            case "loader" -> "Грузчик";
            case "roadworker" -> "Ремонтник дорог";
            case "collector" -> "Инкассатор";
            default -> "нет";
        };
    }
}
