# Descetralized Design

O obejctivo deste trabalho criaçao de aucrions e bid concoreencias em rede p2p desctralizada devida em tre camada Securece Kamldei , DHT lEGEDER, aCTION aUCTION. A implentaçao da envolta da "privacidade de design" procurando resilncia, confiabilidade, fiabildiade integridade do si do sistem e consistecia eventual com disponibilidade.

## Threat Model

Sobre o design prmeira etapa de ançlise do srequsitos e anlise de ataque  dos problemas dest esistema sao veores de atques com~:
Sybil
eclipse
replay attack
hijack
spooding

## Least Recently Used


## Algoritmo de Cache: Least Recently Used (LRU)

O algoritmo  **Least Recently Used (LRU)** , ou "Menos Recentemente Utilizado", é uma política de substituição de cache que opera com base no princípio da  **temporalidade** . A sua premissa fundamental dita que os itens que foram acedidos recentemente têm uma maior probabilidade de serem acedidos num futuro próximo. Inversamente, os itens que não são utilizados há mais tempo são os candidatos primários à remoção quando a cache atinge a sua capacidade máxima e necessita de libertar espaço para novos dados.

### O Princípio Operacional e a Estrutura de Dados

A implementação eficaz do LRU exige uma estrutura de dados simbiótica que consiga realizar operações cruciais em tempo constante, ou **$O(1)$**. Para alcançar este desempenho, combina-se uma **Tabela de Dispersão (Hash Map)** com uma  **Lista Duplamente Ligada (Doubly Linked List)** .

1. **A Tabela de Dispersão (Hash Map):** Esta estrutura armazena as chaves dos itens e os respetivos ponteiros ou referências para os nós correspondentes na lista duplamente ligada. A sua função é garantir que a verificação da existência de um item (um *Cache Hit* ou  *Cache Miss* ) seja executada em tempo **$O(1)$**.
2. **A Lista Duplamente Ligada:** Esta lista ordena os itens com base na recência de acesso. A cabeça da lista (`Head`) detém o item **Most Recently Used (MRU)** (o mais recentemente utilizado), enquanto a cauda da lista (`Tail`) retém o item **Least Recently Used (LRU)** (o menos recentemente utilizado). Sempre que um item é acedido ou adicionado, ele é movido ou inserido na cabeça da lista. Se a cache estiver cheia, o item na cauda é removido.

### Exemplo Visual: Fluxo Operacional do LRU

A imagem abaixo ilustra o comportamento de uma cache LRU com capacidade definida para  **4 itens** . Ela demonstra duas operações fundamentais: o acesso a um item existente e a adição de um novo item com a cache cheia.

#### Análise do Fluxo na Imagem:

1. **Estado Inicial:** A cache começa vazia.
2. **Adição Sequencial:** Os itens `1, 2, 3, 4` são adicionados. O estado da cache (da cabeça à cauda) torna-se `[4, 3, 2, 1]`. O item `4` é o MRU e o item `1` é o LRU.
3. **Acesso (Cache Hit): Item 2.** O item `2` já existe na cache. Ao ser acedido, ele é movido da sua posição atual para a cabeça da lista. O novo estado é `[2, 4, 3, 1]`.`2` torna-se o MRU; `1` permanece o LRU.
4. **Adição (PUT) com Expulsão: Item 5.** A cache está cheia (capacidade 4). Ao tentar adicionar o item `5`, o algoritmo identifica o item **Least Recently Used** na cauda da lista (item `1`). O item `1` é **expulsado** ( *evicted* ). O item `5` é então inserido na cabeça da lista. O estado final é `[5, 2, 4, 3]`.

### Implementação: Por que Hash Map + Lista Duplamente Ligada?

A seção inferior da imagem detalha a estrutura de implementação:

* **Hash Map:** Permite pesquisas rápidas. Ao pesquisar a "Chave: 2", o mapa aponta diretamente para o nó correspondente na lista, em tempo **$O(1)$**.
* **Lista Duplamente Ligada:** Permite reordenações rápidas. Quando o item `2` é acedido, precisamos de o mover para a cabeça. Numa lista *simplesmente* ligada, teríamos de percorrer a lista para encontrar o nó anterior e atualizar os ponteiros, o que levaria tempo **$O(N)$**. Numa lista *duplamente* ligada, cada nó conhece o seu anterior e o seu próximo. Podemos "desligar" o nó da sua posição atual e "religá-lo" na cabeça, atualizando apenas um número fixo de ponteiros, tudo em tempo constante **$O(1)$**.
* **Conclusão:** O Hash Map fornece acesso rápido e a Lista Duplamente Ligada fornece reordenação rápida. Juntos, eles permitem que as operações `GET` (aceder) e `PUT` (adicionar) sejam executadas com complexidade de tempo constante **$O(1)$**, que é o desempenho ideal para um sistema de cache de alto desempenho.
