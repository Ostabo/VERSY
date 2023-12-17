package aqua.blatt7.endpoint;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecureEndpoint extends Endpoint {

    private final Endpoint endpoint;
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    public SecureEndpoint() {
        this(null);
    }

    public SecureEndpoint(Integer port) {
        try {
            endpoint = port != null ? new Endpoint(port) : new Endpoint();
            final String keySeed = "CAFEBABECAFEBABE";
            final String algorithm = "AES";
            encryptCipher = Cipher.getInstance(algorithm);
            decryptCipher = Cipher.getInstance(algorithm);
            final SecretKeySpec key = new SecretKeySpec(keySeed.getBytes(), algorithm);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(payload);
            os.close();
            byte[] encryptedBytes = encryptCipher.doFinal(baos.toByteArray());
            endpoint.send(receiver, Base64.getEncoder().encodeToString(encryptedBytes));
        } catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message blockingReceive() {
        return decrypt(endpoint.blockingReceive());
    }

    @Override
    public Message nonBlockingReceive() {
        Message message = endpoint.nonBlockingReceive();
        return message == null ? null : decrypt(message);
    }

    private Message decrypt(Message message) {
        try {
            byte[] decryptedPayload = decryptCipher.doFinal(
                    Base64.getDecoder().decode(
                            message.getPayload().toString().getBytes()
                    )
            );
            ByteArrayInputStream bais = new ByteArrayInputStream(decryptedPayload);
            ObjectInputStream is = new ObjectInputStream(bais);
            return new Message(
                    (Serializable) is.readObject(),
                    message.getSender()
            );
        } catch (IllegalBlockSizeException | BadPaddingException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
