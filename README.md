# Axy Mod — Fabric 1.20.1

Transforma os jogadores na **Axy** (axolote): proporções customizadas, 8 skins de emoção, cauda e guelras de axolote e um **painel de emotes** (tecla **G**).

## Como compilar
Precisa de **JDK 17 ou superior** e internet (baixa Minecraft/Fabric na primeira vez).

```
./gradlew build          # Windows: gradlew.bat build
```
O mod fica em `build/libs/axymod-1.0.0.jar` (ignore o `-sources.jar`).
Também há um workflow do GitHub Actions (`.github/workflows/build.yml`): ao dar push, o jar é gerado e fica em Actions > build > Artifacts.

Para testar direto: `./gradlew runClient`.

## Como usar
- Instale **Fabric Loader** (>= 0.14.21) e **Fabric API** (0.92.x para 1.20.1) e coloque o `.jar` em `mods/`.
- **G** abre o painel: lista os 8 emotes pelo nome, com o rostinho de cada um, uma prévia do seu personagem tocando o emote e o botão **Modo Axy: LIGADO/DESLIGADO**. (A tecla pode ser trocada em Controles.)
- Emotes: Normal, Confused, Excited, Mad, Sad, Scared, Sick, Silly. Cada um troca a skin (expressão do rosto) e toca uma animação por ~5–6 s, depois volta ao Normal.
- Para os **outros jogadores verem** seus emotes o mod precisa estar também no servidor (em mundo single-player funciona direto). Sem o mod no servidor, o emote aparece só para você.

## Proporções (% do tamanho normal)
| Parte | Altura | Largura | Profundidade |
|---|---|---|---|
| Cabeça | 90% | 90% | 90% |
| Tronco | 75% | 70% | 70% |
| Braços | 75% | 60% | 70% |
| Pernas | 75% | 57% | 70% |

Os valores ficam no topo de `src/main/java/com/axymod/client/AxyAnimator.java` (constantes `HEAD`, `BODY_*`, `ARM_*`, `LEG_*`).

## Notas
- As skins do Axy usam braços finos (Alex), então o mod força o modelo "slim".
- O visual vale para todos os jogadores na sua tela (o botão do painel desliga). A caixa de colisão e a altura dos olhos continuam as do jogador normal.
- Cauda e guelras vêm do addon "Axolotl Parts" (autor: **Strippz**), convertidas do formato Bedrock para Java.
- O pack `LONGLIVECORAL` que veio no zip do addon (texturas de coral) não foi usado.
