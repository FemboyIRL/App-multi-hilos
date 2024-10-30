
import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class ParaRecibir implements Runnable {

    final DataInputStream entrada;

    public ParaRecibir(Socket s) throws IOException {
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();
                switch (mensaje) {

                    case "/file":
                        String nombreArchivo = entrada.readUTF();
                        String contenidoArchivo = entrada.readUTF();
                        crearArchivoRecibido(contenidoArchivo, nombreArchivo);
                        break;

                    default:
                        System.out.println(mensaje);
                }
            } catch (IOException ex) {
                System.out.println("Un error inesperado ha ocurrido " + ex);
                break;
            }
        }
    }

    public void crearArchivoRecibido(String contenido, String nombre) throws IOException {
        FileWriter fw = new FileWriter(System.getProperty("user.dir") + "/src/Recibidos/" + nombre);
        fw.write(contenido);
        fw.close();
    }

    public boolean crearDirectorioParaRecibir() {
        return new File(System.getProperty("user.dir") + "/src/Recibidos/").mkdir();
    }

}
