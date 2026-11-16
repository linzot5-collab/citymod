package com.citymod.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Описание меню: кнопка = подпись + серверная команда.
 * {a} в команде заменяется выбранной суммой ставки. Сервер проверяет всё сам.
 */
public final class Menus {
    public record Entry(String label, String cmd) {}

    private Menus() {}

    private static Entry e(String label, String cmd) { return new Entry(label, cmd); }

    public static boolean usesAmount(String id) {
        return switch (id) {
            case "chips", "roulette", "blackjack", "wheel", "horses", "dice", "slots" -> true;
            default -> false;
        };
    }

    public static String title(String id) {
        return switch (id) {
            case "bank" -> "Банк";
            case "chips" -> "Касса казино: фишки";
            case "roulette" -> "Американская рулетка";
            case "blackjack" -> "Блэкджек";
            case "wheel" -> "Колесо фортуны";
            case "horses" -> "Скачки (кони: x2.6 / 3.8 / 5.5 / 8 / 12 / 20)";
            case "dice" -> "Кости (против дилера)";
            case "slots" -> "Игровой автомат";
            case "shop" -> "Супермаркет";
            case "clothes" -> "Магазин одежды";
            case "bar" -> "Бар";
            case "jobs" -> "Биржа труда";
            default -> "Меню";
        };
    }

    public static List<Entry> entries(String id) {
        List<Entry> l = new ArrayList<>();
        switch (id) {
            case "bank" -> {
                l.add(e("Оформить карту (50 $)", "bank card"));
                l.add(e("Внести 100 $", "bank deposit 100"));
                l.add(e("Внести 500 $", "bank deposit 500"));
                l.add(e("Внести всё", "bank depositall"));
                l.add(e("Снять 100 $", "bank withdraw 100"));
                l.add(e("Снять 500 $", "bank withdraw 500"));
                l.add(e("Снять всё", "bank withdrawall"));
                l.add(e("Кредит 500 $", "bank credit 500"));
                l.add(e("Кредит 2000 $", "bank credit 2000"));
                l.add(e("Погасить 500 $", "bank repay 500"));
                l.add(e("Погасить всё", "bank repay 100000"));
                l.add(e("Освободить окно", "bank done"));
            }
            case "chips" -> {
                l.add(e("Купить фишки (сумма)", "chips buy {a}"));
                l.add(e("Продать фишки (сумма)", "chips sell {a}"));
                l.add(e("Продать все фишки", "chips sellall"));
            }
            case "roulette" -> {
                l.add(e("Красное (x2)", "roulette red {a}"));
                l.add(e("Чёрное (x2)", "roulette black {a}"));
                l.add(e("Чётное (x2)", "roulette even {a}"));
                l.add(e("Нечётное (x2)", "roulette odd {a}"));
                l.add(e("1-18 (x2)", "roulette low {a}"));
                l.add(e("19-36 (x2)", "roulette high {a}"));
                l.add(e("1-я дюжина (x3)", "roulette dozen1 {a}"));
                l.add(e("2-я дюжина (x3)", "roulette dozen2 {a}"));
                l.add(e("3-я дюжина (x3)", "roulette dozen3 {a}"));
                l.add(e("Зеро 0 (x36)", "roulette 0 {a}"));
                l.add(e("Дабл-зеро 00 (x36)", "roulette 00 {a}"));
            }
            case "blackjack" -> {
                l.add(e("Сделать ставку", "bj bet {a}"));
                l.add(e("Взять карту", "bj hit"));
                l.add(e("Хватит", "bj stand"));
                l.add(e("Удвоить", "bj double"));
            }
            case "wheel" -> l.add(e("Крутить колесо!", "wheel {a}"));
            case "slots" -> l.add(e("Крутить барабаны", "slots {a}"));
            case "dice" -> l.add(e("Бросить кости против дилера", "dice {a} dealer"));
            case "horses" -> {
                l.add(e("№1 Молния (x2.6)", "horses bet 1 {a}"));
                l.add(e("№2 Ветер (x3.8)", "horses bet 2 {a}"));
                l.add(e("№3 Гром (x5.5)", "horses bet 3 {a}"));
                l.add(e("№4 Искра (x8)", "horses bet 4 {a}"));
                l.add(e("№5 Тень (x12)", "horses bet 5 {a}"));
                l.add(e("№6 Комета (x20)", "horses bet 6 {a}"));
            }
            case "shop" -> {
                l.add(e("Хлеб x3 — 5 $", "shop buy bread"));
                l.add(e("Стейк x4 — 12 $", "shop buy steak"));
                l.add(e("Факелы x16 — 8 $", "shop buy torch"));
                l.add(e("Железный топор — 70 $", "shop buy iron_axe"));
                l.add(e("Железная кирка — 80 $", "shop buy iron_pickaxe"));
                l.add(e("Ножницы — 40 $", "shop buy shears"));
            }
            case "clothes" -> {
                l.add(e("Кожаная шапка — 15 $", "shop buy helmet"));
                l.add(e("Кожаная куртка — 30 $", "shop buy chestplate"));
                l.add(e("Кожаные штаны — 25 $", "shop buy leggings"));
                l.add(e("Кожаные ботинки — 15 $", "shop buy boots"));
            }
            case "bar" -> {
                l.add(e("Пиво — 8 $", "shop buy beer"));
                l.add(e("Коктейль (скорость) — 15 $", "shop buy cocktail"));
            }
            case "jobs" -> {
                l.add(e("Дровосек (4 $ за бревно)", "job lumberjack"));
                l.add(e("Грузчик (12 $ за ящик)", "job loader"));
                l.add(e("Ремонтник дорог (10 $ за ремонт)", "job roadworker"));
                l.add(e("Инкассатор (25 $ за банкомат)", "job collector"));
                l.add(e("Уволиться", "job none"));
            }
            default -> { }
        }
        return l;
    }
}
