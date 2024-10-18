package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ServidorMulti {
    static HashMap<Integer,UnCliente> clientes = new HashMap<>();
    static List<UnCliente> usuariosConectados = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 1;
        while ( true ){
            Socket s= servidorSocket.accept();
            System.out.println("Conexion establecida con el cliente #" + contador);
            UnCliente unCliente = new UnCliente(s);
            Thread hilo = new Thread(unCliente);
            clientes.put(contador, unCliente);
            hilo.start();
            contador++;
        }
    }
}