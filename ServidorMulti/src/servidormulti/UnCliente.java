package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
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
            return;
        }
        
        for (UnCliente clienteConectado : ServidorMulti.usuariosConectados) {
            if (clienteConectado.user.getName().equals(nombre)) {
                salida.writeUTF("El usuario " + nombre + " ya está conectado.");
                run();
            }
        }
        
        loginToServer(userFromDB);

    }

    private void loginToServer(User user) throws IOException {
        salida.writeUTF("Ingrese la contraseña para el usuario " + user.getName());
        String password = entrada.readUTF();

        if (password.equals(user.getPassword())) {
            salida.writeUTF("Inicio de sesión exitoso. Bienvenido, " + user.getName() + "!");
            this.user = user;
            synchronized (ServidorMulti.usuariosConectados) {
                ServidorMulti.usuariosConectados.add(this);
            }
        } else {
            salida.writeUTF("Contraseña incorrecta");
            run();
        }
    }

    private void registerClient(String nombre) throws IOException {
        salida.writeUTF("Por favor, registre una contraseña:");
        String password = entrada.readUTF();

        this.user = new User(0, nombre, password);

        UserRepository.registerUser(user);

        salida.writeUTF("Registro exitoso. Bienvenido, " + nombre + "!");
    }

    private void sendMessageToUser(User usuarioRecibido, String nombre, String mensaje) throws IOException {
        UnCliente clienteEncontrado = null;
        usuarioRecibido = UserRepository.getUserFromDB(nombre);
        if (usuarioRecibido == null) {
            salida.writeUTF("El usuario " + nombre + " no existe");
            return;
        }
        for (Map.Entry<Integer, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
            UnCliente cliente = entry.getValue();
            synchronized (cliente) {
                if (cliente.user.getName().equals(usuarioRecibido.getName())) {
                    clienteEncontrado = cliente;
                    System.out.println(clienteEncontrado.user.getName());
                    System.out.println("Cliente encontrado en la posición: " + entry.getKey());
                    break;
                }
            }
        }
        if (clienteEncontrado == null) {
            salida.writeUTF("El usuario " + usuarioRecibido.getName() + " no esta conectado");
            return;
        }
        clienteEncontrado.salida.writeUTF("Mensaje de |" + user.getName() + "|: " + mensaje);
        salida.writeUTF("Mensaje enviado exitosamente");
    }

    private void listenForMessages() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();
                if (mensaje.startsWith("/")) {
                    String[] partes = mensaje.split(" ");
                    String comando = partes[0];
                    switch (comando) {
                        case "/msj":
                            salida.writeUTF("Ingrese el mensaje a enviar");
                            String mensajeEnviar = entrada.readUTF();
                            User usuarioRecibido = null;
                            if (partes[1].equals("-v")) {
                                String usuariosRecibiendo = partes[2];
                                String[] usuariosRecibiendoArray = usuariosRecibiendo.split(",");
                                for (String usuarioRecibiendoNombre : usuariosRecibiendoArray) {
                                    User usuarioRecibiendo = UserRepository.getUserFromDB(usuarioRecibiendoNombre);
                                    for (Map.Entry<Integer, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                                        UnCliente cliente = entry.getValue();
                                        synchronized (cliente) {
                                            if (cliente.user.getName().equals(usuarioRecibiendo.getName())) {
                                                System.out.println("Cliente encontrado en la posición: " + entry.getKey());
                                                sendMessageToUser(usuarioRecibido, usuarioRecibiendo.getName(), mensajeEnviar);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            sendMessageToUser(usuarioRecibido, partes[1], mensajeEnviar);
                            break;
                        case "/ayuda":

                            break;
                        default:
                            this.salida.writeUTF("Comando no encontrado /ayuda para la lista de comandos");
                            break;
                    }
                } else {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF("|" + user.getName() + "|:" + mensaje);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error al leer el mensaje: " + ex.getMessage());
                break;
            }
        }
    }
}
