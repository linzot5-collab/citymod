# City Mod (Forge 1.21.1)

Сборка: JDK 21, затем `gradle wrapper` (один раз) и `./gradlew build`. Jar: build/libs/.
Запуск для теста: `./gradlew runClient`. Город строится сам при первом запуске нового мира (около спавна).

Команды: /balance /pay /bank (card|deposit|withdraw|credit|repay|queue|done) /chips (buy|sell|sellall)
/dice <фишки> <ник|dealer> (+ accept/decline) /roulette <ставка> <фишки> /bj (bet|hit|stand|double)
/wheel <фишки> /slots <фишки> /horses bet <1-6> <фишки> /job <имя> /city spawn
Операторам: /givemoney <игрок> <сумма>, /npc spawn <роль> [скин]

Телефон (предмет "Телефон", ПКМ; выдаётся при первом входе, также /phone): приложения Крипто, Майнинг, Банк, Кошелёк.
Команды телефона: /crypto buy <0-3> <$> | /crypto sell <0-3> <%> | /mining buy <id> | /mining target <0-3>
