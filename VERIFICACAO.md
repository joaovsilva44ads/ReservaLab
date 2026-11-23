# Registro de verificação – ReservaLab com SQLite

Testes manuais feitos via HTTP (curl) contra o servidor em http://localhost:8777, reiniciando o processo entre as etapas.

| # | Teste | Resultado observado |
|---|-------|---------------------|
| 1 | Criar reserva (CAM, 2027-01-15, MANHA) | 201, id 1, status ATIVA |
| 2 | Mesma reserva por outra pessoa | 409 "Esse equipamento já está reservado nesse dia e turno." |
| 3 | Equipamento inexistente / turno, data ou nome inválidos | 400 com a mensagem de cada regra |
| 4 | **Reiniciar o servidor** e listar reservas | Reserva 1 continua no histórico (ATIVA) |
| 5 | Cancelar reserva 1 | 200; cancelar de novo retorna 404 |
| 6 | Reservar de novo o mesmo período | 201, id 2, ATIVA (período liberado) |
| 7 | **Reiniciar de novo** e listar | Reserva 2 (ATIVA) e reserva 1 (CANCELADA) presentes |
| 8 | Catálogo após as reinicializações | Continua com 5 equipamentos, sem duplicar |

Antes (RepositorioMemoria): reservas sumiam ao reiniciar. Depois (RepositorioSqlite): permanecem.

## Decisões
- Banco em `data/reservalab.db`, criado automaticamente; tabelas `equipamentos` e `reservas` (FK `equipamento_id`, `PRAGMA foreign_keys=ON`).
- Catálogo inserido com `INSERT OR IGNORE`, então reiniciar não duplica.
- Duplicidade impedida por índice único parcial `(equipamento_id, data, turno) WHERE status='ATIVA'`, que é atômico e libera o período após o cancelamento.
- Cancelamento é `UPDATE ... SET status='CANCELADA'`; nada é apagado.
- Todos os parâmetros usam `PreparedStatement`.

## Como executar
`sh executar.sh` (Linux/macOS) ou `executar.bat` (Windows) e abrir http://localhost:8777.
