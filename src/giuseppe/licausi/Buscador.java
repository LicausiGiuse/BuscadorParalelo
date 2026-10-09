package giuseppe.licausi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Buscador {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Error: debes indicar una palabra y un archivo.");
            System.exit(1);
        }

        String palabra = args[0];
        String rutaArchivo = args[1];

        int apariciones = 0;
        int lineas = 0;

        long inicio = System.currentTimeMillis();
        long pid = ProcessHandle.current().pid();

        try (BufferedReader br = new BufferedReader(new FileReader("parametro.txt"))) {

            // (?i) = ignora mayúsculas/minúsculas
            // \\b = palabra completa
            Pattern patron = Pattern.compile(
                    "\\b" + Pattern.quote("cama") + "\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );

            String linea;

            while ((linea = br.readLine()) != null) {

                lineas++;

                Matcher matcher = patron.matcher(linea);

                while (matcher.find()) {
                    apariciones++;
                }
            }

            long tiempo = System.currentTimeMillis() - inicio;

            String nombreArchivo = new java.io.File(rutaArchivo).getName();

            System.out.println(
                    nombreArchivo + ";" +
                            apariciones + ";" +
                            lineas + ";" +
                            pid + ";" +
                            tiempo
            );

        } catch (IOException e) {

            System.err.println(
                    "Error al leer el archivo " +
                            rutaArchivo + ": " +
                            e.getMessage()
            );

            System.exit(1);
        }
    }
}