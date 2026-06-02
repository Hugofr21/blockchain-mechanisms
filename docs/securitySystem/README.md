# Security Sytem

O **SecureAuction Ledger** é uma solução **blockchain P2P** para realização de leilões e licitações descentralizadas.
A aplicação combina:

* **Keycloak** – fornecedor de identidade (OIDC / PKCE).
* **ModSecurity WAF** – inspeção de camada 7 com regras OWASP CRS 3.3.
* **API‑Gateway** – rate‑limit, validação de JWT, logging centralizado.
* **Peer‑Nodes** – rede Kademlia, PoW‑tied NodeId, assinatura ECDSA‑PoP, nonce + timestamp + HLC, opcional TLS 1.3.
* **Observability Stack** – OpenTelemetry ➜ Prometheus ➜ Grafana (autenticação via Keycloak).
* **Vault** – armazenamento de chaves (JCEKS) e de segredos críticos.

### Principais componentes que **já** possuem controles de segurança

| Componente                  | Controles existentes                                                                                                        |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Keycloak**          | OIDC + PKCE, usuários/roles, rotação de tokens, login multifator opcional                                                |
| **WAF (ModSecurity)** | Regras OWASP CRS 3.3, proteção contra XSS/SQLi/CSRF                                                                       |
| **API‑Gateway**      | Rate‑limit, JWT validation, logging de request/response                                                                    |
| **P2P Nodes**         | PoW‑tied NodeId, assinatura ECDSA (Proof‑of‑Possession), Nonce + Timestamp, HLC, Kademlia routing, TLS 1.3 (opcional) |
| **Obs. Stack**        | Telemetria OpenTelemetry, dashboards Grafana com Auth via Keycloak                                                          |
| **Vault**             | Armazenamento de chaves privadas (JCEKS) encriptadas, controle de acesso com policies                                       |

## Acotes & Assets

## ATTACQUES

CSRF
XSS
SYBIL
ELECPSE
REPLAY ATTACK
Buffer Overlow

## OWASP Top 10 ↔ STRIDE ↔ Mitigações

| OWASP Top 10 (2023)                                     | STRIDE                            | Vetor de Ataque na P2P                                          | Contramedidas Implementadas / Recomendadas                                                      |
| -------------------------------------------------------- | --------------------------------- | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| A01 –**Broken Access Control**                    | Spoofing, Elevation of Privilege  | Uso indevido de JWT/Token, acesso a rotas internas do peer      | mTLS + PoA token, políticas de IAM no Vault, validação de scopes (`aud`, `iss`).         |
| A02 –**Cryptographic Failures**                   | Information Disclosure, Tampering | Chaves privadas em plaintext, uso de criptografia proprietária | JCEKS + HSM, TLS 1.3, AES‑256‑GCM, rotação de chaves a cada 30 dias, Public‑Key Pinning. |
| A03 –**Injection**                                | Tampering                         | Mensagens de rede com campos não sanitizados, smart‑contracts | Input validation (JSON schema), whitelist de campos, Slither + MythX para contratos.            |
| A04 –**Insecure Design**                          | Tampering, Repudiation            | Falta de replay protection, falta de assinatura de mensagens    | Nonce + HLC + Message‑Seq, assinatura ECDSA (PoP) em cada transação.                         |
| A05 –**Security Misconfiguration**                | All                               | Docker containers rodando como root, portas expostas            | Hardening de containers, seccomp/AppArmor, rede privada, portas internas não mapeadas.         |
| A06 –**Vulnerable & Out‑of‑Date Components**    | All                               | Bibliotecas Java sem patches, imagens com CVE                   | Trivy scan CI/CD, atualização automática de dependências via Dependabot.                    |
| A07 –**Identification & Authentication Failures** | Spoofing, Repudiation             | Tokens JWT sem revogação, chaves de nó não verificadas      | Revogação via Keycloak, mTLS, token de bootstrap revogável.                                  |
| A08 –**Software & Data Integrity Failures**       | Tampering, Repudiation            | Manipulação de blocos, falta de verificação de hash         | Merkle root + assinatura do bloco, imutabilidade de logs, PoW + PoS.                            |
| A09 –**Security Logging & Monitoring Failure**    | Repudiation                       | Ausência de logs auditáveis                                   | Logs structured + Elastic + write‑once, alertas SIEM.                                          |
| A10 –**Server‑Side Request Forgery (SSRF)**      | Spoofing                          | Peers podem solicitar recursos internos via `GET_DATA`        | Whitelisting de endereços IP, validação de URL; limite de redirecionamentos.                 |
