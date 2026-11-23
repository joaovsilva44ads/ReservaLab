# O Sistema que Esquecia Tudo: ReservaLab

## Situação-problema

O **ReservaLab** é um sistema web usado pelo laboratório de uma instituição de ensino para organizar a reserva de equipamentos (câmera, projetor, microfone, notebook e tablet).

A aplicação permite consultar o catálogo, reservar um equipamento para uma data e um turno, acompanhar agendamentos e cancelar reservas. Tudo parece funcionar... **até o servidor ser desligado**. Ao reiniciar, todas as reservas desaparecem.

Sua equipe foi contratada para corrigir esse problema: substituir o armazenamento temporário por um **banco de dados SQLite integrado ao back-end Java com JDBC**, preservando a interface e as regras de negócio.

**O objetivo não é recriar o site nem reescrever a aplicação inteira.** O desafio está em fazer a aplicação web armazenar, consultar e alterar dados reais em um banco relacional.

## O que o projeto já oferece

- Interface completa em **HTML, CSS e JavaScript**.
- Servidor **Java 17+** com endpoints HTTP e validações de entrada.
- Catálogo de cinco equipamentos.
- Cadastro, listagem e cancelamento de reservas em memória.
- Scripts de execução para Windows e Linux/macOS, sem Maven; as bibliotecas JDBC necessárias já estão incluídas na pasta `lib/`.
- Interface `RepositorioReservas` e classe `RepositorioSqlite` com pontos de implementação.

A execução inicial usa `RepositorioMemoria`, portanto a perda de dados após reiniciar **é intencional**.

## Preparação e execução

Para executar o ReservaLab, você precisa do **JDK 17 ou superior**, com os comandos `java` e `javac` disponíveis. **Não é necessário instalar Maven, SQLite ou bibliotecas adicionais**: os arquivos JAR necessários já acompanham o projeto na pasta `lib/`.

1. Clone o repositório disponibilizado pelo professor ou baixe os arquivos do projeto no GitHub.
2. Abra a pasta do projeto, onde estão o `README.md` e os scripts de execução.
3. Inicie o sistema de acordo com o sistema operacional:
   - **Windows:** execute `executar.bat`.
   - **Linux/macOS:** abra o terminal na pasta do projeto e execute `sh executar.sh`.
4. Acesse **http://localhost:8777** no navegador.

### Bibliotecas JDBC

As bibliotecas necessárias à conexão com o SQLite **já estão incluídas na pasta `lib/`**. Basta iniciar o projeto pelo script correspondente ao seu sistema operacional: ele utiliza essas bibliotecas automaticamente, sem instalação adicional e sem depender de internet para obtê-las.

Não exclua os arquivos `.jar` da pasta `lib/`, pois eles são necessários para executar a implementação com JDBC.

### Verificando o problema

A versão inicial usa `RepositorioMemoria`, por isso ainda **não salva os dados definitivamente**. Para observar esse comportamento:

1. Cadastre uma reserva na interface.
2. Encerre o servidor pressionando `Ctrl+C` na janela do terminal.
3. Inicie o sistema novamente pelo mesmo script.
4. Consulte o histórico e observe que a reserva desapareceu.

Depois de implementar a solução com SQLite, a reserva deverá permanecer registrada mesmo após reiniciar a aplicação.

A porta padrão é `8777` e pode ser alterada pela variável de ambiente `PORT`. O arquivo do banco da solução será armazenado em `data/reservalab.db`; **não inclua esse arquivo na entrega nem o publique no GitHub**.

## Desafios obrigatórios

### 1: Modelar e criar o banco de dados

Crie as tabelas `equipamentos` e `reservas` em SQLite. A tabela de reservas deve guardar: identificador, equipamento, responsável, data, turno e status. Defina chave primária e vínculo entre a reserva e o equipamento.

A criação do banco deve acontecer de forma automática na inicialização, quando ele ainda não existir. O catálogo deve ser preenchido apenas na primeira execução, sem duplicar equipamentos em reinicializações posteriores.

### 2: Consultar equipamentos do banco

Implemente `listarEquipamentos()` em `RepositorioSqlite`, utilizando JDBC. O catálogo exibido no navegador deve passar a vir de um `SELECT`, e não de uma lista fixa em memória.

