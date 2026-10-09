package giuseppe.licausi;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Buscador {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Error: debes indicar exactamente una palabra y un archivo.");
            System.exit(1);
        }

        String palabra = args[0];
        if (palabra == null || palabra.trim().isEmpty()) {
            System.err.println("Error: la palabra a buscar no puede estar vacía.");
            System.exit(1);
        }

        Path rutaArchivo = Paths.get(args[1]);
        if (!Files.isRegularFile(rutaArchivo) || !Files.isReadable(rutaArchivo)) {
            System.err.println("Error: no se puede leer un archivo regular en la ruta " + rutaArchivo + ".");
            System.exit(1);
        }

        int apariciones = 0;
        int lineas = 0;
        long inicio = System.nanoTime();
        long pid = ProcessHandle.current().pid();
        Pattern patron = Pattern.compile(
                "(?U)\\b" + Pattern.quote(palabra) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );

        try (BufferedReader br = Files.newBufferedReader(rutaArchivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas++;
                Matcher matcher = patron.matcher(linea);
                while (matcher.find()) {
                    apariciones++;
                }
            }

            long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
            System.out.println(rutaArchivo.getFileName() + ";" + apariciones + ";" + lineas + ";" + pid + ";" + tiempoMs);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo " + rutaArchivo + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
