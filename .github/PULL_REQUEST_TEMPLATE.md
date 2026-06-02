## Contexto e Motivação

**Resolve Issue / Ticket:** #

## Tipo de Alteração

- [ ] **Bug fix** (Correção não-destrutiva que mitiga uma anomalia)
- [ ] **New feature** (Nova funcionalidade retrocompatível)
- [ ] **Breaking change** (Alteração arquitetónica, de contratos de API ou estruturas de dados que quebra a retrocompatibilidade)
- [ ] **Refactoring / Maintenance** (Otimização termodinâmica, atualização de dependências, reestruturação interna)

## Segurança e Conformidade Criptográfica

- [ ] Atesto que a alteração **não expõe** chaves efémeras, segredos de orquestração, material de assinatura ou tokens de produção.
- [ ] O código não introduz dependências não mapeadas ou não fixadas (*pinned*) nos ficheiros de bloqueio (`Cargo.lock`, `package-lock.json`, etc.).
- [ ] As alterações na máquina de estados ou roteamento suportaram a análise estática (Semgrep / CodeQL) sem introduzir novos bloqueios (Exit Code 1).
- [ ] (Se aplicável) Avaliei o impacto da alteração na topologia de privacidade e delegação de confiança da infraestrutura.

## Estratégia de Testes e Validação

- [ ] A suite de testes unitários e de integração padrão foi executada localmente com sucesso absoluto.
- [ ] Foram desenvolvidos e integrados novos testes automatizados que cobrem os ramos de execução introduzidos.
- [ ] (Se aplicável) Os módulos expostos a entradas não confiáveis passaram nas baterias de testes difusos (*Fuzzing*) sem emissão de *Panics* ou exaustão de memória.

## Checklist de Engenharia e Interfaces

- [ ] Efetuei uma auto-revisão analítica do código submetido.
- [ ] Adicionei comentários explicativos em blocos de elevada complexidade ciclomática, algoritmos de consenso ou manipulação de memória insegura (`unsafe` em Rust).
- [ ] A documentação de arquitetura (incluindo README.md e diagramas) foi sincronizada com esta alteração.
- [ ] O compilador e as ferramentas de *linting* (ex: `cargo clippy`, `eslint`) não emitem novos avisos estruturais (*warnings*).
- [ ] (Aplica-se à SPA/Mobile) As interfaces gráficas preservam os padrões de acessibilidade e a localização de cadeias de caracteres foi atualizada.
- [ ] Confirmo a inexistência de nós âncora de teste (Trust Anchors) ou terminais falsos (*mock endpoints*) ativos no caminho de produção.
