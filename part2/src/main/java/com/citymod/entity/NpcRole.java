package com.citymod.entity;

/** Роль NPC, меню, которое открывается по правому клику, и реплики. */
public enum NpcRole {
    CITIZEN("Горожанин", null, new String[]{
            "Добро пожаловать в наш город!",
            "Говорят, в казино сегодня кому-то повезло.",
            "Работы в городе хватает: можно рубить лес или таскать грузы.",
            "Не забудьте оформить карту в банке."}),
    SHOPKEEPER("Продавец", "shop", new String[]{
            "Здравствуйте! Что-нибудь подсказать?",
            "Товар свежий, цены честные.",
            "Для дровосека рекомендую железный топор."}),
    BANKER("Банкир", "queue", new String[]{
            "Добро пожаловать в банк. Карта стоит 50 $.",
            "Кредит выдаём до 5000 $, комиссия 10%.",
            "Встаньте в очередь, вас вызовут к окну."}),
    CROUPIER("Крупье", "chips", new String[]{
            "Делайте ваши ставки!",
            "Фишки можно купить здесь же, в кассе.",
            "Удача любит смелых."}),
    FOREMAN("Прораб", "jobs", new String[]{
            "Нужны люди! Дровосеки, грузчики, ремонтники, инкассаторы.",
            "Платим за каждое выполненное действие.",
            "Хорошая работа — хорошие деньги."}),
    BARTENDER("Бармен", "bar", new String[]{
            "Что будете пить?",
            "В клубе сегодня отличный диджей.",
            "Музыка играет до утра."}),
    CLOTHIER("Продавец одежды", "clothes", new String[]{
            "Новая коллекция уже на полках!",
            "Кожа сейчас в моде."}),
    DJ("Диджей", null, new String[]{
            "Как вам трек?",
            "Сегодня играем всю ночь!"});

    public final String title;
    public final String menu;
    private final String[] lines;

    NpcRole(String title, String menu, String[] lines) {
        this.title = title;
        this.menu = menu;
        this.lines = lines;
    }

    public String randomLine(net.minecraft.util.RandomSource rnd) {
        return lines[rnd.nextInt(lines.length)];
    }

    public static NpcRole byId(int id) {
        NpcRole[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
