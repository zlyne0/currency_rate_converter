Aplikacja ma zazadanie pomóc w wypisaniu pit-38 na podstawie raportu tranzakcji i dywident z exante.
Aplikacja ma głowne trzy funkcjonalności. Uruchamia się je z ulubionego IDE z metod "main"

# 1. Funkcjonalność przliczania walut.
Wartości w obcych walutach dla danego dnia są przeliczane na PLN na pracujący dzień wstecz. 
Kursy walut pobrane są ze stron NBP  
```
https://nbp.pl/statystyka-i-sprawozdawczosc/kursy/archiwum-tabela-a-csv-xls/
```
Bezpośredni link np.
```
https://static.nbp.pl/dane/kursy/Archiwum/archiwum_tab_a_2023.xls
```

Sposób użycia uruchomienie metody main w pliku promitech/currencyrateconverter/Main.kt

```kotlin
val moneyInOutFile = CurrencyRateInOutFile("currency_rate.xlsx", "currency_rate_output.xlsx")
```
Na wejściu podaje się pliku z wartościami w obcych walutach, na wyjściu powstaje pliku 
z przeliczonymi wartościami w PLN

# 2. Przeliczenie podatku od dywident
Uruchomienie main w pliku promitech/report/exante/MainDivident.kt

```kotlin
val report = DividentReport("divident.xlsx", "divident_output.xls")
```
Na wejściu plik raportu divident z exante dla wybranego roku. 
Na standardowe wyjście wyświetlane jest podsumowanie. 
Podsumowanie jest także wpisywane do generowanego pliku.

Przykład podsumowania
```
Podsumowanie
  Suma dywidendy: 6000.00 PLN
  Rzeczywisty podatek zaplacony u zrodla: 800.00 PLN, (nie wpisuje w deklaracje bo pozycja 46 nie moze przekracac kwoty z pozycji 45)
Pola z deklaracji pit-38
  (Pole 45) PL 19% naleznego podatku od dywidend: 1200.00 PLN
  (Pole 46) Podatek zaplacony u zrodla: 700.00 PLN
  (Pole 47) Roznica: 400 PLN
  (Pole 49) Podatek do zaplaty: 400 PLN
```

# 3. Przeliczanie podatku od tranzakcji
Uruchomienie main w pliku promitech/report/exante/MainTrades.kt
```kotlin
val tradeReport = TradeReport("trade.xlsx", "trade_output.xlsx", rateRepository)
```
Na wejściu plik raportu tranzakcji, najlepiej od początku otwarcia konta,
bo trzeba przeliczać wartość w PLN otwieranej pozycji z roku wstecz.

Przykład podsumowania
```
Podsumowanie PIT-38 rok 2023
  Inne koszty/prowizje: 500.80 PLN
  Przychod (Pole 22): 150000.00 PLN
  Koszt uzyskania przychodu (Pole 23): 120000.0000 PLN
  Dochod (Pole 26): 9000.00 PLN
Podsumowanie PIZ/ZG
1.
  Panstwo (Pole 6): Japonia
  Kod kraju (Pole 7): JP
  Dochód (Pole 29): 3000.00 PLN
2.
  Panstwo (Pole 6): Stany Zjedn. Ameryki
  Kod kraju (Pole 7): US
  Dochód (Pole 29): 7000.00 PLN
```

### Autor nie ponosi odpowiedzialności z nieprawidłowe działanie aplikacji. Używasz jej na własną odpowiedzialność. 