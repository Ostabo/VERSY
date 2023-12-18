package aqua.blatt7.endpoint;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint extends Endpoint {

    private final Endpoint endpoint;

    // Keysize must be big enough for message but small enough for performance/socket buffer - breaks after 7 fish...
    private final int MAX_MESSAGE_SIZE = (int) (1024 * 4.75);
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    private final Map<InetSocketAddress, PublicKey> knownPublicKeys = new HashMap<>();
    private final Map<InetSocketAddress, Serializable> cachedPayloads = new HashMap<>();
    private final PublicKey publicKey;

    public SecureEndpoint() {
        this(null);
    }

    public SecureEndpoint(Integer port) {
        try {
            endpoint = port != null ? new Endpoint(port) : new Endpoint();

            final String algorithm = "RSA";
            var keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
            keyPairGenerator.initialize(MAX_MESSAGE_SIZE);
            var keyPair = keyPairGenerator.generateKeyPair();
            publicKey = keyPair.getPublic();
            final PrivateKey privateKey = keyPair.getPrivate();

            encryptCipher = Cipher.getInstance(algorithm);
            decryptCipher = Cipher.getInstance(algorithm);
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            if (!knownPublicKeys.containsKey(receiver)) {
                cachedPayloads.put(receiver, payload);
                endpoint.send(receiver, new KeyExchangeMessage(publicKey));
                return;
            }
            PublicKey recieverPublicKey = knownPublicKeys.get(receiver);
            encryptCipher.init(Cipher.ENCRYPT_MODE, recieverPublicKey);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_MESSAGE_SIZE);
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(payload);
            os.close();
            byte[] encryptedBytes = encryptCipher.doFinal(baos.toByteArray());
            endpoint.send(receiver, Base64.getEncoder().encodeToString(encryptedBytes));
        } catch (IllegalBlockSizeException | BadPaddingException | IOException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message blockingReceive() {
        Message message = endpoint.blockingReceive();
        if (handleIfKeyExchangeMessage(message))
            return blockingReceive();
        return decrypt(message);
    }

    @Override
    public Message nonBlockingReceive() {
        Message message = endpoint.nonBlockingReceive();
        if (message == null)
            return null;
        else if (handleIfKeyExchangeMessage(message))
            return null;
        return decrypt(message);
    }

    private boolean handleIfKeyExchangeMessage(Message message) {
        if (message.getPayload() instanceof KeyExchangeMessage keyExchangeMessage) {
            knownPublicKeys.put(message.getSender(), keyExchangeMessage.publicKey());
            Serializable cachedMessage = cachedPayloads.remove(message.getSender());
            InetSocketAddress sender = message.getSender();

            if (cachedMessage == null)
                endpoint.send(sender, new KeyExchangeMessage(publicKey));
            else
                send(sender, cachedMessage);
            return true;
        }
        return false;
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
