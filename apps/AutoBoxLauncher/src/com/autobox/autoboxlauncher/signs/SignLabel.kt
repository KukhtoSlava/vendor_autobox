package com.polishsigns

/**
 * GTSRB class ID to Polish sign code and description mapping.
 */
data class SignLabel(
    val id: Int,
    val polishCode: String,
    val description: String,
)

object SignLabels {
    val ALL = listOf(
        SignLabel(0,  "B-33(20)",  "Ograniczenie prędkości 20 km/h"),
        SignLabel(1,  "B-33(30)",  "Ograniczenie prędkości 30 km/h"),
        SignLabel(2,  "B-33(50)",  "Ograniczenie prędkości 50 km/h"),
        SignLabel(3,  "B-33(60)",  "Ograniczenie prędkości 60 km/h"),
        SignLabel(4,  "B-33(70)",  "Ograniczenie prędkości 70 km/h"),
        SignLabel(5,  "B-33(80)",  "Ograniczenie prędkości 80 km/h"),
        SignLabel(6,  "B-34",      "Koniec ograniczenia prędkości 80 km/h"),
        SignLabel(7,  "B-33(100)", "Ograniczenie prędkości 100 km/h"),
        SignLabel(8,  "B-33(120)", "Ograniczenie prędkości 120 km/h"),
        SignLabel(9,  "B-25",      "Zakaz wyprzedzania"),
        SignLabel(10, "B-26",      "Zakaz wyprzedzania przez pojazdy ciężarowe"),
        SignLabel(11, "A-6a",      "Pierwszeństwo na zwężeniu"),
        SignLabel(12, "D-1",       "Droga z pierwszeństwem przejazdu"),
        SignLabel(13, "A-7",       "Ustąp pierwszeństwa"),
        SignLabel(14, "B-20",      "Stop"),
        SignLabel(15, "B-1",       "Zakaz ruchu"),
        SignLabel(16, "B-5",       "Zakaz wjazdu pojazdów ciężarowych"),
        SignLabel(17, "B-2",       "Zakaz wjazdu"),
        SignLabel(18, "A-30",      "Inne niebezpieczeństwo"),
        SignLabel(19, "A-2",       "Niebezpieczny zakręt w lewo"),
        SignLabel(20, "A-1",       "Niebezpieczny zakręt w prawo"),
        SignLabel(21, "A-3",       "Niebezpieczne zakręty"),
        SignLabel(22, "A-11",      "Nierówna droga"),
        SignLabel(23, "A-18",      "Śliska jezdnia"),
        SignLabel(24, "A-12a",     "Zwężenie jezdni z prawej strony"),
        SignLabel(25, "A-14",      "Roboty drogowe"),
        SignLabel(26, "A-29",      "Sygnały świetlne"),
        SignLabel(27, "A-16",      "Przejście dla pieszych"),
        SignLabel(28, "A-17",      "Dzieci"),
        SignLabel(29, "A-24",      "Rowerzyści"),
        SignLabel(30, "A-32",      "Oblodzenie drogi"),
        SignLabel(31, "A-18b",     "Dzikie zwierzęta"),
        SignLabel(32, "B-34",      "Koniec zakazów"),
        SignLabel(33, "C-2",       "Nakaz jazdy w prawo za znakiem"),
        SignLabel(34, "C-3",       "Nakaz jazdy w lewo za znakiem"),
        SignLabel(35, "C-5",       "Nakaz jazdy prosto"),
        SignLabel(36, "C-6",       "Nakaz jazdy prosto lub w prawo"),
        SignLabel(37, "C-7",       "Nakaz jazdy prosto lub w lewo"),
        SignLabel(38, "C-9",       "Nakaz jazdy z prawej strony znaku"),
        SignLabel(39, "C-10",      "Nakaz jazdy z lewej strony znaku"),
        SignLabel(40, "C-12",      "Rondo"),
        SignLabel(41, "B-25(end)", "Koniec zakazu wyprzedzania"),
        SignLabel(42, "B-26(end)", "Koniec zakazu wyprzedzania przez pojazdy ciężarowe"),
    )
}
