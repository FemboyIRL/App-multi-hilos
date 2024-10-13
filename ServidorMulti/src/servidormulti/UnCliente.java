package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import methods.UserRepository;
import models.User;

class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    User user;

    UnCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            salida.writeUTF("Por favor, registre un nombre de usuario:");
            String nombre = entrada.readUTF();
            validateUser(nombre);
            listenForMessages();
        } catch (IOException e) {
            System.out.println("Error en el hilo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateUser(String nombre) throws IOException {
        User userFromDB = UserRepository.getUserFromDB(nombre);

        if (userFromDB == null) {
            registerClient(nombre);
        } else {
            loginToServer(userFromDB);
        }
    }

    private void loginToServer(User user) throws IOException {
        salida.writeUTF("Ingrese la contraseña para el usuario " + user.getName());
        String password = entrada.readUTF();

        if (password.equals(user.getPassword())) {
            salida.writeUTF("Inicio de sesión exitoso. Bienvenido, " + user.getName() + "!");
            this.user = user;
        } else {
            salida.writeUTF("Contraseña incorrecta");
            loginToServer(user);
        }
    }

    private void registerClient(String nombre) throws IOException {
        salida.writeUTF("Por favor, registre una contraseña:");
        String password = entrada.readUTF();

        this.user = new User(0, nombre, password);

        UserRepository.registerUser(user);

        salida.writeUTF("Registro exitoso. Bienvenido, " + nombre + "!");
    }

    private void listenForMessages() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();
                for (UnCliente cliente : ServidorMulti.clientes.values()) {
                    cliente.salida.writeUTF("|" + user.getName() + "|:" + mensaje);
                }
            } catch (IOException ex) {
                System.out.println("Error al leer el mensaje: " + ex.getMessage());
                break; 
            }
        }
    }
}
