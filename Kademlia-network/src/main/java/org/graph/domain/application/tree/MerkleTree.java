package org.graph.domain.application.tree;


import java.util.ArrayList;
import java.util.List;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.utils.HashUtils;

public class MerkleTree {
    private MerkleNode root;
    private final List<MerkleNode> leaves;

    public MerkleTree(List<Transaction> transactions) {
        this.leaves = new ArrayList<>();
        if (transactions != null && !transactions.isEmpty()) {
            buildTree(transactions);
        }
    }


    public String getRootHash() {
        return root == null ? "" : root.getHash();
    }

    private void buildTree(List<Transaction> transactions) {
        List<MerkleNode> nodes = new ArrayList<>();
        for (Transaction tx : transactions) {
            String txHash = HashUtils.calculateSha256(tx.getTxId());
            MerkleNode leaf = new MerkleNode(txHash);
            nodes.add(leaf);
            leaves.add(leaf);
        }

        while (nodes.size() > 1) {
            List<MerkleNode> nextLevel = new ArrayList<>();

            for (int i = 0; i < nodes.size(); i += 2) {
                MerkleNode left = nodes.get(i);
                MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : left;

                MerkleNode parent = new MerkleNode(left, right);
                nextLevel.add(parent);
            }
            nodes = nextLevel;
        }

        this.root = nodes.getFirst();
    }


    /**
     * Gera a Prova de Merkle (Merkle Path) para uma transação específica.
     * Complexidade: O(log N) - Muito mais rápido que reconstruir a árvore.
     */
    public List<String> getProof(String targetTxHash) {
        List<String> proof = new ArrayList<>();

        // 1. Encontrar a folha (O(N) linear scan na lista de folhas ou O(1) se usar Map)
        MerkleNode current = null;
        for (MerkleNode leaf : leaves) {
            if (leaf.getHash().equals(targetTxHash)) {
                current = leaf;
                break;
            }
        }

        if (current == null) return proof; // Transação não existe neste bloco

        // 2. Navegar para cima coletando os irmãos
        while (current != this.root && current.getParent() != null) {
            MerkleNode parent = current.getParent();
            MerkleNode left = parent.getLeft();
            MerkleNode right = parent.getRight();

            // Se eu sou a esquerda, preciso do hash da direita para provar
            if (current == left) {
                proof.add(right.getHash());
            } else {
                // Se eu sou a direita, preciso do hash da esquerda
                proof.add(left.getHash());
            }

            current = parent; // Sobe um nível
        }

        return proof;
    }

    /**
     * MÉTODO ESTÁTICO DE VALIDAÇÃO (Client Side)
     * Isso roda no cliente leve ou outro peer para validar a prova sem ter o bloco todo.
     * * @param rootHash O hash da raiz do bloco (Block Header)
     * @param txHash O hash da transação que queremos verificar
     * @param proof A lista de hashes irmãos fornecida pela rede
     * @param index O índice da transação no bloco (necessário para saber a ordem de concatenação)
     */
    public static boolean verifyProof(String rootHash, String txHash, List<String> proof, int index) {
        String computedHash = txHash;

        for (String siblingHash : proof) {
            if (index % 2 == 0) {
                // Eu sou par (esquerda), então irmão é direita: Hash(Eu + Irmão)
                computedHash = HashUtils.calculateSha256(computedHash + siblingHash);
            } else {
                // Eu sou ímpar (direita), então irmão é esquerda: Hash(Irmão + Eu)
                computedHash = HashUtils.calculateSha256(siblingHash + computedHash);
            }
            index /= 2; // Sobe para o próximo nível
        }

        return computedHash.equals(rootHash);
    }


    public int depthMerkle(MerkleNode node) {
        if (node == null) {
            return 0;
        }
        return Math.max(depthMerkle(node.getLeft()), depthMerkle(node.getRight())) + 1 ;
    }

    public void display(MerkleNode node, String prefix, boolean isLeft) {
        if (node != null) {
            if (isLeft) {
                display(node.getLeft(), prefix + "|    ", false);
            }

            System.out.println(prefix + (isLeft ? "└────────── " : "┌──────────── ") + node.getHash());
            display(node.getRight(), prefix + (isLeft ? "    " : "│   "), true);

        }

    }

}
