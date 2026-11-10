# Axy Mod — Fabric 1.20.1

Transforma jogadores na **Axy** (axolote) — só quem usar **/axy morph**; todo o resto continua com o modelo vanilla normal: proporções customizadas, 8 skins de emoção, cauda e guelras de axolote e um **painel de emotes** (tecla **G**).

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
- **/axy morph** transforma você na Axy e **/axy unmorph** volta ao normal (qualquer jogador pode usar em si mesmo).
  Operadores (nível 2) também podem usar em outros: `/axy morph <jogadores>` e `/axy unmorph <jogadores>` (aceita @a, @p, nomes...).
  Quem está transformado fica salvo no mundo (`axymod_morphed.txt`) e todos que têm o mod veem a transformação.
- **G** abre o painel (os emotes só funcionam para quem está transformado; o botão do modo Axy executa os mesmos comandos): lista os 8 emotes pelo nome, com o rostinho de cada um, uma prévia do seu personagem tocando o emote e o botão **Modo Axy: LIGADO/DESLIGADO**. (A tecla pode ser trocada em Controles.)
- Emotes: Normal, Confused, Excited, Mad, Sad, Scared, Sick, Silly. Cada um troca a skin (expressão do rosto) e toca uma animação por ~5–6 s, depois volta ao Normal.
- O mod precisa estar também no servidor para os comandos existirem e para os outros jogadores verem a transformação e os emotes (em mundo single-player funciona direto).

## Proporções (% do tamanho normal)
| Parte | Altura | Largura | Profundidade |
|---|---|---|---|
| Cabeça | 90% | 90% | 90% |
| Tronco | 60% | 70% | 70% |
| Braços | 60% | 60% | 70% |
| Pernas | 60% | 57% | 70% |

Os valores ficam no topo de `src/main/java/com/axymod/client/AxyAnimator.java` (constantes `HEAD`, `BODY_*`, `ARM_*`, `LEG_*`).

## Notas
- As skins do Axy usam braços finos (Alex), então o mod força o modelo "slim".
- O visual só vale para quem está transformado. A caixa de colisão e a altura dos olhos continuam as do jogador normal.
- Cauda e guelras vêm do addon "Axolotl Parts" (autor: **Strippz**), convertidas do formato Bedrock para Java.
- O pack `LONGLIVECORAL` que veio no zip do addon (texturas de coral) não foi usado.

## Correção de bugs nas animações (v1.0.1)
- O modelo do jogador é reaproveitado entre frames e o vanilla não reescreve todos os valores. O Axy
  multiplicava/somava em cima deles a cada frame (pivô X das pernas, pivô X de cabeça/tronco, roll da cabeça),
  então o erro acumulava: pernas se fundiam no meio, cabeça saía do lugar e ficava torta.
  Agora `AxyAnimator.restoreVanillaBase` restaura esses valores todo frame antes de aplicar proporções e emotes.

## Morph por comando (v1.1.0)
- Antes o visual Axy valia para todos os jogadores; agora só vale para quem usou `/axy morph`.
- Novo estado de "transformado" guardado no servidor e sincronizado com os clientes (pacote `morph_s2c`).

## Ajustes v1.1.1
- **Itens na mão**: o vanilla coloca o item a 10px do ombro (mão do braço normal). Como o braço da Axy é mais curto, o item agora sobe na mesma proporção (`AxyAnimator.shiftHeldItem`).
- **Elytra e capa**: eram desenhadas a partir do tronco vanilla (maior e mais alto). Agora são deslocadas e escaladas junto com o tronco da Axy (`AxyAnimator.bodyAttachTransform`).
- **Agachar**: o vanilla abaixa o modelo 2px ao agachar, o que enterrava as pernas curtas no chão. Isso foi zerado para quem está transformado (`AxyAnimator.SNEAK_OFFSET_Y`; se os pés flutuarem, use algo entre -0.125 e 0).

## Novo visual do painel (v1.2.0)
- Painel de emotes redesenhado: fundo preto com linhas de grade finas, título pixelado colorido, botão azul de destaque para o modo Axy
  e brilho da cor do emote (inspirado no site do Essential), mantendo a prévia do personagem e o rostinho de cada emote.
- Logo da Scute AI no cabeçalho e no rodapé (`assets/axymod/textures/gui/scute_logo_*.png`).
- Cores de cada emote e do tema ficam no topo de `EmotePanelScreen.java`.
