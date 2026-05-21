# D12 × 2 — Dice Roller

Aplicação Android que simula o lançamento de dois dados dodecaédricos (D12) com renderização 3D em tempo real, desenvolvida em Kotlin com Jetpack Compose.

## Arquitetura

* **MainActivity**: Ponto de entrada da aplicação, aplica o tema e carrega o ecrã principal
* **DiceRollerScreen**: Ecrã principal com os dois dados, soma e botão de lançamento
* **Color.kt**: Paleta de cores do tema (Blue40, DarkGray, LightGray)
* **Theme.kt**: Esquema Material 3 com cores claras aplicado a toda a aplicação
* **Type.kt**: Tipografia base da aplicação

## Funcionalidades

* Renderização 3D de dois dodecaedros com iluminação difusa e reflexo especular
* Rotação livre dos dados através de arrastamento tátil
* Animação de lançamento em três eixos com durações desfasadas
* Simulação de gravidade com efeito de ressalto
* Projeção dos números sobre cada face visível com transformação matricial
* Dois esquemas de cor: carmesim (CrimsonDie) e safira (SapphireDie)
* Apresentação da soma dos dois dados após a aterragem

## Estrutura

* 12 faces pentagonais por dado, definidas por 20 vértices e a constante PHI
* Ângulos-alvo pré-calculados para cada face (FACE_TARGETS)
* Curva de desaceleração personalizada (CubicBezierEasing)
* Simulação de física com velocidade inicial, gravidade e coeficiente de restituição

## Execução

Abrir o projeto no Android Studio, sincronizar o Gradle e clicar em **Run** para instalar num dispositivo ou emulador com API 24+.

Em alternativa, é possível compilar e instalar via terminal:

```
./gradlew installDebug
```

## Licença

GNU v3.0
