package org.graph.infrastructure.storage;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Este componente de armazenamento deve ser projetado considerando explicitamente a ocorrência de condições de corrida
 * decorrentes do modelo de operação da rede Kademlia. Em ambientes distribuídos, um mesmo nó pode receber múltiplas
 * subscrições quase simultâneas para valores ou objetos logicamente relacionados, provenientes de diferentes pares da rede.
 * Essas interações concorrentes introduzem riscos reais de inconsistência quando não há coordenação adequada entre as
 * operações de leitura e escrita.
 *
 * Cada máquina participante mantém apenas uma visão parcial e local do estado global do sistema, operando segundo um ciclo
 * de Read-Modity-Read que não é, por natureza, transacional. Quando duas ou mais subscrições são processadas em paralelo,
 * não há garantia de que uma operação consiga observar os efeitos completos da outra. Como consequência direta, podem ocorrer
 * leituras incompletas ou divergentes durante operações como {@code findValue}, comprometendo a convergência dos dados distribuídos na rede Kademlia.
 *
 * O raciocínio de assumir que a simples repetição de leituras resolverá essas inconsistências é falho, pois ignora a ausência de
 * ordenação global e de exclusão mútua entre nós independentes. Sem um mecanismo explícito de resolução de conflitos,
 * o sistema permanece vulnerável a sobrescritas silenciosas e à perda de atualizações concorrentes.
 *
 * Para mitigar esse problema, é indispensável adotar uma estratégia determinística de mesclagem de objetos,
 * especialmente quando os valores armazenados são estruturas compostas, como listas ou coleções agregadas.
 * Essa estratégia de merge deve ser idempotente, associativa e comutativa, garantindo que diferentes ordens de
 * aplicação das atualizações conduzam ao mesmo estado final. A ausência dessa abordagem representa não apenas um erro de implementação,
 * mas uma violação conceitual dos princípios básicos de consistência eventual em sistemas distribuídos.
 */

public class StorageDHT {
    private final ConcurrentHashMap<BigInteger, StoredValue> data;

    public StorageDHT(){
        this.data = new ConcurrentHashMap<>();
    }

    private record StoredValue(Object value, Class<?> type) {}

    public void put(BigInteger id, Object newValue) {
        Objects.requireNonNull(id, "The key identifier cannot be null.");
        Objects.requireNonNull(newValue, "The value to be stored cannot be null.");

        data.compute(id, (key, existing) -> {
            if (existing != null && existing.value() instanceof Set<?> currentSet && newValue instanceof Set<?> newSet) {

                Set<Object> mergedSet = new HashSet<>(currentSet);
                mergedSet.addAll(newSet);
                return new StoredValue(mergedSet, HashSet.class);

            } else if (newValue instanceof Set<?> newSet) {
                return new StoredValue(new HashSet<>(newSet), HashSet.class);

            } else {
                return new StoredValue(newValue, newValue.getClass());
            }
        });
    }

    public <T> T get(BigInteger id, Class<T> clazz) {
        StoredValue sv = data.get(id);
        if (sv == null) return null;

        if (! clazz.isAssignableFrom(sv.type)) {
            System.err.println("[Storage] Type inconsistency: Expected " +
                    clazz.getSimpleName() + " but found " + sv.type.getSimpleName());

            return null;

        }
        return clazz.cast(sv.value);
    }

    public int size() {
        return data.size();
    }

    public Map<String, String> getAllDataSnapshot() {
        Map<String, String> snapshot = new HashMap<>();

        for (Map.Entry<BigInteger, StoredValue> entry : data.entrySet()) {
            String keyHex = entry.getKey().toString(16);
            Object val = entry.getValue().value();

            String valStr;
            if (val instanceof Set) {
                valStr = "[PUB/SUB List] Number of Subscribers: " + ((Set<?>) val).size();
            } else if (val.getClass().getSimpleName().equals("Block")) {
                valStr = "[BLOCK] " + val.toString();
            } else {
                valStr = val.toString();
            }

            snapshot.put(keyHex, valStr);
        }
        return snapshot;
    }
}