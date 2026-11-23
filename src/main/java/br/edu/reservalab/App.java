package br.edu.reservalab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class App {
    private static final RepositorioReservas REPOSITORIO = new RepositorioSqlite();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8777"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/equipamentos", App::equipamentos);
        server.createContext("/api/reservas", App::reservas);
        server.createContext("/api/cancelar", App::cancelar);
        server.createContext("/", App::estatico);
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();
        System.out.println("ReservaLab disponível em http://localhost:" + port);
        System.out.println("Reinicie o servidor para verificar se as reservas continuam salvas.");
    }
    private static void equipamentos(HttpExchange e) throws IOException {
        if (!metodo(e, "GET")) return;
        try {
            String json = REPOSITORIO.listarEquipamentos().stream()
                .map(x -> "{\"id\":"+q(x.id())+",\"nome\":"+q(x.nome())+
                    ",\"categoria\":"+q(x.categoria())+",\"descricao\":"+q(x.descricao())+
                    ",\"simbolo\":"+q(x.simbolo())+"}")
                .reduce((a,b)->a+","+b).orElse("");
            enviar(e, 200, "["+json+"]");
        } catch (Exception ex) { falha(e, ex); }
    }
    private static void reservas(HttpExchange e) throws IOException {
        try {
            if (e.getRequestMethod().equals("GET")) {
                String json = REPOSITORIO.listarReservas().stream()
                    .map(App::jsonReserva).reduce((a,b)->a+","+b).orElse("");
                enviar(e, 200, "["+json+"]");
            } else if (e.getRequestMethod().equals("POST")) {
                Map<String,String> p = parametros(e);
                String equipamento = p.getOrDefault("equipamento", "");
                String nome = p.getOrDefault("nome", "").trim();
                String data = p.getOrDefault("data", "");
                String turno = p.getOrDefault("turno", "");
                validar(equipamento,nome,data,turno);
                Reserva r = REPOSITORIO.criar(equipamento, nome, data, turno);
                enviar(e, 201, jsonReserva(r));
            } else enviar(e,405,"{\"erro\":\"Método não permitido\"}");
        } catch (Exception ex) { falha(e,ex); }
    }
    private static void cancelar(HttpExchange e) throws IOException {
        if (!metodo(e,"POST")) return;
        try {
            String valor = parametros(e).getOrDefault("id", "");
            long id;
            try { id = Long.parseLong(valor); }
            catch (NumberFormatException ex) { throw new RegraException(400,"Identificador inválido."); }
            if (!REPOSITORIO.cancelar(id)) throw new RegraException(404,"Reserva ativa não encontrada.");
            enviar(e,200,"{\"mensagem\":\"Reserva cancelada com sucesso.\"}");
        } catch (Exception ex) { falha(e,ex); }
    }
    private static void validar(String equipamento, String nome, String data, String turno) {
        if (nome.length()<3 || nome.length()>60)
            throw new RegraException(400,"Informe um responsável com 3 a 60 caracteres.");
        if (REPOSITORIO.listarEquipamentos().stream().noneMatch(x -> x.id().equals(equipamento)))
            throw new RegraException(400,"Equipamento inválido.");
        if (!List.of("MANHA","TARDE","NOITE").contains(turno))
            throw new RegraException(400,"Turno inválido.");
        try {
            if (LocalDate.parse(data).isBefore(LocalDate.now()))
                throw new RegraException(400,"A data não pode ser anterior a hoje.");
        } catch (DateTimeParseException ex) { throw new RegraException(400,"Data inválida."); }
    }
    private static Map<String,String> parametros(HttpExchange e) throws IOException {
        String entrada = new String(e.getRequestBody().readNBytes(65537), StandardCharsets.UTF_8);
        if (entrada.length()>65536) throw new RegraException(400,"Requisição grande demais.");
        Map<String,String> p = new HashMap<>();
        for(String s:entrada.split("&")) {
            String[] a = s.split("=",2);
            if (a.length==2)
                p.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),
                      URLDecoder.decode(a[1],StandardCharsets.UTF_8));
        }
        return p;
    }
    private static String jsonReserva(Reserva r) {
        return "{\"id\":"+r.id()+",\"equipamentoId\":"+q(r.equipamentoId())+
            ",\"responsavel\":"+q(r.responsavel())+",\"data\":"+q(r.data())+
            ",\"turno\":"+q(r.turno())+",\"status\":"+q(r.status())+"}";
    }
    private static String q(String s) {
        StringBuilder b=new StringBuilder("\"");
        for(char c:s.toCharArray()) {
            switch(c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if(c<32) b.append(String.format("\\u%04x",(int)c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
    private static void estatico(HttpExchange e) throws IOException {
        if (!metodo(e,"GET")) return;
        String path=e.getRequestURI().getPath();
        String file=switch(path) {
            case "/" -> "index.html";
            case "/app.js" -> "app.js";
            case "/styles.css" -> "styles.css";
            default -> null;
        };
        if (file==null) { enviar(e,404,"{\"erro\":\"Não encontrado\"}"); return; }
        try(InputStream in=App.class.getResourceAsStream("/web/"+file)) {
            if(in==null) {enviar(e,404,"{\"erro\":\"Arquivo não encontrado\"}"); return;}
            byte[] content=in.readAllBytes();
            e.getResponseHeaders().set("Content-Type",file.endsWith("css")?"text/css; charset=UTF-8":
                file.endsWith("js")?"text/javascript; charset=UTF-8":"text/html; charset=UTF-8");
            e.getResponseHeaders().set("Cache-Control","no-store");
            e.sendResponseHeaders(200,content.length);
            try(var out=e.getResponseBody()){out.write(content);}
        }
    }
    private static boolean metodo(HttpExchange e,String expected) throws IOException {
        if(e.getRequestMethod().equals(expected)) return true;
        enviar(e,405,"{\"erro\":\"Método não permitido\"}");return false;
    }
    private static void falha(HttpExchange e,Exception ex) throws IOException {
        if(ex instanceof RegraException r)
            enviar(e,r.httpStatus(),"{\"erro\":"+q(r.getMessage())+"}");
        else {
            ex.printStackTrace();
            enviar(e,500,"{\"erro\":\"O servidor não conseguiu concluir a operação.\"}");
        }
    }
    private static void enviar(HttpExchange e,int code,String json) throws IOException {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
        e.getResponseHeaders().set("Cache-Control","no-store");
        e.sendResponseHeaders(code,bytes.length);
        try(var out=e.getResponseBody()){out.write(bytes);}
    }
}