### 3: Salvar e listar reservas

Implemente `criar()` e `listarReservas()` com instruções SQL e `PreparedStatement` quando houver parâmetros externos. Cada nova reserva deve gerar um identificador e aparecer no histórico mesmo após reiniciar o servidor.

### 4: Cancelar sem apagar o histórico

Implemente `cancelar()`: o status de uma reserva ativa deve mudar para `CANCELADA`. Não exclua fisicamente esse registro, pois o sistema precisa preservar o histórico de agendamentos.

### 5: Garantir as regras de negócio

- O mesmo equipamento **não pode ter duas reservas ativas para a mesma data e turno**.
- Uma reserva cancelada **libera novamente** aquele equipamento no período correspondente.
- Uma reserva não pode referenciar um equipamento inexistente.
- O sistema deve continuar rejeitando nomes, datas e turnos inválidos.
- As consultas e alterações devem utilizar parâmetros adequados, sem concatenar entradas do usuário no SQL.

### 6: Integrar ao sistema já existente

Depois de concluir a classe `RepositorioSqlite`, altere a inicialização em `App.java` para utilizar o novo repositório. O front-end não deve perder nenhuma funcionalidade.

## Como saber se está pronto?

1. Crie uma reserva pelo navegador e verifique que aparece no histórico.
2. Reinicie o servidor: **a reserva deve continuar lá**.
3. Tente reservar o mesmo equipamento, dia e turno: o sistema deve rejeitar a duplicação.
4. Cancele a primeira reserva e tente reservar novamente o mesmo período: agora deve funcionar.
5. Reinicie o servidor de novo: tanto a reserva cancelada quanto a nova reserva ativa devem permanecer no histórico.
6. Confirme que reiniciar a aplicação não duplica os cinco equipamentos iniciais.

## Entrega

Entregue o código-fonte com a implementação JDBC, as instruções para executar e **um registro curto da verificação**, informando quais testes manuais você realizou e o que observou antes e depois da reinicialização.

O arquivo `.db`, a pasta `build/` e outras saídas geradas localmente não devem fazer parte da entrega. Mantenha as bibliotecas `.jar` fornecidas na pasta `lib/`, para que o projeto possa ser executado sem configuração adicional.

## Critérios de avaliação

Serão observados: persistência real dos dados, integração Java/SQLite via JDBC, operações de cadastro/consulta/cancelamento, respeito às regras de negócio, organização do código e evidências de funcionamento.

**Não há necessidade de apresentação formal.** A conclusão das funcionalidades obrigatórias é suficiente para demonstrar o objetivo da atividade; recursos extras são opcionais e não substituem os requisitos.

## Estrutura do projeto

```text
ReservaLab/
├── src/
│   └── main/
│       ├── java/br/edu/reservalab/
│       │   ├── App.java                 # Servidor HTTP e requisições
│       │   ├── RepositorioReservas.java  # Contrato de acesso aos dados
│       │   ├── RepositorioMemoria.java   # Armazenamento temporário atual
│       │   ├── RepositorioSqlite.java    # Implementação JDBC a desenvolver
│       │   ├── Equipamento.java         # Modelo de equipamento
│       │   ├── Reserva.java             # Modelo de reserva
│       │   └── RegraException.java      # Erros de negócio
│       └── resources/web/
│           ├── index.html               # Interface
│           ├── app.js                   # Comunicação com o servidor
│           └── styles.css               # Estilos
├── ferramentas/
│   └── PrepararDependencias.java        # Verificação auxiliar das bibliotecas
├── lib/                                  # Bibliotecas JDBC já fornecidas
├── executar.bat                          # Inicia o projeto no Windows
├── executar.sh                           # Inicia no Linux/macOS
├── .gitignore                            # Exclui arquivos gerados do Git
└── README.md                             # Instruções e enunciado
```

A pasta oculta `.git/` pode estar presente quando o projeto é clonado; ela contém o histórico local de controle de versão e não participa da execução da aplicação. As pastas `build/` e `data/` poderão ser criadas durante o uso do sistema e não devem ser publicadas.

**Atenção:** o projeto é um exercício educacional e não implementa autenticação de usuários. Execute-o apenas no ambiente local de desenvolvimento.
