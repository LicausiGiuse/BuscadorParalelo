package giuseppe.licausi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Gestor {

    private static final int MAX_PROCESOS = 4;
    private static final Path INFORME = Paths.get("informe.txt");
    private static final Path ERRORES = Paths.get("errores.log");

    public static void main(String[] args) {
        if (!inicializarSalidas()) {
            return;
        }

        if (args.length != 2) {
            registrarError("Uso: java giuseppe.licausi.Gestor palabra carpeta. Debes indicar exactamente dos parámetros.");
            return;
        }

        String palabra = args[0];
        if (palabra == null || palabra.trim().isEmpty()) {
            registrarError("La palabra a buscar no puede estar vacía.");
            return;
        }

        File carpeta = new File(args[1]);
        if (!carpeta.exists()) {
            registrarError("La carpeta no existe: " + carpeta.getAbsolutePath());
            return;
        }
        if (!carpeta.isDirectory()) {
            registrarError("La ruta indicada no es una carpeta: " + carpeta.getAbsolutePath());
            return;
        }

        Path rutaInforme = INFORME.toAbsolutePath().normalize();
        File[] archivos = carpeta.listFiles(file ->
                file.isFile()
                        && !file.toPath().toAbsolutePath().normalize().equals(rutaInforme)
                        && file.getName().toLowerCase(Locale.ROOT).endsWith(".txt"));
        if (archivos == null) {
            registrarError("No se pudo leer el contenido de la carpeta: " + carpeta.getAbsolutePath());
            return;
        }
        if (archivos.length == 0) {
            escribirInforme("No se han encontrado archivos .txt en " + carpeta.getAbsolutePath());
            System.out.println("No se han encontrado archivos .txt.");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(MAX_PROCESOS);
        List<Future<String>> resultados = new ArrayList<>();
        try {
            for (File archivo : archivos) {
                resultados.add(executor.submit(() -> ejecutarBuscador(palabra, archivo)));
            }
            executor.shutdown();

            for (Future<String> resultado : resultados) {
                try {
                    String error = resultado.get();
                    if (error != null) {
                        registrarError(error);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                    registrarError("El proceso Gestor fue interrumpido mientras esperaba a los Buscadores.");
                    return;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    registrarError("Error ejecutando un Buscador: " + causa.getMessage());
                }
            }
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }

        System.out.println("Proceso terminado. Consulta informe.txt y errores.log.");
    }

    private static boolean inicializarSalidas() {
        try {
            Files.write(INFORME, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(ERRORES, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("No se pudieron crear informe.txt y errores.log: " + e.getMessage());
            return false;
        }
    }

    private static String ejecutarBuscador(String palabra, File archivo) {
        String java = Paths.get(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        ProcessBuilder pb = new ProcessBuilder(
                java,
                "-cp",
                System.getProperty("java.class.path"),
                "giuseppe.licausi.Buscador",
                palabra,
                archivo.getAbsolutePath()
        );
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(INFORME.toFile()));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(ERRORES.toFile()));

        try {
            Process proceso = pb.start();
            int codigoSalida = proceso.waitFor();
            if (codigoSalida != 0) {
                return "El Buscador de " + archivo.getName() + " terminó con código " + codigoSalida
                        + ". Consulta errores.log para ver el detalle.";
            }
            return null;
        } catch (IOException e) {
            return "No se pudo iniciar el Buscador para " + archivo.getName() + ": " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Se interrumpió el Buscador para " + archivo.getName() + ".";
        }
    }

    private static void escribirInforme(String mensaje) {
        try {
            Files.writeString(INFORME, mensaje + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            registrarError("No se pudo escribir informe.txt: " + e.getMessage());
        }
    }

    private static void registrarError(String mensaje) {
        System.err.println(mensaje);
        try {
            Files.writeString(ERRORES, mensaje + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Tampoco se pudo escribir errores.log: " + e.getMessage());
        }
    }
}


