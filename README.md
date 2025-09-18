# PPGbetter
 better photopletysmograph from a mobile camera - android app


# Aplikacja do pomiaru tętna

W ramach projektu opracowano aplikację mobilną na system Android umożliwiającą pomiar tętna z wykorzystaniem fotopletyzmografii (PPG). Aplikacja rejestruje sygnał PPG za pomocą tylnej kamery smartfona oraz wbudowanej latarki, co pozwala na nieinwazyjne śledzenie pulsacji krwi w palcu użytkownika.

## Zasada działania

Użytkownik uruchamia aplikację i przykłada opuszek palca do tylnej kamery telefonu. Latarka automatycznie oświetla skórę, a kamera rejestruje obraz, w którym zmiany intensywności światła odbitego od tkanek odpowiadają cyklicznym zmianom objętości krwi.

Na ekranie aplikacji wyświetlany jest wykres sygnału PPG w czasie rzeczywistym, co umożliwia obserwację zmian rytmu serca w trakcie trwania pomiaru. Dodatkowo użytkownik może ręcznie oznaczać momenty wdechu i wydechu za pomocą dedykowanego przycisku. Każde takie zdarzenie zapisywane jest do pliku razem z znacznikiem czasu, co umożliwia późniejszą analizę zależności między cyklem oddechowym a tętnem.
<p align="center">
<img src="https://github.com/user-attachments/assets/2480a2f7-b0ac-4e8d-8412-3b42f78c8079" alt="Interfejs aplikacji mobilnej podczas pomiaru tętna" width="400"/>
</p>


## Przetwarzanie sygnału i obliczanie tętna

Dla każdej klatki nagrania wideo aplikacja wyodrębnia zielony kanał, który najdokładniej odwzorowuje zmiany przepływu krwi. Obliczana jest średnia wartość jasności pikseli, co tworzy jednowymiarowy sygnał czasowy – sygnał PPG.

Sygnał ten jest filtrowany przy użyciu filtra dolnoprzepustowego w celu redukcji szumów. Na podstawie przefiltrowanego sygnału identyfikowane są lokalne maksima, które odpowiadają kolejnym uderzeniom serca. Różnice czasowe między tymi pikami pozwalają na wyznaczenie tętna (BPM) według wzoru: `BPM = 60 / (średni czas między pikami [s])`


Wynik jest aktualizowany dynamicznie w trakcie trwania pomiaru, a dane — w tym wartości sygnału PPG oraz oznaczenia wdechów i wydechów — zapisywane są do pliku w celu dalszej analizy.

---

# Porównanie pomiaru tętna za pomocą aplikacji mobilnej i urządzenia Polar

W celu porównania dokładności pomiaru tętna przeanalizowano dane pochodzące z dwóch źródeł: aplikacji mobilnej wykorzystującej sygnał PPG oraz urządzenia Polar, którego zapis EKG został poddany analizie za pomocą algorytmu AI opartego na detekcji szczytów R i technikach przetwarzania sygnałów.

## Pierwszy pomiar

<p align="center">
<img src="https://github.com/user-attachments/assets/5c8a0faa-e583-48bc-b7ac-819e85a7022a" alt="Zestawienie HR: aplikacja mobilna a urządzenie Polar z algorytmem AI" height="400"/>
</p>

Na pierwszym wykresie widoczna jest wyraźna różnica między przebiegiem tętna uzyskanego z aplikacji mobilnej a pomiarem EKG z urządzenia Polar. Aplikacja generuje bardziej wygładzony i stabilny sygnał, z wartościami tętna w wąskim zakresie 80–85 bpm. W przeciwieństwie do niej, metoda oparta na detekcji szczytów R wykazuje większą zmienność rytmu, z wahaniami od około 75 do 100 bpm. Pomiar EKG dokładniej odwzorowuje krótkoterminowe zmiany fizjologiczne, ale może być bardziej podatny na zakłócenia. Zauważalny wzrost tętna w przedziale 40–60 sekundy może wynikać z chwilowego wysiłku fizycznego lub zmiany pozycji ciała.

## Drugi pomiar

<p align="center">
<img src="https://github.com/user-attachments/assets/bd6179b1-4373-4dbb-bd0c-010c1f069eec" alt="Zestawienie HR: aplikacja mobilna a urządzenie Polar z algorytmem AI" height="400"/>
</p>


Na drugim wykresie przedstawiono dane z innego okresu rejestracji, w którym badana osoba pozostawała w spoczynku. Oba przebiegi, z aplikacji mobilnej oraz z urządzenia Polar, wykazują większą zbieżność. Wartości tętna mieszczą się w węższym zakresie, od 80 do 85 uderzeń na minutę, choć w sygnale uzyskanym metodą opartą na detekcji szczytów R nadal widoczna jest nieco większa zmienność. Mniejsze różnice między metodami sugerują, że w stabilnych warunkach ich dokładność może być porównywalna. Mimo to EKG pozostaje bardziej precyzyjne w rejestrowaniu krótkoterminowych wahań rytmu serca.

## Obserwacje

Pomiar tętna z wykorzystaniem aplikacji mobilnej opartej na PPG charakteryzuje się większą stabilnością sygnału, ze względu na wygładzenia sygnału oraz mniejszą wrażliwość na krótkotrwałe wahania. Natomiast metoda oparta na analizie sygnału EKG dla urządzenia Polar cechuje się większą reaktywnością, skutkując bardziej dynamicznym przebiegiem sygnału, ale również większą podatnością na zakłócenia. W zależności od przyjętego celu, ogólnego monitorowania parametrów życiowych lub analizy krótkoterminowych zmian, każda z metod wykazuje odmienne zalety i ograniczenia.


