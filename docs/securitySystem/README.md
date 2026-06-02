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

## Escopo & Ativos (Assets)

| Tipo de Ativo                                                   | Descrição                                                | Classificação de Impacto* |
| --------------------------------------------------------------- | ---------------------------------------------------------- | --------------------------- |
| **Identidades de Usuário** (Keycloak)                    | Credenciais, MFA, atributos de papel.                      | **Alto**              |
| **Tokens JWT**                                            | Access / Refresh com escopos.                            | **Alto**              |
| **Chaves Privadas dos Peer‑Nodes** (Vault)               | Keystore JCEKS, usados para assinatura PoP e TLS mTLS.    | **Alto**              |
| **Ledger (Blockchain)**                                   | Cadeia de blocos imutável contendo todas as licitações. | **Crítico**          |
| **Dados de Licitação** (valor, participante, timestamp) | Informações de negócio sensíveis.                      | **Alto**              |
| **Segredos de Infraestrutura** (DB passwords, API keys)   | Credenciais de PostgreSQL, Vault, etc.                     | **Alto**              |
| **Código da Aplicação**                                | Repositório Git, pipelines CI/CD.                         | **Alto**              |
| **Métricas / Telemetria**                                | Dados de performance e saúde dos nós.                    | **Médio**            |
| **Logs de Auditoria**                                     | JSON‑structured logs de eventos críticos.                | **Alto**              |

## Vetores de Ataque Identificados

| Vetor                                                 | Descrição                                                                                         | Controle Atual                                                               | Gap / Observação                                                                |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| **CSRF**                                        | Ataque de*Cross‑Site Request Forgery* contra endpoints do API‑Gateway.                          | SameSite = Strict + anti‑CSRF token nos formulários web.                 | Tokens ainda transmitidos em query‑string em alguns endpoints legados.           |
| **XSS**                                         | Injeção de scripts via campos de descrição de leilão.                                          | WAF com regras OWASP CRS, sanitização no front‑end.                      | Falta de*Content‑Security‑Policy* (CSP) forte.                                |
| **Sybil**                                       | Criação de múltiplas identidades de nó para influenciar consenso.                               | PoW‑tied NodeId, Proof‑of‑Authority bootstrap token.                     | PoW pode ser evitado com hardware dedicado; necessidade de*Stake* adicional.    |
| **Replay Attack**                               | Reenvio de mensagens válidas (ex.:`HELLO`, `BID`) para contornar *nonce* ou  *timestamp* . | Nonce + timestamp + janela de ±5 s, HLC.                                 | Não há verificação de*replay‑counter* em algumas mensagens  *GET_DATA* . |
| **Buffer Overflow**                             | Mensagens com payload > 2 MiB podem overflow buffers C‑style.                                    | Header `payloadLength` limitado a 2 MiB, validação no `MessageUtils`. | Código antigo em C‑bindings (para libs de crypto) ainda não revisado.          |
| **Man‑in‑the‑Middle (MITM)**                 | Interceptação de comunicação P2P ou API‑Gateway.                                               | TLS 1.3 opcional, assinatura de mensagens, HMAC.                            | TLS opcional → ainda aceito conexões sem criptografia.                          |
| **Eclipse**                                     | Conquista de todos os buckets de routing de um peer.                                                | Kademlia*k*= 20, refresh de buckets a cada 5 min.                        | Não há verificação de diversidade de rede (sub‑net).                         |
| **Double‑Spend**                               | Envio da mesma licitação em blocos diferentes.                                                    | Verificação de `txId` no ledger, orfan‑pool.                            | Risco aumentado se o quorum for 2‑of‑3 (menor que 3).                           |
| **DDoS/Rate‑Limiting**                         | Flood de mensagens `INV`, `GET_BLOCK`.                                                          | Rate‑limit no API‑Gateway; limit na camada P2P (token‑bucket, 10 msg/s). | Falta de*burst* control para mensagens de  *heartbeat* .                      |
| **SQL/NoSQL Injection** (Back‑end do Keycloak) | Injeção via parâmetros de login.                                                                 | Keycloak usa prepared statements.                                            | Não testado com fuzzer de parâmetros avançado.                                 |

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
