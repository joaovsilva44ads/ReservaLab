import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.jar.JarFile;

/** Baixa as bibliotecas JDBC sem Maven (uma unica vez). Requer JDK 17+. */
public class PrepararDependencias {
    private static final String BASE = "https://repo.maven.apache.org/maven2/";
    private record Dependencia(String pasta, String arquivo, String classe) {}
    private static final Dependencia[] LIBS = {
        new Dependencia("org/xerial/sqlite-jdbc/3.54.1.0/", "sqlite-jdbc-3.54.1.0.jar", "org/sqlite/JDBC.class"),
        new Dependencia("org/slf4j/slf4j-api/2.0.17/", "slf4j-api-2.0.17.jar", "org/slf4j/Logger.class"),
        new Dependencia("org/slf4j/slf4j-nop/2.0.17/", "slf4j-nop-2.0.17.jar", "org/slf4j/nop/NOPServiceProvider.class")
    };
    private static boolean valido(Path caminho, String classe) {
        if (!Files.isRegularFile(caminho)) return false;
        try (JarFile jar = new JarFile(caminho.toFile())) { return jar.getEntry(classe) != null; }
        catch (IOException ex) { return false; }
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("lib"));
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        for (Dependencia d : LIBS) {
            Path destino = Path.of("lib", d.arquivo());
            if (valido(destino, d.classe())) { System.out.println("OK: " + destino); continue; }
            System.out.println("Obtendo biblioteca: " + d.arquivo());
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + d.pasta() + d.arquivo()))
                .timeout(Duration.ofSeconds(90)).GET().build();
            Path temp = destino.resolveSibling(d.arquivo() + ".incompleto");
            try {
                HttpResponse<Path> res = http.send(req, HttpResponse.BodyHandlers.ofFile(temp));
                if (res.statusCode() != 200 || !valido(temp, d.classe()))
                    throw new IOException("Arquivo invalido ou HTTP " + res.statusCode());
                Files.move(temp, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("OK: " + destino);
            } catch (Exception ex) {
                System.err.println("Nao foi possivel obter " + d.arquivo() + ": " + ex.getMessage());
                System.err.println("Baixe manualmente em: " + BASE + d.pasta() + d.arquivo());
                System.err.println("Coloque o arquivo na pasta lib/ e execute novamente.");
                Files.deleteIfExists(temp);
                System.exit(1);
            }
        }
        System.out.println("Bibliotecas JDBC prontas. Nao e necessario Maven.");
    }
}
