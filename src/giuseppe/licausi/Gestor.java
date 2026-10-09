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

        List<ProcesoActivo> procesosActivos = new ArrayList<>();
        try {
            for (File archivo : archivos) {
                // Si ya hay cuatro procesos hijos activos, esperamos a que termine uno.
                if (procesosActivos.size() == MAX_PROCESOS) {
                    ProcesoActivo terminado = procesosActivos.remove(0);
                    esperarProceso(terminado);
                }

                ProcesoActivo proceso = iniciarBuscador(palabra, archivo);
                if (proceso != null) {
                    procesosActivos.add(proceso);
                }
            }

            for (ProcesoActivo proceso : procesosActivos) {
                esperarProceso(proceso);
            }
        } catch (InterruptedException e) {
            for (ProcesoActivo proceso : procesosActivos) {
                proceso.proceso.destroyForcibly();
            }
            registrarError("El Gestor fue interrumpido mientras esperaba a los procesos Buscador.");
            return;
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

    private static ProcesoActivo iniciarBuscador(String palabra, File archivo) {
        String java = Paths.get(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString();
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
            return new ProcesoActivo(archivo, pb.start());
        } catch (IOException e) {
            registrarError("No se pudo iniciar el Buscador para " + archivo.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static void esperarProceso(ProcesoActivo activo) throws InterruptedException {
        int codigoSalida = activo.proceso.waitFor();
        if (codigoSalida != 0) {
            registrarError("El Buscador de " + activo.archivo.getName() + " terminó con código " + codigoSalida
                    + ". Consulta errores.log para ver el detalle.");
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

    private static class ProcesoActivo {
        private final File archivo;
        private final Process proceso;

        private ProcesoActivo(File archivo, Process proceso) {
            this.archivo = archivo;
            this.proceso = proceso;
        }
    }
}
