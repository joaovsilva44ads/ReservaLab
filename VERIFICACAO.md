# Registro de verificação – ReservaLab com SQLite

Testes manuais feitos pelo navegador em http://localhost:8777, reiniciando o servidor (Ctrl+C e executar.bat) entre as etapas.

| # | Teste | Resultado observado |
|---|-------|---------------------|
| 1 | Criar reserva pela interface | A reserva apareceu no histórico e "Reservas ativas" passou para 1 |
| 2 | Reservar o mesmo equipamento, data e turno | ______________________ |
| 3 | Reiniciar o servidor e atualizar a página | ______________________ |
| 4 | Cancelar a reserva | ______________________ |
| 5 | Reservar de novo o mesmo período | ______________________ |
| 6 | Reiniciar o servidor de novo | ______________________ |
| 7 | Conferir o catálogo | ______________________ |

Antes (RepositorioMemoria): as reservas sumiam ao reiniciar.
Depois (RepositorioSqlite): ______________________

## Decisões
- Banco em `data/reservalab.db`, criado automaticamente; tabelas `equipamentos` e `reservas` (chave estrangeira `equipamento_id`, `PRAGMA foreign_keys=ON`).
- Catálogo inserido com `INSERT OR IGNORE`, então reiniciar não duplica.
- Duplicidade impedida por índice único parcial `(equipamento_id, data, turno) WHERE status='ATIVA'`, que libera o período após o cancelamento.
- Cancelamento é `UPDATE ... SET status='CANCELADA'`; nada é apagado.
- Todos os parâmetros usam `PreparedStatement`.

## Como executar
`sh executar.sh` (Linux/macOS) ou `executar.bat` (Windows) e abrir http://localhost:8777.
