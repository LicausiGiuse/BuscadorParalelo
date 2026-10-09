package giuseppe.licausi;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Gestor {

    private static final int MAX_PROCESOS = 4;

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println(
                    "Uso: java giuseppe.licausi.Gestor palabra carpeta"
            );
            return;
        }

        String palabra = args[0];
        File carpeta = new File(args[1]);

        // Comprobar que existe
        if (!carpeta.exists()) {
            System.err.println("Error: la carpeta no existe.");
            return;
        }

        // Comprobar que es una carpeta
        if (!carpeta.isDirectory()) {
            System.err.println("Error: la ruta indicada no es una carpeta.");
            return;
        }

        // Obtener solamente los .txt
        File[] archivos = carpeta.listFiles(
                file -> file.isFile() &&
                        file.getName().toLowerCase().endsWith(".txt")
        );

        if (archivos == null) {
            System.err.println("Error al acceder a la carpeta.");
            return;
        }

        if (archivos.length == 0) {
            System.out.println("No se han encontrado archivos .txt.");
            return;
        }

        /*
         * Máximo 4 procesos simultáneos.
         */
        ExecutorService executor =
                Executors.newFixedThreadPool(MAX_PROCESOS);

        List<Future<String>> resultados = new ArrayList<>();

        /*
         * Crear los procesos.
         */
        for (File archivo : archivos) {

            Future<String> resultado = executor.submit(
                    () -> ejecutarBuscador(palabra, archivo)
            );

            resultados.add(resultado);
        }

        /*
         * Esperamos a que terminen todos.
         */
        executor.shutdown();

        /*
         * Crear informe y log de errores.
         */
        try (
                BufferedWriter informe = new BufferedWriter(
                        new FileWriter("informe.txt")
                );

                BufferedWriter errores = new BufferedWriter(
                        new FileWriter("errores.log")
                )
        ) {

            for (Future<String> resultado : resultados) {

                try {

                    String salida = resultado.get();

                    if (salida != null && !salida.isEmpty()) {

                        informe.write(salida);
                        informe.newLine();
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    errores.write(
                            "El proceso Gestor fue interrumpido: "
                                    + e.getMessage()
                    );

                    errores.newLine();

                } catch (ExecutionException e) {

                    errores.write(
                            "Error ejecutando un Buscador: "
                                    + e.getCause()
                    );

                    errores.newLine();
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "Error al crear los archivos de salida: "
                            + e.getMessage()
            );
        }

        System.out.println("Proceso terminado.");
        System.out.println("Consulta informe.txt y errores.log.");
    }


    private static String ejecutarBuscador(
            String palabra,
            File archivo
    ) {

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "giuseppe.licausi.Buscador",
                palabra,
                archivo.getAbsolutePath()
        );

        try {

            Process proceso = pb.start();

            /*
             * Leer salida estándar del Buscador.
             */
            BufferedReader salida = new BufferedReader(
                    new InputStreamReader(
                            proceso.getInputStream()
                    )
            );

            /*
             * Leer salida de error.
             */
            BufferedReader error = new BufferedReader(
                    new InputStreamReader(
                            proceso.getErrorStream()
                    )
            );

            StringBuilder resultado = new StringBuilder();

            String linea;

            while ((linea = salida.readLine()) != null) {

                resultado.append(linea);
            }

            /*
             * Esperar a que termine el proceso.
             */
            int codigoSalida = proceso.waitFor();

            /*
             * Si hubo errores, los devolvemos para
             * que el Gestor pueda registrarlos.
             */
            if (codigoSalida != 0) {

                StringBuilder mensajeError = new StringBuilder();

                while ((linea = error.readLine()) != null) {

                    mensajeError.append(linea).append("\n");
                }

                throw new IOException(
                        mensajeError.toString()
                );
            }

            return resultado.toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Error ejecutando Buscador para "
                            + archivo.getName()
                            + ": "
                            + e.getMessage()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Proceso interrumpido: "
                            + archivo.getName()
            );
        }
    }
}