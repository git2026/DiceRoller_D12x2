# Dice Roller - D12 × 2

Aplicação Android que simula o lançamento de dois dados de 12 faces (D12) com renderização 3D em tempo real, desenvolvida em Kotlin com Jetpack Compose.

## Arquitetura

* **MainActivity**: Aplica o tema e carrega o ecrã principal
* **DiceRollerScreen**: Ecrã principal com os dois dados, soma e botão de lançamento
* **Color.kt**: Kit de cores do tema (Blue40, DarkGray, LightGray)
* **Theme.kt**: Esquema Material 3 com cores claras aplicado a toda a aplicação
* **Type.kt**: Tipografia base da aplicação

## Funcionalidades

* Renderização 3D de dois dodecaedros com iluminação e reflexo
* Rotação livre dos dados através de arrastamento tátil
* Animação de lançamento em três eixos com durações desfasadas
* Simulação de gravidade com efeito de ressalto
* Projeção dos números sobre cada face visível com transformação matricial
* Apresentação da soma dos dois dados após a queda

## Estrutura

* 12 faces pentagonais por dado, definidas por 20 vértices e a constante PHI
* Ângulos-alvo pré-calculados para cada face (FACE_TARGETS)
* Curva de desaceleração personalizada (CubicBezierEasing)
* Simulação de física com velocidade inicial, gravidade e coeficiente de restituição

## Execução

Abrir o projeto no Android Studio, sincronizar o Gradle e clicar em **Run** para instalar num dispositivo ou emulador com API 24+

Em alternativa, é possível compilar e instalar via terminal, com o seguinte comando:

```
./gradlew installDebug
```

## Licença

GNU v3.0
